package edu.ump.timetabling.model;

/**
 * Encapsulates the results of evaluating a timetable solution.
 * Matches the academic constraints (H1-H8):
 *
 * H1: No students allowed to take more than one exam at the same time (Student Clash)
 * H2: The exam must be scheduled only once (Unassigned)
 * H3: Room capacity must accommodate total students
 * H4: Number of exam rooms used must not exceed total rooms in a timeslot (Double Booking)
 * H5: A single exam split into multiple rooms must use rooms in the SAME building block (e.g. Block Z, Block W)
 * H6: The exam must be assigned to the appropriate campus (Campus Match)
 * H7: Similar exams held in different campus must be assigned to the same timeslot (Split Sync)
 * H8: Larger examinations must be scheduled early in the exam period
 *
 * Soft Constraints:
 * S1: Exam Spreading (Carter Proximity)
 * S2: Building Proximity (Room distance for split exams)
 * S3: Minimization of Room Splitting
 * S4: Uniform Duration in Same Room
 */
public class EvaluationResult {
    // Hard Constraints (H1-H7)
    private final int h1StudentConflicts;   // H1: No Student Conflict in Same Period
    private final int h2Unassigned;         // H2: Single Assignment (Unassigned exams)
    private final int h3RoomCapacity;       // H3: Room Capacity Accommodation
    private final int h4RoomDoubleBooking;  // H4: Max Room Usage per Timeslot
    private final int h5CampusMismatch;     // H5: Campus Alignment Matching
    private final int h6SplitExamSync;      // H6: Cross-Campus Synchronized Exams
    private final int h7LargeExamLate;      // H7: Large Exams Scheduled Early

    // Soft Constraints (S1-S4)
    private final double s1ExamSpreading;
    private final int s2BuildingDistance;
    private final int s3RoomSplitting;
    private final int s4UniformDuration;

    private final int totalHardViolations;
    private final double totalSoftPenalty;
    private final double penaltyPerStudent;
    private final double compositePenaltyPerStudent;
    private final boolean feasible;

    /**
     * Primary academic constructor with H1-H7.
     */
    public EvaluationResult(int h1, int h2, int h3, int h4, int h5CampusMismatch, int h6SplitExamSync, int h7LargeExamLate,
                            double s1, int s2, int s3, int s4,
                            double totalSoftPenalty, int totalStudents) {
        this.h1StudentConflicts = h1;
        this.h2Unassigned = h2;
        this.h3RoomCapacity = h3;
        this.h4RoomDoubleBooking = h4;
        this.h5CampusMismatch = h5CampusMismatch;
        this.h6SplitExamSync = h6SplitExamSync;
        this.h7LargeExamLate = h7LargeExamLate;

        this.s1ExamSpreading = s1;
        this.s2BuildingDistance = s2;
        this.s3RoomSplitting = s3;
        this.s4UniformDuration = s4;

        this.totalHardViolations = h1 + h2 + h3 + h4 + h5CampusMismatch + h6SplitExamSync + h7LargeExamLate;
        this.totalSoftPenalty = totalSoftPenalty;
        this.penaltyPerStudent = s1;
        this.compositePenaltyPerStudent = totalSoftPenalty;
        this.feasible = (this.totalHardViolations == 0);
    }

    /**
     * Backwards-compatible constructor accepting legacy 8-constraint parameters.
     */
    public EvaluationResult(int h1, int h2, int h3, int h4, int ignoredH5, int h6CampusMismatch, int h7SplitExamSync, int h8LargeExamLate,
                            double s1, int s2, int s3, int s4,
                            double totalSoftPenalty, int totalStudents) {
        this(h1, h2, h3, h4, h6CampusMismatch, h7SplitExamSync, h8LargeExamLate, s1, s2, s3, s4, totalSoftPenalty, totalStudents);
    }

    // Getters for H1-H7
    public int getH1StudentConflicts()  { return h1StudentConflicts; }
    public int getH2Unassigned()        { return h2Unassigned; }
    public int getH3RoomCapacity()      { return h3RoomCapacity; }
    public int getH4RoomDoubleBooking() { return h4RoomDoubleBooking; }
    public int getH5CampusMismatch()    { return h5CampusMismatch; }
    public int getH6SplitExamSync()     { return h6SplitExamSync; }
    public int getH7LargeExamLate()     { return h7LargeExamLate; }
    public int getH5SameBuildingSplit() { return 0; } // Removed as hard constraint

    // Soft constraint getters
    public double getS1ExamSpreading()  { return s1ExamSpreading; }
    public int getS2BuildingDistance()  { return s2BuildingDistance; }
    public int getS3RoomSplitting()     { return s3RoomSplitting; }
    public int getS4UniformDuration()   { return s4UniformDuration; }
    public double getS1() { return s1ExamSpreading; }
    public int getS2()    { return s2BuildingDistance; }
    public int getS3()    { return s3RoomSplitting; }
    public int getS4()    { return s4UniformDuration; }

    // Legacy aliases so existing UI and logs don't break
    public int getStudentClashes()      { return h1StudentConflicts; }
    public int getRoomOvercapacities()  { return h3RoomCapacity; }
    public int getUnassignedExams()     { return h2Unassigned; }
    public int getH6CampusMismatch()    { return h5CampusMismatch; }
    public int getH8LargeExamLate()     { return h7LargeExamLate; }

    public int getTotalHardViolations()          { return totalHardViolations; }
    public double getTotalSoftPenalty()          { return totalSoftPenalty; }
    public double getPenaltyPerStudent()         { return penaltyPerStudent; }
    public double getCompositePenaltyPerStudent(){ return compositePenaltyPerStudent; }
    public boolean isFeasible()                  { return feasible; }
}
