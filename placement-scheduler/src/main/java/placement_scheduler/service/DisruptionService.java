package placement_scheduler.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import placement_scheduler.controller.DisruptionResponse;
import placement_scheduler.controller.PanelDropRequest;
import placement_scheduler.controller.RoomUnavailableRequest;
import placement_scheduler.controller.StudentWithdrawalRequest;
import placement_scheduler.entity.DisruptionEvent;
import placement_scheduler.entity.Interview;
import placement_scheduler.entity.Panel;
import placement_scheduler.entity.ReplanRun;
import placement_scheduler.entity.Room;
import placement_scheduler.entity.Student;
import placement_scheduler.entity.UnscheduledReason;
import placement_scheduler.repository.DisruptionEventRepository;
import placement_scheduler.repository.InterviewRepository;
import placement_scheduler.repository.PanelRepository;
import placement_scheduler.repository.RoomRepository;
import placement_scheduler.repository.StudentRepository;
import placement_scheduler.repository.UnscheduledReasonRepository;
import placement_scheduler.repository.ReplanRunRepository;
import placement_scheduler.controller.StudentWithdrawalRequest;
import placement_scheduler.entity.Student;
import placement_scheduler.repository.StudentRepository;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DisruptionService {

        private final DisruptionEventRepository disruptionEventRepository;
        private final PanelRepository panelRepository;
        private final RoomRepository roomRepository;
        private final InterviewRepository interviewRepository;
        private final UnscheduledReasonRepository unscheduledReasonRepository;
        private final SchedulingService schedulingService;
        private final ReplanRunRepository replanRunRepository;
        private final StudentRepository studentRepository;

        public DisruptionService(
                        DisruptionEventRepository disruptionEventRepository,
                        PanelRepository panelRepository,
                        RoomRepository roomRepository,
                        InterviewRepository interviewRepository,
                        UnscheduledReasonRepository unscheduledReasonRepository,
                        ReplanRunRepository replanRunRepository,
                        SchedulingService schedulingService,
                        StudentRepository studentRepository) {

                this.disruptionEventRepository = disruptionEventRepository;
                this.panelRepository = panelRepository;
                this.roomRepository = roomRepository;
                this.interviewRepository = interviewRepository;
                this.unscheduledReasonRepository = unscheduledReasonRepository;
                this.replanRunRepository = replanRunRepository;
                this.schedulingService = schedulingService;
                this.studentRepository = studentRepository;
        }

        @Transactional
        public DisruptionResponse handlePanelDrop(PanelDropRequest request) {

                // ============================================================
                // 1. Validate panel
                // ============================================================

                Panel panel = panelRepository
                                .findById(request.getPanelId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No panel found with id "
                                                                + request.getPanelId()));

                if ("INACTIVE".equals(panel.getStatus())) {

                        throw new IllegalStateException(
                                        "Panel "
                                                        + request.getPanelId()
                                                        + " is already INACTIVE.");
                }

                // ============================================================
                // 2. Parse effective time
                // ============================================================

                LocalTime effectiveTime;

                try {

                        effectiveTime = LocalTime.parse(
                                        request.getEffectiveTime());

                } catch (Exception e) {

                        throw new IllegalArgumentException(
                                        "Invalid effectiveTime. Use HH:mm format, e.g. 13:00.");
                }

                // ============================================================
                // 3. Validate day
                // ============================================================

                if (request.getDay() == null
                                || request.getDay() <= 0) {

                        throw new IllegalArgumentException(
                                        "Day must be greater than 0.");
                }

                // ============================================================
                // 4. Deactivate panel
                //
                // This prevents the replanning engine from selecting this
                // panel again.
                // ============================================================

                panel.setStatus("INACTIVE");

                panelRepository.save(panel);

                // ============================================================
                // 5. Create disruption event
                // ============================================================

                DisruptionEvent event = new DisruptionEvent();

                event.setEventType(
                                "PANEL_DROP");

                event.setTargetType(
                                "PANEL");

                event.setTargetId(
                                panel.getPanelId());

                event.setDetails(
                                request.getDetails());

                event.setOccurredAt(
                                LocalDateTime.now());

                event = disruptionEventRepository.save(event);

                // ============================================================
                // 6. Find affected interviews
                //
                // Two cases:
                //
                // A) Interview is already running when panel drops
                //
                // Example:
                // 12:45 - 13:15
                // Panel drops at 13:00
                //
                // B) Interview starts at or after the drop
                //
                // Example:
                // 13:00 - 13:15
                // 13:15 - 13:30
                // ============================================================

                List<Interview> affectedInterviews = new ArrayList<>();

                // A. Interview crossing the disruption time
                affectedInterviews.addAll(
                                interviewRepository
                                                .findInterviewsOverlappingPanelDrop(
                                                                panel.getPanelId(),
                                                                request.getDay(),
                                                                "SCHEDULED",
                                                                effectiveTime));

                // B. Interviews starting at or after disruption time
                affectedInterviews.addAll(
                                interviewRepository
                                                .findInterviewsStartingAfterPanelDrop(
                                                                panel.getPanelId(),
                                                                request.getDay(),
                                                                "SCHEDULED",
                                                                effectiveTime));

                ReplanRun replanRun = new ReplanRun();

                replanRun.setEvent(event);
                replanRun.setStartedAt(LocalDateTime.now());
                replanRun.setStatus("RUNNING");
                replanRun.setInterviewsAffected(affectedInterviews.size());
                replanRun.setInterviewsMoved(0);
                replanRun.setInterviewsCancelled(0);
                replanRun = replanRunRepository.save(replanRun);

                // ============================================================
                // 7. Remove duplicate interviews
                //
                // We use interviewId as the unique key.
                // ============================================================

                Map<Long, Interview> uniqueInterviews = new LinkedHashMap<>();

                for (Interview interview : affectedInterviews) {

                        uniqueInterviews.put(
                                        interview.getInterviewId(),
                                        interview);
                }

                affectedInterviews = new ArrayList<>(
                                uniqueInterviews.values());

                List<Long> affectedInterviewIds = affectedInterviews.stream()
                                .map(Interview::getInterviewId)
                                .toList();

                List<Long> affectedStudentIds = affectedInterviews.stream()
                                .filter(i -> i.getStudent() != null)
                                .map(i -> i.getStudent().getStudentId())
                                .distinct()
                                .toList();

                List<UnscheduledReason> disruptionReasons = new ArrayList<>();

                for (Interview interview : affectedInterviews) {
                        UnscheduledReason reason = new UnscheduledReason();
                        reason.setInterview(interview);
                        reason.setReason("PANEL_DROP_DISRUPTION (event #" + event.getEventId() + ")");
                        reason.setLoggedAt(LocalDateTime.now());
                        disruptionReasons.add(reason);
                }

                unscheduledReasonRepository.saveAll(disruptionReasons);

                // ============================================================
                // 8. Display disruption information
                // ============================================================

                System.out.println(
                                "========================================");

                System.out.println(
                                "PANEL DROP DISRUPTION");

                System.out.println(
                                "Panel ID: "
                                                + panel.getPanelId());

                System.out.println(
                                "Day: "
                                                + request.getDay());

                System.out.println(
                                "Effective time: "
                                                + effectiveTime);

                System.out.println(
                                "Disruption event ID: "
                                                + event.getEventId());

                System.out.println(
                                "Affected interviews: "
                                                + affectedInterviews.size());

                System.out.println(
                                "========================================");

                // ============================================================
                // 9. Release old assignments
                //
                // We do NOT permanently delete the interviews.
                // They remain in the database and will be replanned.
                // ============================================================

                // ============================================================
                // 10. Replan the entire batch
                //
                // SchedulingService will:
                //
                // - consider only ACTIVE panels and AVAILABLE rooms
                // - check company slots
                // - check student conflicts
                // - check panel conflicts
                // - check room conflicts
                // - schedule what is possible
                // - leave impossible interviews UNSCHEDULED
                // - record failure reasons
                // - log an InterviewChange row for every successful replan,
                // tagged with THIS event's type (PANEL_DROP), not a
                // hardcoded label — matters now that replanInterviews() is
                // shared with handleRoomUnavailable() below.
                // ============================================================

                System.out.println(
                                "Starting batch replanning...");

                ReplanResult result = schedulingService.replanInterviews(
                                affectedInterviews,
                                replanRun.getReplanId(),
                                event.getEventType(),
                                request.getDay(),
                                effectiveTime);

                replanRun.setCompletedAt(LocalDateTime.now());
                replanRun.setStatus("COMPLETED");
                replanRun.setInterviewsMoved(result.getReplannedCount());
                replanRun.setInterviewsCancelled(result.getFailedCount());

                replanRunRepository.save(replanRun);

                // ============================================================
                // 11. Final result
                // ============================================================

                System.out.println(
                                "========================================");

                System.out.println(
                                "PANEL DROP PROCESSING COMPLETED");

                System.out.println(
                                "Panel ID: "
                                                + panel.getPanelId());

                System.out.println(
                                "Affected interviews: "
                                                + affectedInterviews.size());

                System.out.println(
                                "Successfully replanned: "
                                                + result.getReplannedCount());

                System.out.println(
                                "Failed to replan: "
                                                + result.getFailedCount());

                System.out.println(
                                "========================================");

                return new DisruptionResponse(
                                replanRun.getReplanId(),
                                affectedInterviewIds,
                                affectedStudentIds,
                                result.getReplannedCount(),
                                result.getFailedCount());
        }

        @Transactional
        public DisruptionResponse handleRoomUnavailable(
                        RoomUnavailableRequest request) {

                // ============================================================
                // 1. Validate room
                // ============================================================

                Room room = roomRepository
                                .findById(request.getRoomId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No room found with id "
                                                                + request.getRoomId()));
                List<DisruptionEvent> existing = disruptionEventRepository.findByTargetTypeAndTargetId("ROOM",
                                room.getRoomId());
                for (DisruptionEvent ex : existing) {
                        if (ex.getDetails() != null && ex.getDetails().startsWith("DAY: " + request.getDay() + " ")) {
                                throw new IllegalStateException("Room " + room.getName() + " is already blocked on Day "
                                                + request.getDay());
                        }
                }

                // ============================================================
                // 2. Parse effective time
                // ============================================================

                LocalTime effectiveTime;

                try {

                        effectiveTime = LocalTime.parse(
                                        request.getEffectiveTime());

                } catch (Exception e) {

                        throw new IllegalArgumentException(
                                        "Invalid effectiveTime. Use HH:mm format, e.g. 13:00.");
                }

                // ============================================================
                // 3. Validate day
                // ============================================================

                if (request.getDay() == null
                                || request.getDay() <= 0) {

                        throw new IllegalArgumentException(
                                        "Day must be greater than 0.");
                }

                // ============================================================
                // 4. Deactivate room - Day-Specific Keep Status AVAILABLE
                // ============================================================

                // ============================================================
                // 5. Create disruption event
                // ============================================================

                DisruptionEvent event = new DisruptionEvent();

                event.setEventType(
                                "ROOM_UNAVAILABLE");

                event.setTargetType(
                                "ROOM");

                event.setTargetId(
                                room.getRoomId());

                event.setDetails("DAY: " + request.getDay() + " | TIME: " + request.getEffectiveTime() + " | Details: "
                                + (request.getDetails() != null ? request.getDetails() : ""));

                event.setOccurredAt(
                                LocalDateTime.now());

                event = disruptionEventRepository.save(event);

                // ============================================================
                // 6. Find affected interviews
                //
                // Same two cases as panel drop, keyed on room instead. UNLIKE
                // a panel drop, this can span MULTIPLE different companies at
                // once, since a room isn't owned by any one company.
                //
                // A) Interview is already running when the room goes down
                // B) Interview starts at or after the disruption
                // ============================================================

                List<Interview> affectedInterviews = new ArrayList<>();

                // A. Interview crossing the disruption time
                affectedInterviews.addAll(
                                interviewRepository
                                                .findInterviewsOverlappingRoomDrop(
                                                                room.getRoomId(),
                                                                request.getDay(),
                                                                "SCHEDULED",
                                                                effectiveTime));

                // B. Interviews starting at or after disruption time
                affectedInterviews.addAll(
                                interviewRepository
                                                .findInterviewsStartingAfterRoomDrop(
                                                                room.getRoomId(),
                                                                request.getDay(),
                                                                "SCHEDULED",
                                                                effectiveTime));

                ReplanRun replanRun = new ReplanRun();
                replanRun.setEvent(event);
                replanRun.setStartedAt(LocalDateTime.now());
                replanRun.setStatus("RUNNING");
                replanRun.setInterviewsAffected(affectedInterviews.size());
                replanRun.setInterviewsMoved(0);
                replanRun.setInterviewsCancelled(0);

                replanRun = replanRunRepository.save(replanRun);

                // ============================================================
                // 7. Remove duplicate interviews
                // ============================================================

                Map<Long, Interview> uniqueInterviews = new LinkedHashMap<>();

                for (Interview interview : affectedInterviews) {

                        uniqueInterviews.put(
                                        interview.getInterviewId(),
                                        interview);
                }

                affectedInterviews = new ArrayList<>(
                                uniqueInterviews.values());

                List<Long> affectedInterviewIds = affectedInterviews.stream()
                                .map(Interview::getInterviewId)
                                .toList();

                List<Long> affectedStudentIds = affectedInterviews.stream()
                                .filter(i -> i.getStudent() != null)
                                .map(i -> i.getStudent().getStudentId())
                                .distinct()
                                .toList();

                List<UnscheduledReason> disruptionReasons = new ArrayList<>();

                for (Interview interview : affectedInterviews) {
                        UnscheduledReason reason = new UnscheduledReason();
                        reason.setInterview(interview);
                        reason.setReason("ROOM_UNAVAILABLE_DISRUPTION (event #" + event.getEventId() + ")");
                        reason.setLoggedAt(LocalDateTime.now());
                        disruptionReasons.add(reason);
                }

                unscheduledReasonRepository.saveAll(disruptionReasons);

                // ============================================================
                // 8. Display disruption information
                // ============================================================

                System.out.println(
                                "========================================");

                System.out.println(
                                "ROOM UNAVAILABILITY DISRUPTION");

                System.out.println(
                                "Room ID: "
                                                + room.getRoomId());

                System.out.println(
                                "Day: "
                                                + request.getDay());

                System.out.println(
                                "Effective time: "
                                                + effectiveTime);

                System.out.println(
                                "Disruption event ID: "
                                                + event.getEventId());

                System.out.println(
                                "Affected interviews: "
                                                + affectedInterviews.size()
                                                + " (may span multiple companies)");

                System.out.println(
                                "========================================");

                // ============================================================
                // 9. Release old assignments
                // ============================================================

                // ============================================================
                // 10. Replan the entire batch
                // ============================================================

                System.out.println(
                                "Starting batch replanning...");

                ReplanResult result = schedulingService.replanInterviews(
                                affectedInterviews,
                                replanRun.getReplanId(),
                                event.getEventType(),
                                request.getDay(),
                                effectiveTime);

                replanRun.setCompletedAt(LocalDateTime.now());
                replanRun.setStatus("COMPLETED");
                replanRun.setInterviewsMoved(result.getReplannedCount());
                replanRun.setInterviewsCancelled(result.getFailedCount());

                replanRunRepository.save(replanRun);
                // ============================================================
                // 11. Final result
                // ============================================================

                System.out.println(
                                "========================================");

                System.out.println(
                                "ROOM UNAVAILABILITY PROCESSING COMPLETED");

                System.out.println(
                                "Room ID: "
                                                + room.getRoomId());

                System.out.println(
                                "Affected interviews: "
                                                + affectedInterviews.size());

                System.out.println(
                                "Successfully replanned: "
                                                + result.getReplannedCount());

                System.out.println(
                                "Failed to replan: "
                                                + result.getFailedCount());

                System.out.println(
                                "========================================");

                return new DisruptionResponse(
                                replanRun.getReplanId(),
                                affectedInterviewIds,
                                affectedStudentIds,
                                result.getReplannedCount(),
                                result.getFailedCount());
        }

        @Transactional
        public ReplanResult handleStudentWithdrawal(
                        StudentWithdrawalRequest request) {

                // ============================================================
                // 1. Validate student
                // ============================================================

                Student student = studentRepository
                                .findById(request.getStudentId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No student found with id "
                                                                + request.getStudentId()));

                if ("WITHDRAWN".equals(student.getStatus())) {

                        throw new IllegalStateException(
                                        "Student "
                                                        + request.getStudentId()
                                                        + " has already withdrawn.");
                }

                // ============================================================
                // 2. Parse effective time
                // ============================================================

                LocalTime effectiveTime;

                try {

                        effectiveTime = LocalTime.parse(
                                        request.getEffectiveTime());

                } catch (Exception e) {

                        throw new IllegalArgumentException(
                                        "Invalid effectiveTime. Use HH:mm format, e.g. 13:00.");
                }

                // ============================================================
                // 3. Validate day
                // ============================================================

                if (request.getDay() == null
                                || request.getDay() <= 0) {

                        throw new IllegalArgumentException(
                                        "Day must be greater than 0.");
                }

                // ============================================================
                // 4. Mark the student withdrawn. FULL withdrawal — every
                // company's process, not just one. This is distinct from
                // panel/room drop: nothing about the STUDENT's own capacity is
                // being disabled here, the student themselves is simply out.
                // ============================================================

                student.setStatus("WITHDRAWN");
                studentRepository.save(student);

                // ============================================================
                // 5. Create disruption event
                // ============================================================

                DisruptionEvent event = new DisruptionEvent();

                event.setEventType("STUDENT_WITHDRAWAL");
                event.setTargetType("STUDENT");
                event.setTargetId(student.getStudentId());
                event.setDetails(request.getDetails());
                event.setOccurredAt(LocalDateTime.now());

                event = disruptionEventRepository.save(event);

                // ============================================================
                // 6. Find the student's own affected interviews. TWO different
                // groups, handled differently:
                //
                // A) SCHEDULED interviews at/after the effective time — these
                // HOLD real resources (panel/room/time) that need releasing.
                // Already-past interviews (before effectiveTime) are left
                // alone, since they already happened.
                //
                // B) UNSCHEDULED interviews for this student, ALL of them,
                // unconditionally — these never held a day/time at all, so
                // there's no before/after distinction possible for them. The
                // student withdrawing means these will never happen either.
                // ============================================================

                List<Interview> scheduledToCancel = new ArrayList<>();

                scheduledToCancel.addAll(
                                interviewRepository
                                                .findInterviewsOverlappingStudentWithdrawal(
                                                                student.getStudentId(),
                                                                request.getDay(),
                                                                "SCHEDULED",
                                                                effectiveTime));

                scheduledToCancel.addAll(
                                interviewRepository
                                                .findInterviewsAfterStudentWithdrawal(
                                                                student.getStudentId(),
                                                                request.getDay(),
                                                                "SCHEDULED",
                                                                effectiveTime));

                // Dedup, same pattern as panel/room drop.
                Map<Long, Interview> uniqueScheduled = new LinkedHashMap<>();

                for (Interview interview : scheduledToCancel) {
                        uniqueScheduled.put(interview.getInterviewId(), interview);
                }

                scheduledToCancel = new ArrayList<>(uniqueScheduled.values());

                List<Interview> unscheduledToCancel = interviewRepository.findByStudentStudentIdAndStatus(
                                student.getStudentId(),
                                "UNSCHEDULED");

                List<Interview> allCancelled = new ArrayList<>();
                allCancelled.addAll(scheduledToCancel);
                allCancelled.addAll(unscheduledToCancel);

                ReplanRun replanRun = new ReplanRun();
                replanRun.setEvent(event);
                replanRun.setStartedAt(LocalDateTime.now());
                replanRun.setStatus("RUNNING");
                replanRun.setInterviewsAffected(allCancelled.size());
                replanRun.setInterviewsMoved(0);
                replanRun.setInterviewsCancelled(0);
                replanRun = replanRunRepository.save(replanRun);

                // ============================================================
                // 7. Display disruption information
                // ============================================================

                System.out.println("========================================");
                System.out.println("STUDENT WITHDRAWAL DISRUPTION");
                System.out.println("Student ID: " + student.getStudentId());
                System.out.println("Day: " + request.getDay());
                System.out.println("Effective time: " + effectiveTime);
                System.out.println("Disruption event ID: " + event.getEventId());
                System.out.println("Scheduled interviews being released: " + scheduledToCancel.size());
                System.out.println("Already-unscheduled interviews being cancelled: " + unscheduledToCancel.size());
                System.out.println("Total cancelled: " + allCancelled.size());
                System.out.println("========================================");

                // ============================================================
                // 8. Cancel every affected interview. IMPORTANT: status is
                // CANCELLED, not UNSCHEDULED — "UNSCHEDULED" means "still needs
                // a slot," which is no longer true for a withdrawn student's
                // interviews. They must never be picked up by any future
                // generateSchedule() or replanInterviews() call.
                //
                // (3) PROPER REASON, so this is queryable and distinguishable
                // from ordinary scheduling failures: every cancelled interview
                // gets STUDENT_WITHDRAWAL_DISRUPTION tagged with this event id.
                // ============================================================

                List<UnscheduledReason> withdrawalReasons = new ArrayList<>();

                for (Interview interview : allCancelled) {

                        System.out.println(
                                        "Cancelling interview "
                                                        + interview.getInterviewId()
                                                        + " | Company: " + interview.getCompany().getCompanyId()
                                                        + " | Old day: " + interview.getDay()
                                                        + " | Old time: " + interview.getStartTime() + " - "
                                                        + interview.getEndTime()
                                                        + " | Old panel: "
                                                        + (interview.getPanel() != null
                                                                        ? interview.getPanel().getPanelId()
                                                                        : null)
                                                        + " | Old room: "
                                                        + (interview.getRoom() != null ? interview.getRoom().getRoomId()
                                                                        : null));

                        interview.setPanel(null);
                        interview.setRoom(null);
                        interview.setDay(null);
                        interview.setStartTime(null);
                        interview.setEndTime(null);
                        interview.setStatus("CANCELLED");
                        interview.setUpdatedAt(LocalDateTime.now());

                        UnscheduledReason reason = new UnscheduledReason();
                        reason.setInterview(interview);
                        reason.setReplanId(replanRun.getReplanId());  
                        reason.setReason("STUDENT_WITHDRAWAL_DISRUPTION (event #" + event.getEventId() + ")");
                        reason.setLoggedAt(LocalDateTime.now());
                        withdrawalReasons.add(reason);
                }

                interviewRepository.saveAll(allCancelled);
                unscheduledReasonRepository.saveAll(withdrawalReasons);

                // ============================================================
                // 9. BACKFILL — this is what distinguishes withdrawal from
                // panel/room drop. The withdrawn student's own scheduled
                // interviews just freed real panel/room/time capacity. Rather
                // than leaving that capacity unused, sweep the PRE-EXISTING
                // unscheduled pool (every other student's interviews still
                // sitting UNSCHEDULED) and give them a fresh shot at it.
                //
                // This naturally excludes the interviews we just cancelled
                // above, since their status is now CANCELLED, not UNSCHEDULED —
                // findByStatus("UNSCHEDULED") will never return them.
                //
                // No day/time cutoff (notBeforeDay/notBeforeTime both null) —
                // unlike panel/room drop, these interviews never held an
                // original slot to protect against "moving into the past" from.
                // They're getting a genuinely fresh attempt at any capacity that
                // exists, exactly like the very first generateSchedule() run.
                // ============================================================

                List<Interview> backfillCandidates = interviewRepository.findByStatus("UNSCHEDULED");

                System.out.println("Starting backfill on " + backfillCandidates.size()
                                + " pre-existing unscheduled interview(s)...");

                ReplanResult backfillResult = schedulingService.replanInterviews(
                                backfillCandidates,
                                replanRun.getReplanId(),
                                event.getEventType(),
                                null,
                                null);

                replanRun.setCompletedAt(LocalDateTime.now());
                replanRun.setStatus("COMPLETED");
                replanRun.setInterviewsMoved(backfillResult.getReplannedCount());
                replanRun.setInterviewsCancelled(allCancelled.size());

                replanRunRepository.save(replanRun);

                // ============================================================
                // 10. Final result
                // ============================================================

                System.out.println("========================================");
                System.out.println("STUDENT WITHDRAWAL PROCESSING COMPLETED");
                System.out.println("Student ID: " + student.getStudentId());
                System.out.println("Cancelled: " + allCancelled.size());
                System.out.println("Backfilled from existing unscheduled pool: " + backfillResult.getReplannedCount());
                System.out.println("Still unscheduled after backfill attempt: " + backfillResult.getFailedCount());
                System.out.println("========================================");

                return new ReplanResult(
                                backfillResult.getReplannedCount(),
                                backfillResult.getFailedCount(),
                                allCancelled.size());
        }
}