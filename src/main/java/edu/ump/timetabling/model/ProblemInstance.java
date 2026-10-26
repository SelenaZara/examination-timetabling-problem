package edu.ump.timetabling.model;

import java.util.*;

/**
 * Problem Instance containing the dataset information, conflict matrix, exams, rooms, periods,
 * and inter-room distance matrix for building proximity evaluation (S2).
 */
public class ProblemInstance {
    private final String datasetId;
    private final String datasetName;
    private final String semester;
    private final String session;
    private final String campus;
    private final int totalPeriods; // Typically 30 periods
    private final List<Exam> exams;
    private final List<Room> rooms;
    private final int[][] conflictMatrix; // conflictMatrix[i][j] = number of students taking both exam i and j
    private final int[][] distanceMatrix; // distanceMatrix[r1][r2] = physical distance between room r1 and r2
    private final int[] degree; // Number of conflicting exams for exam i
    private final int[] weightedDegree; // Sum of student conflicts for exam i
    private final int totalStudents;
    private final Map<String, String> coincidenceMap;

    public ProblemInstance(String datasetId, String datasetName, String semester, String session,
                           String campus, int totalPeriods, List<Exam> exams, List<Room> rooms,
                           int[][] conflictMatrix, int totalStudents) {
        this(datasetId, datasetName, semester, session, campus, totalPeriods, exams, rooms,
                conflictMatrix, null, null, totalStudents);
    }

    public ProblemInstance(String datasetId, String datasetName, String semester, String session,
                           String campus, int totalPeriods, List<Exam> exams, List<Room> rooms,
                           int[][] conflictMatrix, int[][] distanceMatrix, int totalStudents) {
        this(datasetId, datasetName, semester, session, campus, totalPeriods, exams, rooms,
                conflictMatrix, distanceMatrix, null, totalStudents);
    }

    public ProblemInstance(String datasetId, String datasetName, String semester, String session,
                           String campus, int totalPeriods, List<Exam> exams, List<Room> rooms,
                           int[][] conflictMatrix, int[][] distanceMatrix, Map<String, String> coincidenceMap, int totalStudents) {
        this.datasetId = datasetId;
        this.datasetName = datasetName;
        this.semester = semester;
        this.session = session;
        this.campus = campus;
        this.totalPeriods = totalPeriods;
        this.exams = Collections.unmodifiableList(exams);
        this.rooms = Collections.unmodifiableList(rooms);
        this.conflictMatrix = conflictMatrix;
        this.coincidenceMap = coincidenceMap != null ? Collections.unmodifiableMap(new HashMap<>(coincidenceMap)) : Collections.emptyMap();
        this.totalStudents = totalStudents;

        int numRooms = rooms.size();
        if (distanceMatrix != null && distanceMatrix.length >= numRooms) {
            this.distanceMatrix = distanceMatrix;
        } else {
            // Build default distance matrix from room block/campus information
            this.distanceMatrix = buildDefaultDistanceMatrix(rooms);
        }

        int numExams = exams.size();
        this.degree = new int[numExams];
        this.weightedDegree = new int[numExams];

        for (int i = 0; i < numExams; i++) {
            int deg = 0;
            int wDeg = 0;
            for (int j = 0; j < numExams; j++) {
                if (i != j && conflictMatrix[i][j] > 0) {
                    deg++;
                    wDeg += conflictMatrix[i][j];
                }
            }
            this.degree[i] = deg;
            this.weightedDegree[i] = wDeg;
        }
    }

    private static int[][] buildDefaultDistanceMatrix(List<Room> rooms) {
        int n = rooms.size();
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            Room r1 = rooms.get(i);
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    dist[i][j] = 0;
                } else {
                    Room r2 = rooms.get(j);
                    if (!r1.getCampus().equalsIgnoreCase(r2.getCampus())) {
                        dist[i][j] = 57; // 57 km between Gambang and Pekan campuses
                    } else if (r1.getBlock().equalsIgnoreCase(r2.getBlock())) {
                        dist[i][j] = 1; // Adjacent / same building block
                    } else {
                        dist[i][j] = 3; // Different blocks on same campus
                    }
                }
            }
        }
        return dist;
    }

    public String getDatasetId() { return datasetId; }
    public String getDatasetName() { return datasetName; }
    public String getSemester() { return semester; }
    public String getSession() { return session; }
    public String getCampus() { return campus; }
    public int getTotalPeriods() { return totalPeriods; }
    public List<Exam> getExams() { return exams; }
    public List<Room> getRooms() { return rooms; }
    public int getNumExams() { return exams.size(); }
    public int getNumRooms() { return rooms.size(); }
    public int[][] getConflictMatrix() { return conflictMatrix; }
    public int[][] getDistanceMatrix() { return distanceMatrix; }
    public int[] getDegree() { return degree; }
    public int[] getWeightedDegree() { return weightedDegree; }
    public int getTotalStudents() { return totalStudents; }
    public String getCoincidence(String courseCode) { return coincidenceMap != null ? coincidenceMap.get(courseCode) : null; }
    public Map<String, String> getCoincidenceMap() { return coincidenceMap; }

    public int getConflict(int examIndex1, int examIndex2) {
        return conflictMatrix[examIndex1][examIndex2];
    }

    public int getDistance(int roomIndex1, int roomIndex2) {
        if (roomIndex1 < 0 || roomIndex1 >= rooms.size() || roomIndex2 < 0 || roomIndex2 >= rooms.size()) {
            return 0;
        }
        return distanceMatrix[roomIndex1][roomIndex2];
    }
}
