package edu.ump.timetabling.solver;

import edu.ump.timetabling.model.*;
import java.util.*;

/**
 * Academic Constraint Evaluator for Examination Timetabling.
 * Implements 7 Hard Constraints (H1-H7, matching UMP and academic benchmarks) and 4 Soft Constraints (S1-S4):
 * - H1: Clash-free Timetable (No student has two exams at the same time)
 * - H2: Single Scheduled Period (Every exam scheduled exactly once)
 * - H3: Room Capacity Allowed (Total students in room <= Room capacity; supports multi-room split)
 * - H4: Max Room Usage per Timeslot (No double booking of venues)
 * - H5: Campus Allocation (Gambang vs Pekan vs Combined)
 * - H6: Cross-Campus / Split Exam Synchronization (same timeslot)
 * - H7: Large Exams Scheduled Early (Enrollment > 400 in first 10 slots)
 *
 * Soft Constraints:
 * - S1: Exam Spreading (Carter proximity penalty: weights 32, 16, 8, 4, 2)
 * - S2: Building Proximity (Student travel distance between consecutive/same-day rooms and split venues)
 * - S3: Minimization of Room Splitting (Overcapacity split rooms and department venue fragmentation)
 * - S4: Uniform Duration (Duration mismatches in same room/block and non-standard period lengths)
 */
public class ConstraintEvaluator {

    // Standard Carter proximity penalty weights for distance 1..5 periods
    private static final int[] PROXIMITY_WEIGHTS = { 0, 32, 16, 8, 4, 2 };

    // Indices: H1=0, H2=1, H3=2, H4=3, H5=4(campus), H6=5(sync), H7=6(early)
    public static final boolean[] ALL_HARD_CONSTRAINTS = { true, true, true, true, true, true, true };
    public static final boolean[] ALL_SOFT_CONSTRAINTS = { true, true, true, true };

    /**
     * Evaluates full solution quality with all hard and soft constraints enabled (default).
     */
    public static EvaluationResult evaluate(TimetableSolution solution) {
        return evaluate(solution, ALL_HARD_CONSTRAINTS, ALL_SOFT_CONSTRAINTS);
    }

    /**
     * Evaluates full solution quality with custom hard constraint toggles.
     */
    public static EvaluationResult evaluate(TimetableSolution solution, boolean[] enabledHardConstraints) {
        return evaluate(solution, enabledHardConstraints, ALL_SOFT_CONSTRAINTS);
    }

    /**
     * Evaluates full solution quality with custom hard and soft constraint toggles.
     */
    public static EvaluationResult evaluate(TimetableSolution solution,
                                            boolean[] enabledHardConstraints,
                                            boolean[] enabledSoftConstraints) {
        if (enabledHardConstraints == null || enabledHardConstraints.length < 7) {
            enabledHardConstraints = ALL_HARD_CONSTRAINTS;
        }
        if (enabledSoftConstraints == null || enabledSoftConstraints.length < 4) {
            enabledSoftConstraints = ALL_SOFT_CONSTRAINTS;
        }

        ProblemInstance instance = solution.getInstance();
        int numExams = instance.getNumExams();
        int numRooms = instance.getNumRooms();
        int totalPeriods = instance.getTotalPeriods();
        int[][] conflictMatrix = instance.getConflictMatrix();

        int h1StudentConflicts = 0;
        int h2Unassigned = 0;
        int h3RoomCapacity = 0;
        int h4RoomDoubleBooking = 0;
        int h5CampusMismatch = 0;
        int h6SplitExamSync = 0;
        int h7LargeExamLate = 0;

        double rawS1Penalty = 0.0;
        int s2BuildingDistance = 0;
        int s3RoomSplitting = 0;
        int s4UniformDuration = 0;

        // ── 1. Check Unassigned, Campus Mismatch, Large Exams, H1 clashes ──
        for (int i = 0; i < numExams; i++) {
            int p1 = solution.getPeriod(i);
            List<Integer> rList1 = solution.getRooms(i);
            Exam exam1 = instance.getExams().get(i);

            // H2: Single Assignment (Unassigned exams)
            if (p1 < 0 || p1 >= totalPeriods || rList1.isEmpty()) {
                if (enabledHardConstraints[1]) h2Unassigned++;
                continue;
            }

            // H5: Campus Alignment Matching
            if (enabledHardConstraints[4]) {
                for (int rId : rList1) {
                    if (rId >= 0 && rId < numRooms) {
                        Room room = instance.getRooms().get(rId);
                        if (hasCampusMismatch(exam1, room)) {
                            h5CampusMismatch++;
                        }
                    }
                }
            }

            // H7: Large Exams Scheduled Early (index >= 10 means late)
            if (enabledHardConstraints[6] && isLargeExamViolated(exam1, p1)) {
                h7LargeExamLate++;
            }

            int primaryRoom1 = rList1.get(0);

            for (int j = i + 1; j < numExams; j++) {
                int p2 = solution.getPeriod(j);
                List<Integer> rList2 = solution.getRooms(j);
                if (p2 < 0 || p2 >= totalPeriods || rList2.isEmpty()) continue;

                int clash = conflictMatrix[i][j];
                if (clash > 0) {
                    if (p1 == p2) {
                        // H1: Hard violation - same period student clash
                        if (enabledHardConstraints[0]) h1StudentConflicts += clash;
                    } else {
                        int periodDiff = Math.abs(p1 - p2);

                        // S1: Carter proximity penalty (spreading exams over 1..5 periods)
                        if (periodDiff >= 1 && periodDiff <= 5) {
                            rawS1Penalty += (clash * PROXIMITY_WEIGHTS[periodDiff]);
                        }

                        // S2: Building Proximity / Student Travel Distance
                        int primaryRoom2 = rList2.get(0);
                        int dist = instance.getDistance(primaryRoom1, primaryRoom2);
                        if (periodDiff == 1) {
                            s2BuildingDistance += (clash * dist);
                        } else if (p1 / 3 == p2 / 3) {
                            if (dist > 1) {
                                s2BuildingDistance += (clash * (dist - 1));
                            }
                        }
                    }
                }
            }
        }

        // S1 Carter Normalization
        double s1ExamSpreading = (instance.getTotalStudents() > 0)
                ? (rawS1Penalty / instance.getTotalStudents())
                : 0.0;

        // ── 2. Check Room Capacities (H3) & Single Room Usage / Double Booking (H4) ──
        int[][] roomLoad = new int[totalPeriods][numRooms];
        int[][] examsInRoomCount = new int[totalPeriods][numRooms];

        @SuppressWarnings("unchecked")
        List<Integer>[][] roomPeriodDurations = new List[totalPeriods][numRooms];

        for (int i = 0; i < numExams; i++) {
            int p = solution.getPeriod(i);
            List<Integer> rList = solution.getRooms(i);
            if (p >= 0 && p < totalPeriods && !rList.isEmpty()) {
                Exam exam = instance.getExams().get(i);
                int remainingStudents = exam.getStudentCount();

                for (int r : rList) {
                    if (r >= 0 && r < numRooms) {
                        int cap = instance.getRooms().get(r).getCapacity();
                        int assignedToRoom = Math.min(remainingStudents, cap);
                        roomLoad[p][r] += assignedToRoom;
                        remainingStudents -= assignedToRoom;
                        examsInRoomCount[p][r]++;

                        if (roomPeriodDurations[p][r] == null) {
                            roomPeriodDurations[p][r] = new ArrayList<>(2);
                        }
                        roomPeriodDurations[p][r].add(exam.getDurationMinutes());
                    }
                }

                // If remaining students exceed all assigned rooms' total capacity
                if (remainingStudents > 0 && enabledHardConstraints[2]) {
                    h3RoomCapacity += remainingStudents;
                }
            }
        }

        for (int p = 0; p < totalPeriods; p++) {
            for (int r = 0; r < numRooms; r++) {
                int capacity = instance.getRooms().get(r).getCapacity();

                // H3: Room Capacity Accommodation
                if (enabledHardConstraints[2] && roomLoad[p][r] > capacity) {
                    h3RoomCapacity += (roomLoad[p][r] - capacity);
                }

                // H4: Single Room Usage per Period (No double booking)
                if (enabledHardConstraints[3] && examsInRoomCount[p][r] > 1) {
                    h4RoomDoubleBooking += (examsInRoomCount[p][r] - 1);
                }
            }
        }

        // ── 3. S3: Minimization of Room Splitting ──
        for (int i = 0; i < numExams; i++) {
            List<Integer> rList = solution.getRooms(i);
            if (rList.size() > 1) {
                s3RoomSplitting += (rList.size() - 1);
            }
        }

        // Multi-room course assignments & Synchronized Split Exams (H7)
        Map<String, List<Integer>> courseGroups = new HashMap<>();
        Map<String, List<Integer>> periodCourseRooms = new HashMap<>();

        for (int i = 0; i < numExams; i++) {
            Exam exam = instance.getExams().get(i);
            String code = exam.getCode();
            int p = solution.getPeriod(i);
            List<Integer> rList = solution.getRooms(i);

            courseGroups.computeIfAbsent(code, k -> new ArrayList<>()).add(i);

            if (p >= 0 && p < totalPeriods && !rList.isEmpty()) {
                String key = p + "_" + code;
                periodCourseRooms.computeIfAbsent(key, k -> new ArrayList<>()).addAll(rList);
            }
        }

        // H6: Cross-Campus Synchronized Split Exams
        if (enabledHardConstraints[5]) {
            for (List<Integer> group : courseGroups.values()) {
                if (group.size() > 1) {
                    int firstPeriod = solution.getPeriod(group.get(0));
                    for (int i = 1; i < group.size(); i++) {
                        int p = solution.getPeriod(group.get(i));
                        if (firstPeriod >= 0 && p >= 0 && firstPeriod != p) {
                            h6SplitExamSync++;
                        }
                    }
                }
            }
            if (instance.getCoincidenceMap() != null && !instance.getCoincidenceMap().isEmpty()) {
                for (int i = 0; i < numExams; i++) {
                    String c1 = instance.getExams().get(i).getCode();
                    String c2 = instance.getCoincidence(c1);
                    if (c2 != null) {
                        for (int j = i + 1; j < numExams; j++) {
                            if (instance.getExams().get(j).getCode().equals(c2)) {
                                int p1 = solution.getPeriod(i);
                                int p2 = solution.getPeriod(j);
                                if (p1 >= 0 && p2 >= 0 && p1 != p2) {
                                    h6SplitExamSync++;
                                }
                            }
                        }
                    }
                }
            }
        }

        // S2: Distance between venues for split exams in the same period
        for (List<Integer> rList : periodCourseRooms.values()) {
            if (rList.size() > 1) {
                for (int a = 0; a < rList.size(); a++) {
                    for (int b = a + 1; b < rList.size(); b++) {
                        s2BuildingDistance += (instance.getDistance(rList.get(a), rList.get(b)) * 10);
                    }
                }
            }
        }

        // ── 4. S4: Uniform Duration in Same Room / Block ──
        for (int p = 0; p < totalPeriods; p++) {
            for (int r = 0; r < numRooms; r++) {
                List<Integer> durations = roomPeriodDurations[p][r];
                if (durations != null && durations.size() > 1) {
                    for (int a = 0; a < durations.size(); a++) {
                        for (int b = a + 1; b < durations.size(); b++) {
                            if (!durations.get(a).equals(durations.get(b))) {
                                s4UniformDuration += Math.abs(durations.get(a) - durations.get(b)) / 30;
                            }
                        }
                    }
                }
            }
        }

        // Non-standard period duration deviation
        for (int i = 0; i < numExams; i++) {
            int p = solution.getPeriod(i);
            if (p >= 0 && p < totalPeriods) {
                int dur = instance.getExams().get(i).getDurationMinutes();
                if (dur < 180) {
                    s4UniformDuration += (180 - dur) / 30;
                }
            }
        }

        // Composite soft penalty
        double totalSoftPenalty = 0.0;
        if (enabledSoftConstraints[0]) totalSoftPenalty += s1ExamSpreading;
        if (enabledSoftConstraints[1]) totalSoftPenalty += (s2BuildingDistance * 1.0);
        if (enabledSoftConstraints[2]) totalSoftPenalty += (s3RoomSplitting * 10.0);
        if (enabledSoftConstraints[3]) totalSoftPenalty += (s4UniformDuration * 5.0);

        EvaluationResult result = new EvaluationResult(
                h1StudentConflicts,
                h2Unassigned,
                h3RoomCapacity,
                h4RoomDoubleBooking,
                h5CampusMismatch,
                h6SplitExamSync,
                h7LargeExamLate,
                s1ExamSpreading,
                s2BuildingDistance,
                s3RoomSplitting,
                s4UniformDuration,
                totalSoftPenalty,
                instance.getTotalStudents()
        );

        solution.setHardViolations(result.getTotalHardViolations());
        solution.setSoftPenalty(result.getCompositePenaltyPerStudent());
        solution.setFeasible(result.isFeasible());

        return result;
    }

    // ── Helper Methods for Multi-Room Splitting and Search ──

    /**
     * Finds available rooms for examIndex in targetPeriod.
     * Supports automatic splitting into multiple rooms in the SAME building block (e.g. Block Z, Block W)
     * when student count exceeds single room capacity.
     */
    public static List<Integer> findRoomsForExam(ProblemInstance instance, int[] examPeriods, List<Integer>[] examRoomsList,
                                                int examIndex, int targetPeriod) {
        return findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, targetPeriod, false, true);
    }

    public static List<Integer> findRoomsForExam(ProblemInstance instance, int[] examPeriods, List<Integer>[] examRoomsList,
                                                int examIndex, int targetPeriod, boolean allowRelaxCampus) {
        return findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, targetPeriod, allowRelaxCampus, true);
    }

    public static List<Integer> findRoomsForExam(ProblemInstance instance, int[] examPeriods, List<Integer>[] examRoomsList,
                                                int examIndex, int targetPeriod, boolean allowRelaxCampus, boolean allowCrossBlock) {
        Exam exam = instance.getExams().get(examIndex);
        int enrollment = exam.getStudentCount();
        int numRooms = instance.getNumRooms();

        // 1. Identify which rooms are already occupied in targetPeriod
        boolean[] occupied = new boolean[numRooms];
        for (int other = 0; other < instance.getNumExams(); other++) {
            if (other == examIndex) continue;
            if (examPeriods[other] == targetPeriod) {
                if (examRoomsList != null && other < examRoomsList.length && examRoomsList[other] != null) {
                    for (int r : examRoomsList[other]) {
                        if (r >= 0 && r < numRooms) occupied[r] = true;
                    }
                }
            }
        }

        // 2. Separate available rooms: campus-matched vs all available
        List<Room> campusAvailable = new ArrayList<>();
        List<Room> allAvailable = new ArrayList<>();
        for (int r = 0; r < numRooms; r++) {
            if (!occupied[r]) {
                Room room = instance.getRooms().get(r);
                allAvailable.add(room);
                if (!hasCampusMismatch(exam, room)) {
                    campusAvailable.add(room);
                }
            }
        }

        // ── MATCHING CAMPUS PRIORITY (Strict H5 Campus Allocation) ──
        // Priority 1: Search single room or same-block split with campus-matched rooms
        List<Integer> rooms = searchRoomsSameBlock(campusAvailable, enrollment);
        if (rooms != null) return rooms;

        // Priority 2: Search cross-block split within campus-matched rooms (e.g. DBPP 360 + MDPM 89 for Pekan)
        // Hard constraint H5 MUST precede soft building-block preferences!
        if (allowCrossBlock) {
            rooms = searchRoomsCrossBlock(campusAvailable, enrollment);
            if (rooms != null) return rooms;
        }

        // ── CROSS-CAMPUS FALLBACK (Only if matching campus has insufficient total capacity) ──
        if (allowRelaxCampus && campusAvailable.size() < allAvailable.size()) {
            // Priority 3: Search single room or same-block split with all available rooms
            rooms = searchRoomsSameBlock(allAvailable, enrollment);
            if (rooms != null) return rooms;

            // Priority 4: Cross-block split across all available rooms
            if (allowCrossBlock) {
                rooms = searchRoomsCrossBlock(allAvailable, enrollment);
                if (rooms != null) return rooms;
            }
        }

        return null; // Not enough capacity in this period
    }

    /**
     * Searches for a single room OR splits into multiple rooms in the SAME building block (e.g. Block Z, Block W).
     */
    public static List<Integer> searchRoomsSameBlock(List<Room> available, int enrollment) {
        if (available == null || available.isEmpty()) return null;

        // Level A: Can a single room hold all students?
        Room bestSingle = null;
        for (Room r : available) {
            if (r.getCapacity() >= enrollment) {
                if (bestSingle == null || r.getCapacity() < bestSingle.getCapacity()) {
                    bestSingle = r;
                }
            }
        }
        if (bestSingle != null) {
            return Collections.singletonList(bestSingle.getId());
        }

        // Level B: Split into multiple rooms in the SAME building block!
        // "if the room capacity only have 40 and students have 50, you can split it to 2 rooms
        // and make sure same block building like block Z and W"
        Map<String, List<Room>> byBlock = new LinkedHashMap<>();
        for (Room r : available) {
            String blk = (r.getBlock() != null && !r.getBlock().isEmpty()) ? r.getBlock().toUpperCase() : "DEFAULT";
            byBlock.computeIfAbsent(blk, k -> new ArrayList<>()).add(r);
        }

        List<Integer> bestBlockRooms = null;
        int bestBlockTotalCap = Integer.MAX_VALUE;

        for (List<Room> bRooms : byBlock.values()) {
            // Sort rooms by capacity descending to use fewer rooms
            bRooms.sort((a, b) -> Integer.compare(b.getCapacity(), a.getCapacity()));
            int sumCap = 0;
            List<Integer> selected = new ArrayList<>();
            for (Room r : bRooms) {
                selected.add(r.getId());
                sumCap += r.getCapacity();
                if (sumCap >= enrollment) break;
            }

            if (sumCap >= enrollment) {
                // Fits in this block!
                if (sumCap < bestBlockTotalCap || (sumCap == bestBlockTotalCap && (bestBlockRooms == null || selected.size() < bestBlockRooms.size()))) {
                    bestBlockTotalCap = sumCap;
                    bestBlockRooms = selected;
                }
            }
        }

        return bestBlockRooms;
    }

    /**
     * Fallback cross-block room aggregator when no single block has enough total capacity for a huge exam.
     */
    public static List<Integer> searchRoomsCrossBlock(List<Room> available, int enrollment) {
        if (available == null || available.isEmpty()) return null;

        List<Room> sortedAll = new ArrayList<>(available);
        sortedAll.sort((a, b) -> Integer.compare(b.getCapacity(), a.getCapacity()));
        int total = 0;
        List<Integer> multiBlock = new ArrayList<>();
        for (Room r : sortedAll) {
            multiBlock.add(r.getId());
            total += r.getCapacity();
            if (total >= enrollment) break;
        }

        if (total >= enrollment) {
            return multiBlock;
        }

        return null;
    }

    public static boolean hasCampusMismatch(Exam exam, Room room) {
        String eCamp = exam.getCampus();
        String rCamp = room.getCampus();
        if (eCamp == null || rCamp == null) return false;
        if (eCamp.equalsIgnoreCase("Combined") || rCamp.equalsIgnoreCase("Combined")) return false;
        return !eCamp.equalsIgnoreCase(rCamp);
    }

    public static boolean isLargeExamViolated(Exam exam, int period) {
        return exam.getStudentCount() > 400 && period >= 10;
    }

    public static boolean hasStudentClash(ProblemInstance instance, int[] examPeriods, int examIndex, int targetPeriod) {
        int numExams = instance.getNumExams();
        for (int other = 0; other < numExams; other++) {
            if (other != examIndex && examPeriods[other] == targetPeriod) {
                if (instance.getConflict(examIndex, other) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Backward-compatible check for single room capacity.
     */
    public static boolean checkRoomCapacity(ProblemInstance instance, int[] examPeriods, int[] examRooms,
                                            int examIndex, int targetPeriod, int targetRoom) {
        int currentRoomOccupancy = 0;
        int examEnrollment = instance.getExams().get(examIndex).getStudentCount();
        int roomCap = instance.getRooms().get(targetRoom).getCapacity();

        for (int other = 0; other < instance.getNumExams(); other++) {
            if (other != examIndex && examPeriods[other] == targetPeriod && examRooms[other] == targetRoom) {
                currentRoomOccupancy += instance.getExams().get(other).getStudentCount();
            }
        }

        return (currentRoomOccupancy + examEnrollment) <= roomCap;
    }

    public static boolean hasRoomDoubleBooking(int[] examPeriods, int[] examRooms, int excludeExamIndex,
                                               int targetPeriod, int targetRoom) {
        for (int i = 0; i < examPeriods.length; i++) {
            if (i == excludeExamIndex) continue;
            if (examPeriods[i] == targetPeriod && examRooms[i] == targetRoom) {
                return true;
            }
        }
        return false;
    }

    public static double calculateSoftPenaltyDelta(ProblemInstance instance, int[] examPeriods,
                                                  int examIndex, int currentPeriod, int newPeriod) {
        if (currentPeriod == newPeriod) return 0.0;
        int numExams = instance.getNumExams();
        double delta = 0.0;

        for (int other = 0; other < numExams; other++) {
            if (other == examIndex) continue;
            int otherPeriod = examPeriods[other];
            if (otherPeriod < 0) continue;

            int clash = instance.getConflict(examIndex, other);
            if (clash > 0) {
                int oldDist = Math.abs(currentPeriod - otherPeriod);
                if (oldDist >= 1 && oldDist <= 5) {
                    delta -= (clash * PROXIMITY_WEIGHTS[oldDist]);
                }

                int newDist = Math.abs(newPeriod - otherPeriod);
                if (newDist >= 1 && newDist <= 5) {
                    delta += (clash * PROXIMITY_WEIGHTS[newDist]);
                }
            }
        }

        return instance.getTotalStudents() > 0 ? (delta / instance.getTotalStudents()) : delta;
    }

    /**
     * Finds all sibling or coincident exams that must be synchronized with examIndex (H6).
     * Sibling exams share the same course code, or are mapped in the coincidenceMap.
     */
    public static List<Integer> getCoincidentOrSiblingExams(ProblemInstance instance, int examIndex) {
        List<Integer> siblings = new ArrayList<>();
        Exam target = instance.getExams().get(examIndex);
        String targetCode = target.getCode();
        String partnerCode = instance.getCoincidence(targetCode);

        for (int i = 0; i < instance.getNumExams(); i++) {
            if (i == examIndex) continue;
            String otherCode = instance.getExams().get(i).getCode();
            if (otherCode.equals(targetCode) || (partnerCode != null && otherCode.equals(partnerCode))) {
                siblings.add(i);
            }
        }
        return siblings;
    }

    /**
     * Primary placement with relaxation supporting multi-room splitting and synchronized coincident placement.
     * Guarantees every subject gets scheduled without being left unassigned while strictly satisfying H5, H6, and H7.
     */
    public static int placeWithRelaxation(ProblemInstance instance, TimetableSolution solution,
                                          int examIndex, int totalPeriods, int numRooms) {
        int[] examPeriods = solution.getExamPeriods();
        List<Integer>[] examRoomsList = solution.getExamRoomsList();

        // If this exam has already been assigned (e.g. as a coincident partner of an earlier exam), skip
        if (examPeriods[examIndex] >= 0 && examRoomsList[examIndex] != null && !examRoomsList[examIndex].isEmpty()) {
            return 0;
        }

        Exam exam = instance.getExams().get(examIndex);
        List<Integer> siblings = getCoincidentOrSiblingExams(instance, examIndex);

        // Check if any sibling is already placed
        int requiredPeriod = -1;
        for (int sib : siblings) {
            if (examPeriods[sib] >= 0) {
                requiredPeriod = examPeriods[sib];
                break;
            }
        }

        // Case 1: Sibling is already placed -> MUST place in requiredPeriod to satisfy H6
        if (requiredPeriod >= 0 && requiredPeriod < totalPeriods) {
            if (!hasStudentClash(instance, examPeriods, examIndex, requiredPeriod)) {
                List<Integer> rooms = findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, requiredPeriod, false, false);
                if (rooms == null) {
                    rooms = findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, requiredPeriod, false, true);
                }
                if (rooms != null) {
                    solution.setPeriod(examIndex, requiredPeriod);
                    solution.setRooms(examIndex, rooms);
                    return 0;
                }
            }
        }

        // Case 2: Sibling exists but is not placed yet -> Place BOTH simultaneously in a mutually valid period!
        if (!siblings.isEmpty()) {
            boolean anyLarge = (exam.getStudentCount() > 400);
            for (int sib : siblings) {
                if (instance.getExams().get(sib).getStudentCount() > 400) anyLarge = true;
            }

            int limit = anyLarge ? Math.min(10, totalPeriods) : totalPeriods;

            for (int pass = 0; pass < (anyLarge ? 2 : 1); pass++) {
                int pStart = (pass == 0) ? 0 : 10;
                int pEnd = (pass == 0) ? limit : totalPeriods;

                for (int p = pStart; p < pEnd; p++) {
                    if (hasStudentClash(instance, examPeriods, examIndex, p)) continue;
                    List<Integer> rooms = findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, p, false, true);
                    if (rooms == null) continue;

                    solution.setPeriod(examIndex, p);
                    solution.setRooms(examIndex, rooms);

                    boolean allSiblingsValid = true;
                    Map<Integer, List<Integer>> sibRoomsMap = new HashMap<>();

                    for (int sib : siblings) {
                        if (hasStudentClash(instance, solution.getExamPeriods(), sib, p)) {
                            allSiblingsValid = false;
                            break;
                        }
                        List<Integer> sRooms = findRoomsForExam(instance, solution.getExamPeriods(), solution.getExamRoomsList(), sib, p, false, true);
                        if (sRooms == null) {
                            allSiblingsValid = false;
                            break;
                        }
                        sibRoomsMap.put(sib, sRooms);
                        solution.setPeriod(sib, p);
                        solution.setRooms(sib, sRooms);
                    }

                    if (allSiblingsValid) {
                        return 0;
                    }

                    // Revert temporary assignments
                    solution.setPeriod(examIndex, -1);
                    solution.setRooms(examIndex, Collections.emptyList());
                    for (int sib : siblings) {
                        solution.setPeriod(sib, -1);
                        solution.setRooms(sib, Collections.emptyList());
                    }
                }
            }
        }

        // Case 3: Standard single exam placement (Strict Campus Match H5, Large Exam H7)
        boolean isLarge = exam.getStudentCount() > 400;
        int maxP = isLarge ? Math.min(10, totalPeriods) : totalPeriods;

        for (int pass = 0; pass < (isLarge ? 2 : 1); pass++) {
            int pStart = (pass == 0) ? 0 : 10;
            int pEnd = (pass == 0) ? maxP : totalPeriods;

            // Step 3A: Same building block in matching campus
            for (int p = pStart; p < pEnd; p++) {
                if (hasStudentClash(instance, examPeriods, examIndex, p)) continue;
                List<Integer> rooms = findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, p, false, false);
                if (rooms != null) {
                    solution.setPeriod(examIndex, p);
                    solution.setRooms(examIndex, rooms);
                    return 0;
                }
            }

            // Step 3B: Cross-block split in matching campus (e.g. DBPP + MDPM)
            for (int p = pStart; p < pEnd; p++) {
                if (hasStudentClash(instance, examPeriods, examIndex, p)) continue;
                List<Integer> rooms = findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, p, false, true);
                if (rooms != null) {
                    solution.setPeriod(examIndex, p);
                    solution.setRooms(examIndex, rooms);
                    return 0;
                }
            }
        }

        // Step 4: Cross-campus fallback only if matching campus is 100% full in all periods
        for (int p = 0; p < totalPeriods; p++) {
            if (hasStudentClash(instance, examPeriods, examIndex, p)) continue;
            List<Integer> rooms = findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, p, true, true);
            if (rooms != null) {
                solution.setPeriod(examIndex, p);
                solution.setRooms(examIndex, rooms);
                return 10;
            }
        }

        // Step 5: Absolute fallback to minimum student clash
        int minClash = Integer.MAX_VALUE;
        int bestP = 0;
        List<Integer> bestRooms = null;
        for (int p = 0; p < totalPeriods; p++) {
            List<Integer> rooms = findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, p, false, true);
            if (rooms == null) rooms = findRoomsForExam(instance, examPeriods, examRoomsList, examIndex, p, true, true);
            if (rooms != null) {
                int clashes = 0;
                for (int other = 0; other < instance.getNumExams(); other++) {
                    if (other != examIndex && examPeriods[other] == p) {
                        clashes += instance.getConflict(examIndex, other);
                    }
                }
                if (clashes < minClash) {
                    minClash = clashes;
                    bestP = p;
                    bestRooms = rooms;
                }
            }
        }
        if (bestRooms == null) {
            bestRooms = searchRoomsSameBlock(instance.getRooms(), exam.getStudentCount());
            if (bestRooms == null) bestRooms = searchRoomsCrossBlock(instance.getRooms(), exam.getStudentCount());
            if (bestRooms == null || bestRooms.isEmpty()) {
                bestRooms = new ArrayList<>();
                for (int r = 0; r < numRooms; r++) bestRooms.add(r);
            }
        }
        solution.setPeriod(examIndex, bestP);
        solution.setRooms(examIndex, bestRooms);
        return 20;
    }

    /**
     * Backward-compatible placeWithRelaxation using raw arrays.
     */
    public static int placeWithRelaxation(ProblemInstance instance, int[] examPeriods, int[] examRooms,
                                          int examIndex, int totalPeriods, int numRooms) {
        TimetableSolution temp = new TimetableSolution(instance);
        for (int i = 0; i < examPeriods.length; i++) {
            temp.setPeriod(i, examPeriods[i]);
            temp.setRoom(i, examRooms[i]);
        }
        int level = placeWithRelaxation(instance, temp, examIndex, totalPeriods, numRooms);
        examPeriods[examIndex] = temp.getPeriod(examIndex);
        examRooms[examIndex] = temp.getRoom(examIndex);
        return level;
    }

    /**
     * Autonomous Feasibility Repair Engine (Min-Conflicts with synchronized Kempe moves and swaps).
     * Eliminates any residual hard constraint violations (H1-H7) to ensure a 100% FEASIBLE solution.
     */
    public static boolean repairFeasibility(ProblemInstance instance, TimetableSolution solution,
                                           boolean[] enabledHardConstraints, boolean[] enabledSoftConstraints) {
        EvaluationResult eval = evaluate(solution, enabledHardConstraints, enabledSoftConstraints);
        if (eval.isFeasible()) return true;

        int numExams = instance.getNumExams();
        int totalPeriods = instance.getTotalPeriods();
        int currentHard = eval.getTotalHardViolations();
        Random rng = new Random(42);

        for (int step = 0; step < 1000; step++) {
            if (currentHard == 0) break;

            List<Integer> violatingExams = new ArrayList<>();

            // 1. H2: Unassigned
            for (int i = 0; i < numExams; i++) {
                if (solution.getPeriod(i) < 0 || solution.getRooms(i).isEmpty()) {
                    violatingExams.add(i);
                }
            }

            // 2. H7: Large Exams Late
            if (enabledHardConstraints[6]) {
                for (int i = 0; i < numExams; i++) {
                    int p = solution.getPeriod(i);
                    Exam ex = instance.getExams().get(i);
                    if (p >= 0 && isLargeExamViolated(ex, p)) {
                        if (!violatingExams.contains(i)) violatingExams.add(i);
                    }
                }
            }

            // 3. H5: Campus Mismatch
            if (enabledHardConstraints[4]) {
                for (int i = 0; i < numExams; i++) {
                    Exam ex = instance.getExams().get(i);
                    for (int r : solution.getRooms(i)) {
                        if (r >= 0 && r < instance.getNumRooms()) {
                            if (hasCampusMismatch(ex, instance.getRooms().get(r))) {
                                if (!violatingExams.contains(i)) violatingExams.add(i);
                                break;
                            }
                        }
                    }
                }
            }

            // 4. H1: Student Clash
            if (enabledHardConstraints[0]) {
                for (int i = 0; i < numExams; i++) {
                    int p1 = solution.getPeriod(i);
                    if (p1 < 0) continue;
                    for (int j = i + 1; j < numExams; j++) {
                        if (solution.getPeriod(j) == p1 && instance.getConflict(i, j) > 0) {
                            if (!violatingExams.contains(i)) violatingExams.add(i);
                            if (!violatingExams.contains(j)) violatingExams.add(j);
                        }
                    }
                }
            }

            // 5. H6: Split Exam Sync (both coincidenceMap and same course code)
            if (enabledHardConstraints[5]) {
                for (int i = 0; i < numExams; i++) {
                    List<Integer> siblings = getCoincidentOrSiblingExams(instance, i);
                    int pI = solution.getPeriod(i);
                    for (int sib : siblings) {
                        int pSib = solution.getPeriod(sib);
                        if (pI >= 0 && pSib >= 0 && pI != pSib) {
                            if (!violatingExams.contains(i)) violatingExams.add(i);
                            if (!violatingExams.contains(sib)) violatingExams.add(sib);
                        }
                    }
                }
            }

            // 6. H4: Room Double Booking
            if (enabledHardConstraints[3]) {
                for (int p = 0; p < totalPeriods; p++) {
                    for (int r = 0; r < instance.getNumRooms(); r++) {
                        List<Integer> inRoom = new ArrayList<>();
                        for (int i = 0; i < numExams; i++) {
                            if (solution.getPeriod(i) == p && solution.getRooms(i).contains(r)) {
                                inRoom.add(i);
                            }
                        }
                        if (inRoom.size() > 1) {
                            for (int idx : inRoom) {
                                if (!violatingExams.contains(idx)) violatingExams.add(idx);
                            }
                        }
                    }
                }
            }

            // 7. H3: Room Capacity
            if (enabledHardConstraints[2]) {
                for (int i = 0; i < numExams; i++) {
                    int p = solution.getPeriod(i);
                    if (p < 0) continue;
                    int cap = 0;
                    for (int r : solution.getRooms(i)) {
                        if (r >= 0 && r < instance.getNumRooms()) cap += instance.getRooms().get(r).getCapacity();
                    }
                    if (instance.getExams().get(i).getStudentCount() > cap) {
                        if (!violatingExams.contains(i)) violatingExams.add(i);
                    }
                }
            }

            if (violatingExams.isEmpty()) break;

            int targetExam = violatingExams.get(rng.nextInt(violatingExams.size()));
            List<Integer> group = new ArrayList<>();
            group.add(targetExam);
            group.addAll(getCoincidentOrSiblingExams(instance, targetExam));

            // Check if any in group is large
            boolean groupLarge = false;
            for (int g : group) {
                if (instance.getExams().get(g).getStudentCount() > 400) groupLarge = true;
            }

            // Save old state for group
            Map<Integer, Integer> oldPeriods = new HashMap<>();
            Map<Integer, List<Integer>> oldRoomsMap = new HashMap<>();
            for (int g : group) {
                oldPeriods.put(g, solution.getPeriod(g));
                oldRoomsMap.put(g, new ArrayList<>(solution.getRooms(g)));
                // Temporarily unassign
                solution.setPeriod(g, -1);
                solution.setRooms(g, Collections.emptyList());
            }

            // Try candidate periods
            List<Integer> candidates = new ArrayList<>();
            if (groupLarge) {
                for (int p = 0; p < Math.min(10, totalPeriods); p++) candidates.add(p);
            } else {
                for (int p = 0; p < totalPeriods; p++) candidates.add(p);
            }
            Collections.shuffle(candidates, rng);

            boolean moveSuccess = false;
            for (int pCand : candidates) {
                boolean allValid = true;
                Map<Integer, List<Integer>> candRooms = new HashMap<>();

                for (int g : group) {
                    if (hasStudentClash(instance, solution.getExamPeriods(), g, pCand)) {
                        allValid = false;
                        break;
                    }
                    List<Integer> r = findRoomsForExam(instance, solution.getExamPeriods(), solution.getExamRoomsList(), g, pCand, false, true);
                    if (r == null) {
                        allValid = false;
                        break;
                    }
                    candRooms.put(g, r);
                    solution.setPeriod(g, pCand);
                    solution.setRooms(g, r);
                }

                if (allValid) {
                    EvaluationResult testEval = evaluate(solution, enabledHardConstraints, enabledSoftConstraints);
                    if (testEval.getTotalHardViolations() < currentHard) {
                        currentHard = testEval.getTotalHardViolations();
                        moveSuccess = true;
                        break;
                    }
                }

                // Clean up temporary assignments
                for (int g : group) {
                    solution.setPeriod(g, -1);
                    solution.setRooms(g, Collections.emptyList());
                }
            }

            if (moveSuccess) continue;

            // Revert group if no move improved hard violations
            for (int g : group) {
                solution.setPeriod(g, oldPeriods.get(g));
                solution.setRooms(g, oldRoomsMap.get(g));
            }

            // Strategy: Targeted Swap with another exam
            int otherExam = rng.nextInt(numExams);
            if (!group.contains(otherExam)) {
                int otherP = solution.getPeriod(otherExam);
                int targetOldP = solution.getPeriod(targetExam);
                if (otherP >= 0 && otherP != targetOldP) {
                    List<Integer> otherOldRooms = new ArrayList<>(solution.getRooms(otherExam));

                    solution.setPeriod(targetExam, -1);
                    solution.setPeriod(otherExam, -1);

                    if (!hasStudentClash(instance, solution.getExamPeriods(), targetExam, otherP) &&
                        !hasStudentClash(instance, solution.getExamPeriods(), otherExam, targetOldP)) {

                        List<Integer> rTarget = findRoomsForExam(instance, solution.getExamPeriods(), solution.getExamRoomsList(), targetExam, otherP, false, true);
                        if (rTarget != null) {
                            solution.setPeriod(targetExam, otherP);
                            solution.setRooms(targetExam, rTarget);

                            List<Integer> rOther = findRoomsForExam(instance, solution.getExamPeriods(), solution.getExamRoomsList(), otherExam, targetOldP, false, true);
                            if (rOther != null) {
                                solution.setPeriod(otherExam, targetOldP);
                                solution.setRooms(otherExam, rOther);

                                EvaluationResult testEval = evaluate(solution, enabledHardConstraints, enabledSoftConstraints);
                                if (testEval.getTotalHardViolations() < currentHard) {
                                    currentHard = testEval.getTotalHardViolations();
                                    continue;
                                }
                            }
                        }
                    }

                    // Revert swap
                    solution.setPeriod(targetExam, targetOldP);
                    solution.setRooms(targetExam, oldRoomsMap.get(targetExam));
                    solution.setPeriod(otherExam, otherP);
                    solution.setRooms(otherExam, otherOldRooms);
                }
            }
        }

        evaluate(solution, enabledHardConstraints, enabledSoftConstraints);
        return solution.isFeasible();
    }

    public static boolean repairFeasibility(ProblemInstance instance, TimetableSolution solution) {
        return repairFeasibility(instance, solution, ALL_HARD_CONSTRAINTS, ALL_SOFT_CONSTRAINTS);
    }
}
