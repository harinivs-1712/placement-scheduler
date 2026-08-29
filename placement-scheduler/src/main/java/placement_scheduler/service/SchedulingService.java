package placement_scheduler.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import placement_scheduler.entity.Company;
import placement_scheduler.entity.CompanySlot;
import placement_scheduler.entity.Interview;
import placement_scheduler.entity.InterviewChange;
import placement_scheduler.entity.Panel;
import placement_scheduler.entity.Room;
import placement_scheduler.entity.Shortlist;
import placement_scheduler.entity.UnscheduledReason;
import placement_scheduler.repository.CompanyRepository;
import placement_scheduler.repository.CompanySlotRepository;
import placement_scheduler.repository.InterviewChangeRepository;
import placement_scheduler.repository.InterviewRepository;
import placement_scheduler.repository.PanelRepository;
import placement_scheduler.repository.RoomRepository;
import placement_scheduler.repository.ShortlistRepository;
import placement_scheduler.repository.UnscheduledReasonRepository;
import placement_scheduler.repository.DisruptionEventRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SchedulingService {

    private final InterviewRepository interviewRepository;
    private final CompanySlotRepository companySlotRepository;
    private final PanelRepository panelRepository;
    private final RoomRepository roomRepository;
    private final CompanyRepository companyRepository;
    private final ShortlistRepository shortlistRepository;
    private final UnscheduledReasonRepository unscheduledReasonRepository;
    private final InterviewChangeRepository interviewChangeRepository;
    private final DisruptionEventRepository disruptionEventRepository;

    public SchedulingService(
            InterviewRepository interviewRepository,
            CompanySlotRepository companySlotRepository,
            PanelRepository panelRepository,
            RoomRepository roomRepository,
            CompanyRepository companyRepository,
            ShortlistRepository shortlistRepository,
            UnscheduledReasonRepository unscheduledReasonRepository,
            InterviewChangeRepository interviewChangeRepository,
            DisruptionEventRepository disruptionEventRepository) {

        this.interviewRepository = interviewRepository;
        this.companySlotRepository = companySlotRepository;
        this.panelRepository = panelRepository;
        this.roomRepository = roomRepository;
        this.companyRepository = companyRepository;
        this.shortlistRepository = shortlistRepository;
        this.unscheduledReasonRepository = unscheduledReasonRepository;
        this.interviewChangeRepository = interviewChangeRepository;
        this.disruptionEventRepository = disruptionEventRepository;
    }

    /**
     * Runs a full, fresh scheduling pass.
     *
     * IMPORTANT: every call resets ALL interviews (regardless of current
     * status) back to UNSCHEDULED with null panel/room/day/start/end, then
     * schedules everything from scratch. A separate, minimal-disturbance
     * REPLAN method belongs on top of this later for handling live
     * disruptions — this method is intentionally "start over," not
     * "resume" or "patch."
     */
    @Transactional
    public void generateSchedule() {

        System.out.println("========================================");
        System.out.println("Starting interview scheduling...");
        System.out.println("========================================");

        resetAllInterviews();

        List<Interview> interviews = interviewRepository.findByStatus("UNSCHEDULED");

        System.out.println("Interviews to schedule: " + interviews.size());

        List<Company> companies = companyRepository.findAll();

        Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(Company::getCompanyId, company -> company));

        System.out.println("Companies loaded: " + companies.size());

        // ------------------------------------------------------------
        // Priority ordering: higher-priority companies first, and within
        // a company, higher-ranked (more preferred) students first.
        // ------------------------------------------------------------

        Map<String, Integer> shortlistRankByCompanyAndStudent = buildShortlistRankIndex();

        interviews.sort(
                Comparator
                        .comparing((Interview i) -> companyMap.get(i.getCompany().getCompanyId()).getPriorityTier())
                        .thenComparing(i -> shortlistRankByCompanyAndStudent.getOrDefault(
                                rankKey(i.getCompany().getCompanyId(), i.getStudent().getStudentId()),
                                Integer.MAX_VALUE)));

        // ------------------------------------------------------------
        // Prefetch everything once — reused for BOTH the scheduling
        // attempt and the reason-classification fallback, so classifying
        // *why* an interview failed never re-queries the DB per
        // interview. (This is the N+1 pattern that had crept back into
        // determineUnscheduledReason — it now takes these same
        // pre-fetched lists as parameters instead of calling
        // findByCompanyCompanyId / findAll() itself.)
        // ------------------------------------------------------------

        Map<Long, List<CompanySlot>> companySlotsByCompany = companySlotRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(cs -> cs.getCompany().getCompanyId()));

        Map<Long, List<Panel>> panelsByCompany = panelRepository.findAll()
                .stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .collect(Collectors.groupingBy(p -> p.getCompany().getCompanyId()));

        List<Room> allRooms = roomRepository.findAll();

        ScheduleIndex scheduleIndex = new ScheduleIndex();

        // Load and record day-specific room disruptions
        List<placement_scheduler.entity.DisruptionEvent> roomDisruptions = disruptionEventRepository
                .findByEventType("ROOM_UNAVAILABLE");
        for (placement_scheduler.entity.DisruptionEvent event : roomDisruptions) {
            String details = event.getDetails();
            if (details != null && details.startsWith("DAY: ")) {
                try {
                    String[] parts = details.split("\\|");
                    String dayStr = parts[0].replace("DAY: ", "").trim();
                    String timeStr = parts[1].replace("TIME: ", "").trim();
                    int blockDay = Integer.parseInt(dayStr);
                    LocalTime blockTime = LocalTime.parse(timeStr);

                    Interview blockDummy = new Interview();
                    blockDummy.setDay(blockDay);
                    blockDummy.setStartTime(blockTime);
                    blockDummy.setEndTime(LocalTime.of(17, 0));

                    Room r = new Room();
                    r.setRoomId(event.getTargetId());
                    blockDummy.setRoom(r);

                    scheduleIndex.record(blockDummy);
                } catch (Exception e) {
                    System.err.println("Failed to parse room unavailability event details: " + e.getMessage());
                }
            }
        }

        List<UnscheduledReason> reasonsToSave = new ArrayList<>();

        int scheduledCount = 0;
        int unscheduledCount = 0;

        for (Interview interview : interviews) {

            Company company = companyMap.get(interview.getCompany().getCompanyId());

            if (company == null) {
                interview.setStatus("UNSCHEDULED");
                interview.setUpdatedAt(LocalDateTime.now());
                reasonsToSave.add(buildReason(
                        interview,
                        "COMPANY_NOT_FOUND",
                        null));
                unscheduledCount++;
                continue;
            }

            List<CompanySlot> companySlots = companySlotsByCompany.getOrDefault(company.getCompanyId(), List.of());

            List<Panel> panels = panelsByCompany.getOrDefault(company.getCompanyId(), List.of());

            boolean scheduled = tryScheduleInterview(
                    interview,
                    company,
                    companySlots,
                    panels,
                    allRooms,
                    scheduleIndex,
                    null,
                    null);

            if (scheduled) {
                scheduleIndex.record(interview);
                scheduledCount++;
            } else {
                String reason = determineUnscheduledReason(
                        interview,
                        company,
                        companySlots,
                        panels,
                        allRooms,
                        scheduleIndex,
                        null,
                        null);
                reasonsToSave.add(buildReason(
        interview,
        reason,
        null));
                unscheduledCount++;
            }
        }

        interviewRepository.saveAll(interviews);
        unscheduledReasonRepository.saveAll(reasonsToSave);

        System.out.println("Scheduled interviews: " + scheduledCount);
        System.out.println("Unscheduled interviews: " + unscheduledCount);
        System.out.println("========================================");
        System.out.println("Scheduling completed.");
        System.out.println("========================================");
    }

    /**
     * Resets every interview row to a clean UNSCHEDULED state, regardless
     * of its current status, and clears out ALL previously logged
     * unscheduled reasons. Without clearing the old reasons, a second
     * generateSchedule() run would accumulate stale explanations
     * alongside fresh ones for the same interview — misleading if you're
     * inspecting UNSCHEDULED_REASON after a re-run with different input
     * data.
     */
    private void resetAllInterviews() {

        List<Interview> allInterviews = interviewRepository.findAll();

        for (Interview interview : allInterviews) {
            interview.setStatus("UNSCHEDULED");
            interview.setUpdatedAt(LocalDateTime.now());
        }

        interviewRepository.saveAll(allInterviews);

        unscheduledReasonRepository.deleteAll();

        System.out.println(allInterviews.size() + " interviews reset to UNSCHEDULED before scheduling.");
        System.out.println("Previous unscheduled reasons cleared.");
    }

    /**
     * Replans a BATCH of already-invalidated interviews (e.g. everything a
     * panel drop just knocked back to UNSCHEDULED) against the CURRENT
     * state of everything else in the system — unlike generateSchedule(),
     * this does NOT reset anything else first. Every interview not in
     * this batch keeps its existing SCHEDULED assignment untouched, which
     * is the whole point of minimal-disturbance replanning: a disruption
     * should only ever move the interviews it actually broke.
     *
     * Replaces calling SchedulingService per-interview in a loop (the
     * previous approach), which rebuilt the entire prefetch — company
     * slots, panels, rooms, and a full ScheduleIndex over every SCHEDULED
     * interview in the database — separately for EACH affected interview.
     * Here all of that is built exactly once for the whole batch.
     *
     * @param affectedInterviews interviews to attempt to re-place. Must
     *                           already be reset to UNSCHEDULED with null
     *                           panel/room/day/time by the caller (the
     *                           disruption handler) before this is called.
     * @param replanId           the DisruptionEvent id that caused this
     *                           replan, used to tag the failure reason if
     *                           an interview still can't be placed. Pass
     *                           null if this replan wasn't triggered by a
     *                           tracked disruption event.
     */
    public ReplanResult replanInterviews(
            List<Interview> affectedInterviews,
            Long replanId,
            String disruptionEventType,
            Integer notBeforeDay,
            LocalTime notBeforeTime) {

        if (affectedInterviews.isEmpty()) {
            return new ReplanResult(0, 0);
        }

        System.out.println("========================================");
        System.out.println("Replanning " + affectedInterviews.size() + " affected interview(s)...");
        System.out.println("========================================");

        List<Company> companies = companyRepository.findAll();

        Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(Company::getCompanyId, company -> company));

        // Priority ordering — same rule as the initial scheduling pass:
        // higher company tier first, then higher shortlist rank within a
        // company. Without this, if a disruption affects several
        // interviews at once, a lower-ranked student could grab the one
        // available fallback slot before a higher-ranked one gets a turn.
        Map<String, Integer> shortlistRankByCompanyAndStudent = buildShortlistRankIndex();

        affectedInterviews.sort(
                Comparator
                        .comparing((Interview i) -> companyMap.get(i.getCompany().getCompanyId()).getPriorityTier())
                        .thenComparing(i -> shortlistRankByCompanyAndStudent.getOrDefault(
                                rankKey(i.getCompany().getCompanyId(), i.getStudent().getStudentId()),
                                Integer.MAX_VALUE)));

        // Prefetch ONCE for the whole batch. Panels filtered to ACTIVE and
        // rooms filtered to AVAILABLE — a dropped panel or room must never
        // be offered as a candidate again, including to the very
        // interviews its own drop just invalidated.
        Map<Long, List<CompanySlot>> companySlotsByCompany = companySlotRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(cs -> cs.getCompany().getCompanyId()));

        Map<Long, List<Panel>> panelsByCompany = panelRepository.findAll()
                .stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .collect(Collectors.groupingBy(p -> p.getCompany().getCompanyId()));

        List<Room> allRooms = roomRepository.findAll()
                .stream()
                .filter(r -> "AVAILABLE".equals(r.getStatus()))
                .collect(Collectors.toList());

        // ScheduleIndex is seeded with every CURRENTLY SCHEDULED interview
        // in the whole system (not just this company) — every one of
        // those is a real, standing conflict this replan must respect,
        // exactly like the initial scheduling pass. Interviews in the
        // affected batch are excluded, since they're mid-replan and don't
        // hold their old slot anymore.
        ScheduleIndex scheduleIndex = new ScheduleIndex();

        // Load and record day-specific room disruptions
        List<placement_scheduler.entity.DisruptionEvent> roomDisruptions = disruptionEventRepository
                .findByEventType("ROOM_UNAVAILABLE");
        for (placement_scheduler.entity.DisruptionEvent event : roomDisruptions) {
            String details = event.getDetails();
            if (details != null && details.startsWith("DAY: ")) {
                try {
                    String[] parts = details.split("\\|");
                    String dayStr = parts[0].replace("DAY: ", "").trim();
                    String timeStr = parts[1].replace("TIME: ", "").trim();
                    int blockDay = Integer.parseInt(dayStr);
                    LocalTime blockTime = LocalTime.parse(timeStr);

                    Interview blockDummy = new Interview();
                    blockDummy.setDay(blockDay);
                    blockDummy.setStartTime(blockTime);
                    blockDummy.setEndTime(LocalTime.of(17, 0));

                    Room r = new Room();
                    r.setRoomId(event.getTargetId());
                    blockDummy.setRoom(r);

                    scheduleIndex.record(blockDummy);
                } catch (Exception e) {
                    System.err.println("Failed to parse room unavailability event details: " + e.getMessage());
                }
            }
        }

        Set<Long> affectedIds = affectedInterviews.stream()
                .map(Interview::getInterviewId)
                .collect(Collectors.toSet());

        for (Interview existing : interviewRepository.findByStatus("SCHEDULED")) {
            if (!affectedIds.contains(existing.getInterviewId())) {
                scheduleIndex.record(existing);
            }
        }

        List<UnscheduledReason> reasonsToSave = new ArrayList<>();
        List<InterviewChange> changesToSave = new ArrayList<>();

        int replannedCount = 0;
        int failedCount = 0;

        for (Interview interview : affectedInterviews) {

            Company company = companyMap.get(interview.getCompany().getCompanyId());

            if (company == null) {
                interview.setStatus("UNSCHEDULED");
                interview.setUpdatedAt(LocalDateTime.now());
                reasonsToSave.add(buildReason(
                        interview,
                        "COMPANY_NOT_FOUND",
                        replanId));
                failedCount++;
                continue;
            }

            Integer oldDay = interview.getDay();
            LocalTime oldStartTime = interview.getStartTime();
            LocalTime oldEndTime = interview.getEndTime();

            Long oldPanelId = interview.getPanel() != null
                    ? interview.getPanel().getPanelId()
                    : null;

            Long oldRoomId = interview.getRoom() != null
                    ? interview.getRoom().getRoomId()
                    : null;

            interview.setPanel(null);
            interview.setRoom(null);
            interview.setDay(null);
            interview.setStartTime(null);
            interview.setEndTime(null);
            interview.setStatus("UNSCHEDULED");
            interview.setUpdatedAt(LocalDateTime.now());

            List<CompanySlot> companySlots = companySlotsByCompany.getOrDefault(company.getCompanyId(), List.of());

            List<Panel> panels = panelsByCompany.getOrDefault(company.getCompanyId(), List.of());

            boolean scheduled = tryScheduleInterview(
                    interview,
                    company,
                    companySlots,
                    panels,
                    allRooms,
                    scheduleIndex,
                    notBeforeDay,
                    notBeforeTime);

            if (scheduled) {

                scheduleIndex.record(interview);

                // Record the OLD -> NEW assignment
                InterviewChange change = new InterviewChange();

                change.setInterview(interview);

                // OLD assignment
                change.setOldDay(oldDay);
                change.setOldStartTime(oldStartTime);
                change.setOldEndTime(oldEndTime);
                change.setOldPanelId(oldPanelId);
                change.setOldRoomId(oldRoomId);

                // NEW assignment
                change.setNewDay(interview.getDay());
                change.setNewStartTime(interview.getStartTime());
                change.setNewEndTime(interview.getEndTime());

                change.setNewPanelId(
                        interview.getPanel() != null
                                ? interview.getPanel().getPanelId()
                                : null);

                change.setNewRoomId(
                        interview.getRoom() != null
                                ? interview.getRoom().getRoomId()
                                : null);

                // Uses the ACTUAL disruption type passed in by the caller,
                // not a hardcoded value — this method is shared across
                // panel drop, room unavailability, and (later) student
                // withdrawal / company delay, so a hardcoded label here
                // would mislabel every replan that wasn't a panel drop.
                change.setChangeType(
                        disruptionEventType != null
                                ? disruptionEventType
                                : "REPLAN");

                change.setReplanId(replanId);

                change.setChangedAt(LocalDateTime.now());

                changesToSave.add(change);

                replannedCount++;

                System.out.println(
                        "Interview "
                                + interview.getInterviewId()
                                + " successfully replanned.");

            } else {
                String reason = determineUnscheduledReason(
                        interview, company, companySlots, panels, allRooms, scheduleIndex, notBeforeDay, notBeforeTime);

                String taggedReason = replanId != null
                        ? reason + " (after replan for event #" + replanId + ")"
                        : reason + " (after replan)";

                reasonsToSave.add(buildReason(
                        interview,
                        taggedReason,
                        replanId));
                failedCount++;
                System.out.println("Interview " + interview.getInterviewId() + " could NOT be replanned: " + reason);
            }
        }

        interviewRepository.saveAll(affectedInterviews);
        unscheduledReasonRepository.saveAll(reasonsToSave);
        interviewChangeRepository.saveAll(changesToSave);

        System.out.println("========================================");
        System.out.println("Replan completed. Replanned: " + replannedCount + " | Still unscheduled: " + failedCount);
        System.out.println("========================================");

        return new ReplanResult(replannedCount, failedCount);
    }

    private Map<String, Integer> buildShortlistRankIndex() {

        List<Shortlist> shortlists = shortlistRepository.findAll();

        Map<String, Integer> rankIndex = new HashMap<>();

        for (Shortlist shortlist : shortlists) {
            rankIndex.put(
                    rankKey(
                            shortlist.getCompany().getCompanyId(),
                            shortlist.getStudent().getStudentId()),
                    shortlist.getRank());
        }

        return rankIndex;
    }

    private String rankKey(Long companyId, Long studentId) {
        return companyId + "_" + studentId;
    }

    private UnscheduledReason buildReason(
            Interview interview,
            String reason,
            Long replanId) {

        UnscheduledReason unscheduledReason = new UnscheduledReason();
        unscheduledReason.setInterview(interview);
        unscheduledReason.setReplanId(replanId);
        unscheduledReason.setReason(reason);
        unscheduledReason.setLoggedAt(LocalDateTime.now());

        return unscheduledReason;
    }

    private boolean tryScheduleInterview(
            Interview interview,
            Company company,
            List<CompanySlot> companySlots,
            List<Panel> panels,
            List<Room> rooms,
            ScheduleIndex scheduleIndex,
            Integer notBeforeDay,
            LocalTime notBeforeTime) {

        Long studentId = interview.getStudent().getStudentId();
        int duration = company.getInterviewDurationMin();

        for (CompanySlot companySlot : companySlots) {

            List<TimeSlot> possibleSlots = generateTimeSlots(
                    companySlot.getDay(),
                    companySlot.getStartTime(),
                    companySlot.getEndTime(),
                    duration);

            for (TimeSlot timeSlot : possibleSlots) {

                if (isBeforeCutoff(timeSlot, notBeforeDay, notBeforeTime)) {
                    // Would place this interview on a day/time that's
                    // already in the past relative to the disruption that
                    // triggered this replan — e.g. a panel that broke on
                    // Day 2 must never have its interviews "rescheduled"
                    // back onto Day 1. Never a candidate, regardless of
                    // whether it's otherwise free.
                    continue;
                }

                if (!scheduleIndex.isStudentFree(
                        studentId, timeSlot.getDay(), timeSlot.getStartTime(), timeSlot.getEndTime())) {
                    continue;
                }

                for (Panel panel : panels) {

                    if (!scheduleIndex.isPanelFree(
                            panel.getPanelId(), timeSlot.getDay(), timeSlot.getStartTime(), timeSlot.getEndTime())) {
                        continue;
                    }

                    for (Room room : rooms) {

                        if (!scheduleIndex.isRoomFree(
                                room.getRoomId(), timeSlot.getDay(), timeSlot.getStartTime(), timeSlot.getEndTime())) {
                            continue;
                        }

                        interview.setDay(timeSlot.getDay());
                        interview.setStartTime(timeSlot.getStartTime());
                        interview.setEndTime(timeSlot.getEndTime());
                        interview.setPanel(panel);
                        interview.setRoom(room);
                        interview.setStatus("SCHEDULED");
                        interview.setUpdatedAt(LocalDateTime.now());

                        return true;
                    }
                }
            }
        }

        interview.setStatus("UNSCHEDULED");
        interview.setUpdatedAt(LocalDateTime.now());

        return false;
    }

    /**
     * Classifies WHY an interview couldn't be scheduled, by re-walking the
     * same search tryScheduleInterview just ran and finding the furthest
     * stage it ever reached, in the same order the real search checks
     * things: student availability first, then panel, then room.
     *
     * This is classification by ELIMINATION, not independent flags — the
     * previous version tracked panel/room availability as flags that were
     * only ever set inside the "student was free" branch, so if the
     * student was busy at every candidate slot, panelAvailable/
     * roomAvailable silently stayed false and the method wrongly reported
     * PANEL_UNAVAILABLE for what was actually a student-scheduling
     * conflict. Elimination avoids that: each flag can only become true
     * at a strictly later stage than the one before it, so the first
     * stage that never succeeds IS the real bottleneck.
     *
     * Reuses the already-prefetched companySlots/panels/rooms passed in
     * from generateSchedule() rather than re-querying the DB per
     * interview.
     */
    private String determineUnscheduledReason(
            Interview interview,
            Company company,
            List<CompanySlot> companySlots,
            List<Panel> panels,
            List<Room> rooms,
            ScheduleIndex scheduleIndex,
            Integer notBeforeDay,
            LocalTime notBeforeTime) {

        if (companySlots.isEmpty()) {
            return "NO_COMPANY_SLOT";
        }

        Long studentId = interview.getStudent().getStudentId();
        int duration = company.getInterviewDurationMin();

        boolean anyCandidateSlotGenerated = false;
        boolean studentFreeAtSomeSlot = false;
        boolean panelFreeAtSomeStudentFreeSlot = false;

        for (CompanySlot companySlot : companySlots) {

            List<TimeSlot> possibleSlots = generateTimeSlots(
                    companySlot.getDay(),
                    companySlot.getStartTime(),
                    companySlot.getEndTime(),
                    duration);

            for (TimeSlot timeSlot : possibleSlots) {

                if (isBeforeCutoff(timeSlot, notBeforeDay, notBeforeTime)) {
                    continue;
                }

                anyCandidateSlotGenerated = true;

                boolean studentFreeHere = scheduleIndex.isStudentFree(
                        studentId, timeSlot.getDay(), timeSlot.getStartTime(), timeSlot.getEndTime());

                if (!studentFreeHere) {
                    // Student is busy at this slot — this slot tells us
                    // nothing about panel/room availability, since the
                    // real search would never have checked them here
                    // either. Move on without touching later-stage flags.
                    continue;
                }

                studentFreeAtSomeSlot = true;

                for (Panel panel : panels) {

                    boolean panelFreeHere = scheduleIndex.isPanelFree(
                            panel.getPanelId(), timeSlot.getDay(), timeSlot.getStartTime(), timeSlot.getEndTime());

                    if (panelFreeHere) {
                        panelFreeAtSomeStudentFreeSlot = true;
                        // Note: we deliberately do NOT also check rooms
                        // here to look for a "room free" flag — if a room
                        // were also free at this exact (day, time), the
                        // real tryScheduleInterview would already have
                        // succeeded, and this method would never have
                        // been called. So by the time we're here, on
                        // failure, "room never simultaneously free" is
                        // already guaranteed — it doesn't need its own
                        // flag, it's the remaining explanation by
                        // elimination once student and panel availability
                        // are both ruled out as the bottleneck.
                        break;
                    }
                }
            }
        }

        // ------------------------------------------------------------
        // Classify by the FURTHEST stage reached, in true search order.
        // ------------------------------------------------------------

        if (!anyCandidateSlotGenerated) {
            // The company's own working-hour window is too short to fit
            // even a single interview of its configured duration — a
            // data/configuration issue, not a contention issue.
            return "NO_VALID_TIME_SLOTS";
        }

        if (!studentFreeAtSomeSlot) {
            // The student's OTHER interviews already consumed every
            // candidate slot inside this company's own availability
            // window — the bottleneck is the student's schedule, not
            // this company's resources.
            return "STUDENT_UNAVAILABLE";
        }

        if (!panelFreeAtSomeStudentFreeSlot) {
            // Whenever the student was free, every one of this company's
            // OWN panels was already booked (or the company has zero
            // panels). This is company-specific capacity pressure.
            return "PANEL_UNAVAILABLE";
        }

        // Student was free and a panel was free at the same slot at least
        // once, yet tryScheduleInterview still failed — by elimination,
        // no room was ever simultaneously free at any such slot. This is
        // the shared-resource contention case (rooms are pooled across
        // every company, unlike panels).
        return "ROOM_UNAVAILABLE";
    }

    private List<TimeSlot> generateTimeSlots(
            int day,
            LocalTime startTime,
            LocalTime endTime,
            int interviewDurationMinutes) {

        List<TimeSlot> slots = new ArrayList<>();

        LocalTime current = startTime;

        while (!current.plusMinutes(interviewDurationMinutes).isAfter(endTime)) {

            LocalTime slotEnd = current.plusMinutes(interviewDurationMinutes);

            slots.add(new TimeSlot(day, current, slotEnd));

            current = slotEnd;
        }

        return slots;
    }

    /**
     * True if a candidate slot falls before the given (day, time) cutoff
     * — meaning it's already in the past relative to whatever disruption
     * triggered this replan, and must never be offered as a candidate.
     *
     * notBeforeDay == null means no cutoff at all (used by
     * generateSchedule()'s fresh full run, which has no notion of "past").
     *
     * A slot on a day AFTER notBeforeDay is always fine. A slot on
     * notBeforeDay itself is only fine if its start time is at or after
     * notBeforeTime. A slot on any day BEFORE notBeforeDay is always
     * rejected, regardless of time.
     */
    private boolean isBeforeCutoff(TimeSlot timeSlot, Integer notBeforeDay, LocalTime notBeforeTime) {

        if (notBeforeDay == null) {
            return false;
        }

        if (timeSlot.getDay() < notBeforeDay) {
            return true;
        }

        if (timeSlot.getDay() > notBeforeDay) {
            return false;
        }

        // Same day as the cutoff — compare times. notBeforeTime == null
        // on the cutoff day means the whole day counts as available.
        if (notBeforeTime == null) {
            return false;
        }

        return timeSlot.getStartTime().isBefore(notBeforeTime);
    }

    private static class ScheduleIndex {

        private final Map<Long, List<Interview>> byStudent = new HashMap<>();
        private final Map<Long, List<Interview>> byPanel = new HashMap<>();
        private final Map<Long, List<Interview>> byRoom = new HashMap<>();

        boolean isStudentFree(Long studentId, int day, LocalTime start, LocalTime end) {
            return isFree(byStudent.get(studentId), day, start, end);
        }

        boolean isPanelFree(Long panelId, int day, LocalTime start, LocalTime end) {
            return isFree(byPanel.get(panelId), day, start, end);
        }

        boolean isRoomFree(Long roomId, int day, LocalTime start, LocalTime end) {
            return isFree(byRoom.get(roomId), day, start, end);
        }

        private boolean isFree(List<Interview> existing, int day, LocalTime start, LocalTime end) {

            if (existing == null) {
                return true;
            }

            for (Interview interview : existing) {

                if (interview.getDay() == null || interview.getDay() != day) {
                    continue;
                }

                if (interview.getStartTime() == null || interview.getEndTime() == null) {
                    continue;
                }

                if (overlaps(interview.getStartTime(), interview.getEndTime(), start, end)) {
                    return false;
                }
            }

            return true;
        }

        private boolean overlaps(
                LocalTime existingStart,
                LocalTime existingEnd,
                LocalTime candidateStart,
                LocalTime candidateEnd) {

            return existingStart.isBefore(candidateEnd) && existingEnd.isAfter(candidateStart);
        }

        void record(Interview interview) {
            if (interview.getStudent() != null) {
                byStudent
                        .computeIfAbsent(interview.getStudent().getStudentId(), k -> new ArrayList<>())
                        .add(interview);
            }

            if (interview.getPanel() != null) {
                byPanel
                        .computeIfAbsent(interview.getPanel().getPanelId(), k -> new ArrayList<>())
                        .add(interview);
            }

            if (interview.getRoom() != null) {
                byRoom
                        .computeIfAbsent(interview.getRoom().getRoomId(), k -> new ArrayList<>())
                        .add(interview);
            }
        }
    }
}