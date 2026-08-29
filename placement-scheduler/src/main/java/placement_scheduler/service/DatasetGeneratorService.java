package placement_scheduler.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import placement_scheduler.entity.Company;
import placement_scheduler.entity.CompanySlot;
import placement_scheduler.entity.Interview;
import placement_scheduler.entity.Panel;
import placement_scheduler.entity.Room;
import placement_scheduler.entity.Shortlist;
import placement_scheduler.entity.Student;

import placement_scheduler.repository.CompanyRepository;
import placement_scheduler.repository.CompanySlotRepository;
import placement_scheduler.repository.InterviewRepository;
import placement_scheduler.repository.PanelRepository;
import placement_scheduler.repository.RoomRepository;
import placement_scheduler.repository.ShortlistRepository;
import placement_scheduler.repository.StudentRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class DatasetGeneratorService {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final CompanySlotRepository companySlotRepository;
    private final RoomRepository roomRepository;
    private final PanelRepository panelRepository;
    private final ShortlistRepository shortlistRepository;
    private final InterviewRepository interviewRepository;

    // Seeded on demand — see generateDataset(..., seed). Defaults to a
    // fresh random seed so normal runs still produce varied data, but a
    // fixed seed can be passed to reproduce an exact dataset for debugging
    // or for the live defense.
    private Random random = new Random();

    // Weight exponent for CGPA-weighted shortlist sampling. Higher k skews
    // selection more heavily toward the top of the eligible pool; k=2 gives
    // a strong-but-not-absolute preference for higher CGPA.
    private static final double SHORTLIST_WEIGHT_EXPONENT = 2.0;

    public DatasetGeneratorService(
            StudentRepository studentRepository,
            CompanyRepository companyRepository,
            CompanySlotRepository companySlotRepository,
            RoomRepository roomRepository,
            PanelRepository panelRepository,
            ShortlistRepository shortlistRepository,
            InterviewRepository interviewRepository) {

        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.companySlotRepository = companySlotRepository;
        this.roomRepository = roomRepository;
        this.panelRepository = panelRepository;
        this.shortlistRepository = shortlistRepository;
        this.interviewRepository = interviewRepository;
    }

    /**
     * Generates the complete initial placement dataset using a default
     * 9:00-17:00 working day and a fresh random seed. Panel count per
     * company is NOT a caller input — it's computed internally, purely
     * from each company's tier (see assignPanelCounts).
     */
    @Transactional
    public void generateDataset(
            int studentCount,
            int companyCount,
            int roomCount,
            int placementDays) {

        generateDataset(
                studentCount,
                companyCount,
                roomCount,
                placementDays,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null);
    }

    /**
     * Generates the complete initial placement dataset.
     *
     * @param placementDays number of days the placement drive runs across.
     *                      Drives day clustering (tier-1 mass recruiters
     *                      anchoring on Day 1, etc.) and interview capacity
     *                      math — no day count is hardcoded anywhere else
     *                      in this class.
     * @param workDayStart  start of the working day used for every company
     *                      slot and every capacity calculation.
     * @param workDayEnd    end of the working day, same reasoning as
     *                      workDayStart.
     * @param seed          pass a fixed value to reproduce an identical
     *                      dataset run to run (useful while debugging the
     *                      scheduler, or to reproduce the exact dataset
     *                      from a prior session before a live defense).
     *                      Pass null for a fresh random dataset.
     */
    @Transactional
    public void generateDataset(
            int studentCount,
            int companyCount,
            int roomCount,
            int placementDays,
            LocalTime workDayStart,
            LocalTime workDayEnd,
            Long seed) {

        this.random = (seed != null) ? new Random(seed) : new Random();

        System.out.println("========================================");
        System.out.println("Starting dataset generation"
                + (seed != null ? " (seed=" + seed + ")" : ""));
        System.out.println("Working day: " + workDayStart + " - " + workDayEnd);
        System.out.println("========================================");

        clearExistingData();

        generateStudents(studentCount);

        List<Company> companies = generateCompanies(companyCount);

        generateRooms(roomCount);

        // Panels per company vary by tier — computed internally, not
        // caller-supplied. A mass recruiter needs more parallel
        // interviewers than a niche company. Computed once and reused for
        // both panel generation and capacity math.
        Map<Long, Integer> panelsByCompany = assignPanelCounts(companies);

        generatePanels(companies, panelsByCompany);

        // Which days each company actually operates on. Tier-1 mass
        // recruiters cluster on Day 1 (per the assignment scenario); tier
        // 2 spreads across the middle of the week; tier 3 biases late.
        // Computed once and reused for both slot generation and capacity
        // math.
        Map<Long, List<Integer>> companyDays = assignCompanyDays(companies, placementDays);

        generateCompanySlots(companies, companyDays, workDayStart, workDayEnd);

        generateShortlistsAndInterviews(
                companies,
                panelsByCompany,
                companyDays,
                workDayStart,
                workDayEnd);

        System.out.println("========================================");
        System.out.println("Dataset generation completed");
        System.out.println("========================================");
    }

    // ============================================================
    // 1. STUDENTS
    // ============================================================

    private void generateStudents(int count) {

        List<Student> students = new ArrayList<>();

        /*
         * CGPA distribution:
         *
         * 50% -> 8.50 - 10.00
         * 30% -> 7.50 - 8.49
         * 15% -> 6.50 - 7.49
         * 5% -> 6.00 - 6.49
         */

        int highCgpaCount = (int) (count * 0.50);
        int upperMidCgpaCount = (int) (count * 0.30);
        int lowerMidCgpaCount = (int) (count * 0.15);

        /*
         * Branch distribution:
         *
         * CSE -> 30%
         * ISE -> 20%
         * ECE -> 20%
         * EEE -> 10%
         * ME -> 10%
         * CIVIL -> 10%
         */

        List<String> branches = new ArrayList<>();

        addRepeated(branches, "CSE", (int) (count * 0.30));
        addRepeated(branches, "ISE", (int) (count * 0.20));
        addRepeated(branches, "ECE", (int) (count * 0.20));
        addRepeated(branches, "EEE", (int) (count * 0.10));
        addRepeated(branches, "ME", (int) (count * 0.10));

        int assignedBranches = branches.size();
        addRepeated(branches, "CIVIL", count - assignedBranches);

        Collections.shuffle(branches, random);

        int studentNumber = 1;

        // 50% high CGPA
        for (int i = 0; i < highCgpaCount; i++) {

            Student student = createStudent(
                    studentNumber++,
                    branches.get(i),
                    generateCgpa(8.50, 10.00));

            students.add(student);
        }

        // 30% upper-middle CGPA
        for (int i = highCgpaCount; i < highCgpaCount + upperMidCgpaCount; i++) {

            Student student = createStudent(
                    studentNumber++,
                    branches.get(i),
                    generateCgpa(7.50, 8.49));

            students.add(student);
        }

        // 15% lower-middle CGPA
        for (int i = highCgpaCount + upperMidCgpaCount; i < highCgpaCount + upperMidCgpaCount
                + lowerMidCgpaCount; i++) {

            Student student = createStudent(
                    studentNumber++,
                    branches.get(i),
                    generateCgpa(6.50, 7.49));

            students.add(student);
        }

        // 5% low CGPA
        for (int i = highCgpaCount + upperMidCgpaCount + lowerMidCgpaCount; i < count; i++) {

            Student student = createStudent(
                    studentNumber++,
                    branches.get(i),
                    generateCgpa(6.00, 6.49));

            students.add(student);
        }

        Collections.shuffle(students, random);

        studentRepository.saveAll(students);

        System.out.println(count + " students generated.");
    }

    private void clearExistingData() {

        System.out.println("Clearing existing dataset...");

        // Delete dependent records first
        interviewRepository.deleteAll();
        shortlistRepository.deleteAll();

        // Delete company-related records
        companySlotRepository.deleteAll();
        panelRepository.deleteAll();

        // Delete independent records
        roomRepository.deleteAll();
        companyRepository.deleteAll();
        studentRepository.deleteAll();

        System.out.println("Previous dataset cleared.");
    }

    private Student createStudent(
            int studentNumber,
            String branch,
            double cgpa) {

        Student student = new Student();

        student.setName("Student " + studentNumber);
        student.setBranch(branch);
        student.setCgpa(cgpa);
        student.setStatus("ACTIVE");
        student.setCreatedAt(LocalDateTime.now());

        return student;
    }

    private double generateCgpa(double min, double max) {

        double cgpa = min + random.nextDouble() * (max - min);

        return Math.round(cgpa * 100.0) / 100.0;
    }

    private void addRepeated(
            List<String> list,
            String value,
            int count) {

        for (int i = 0; i < count; i++) {
            list.add(value);
        }
    }

    // ============================================================
    // 2. COMPANIES
    // ============================================================

    private List<Company> generateCompanies(int count) {

        List<Company> companies = new ArrayList<>();

        /*
         * Tier distribution as PERCENTAGES of the total company count,
         * not fixed caps — this keeps the ratio correct regardless of
         * scale (was previously hardcoded to 6/10/rest, which only
         * worked correctly at count=30).
         *
         * Tier 1 -> 20%
         * Tier 2 -> 33%
         * Tier 3 -> remaining (~47%)
         */

        int tier1Count = (int) Math.round(count * 0.20);
        int tier2Count = (int) Math.round(count * 0.33);
        int tier3Count = count - tier1Count - tier2Count;

        int companyNumber = 1;

        for (int i = 0; i < tier1Count; i++) {
            companies.add(createCompany(companyNumber++, 1));
        }

        for (int i = 0; i < tier2Count; i++) {
            companies.add(createCompany(companyNumber++, 2));
        }

        for (int i = 0; i < tier3Count; i++) {
            companies.add(createCompany(companyNumber++, 3));
        }

        Collections.shuffle(companies, random);

        companies = companyRepository.saveAll(companies);

        System.out.println(count + " companies generated.");

        return companies;
    }

    private Company createCompany(
            int companyNumber,
            int priorityTier) {

        Company company = new Company();

        company.setName("Company " + companyNumber);

        // CGPA cutoff drawn from a per-tier BAND, not a fixed constant —
        // otherwise every tier-1 company has an identical eligible pool,
        // which flattens the overlap variation the assignment wants.
        company.setCgpaCutoff(generateCutoffForTier(priorityTier));

        /*
         * Interview duration — shortened from 30/45 min to 15/30 min.
         * This roughly doubles how many interviews each panel can run per
         * day (e.g. an 8-hour day goes from 16 slots at 30 min to 32
         * slots at 15 min), which meaningfully increases capacity without
         * touching room count or panel count at all.
         *
         * 70% -> 15 minutes
         * 30% -> 30 minutes
         */
        int duration = (random.nextDouble() < 0.70) ? 15 : 30;

        company.setInterviewDurationMin(duration);

        company.setPriorityTier(priorityTier);

        company.setStatus("ACTIVE");

        company.setCreatedAt(LocalDateTime.now());

        // Sector is chosen INDEPENDENTLY of tier — a tier-1 (high-priority,
        // mass-recruiter-eligible) company can just as easily be a core
        // mechanical or civil recruiter as a software one. Tying branch
        // eligibility purely to tier (as before) meant every tier-1 company
        // was a software recruiter, which is an unrealistic correlation.
        CompanySector sector = pickSector();

        company.setEligibleBranches(generateEligibleBranches(priorityTier, sector));

        return company;
    }

    private enum CompanySector {
        TECH,               // CSE / ISE / ECE-leaning software & product roles
        CORE_ELECTRICAL,    // EEE / ECE-leaning electrical & electronics roles
        CORE_MECHANICAL,    // ME-leaning mechanical roles
        INFRASTRUCTURE,     // CIVIL-leaning infrastructure/construction roles
        GENERALIST          // hires broadly across most branches
    }

    /**
     * Sector distribution across the whole company pool. Weighted so most
     * companies at a typical engineering placement drive are still
     * tech-leaning, but a meaningful minority are core-mechanical,
     * core-electrical, infrastructure, or generalist — and any of these
     * can land on any tier.
     */
    private CompanySector pickSector() {

        double r = random.nextDouble();

        if (r < 0.45) return CompanySector.TECH;
        if (r < 0.60) return CompanySector.CORE_ELECTRICAL;
        if (r < 0.75) return CompanySector.CORE_MECHANICAL;
        if (r < 0.87) return CompanySector.INFRASTRUCTURE;
        return CompanySector.GENERALIST;
    }

    private double generateCutoffForTier(int priorityTier) {

        double min;
        double max;

        switch (priorityTier) {
            case 1:
                min = 8.00;
                max = 8.80;
                break;
            case 2:
                min = 7.00;
                max = 7.80;
                break;
            default:
                min = 6.50;
                max = 7.00;
                break;
        }

        double cutoff = min + random.nextDouble() * (max - min);

        return Math.round(cutoff * 100.0) / 100.0;
    }

    /**
     * Branch eligibility is driven by SECTOR (what kind of company this
     * is), not by tier. Tier only scales the probability of reaching for
     * each optional branch within that sector — a tier-1 mechanical
     * recruiter casts a wider net across its sector's related branches
     * than a tier-3 one does, but both are still fundamentally mechanical
     * recruiters, not software recruiters wearing a tier-1 label.
     */
    private Set<String> generateEligibleBranches(int priorityTier, CompanySector sector) {

        Set<String> branches = new HashSet<>();

        // Higher tier -> more likely to add each optional branch, but this
        // no longer determines WHICH branches are even on the table.
        double optionalInclusionProb;

        switch (priorityTier) {
            case 1:
                optionalInclusionProb = 0.65;
                break;
            case 2:
                optionalInclusionProb = 0.45;
                break;
            default:
                optionalInclusionProb = 0.30;
                break;
        }

        switch (sector) {

            case TECH: {
                branches.add("CSE");
                branches.add("ISE");
                if (random.nextDouble() < optionalInclusionProb) branches.add("ECE");
                if (random.nextDouble() < optionalInclusionProb * 0.5) branches.add("EEE");
                break;
            }

            case CORE_ELECTRICAL: {
                branches.add("EEE");
                branches.add("ECE");
                if (random.nextDouble() < optionalInclusionProb) branches.add("CSE");
                if (random.nextDouble() < optionalInclusionProb * 0.5) branches.add("ISE");
                break;
            }

            case CORE_MECHANICAL: {
                branches.add("ME");
                if (random.nextDouble() < optionalInclusionProb) branches.add("EEE");
                if (random.nextDouble() < optionalInclusionProb * 0.6) branches.add("CIVIL");
                break;
            }

            case INFRASTRUCTURE: {
                branches.add("CIVIL");
                if (random.nextDouble() < optionalInclusionProb) branches.add("ME");
                if (random.nextDouble() < optionalInclusionProb * 0.4) branches.add("EEE");
                break;
            }

            case GENERALIST: {
                // Broad by definition — every branch has a real (if
                // varying) chance regardless of tier.
                String[] all = { "CSE", "ISE", "ECE", "EEE", "ME", "CIVIL" };
                for (String b : all) {
                    if (random.nextDouble() < 0.55) {
                        branches.add(b);
                    }
                }
                break;
            }
        }

        if (branches.isEmpty()) {
            String[] fallback = { "CSE", "ISE", "ECE", "EEE", "ME", "CIVIL" };
            branches.add(fallback[random.nextInt(fallback.length)]);
        }

        return branches;
    }

    // ============================================================
    // 3. ROOMS
    // ============================================================

    private void generateRooms(int count) {

        List<Room> rooms = new ArrayList<>();

        for (int i = 1; i <= count; i++) {

            Room room = new Room();

            room.setName("Room " + i);
            room.setStatus("AVAILABLE");

            rooms.add(room);
        }

        roomRepository.saveAll(rooms);

        System.out.println(count + " rooms generated.");
    }

    // ============================================================
    // 4. PANEL COUNT PER TIER + PANELS
    // ============================================================

    /**
     * Panels per company vary by tier — computed entirely internally, no
     * caller input. Tier 1 gets the most parallel interviewers, tier 3
     * the fewest.
     *
     * Tier 1: 4-6 panels
     * Tier 2: 3-4 panels
     * Tier 3: 1-2 panels
     */
    private Map<Long, Integer> assignPanelCounts(List<Company> companies) {

        Map<Long, Integer> panelCounts = new HashMap<>();

        for (Company company : companies) {

            int panels;

            switch (company.getPriorityTier()) {
                case 1:
                    panels = 4 + random.nextInt(3);
                    break;
                case 2:
                    panels = 3 + random.nextInt(2);
                    break;
                default:
                    panels = 1 + random.nextInt(2);
                    break;
            }

            panelCounts.put(company.getCompanyId(), panels);
        }

        return panelCounts;
    }

    private void generatePanels(
            List<Company> companies,
            Map<Long, Integer> panelsPerCompany) {

        List<Panel> panels = new ArrayList<>();

        for (Company company : companies) {

            int panelCount = panelsPerCompany.get(company.getCompanyId());

            for (int i = 1; i <= panelCount; i++) {

                Panel panel = new Panel();

                panel.setCompany(company);
                panel.setLabel("Panel " + i);
                panel.setStatus("ACTIVE");

                panels.add(panel);
            }
        }

        panelRepository.saveAll(panels);

        System.out.println(panels.size() + " panels generated.");
    }

    // ============================================================
    // 5. COMPANY DAY ASSIGNMENT + AVAILABILITY SLOTS
    // ============================================================

    /**
     * Decides which placement days each company actually operates on.
     * Every tier keeps its characteristic LEAN toward part of the week
     * (tier 1 toward Day 1, tier 2 toward the middle, tier 3 toward the
     * end), but any company can now potentially extend to ANY day up to
     * placementDays — previously each tier had a hard cap on how many
     * days it could ever reach (tier 1 capped at 2, tier 2 capped at 2,
     * tier 3 capped at 1), which meant a company that was genuinely
     * oversubscribed had no way to spill into days that had real spare
     * room capacity, even when doing so would be realistic (a company
     * that's badly behind on interviews reasonably would ask for more
     * days).
     *
     * Tier 1 (mass recruiters): always starts on Day 1, then has an
     * independent chance of picking up EACH other day too.
     *
     * Tier 2: starts on a middle-of-week day (small chance of Day 1),
     * then has an independent, smaller chance of picking up each other
     * day.
     *
     * Tier 3: starts on a day biased toward the back half of the week,
     * then has a small independent chance of picking up each other day.
     */
    private Map<Long, List<Integer>> assignCompanyDays(
            List<Company> companies,
            int placementDays) {

        Map<Long, List<Integer>> companyDays = new HashMap<>();

        for (Company company : companies) {

            List<Integer> days = new ArrayList<>();

            switch (company.getPriorityTier()) {

                case 1: {
                    days.add(1);

                    for (int day = 2; day <= placementDays; day++) {
                        if (random.nextDouble() < 0.35) {
                            days.add(day);
                        }
                    }

                    break;
                }

                case 2: {
                    int lastUsableDay = Math.max(2, placementDays - 1);

                    int primaryDay;

                    if (placementDays >= 2 && random.nextDouble() < 0.15) {
                        primaryDay = 1;
                    } else {
                        int rangeStart = Math.min(2, placementDays);
                        int rangeSize = Math.max(1, lastUsableDay - rangeStart + 1);
                        primaryDay = rangeStart + random.nextInt(rangeSize);
                    }

                    days.add(primaryDay);

                    for (int day = 1; day <= placementDays; day++) {
                        if (day == primaryDay) {
                            continue;
                        }
                        if (random.nextDouble() < 0.25) {
                            days.add(day);
                        }
                    }

                    break;
                }

                default: {
                    // Bias the primary day toward the back half of the
                    // placement week where possible.
                    int startFrom = Math.max(1, placementDays - 1);
                    int primaryDay = startFrom + random.nextInt(placementDays - startFrom + 1);
                    primaryDay = Math.min(primaryDay, placementDays);
                    days.add(primaryDay);

                    for (int day = 1; day <= placementDays; day++) {
                        if (day == primaryDay) {
                            continue;
                        }
                        if (random.nextDouble() < 0.15) {
                            days.add(day);
                        }
                    }

                    break;
                }
            }

            Collections.sort(days);

            companyDays.put(company.getCompanyId(), days);
        }

        return companyDays;
    }

    private void generateCompanySlots(
            List<Company> companies,
            Map<Long, List<Integer>> companyDays,
            LocalTime workDayStart,
            LocalTime workDayEnd) {

        List<CompanySlot> slots = new ArrayList<>();

        for (Company company : companies) {

            for (int day : companyDays.get(company.getCompanyId())) {

                CompanySlot slot = new CompanySlot();

                slot.setCompany(company);
                slot.setDay(day);
                slot.setStartTime(workDayStart);
                slot.setEndTime(workDayEnd);

                slots.add(slot);
            }
        }

        companySlotRepository.saveAll(slots);

        System.out.println(slots.size() + " company availability slots generated.");
    }

    // ============================================================
    // 6. ELIGIBILITY + SHORTLIST (WEIGHTED, OVER-CAPACITY) + INTERVIEW
    // ============================================================

    private void generateShortlistsAndInterviews(
            List<Company> companies,
            Map<Long, Integer> panelsPerCompany,
            Map<Long, List<Integer>> companyDays,
            LocalTime workDayStart,
            LocalTime workDayEnd) {

        List<Student> students = studentRepository.findAll();

        List<Shortlist> shortlists = new ArrayList<>();
        List<Interview> interviews = new ArrayList<>();

        // Minutes in a working day, derived from the actual input
        // parameters rather than a hardcoded 8-hour assumption — this
        // must match the same workDayStart/workDayEnd used to generate
        // the CompanySlot rows, since capacity math and slot generation
        // both depend on the same working-day length.
        int minutesPerDay = (int) java.time.Duration.between(workDayStart, workDayEnd).toMinutes();

        for (Company company : companies) {

            // ----------------------------------------------------
            // Eligibility: CGPA cutoff AND branch match. This is a
            // hard filter — it only defines the candidate POOL, it
            // does not by itself create any shortlist or interview
            // rows.
            // ----------------------------------------------------

            List<Student> eligibleStudents = new ArrayList<>();

            for (Student student : students) {

                boolean cgpaEligible = student.getCgpa() >= company.getCgpaCutoff();
                boolean branchEligible = company.getEligibleBranches().contains(student.getBranch());

                if (cgpaEligible && branchEligible) {
                    eligibleStudents.add(student);
                }
            }

            // ----------------------------------------------------
            // Interview capacity for this company, based on its
            // ACTUAL assigned days and panel count. This is NOT used
            // to cap the shortlist below — it's computed purely so
            // we can log shortlisted-vs-capacity as a diagnostic.
            // Capacity and shortlist size are deliberately independent:
            // capacity is a scheduling constraint, shortlist size is
            // a company's own hiring decision, and the gap between
            // them is exactly what should surface as real, expected
            // infeasibility once the scheduler runs — not something
            // this generator should quietly prevent by capping.
            // ----------------------------------------------------

            int panelCount = panelsPerCompany.get(company.getCompanyId());
            int operatingDays = companyDays.get(company.getCompanyId()).size();
            int duration = company.getInterviewDurationMin();
            int interviewsPerPanelPerDay = minutesPerDay / duration;

            int capacity = panelCount * interviewsPerPanelPerDay * operatingDays;

            // ----------------------------------------------------
            // Shortlist size: a tier-based PERCENTAGE OF THE ELIGIBLE
            // POOL, with no reference to capacity at all. This is the
            // company's own selection behavior — mass recruiters cast
            // a wide net over who they'd like to interview, selective
            // companies take a narrow slice — and it's allowed to
            // exceed capacity freely. Whatever can't actually be
            // scheduled is real signal for the scheduler to report,
            // not something this generator should hide by scaling
            // shortlist size down to fit.
            // ----------------------------------------------------

            double shortlistFraction;

            switch (company.getPriorityTier()) {
                case 1:
                    shortlistFraction = 0.40 + random.nextDouble() * 0.20; // 40% - 60%
                    break;
                case 2:
                    shortlistFraction = 0.20 + random.nextDouble() * 0.15; // 20% - 35%
                    break;
                default:
                    shortlistFraction = 0.05 + random.nextDouble() * 0.10; // 5% - 15%
                    break;
            }

            int desiredShortlistCount = (int) Math.round(eligibleStudents.size() * shortlistFraction);

            int shortlistCount = Math.min(eligibleStudents.size(), desiredShortlistCount);

            // ----------------------------------------------------
            // CGPA-weighted random sampling WITHOUT replacement.
            // Replaces the previous strict top-N sort, which made
            // shortlisting fully deterministic by CGPA rank and
            // produced an unrealistically sharp eligibility cliff.
            // Higher CGPA (relative to this company's own cutoff)
            // increases selection probability but never guarantees
            // or excludes anyone outright.
            // ----------------------------------------------------

            List<Student> selected = weightedSampleWithoutReplacement(
                    eligibleStudents,
                    company.getCgpaCutoff(),
                    shortlistCount);

            for (int rank = 0; rank < selected.size(); rank++) {

                Student student = selected.get(rank);

                Shortlist shortlist = new Shortlist();

                shortlist.setStudent(student);
                shortlist.setCompany(company);
                shortlist.setRank(rank + 1);
                shortlist.setCreatedAt(LocalDateTime.now());

                shortlists.add(shortlist);

                // Interview requirement — remains UNSCHEDULED with
                // null panel/room/day/time until the scheduler runs.
                // Over-shortlisting guarantees some of these will
                // legitimately fail to be scheduled; that's expected
                // and should surface via UNSCHEDULED_REASON at
                // scheduling time, not hidden here.

                Interview interview = new Interview();

                interview.setStudent(student);
                interview.setCompany(company);
                interview.setPanel(null);
                interview.setRoom(null);
                interview.setDay(null);
                interview.setStartTime(null);
                interview.setEndTime(null);
                interview.setStatus("UNSCHEDULED");
                interview.setVersion(1);
                interview.setCreatedAt(LocalDateTime.now());
                interview.setUpdatedAt(LocalDateTime.now());

                interviews.add(interview);
            }

            int surplusOverCapacity = Math.max(0, selected.size() - capacity);

            System.out.println(
                    company.getName()
                            + " | Tier " + company.getPriorityTier()
                            + " | Cutoff " + company.getCgpaCutoff()
                            + " | Panels " + panelCount
                            + " | Days " + companyDays.get(company.getCompanyId())
                            + " | Capacity " + capacity
                            + " | Eligible " + eligibleStudents.size()
                            + " | Shortlisted " + selected.size()
                            + " | Minimum capacity overflow: " + surplusOverCapacity);
        }

        shortlistRepository.saveAll(shortlists);
        interviewRepository.saveAll(interviews);

        System.out.println(shortlists.size() + " shortlist records generated.");
        System.out.println(interviews.size() + " interview requirements generated.");
    }

    /**
     * Picks {@code count} students from {@code pool} without replacement,
     * where selection probability increases with CGPA relative to the
     * company's cutoff. Uses cumulative-weight roulette selection,
     * rebuilding the pool after each pick.
     *
     * This is what makes high-CGPA students appear on disproportionately
     * many shortlists (realistic skew) without making it a deterministic
     * ranking (which would create an artificial hard cutoff at the
     * capacity line).
     */
    private List<Student> weightedSampleWithoutReplacement(
            List<Student> pool,
            double companyCutoff,
            int count) {

        List<Student> remaining = new ArrayList<>(pool);
        List<Student> selected = new ArrayList<>();

        int toPick = Math.min(count, remaining.size());

        for (int pick = 0; pick < toPick; pick++) {

            double[] weights = new double[remaining.size()];
            double totalWeight = 0.0;

            for (int i = 0; i < remaining.size(); i++) {

                double margin = remaining.get(i).getCgpa() - companyCutoff;

                // Small floor so even a student right at the cutoff still
                // has a nonzero (if low) chance of being picked.
                double weight = Math.pow(Math.max(margin, 0.01), SHORTLIST_WEIGHT_EXPONENT);

                weights[i] = weight;
                totalWeight += weight;
            }

            double roll = random.nextDouble() * totalWeight;

            double cumulative = 0.0;
            int chosenIndex = remaining.size() - 1; // fallback for float rounding

            for (int i = 0; i < remaining.size(); i++) {
                cumulative += weights[i];
                if (roll <= cumulative) {
                    chosenIndex = i;
                    break;
                }
            }

            selected.add(remaining.remove(chosenIndex));
        }

        return selected;
    }
}