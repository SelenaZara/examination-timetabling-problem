package edu.ump.timetabling.model;

import java.util.*;

/**
 * Represents a complete assignment state for the examination timetabling problem.
 * Encapsulates the standard decision variables:
 * - X_it : Exam i assigned to timeslot t
 * - Y_ir : Exam i assigned to room r (supports multi-room assignments for split exams)
 * - Z_rt : Room r utilized in timeslot t
 */
public class TimetableSolution {
    private final ProblemInstance instance;
    private final int[] examPeriod;           // examPeriod[i] = timeslot t (0..totalPeriods-1)
    private final int[] examRoom;             // primary room index r (0..numRooms-1)
    private final List<Integer>[] examRooms;  // all room indices assigned to exam i (supports split rooms)
    private double softPenalty;
    private int hardViolations;
    private boolean feasible;
    private long computationTimeMs;
    private String algorithmName;
    private int iterations;

    @SuppressWarnings("unchecked")
    public TimetableSolution(ProblemInstance instance) {
        this.instance = instance;
        int n = instance.getNumExams();
        this.examPeriod = new int[n];
        this.examRoom = new int[n];
        this.examRooms = new List[n];
        Arrays.fill(this.examPeriod, -1);
        Arrays.fill(this.examRoom, -1);
        for (int i = 0; i < n; i++) {
            this.examRooms[i] = new ArrayList<>();
        }
        this.softPenalty = Double.MAX_VALUE;
        this.hardViolations = Integer.MAX_VALUE;
        this.feasible = false;
        this.computationTimeMs = 0;
        this.algorithmName = "Uninitialized";
        this.iterations = 0;
    }

    @SuppressWarnings("unchecked")
    public TimetableSolution(TimetableSolution other) {
        this.instance = other.instance;
        int n = other.examPeriod.length;
        this.examPeriod = Arrays.copyOf(other.examPeriod, n);
        this.examRoom = Arrays.copyOf(other.examRoom, n);
        this.examRooms = new List[n];
        for (int i = 0; i < n; i++) {
            this.examRooms[i] = new ArrayList<>(other.examRooms[i]);
        }
        this.softPenalty = other.softPenalty;
        this.hardViolations = other.hardViolations;
        this.feasible = other.feasible;
        this.computationTimeMs = other.computationTimeMs;
        this.algorithmName = other.algorithmName;
        this.iterations = other.iterations;
    }

    public ProblemInstance getInstance() { return instance; }

    public int getPeriod(int examIndex) { return examPeriod[examIndex]; }
    public void setPeriod(int examIndex, int period) { this.examPeriod[examIndex] = period; }

    public int getRoom(int examIndex) {
        if (examRooms != null && examIndex >= 0 && examIndex < examRooms.length && !examRooms[examIndex].isEmpty()) {
            return examRooms[examIndex].get(0);
        }
        return examRoom[examIndex];
    }

    public void setRoom(int examIndex, int roomIndex) {
        this.examRoom[examIndex] = roomIndex;
        if (examRooms != null && examIndex >= 0 && examIndex < examRooms.length) {
            this.examRooms[examIndex].clear();
            if (roomIndex >= 0) {
                this.examRooms[examIndex].add(roomIndex);
            }
        }
    }

    public List<Integer> getRooms(int examIndex) {
        if (examRooms != null && examIndex >= 0 && examIndex < examRooms.length) {
            return examRooms[examIndex];
        }
        if (examRoom[examIndex] >= 0) {
            return Collections.singletonList(examRoom[examIndex]);
        }
        return Collections.emptyList();
    }

    public void setRooms(int examIndex, List<Integer> rooms) {
        if (examRooms != null && examIndex >= 0 && examIndex < examRooms.length) {
            this.examRooms[examIndex].clear();
            if (rooms != null && !rooms.isEmpty()) {
                this.examRooms[examIndex].addAll(rooms);
                this.examRoom[examIndex] = rooms.get(0);
            } else {
                this.examRoom[examIndex] = -1;
            }
        } else {
            this.examRoom[examIndex] = (rooms != null && !rooms.isEmpty()) ? rooms.get(0) : -1;
        }
    }

    public List<Integer>[] getExamRoomsList() {
        return examRooms;
    }

    public int[] getExamPeriods() { return examPeriod; }
    public int[] getExamRooms() { return examRoom; }

    public double getSoftPenalty() { return softPenalty; }
    public void setSoftPenalty(double softPenalty) { this.softPenalty = softPenalty; }

    public int getHardViolations() { return hardViolations; }
    public void setHardViolations(int hardViolations) { this.hardViolations = hardViolations; }

    public boolean isFeasible() { return feasible; }
    public void setFeasible(boolean feasible) { this.feasible = feasible; }

    public long getComputationTimeMs() { return computationTimeMs; }
    public void setComputationTimeMs(long computationTimeMs) { this.computationTimeMs = computationTimeMs; }

    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }

    public int getIterations() { return iterations; }
    public void setIterations(int iterations) { this.iterations = iterations; }

    /**
     * Checks whether exam i is assigned to period t (X_it == 1)
     */
    public boolean getXit(int examIndex, int periodIndex) {
        return examPeriod[examIndex] == periodIndex;
    }

    /**
     * Checks whether exam i is assigned to room r (Y_ir == 1).
     * Accurately reports true for all rooms an exam is split into.
     */
    public boolean getYir(int examIndex, int roomIndex) {
        if (examRooms != null && examIndex >= 0 && examIndex < examRooms.length && !examRooms[examIndex].isEmpty()) {
            return examRooms[examIndex].contains(roomIndex);
        }
        return examRoom[examIndex] == roomIndex;
    }

    /**
     * Checks whether room r is used in period t (Z_rt == 1).
     */
    public boolean getZrt(int roomIndex, int periodIndex) {
        for (int i = 0; i < instance.getNumExams(); i++) {
            if (examPeriod[i] == periodIndex) {
                if (examRooms != null && examRooms[i].contains(roomIndex)) {
                    return true;
                }
                if (examRoom[i] == roomIndex) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if all exams are assigned.
     */
    public boolean isComplete() {
        for (int p : examPeriod) {
            if (p < 0) return false;
        }
        for (int i = 0; i < instance.getNumExams(); i++) {
            if (examRooms != null && examRooms[i].isEmpty() && examRoom[i] < 0) return false;
        }
        return true;
    }
}
