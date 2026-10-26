package edu.ump.timetabling.dataset;

import edu.ump.timetabling.model.Exam;
import java.util.*;

/**
 * Builds conflict matrix from student course registrations following academic course ordering.
 */
public class ConflictMatrixBuilder {

    /**
     * Computes the conflict matrix using the student registration records.
     *
     * @param courses List of Exam objects (represents Course[alc][0] = course code)
     * @param studentRegistrations List of [StudentID, CourseCode] pairs
     * @return 2D symmetric conflict matrix of size [courses.size()][courses.size()]
     */
    public static int[][] buildConflictMatrix(List<Exam> courses, List<String[]> studentRegistrations) {
        int numCourses = courses.size();
        int[][] conflictMatrix = new int[numCourses][numCourses];

        // Map course code to index
        Map<String, Integer> courseIndexMap = new HashMap<>();
        for (int i = 0; i < numCourses; i++) {
            courseIndexMap.put(courses.get(i).getCode().trim().toUpperCase(), i);
        }

        // Group student registrations by student ID for O(S * K^2) efficiency
        // while strictly adhering to the student clash logic
        Map<String, List<String>> studentCourseMap = new LinkedHashMap<>();
        for (String[] reg : studentRegistrations) {
            if (reg.length < 2) continue;
            String studId = reg[0].trim();
            String subCode = reg[1].trim().toUpperCase();

            studentCourseMap.computeIfAbsent(studId, k -> new ArrayList<>()).add(subCode);
        }

        // For each student, find all pairs of enrolled courses and increment conflict matrix
        for (Map.Entry<String, List<String>> entry : studentCourseMap.entrySet()) {
            List<String> enrolledCourses = entry.getValue();
            int n = enrolledCourses.size();

            for (int a = 0; a < n; a++) {
                String subA = enrolledCourses.get(a);
                Integer idxA = courseIndexMap.get(subA);
                if (idxA == null) continue;

                for (int b = a + 1; b < n; b++) {
                    String subB = enrolledCourses.get(b);
                    Integer idxB = courseIndexMap.get(subB);
                    if (idxB == null || idxA.equals(idxB)) continue;

                    conflictMatrix[idxA][idxB]++;
                    conflictMatrix[idxB][idxA]++;
                }
            }
        }

        return conflictMatrix;
    }

    /**
     * Converts conflict matrix to CSV format for export.
     */
    public static String toCsv(List<Exam> courses, int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        sb.append("Course Code,Course Name");
        for (Exam exam : courses) {
            sb.append(",").append(escapeCsv(exam.getCode()));
        }
        sb.append("\n");

        for (int i = 0; i < courses.size(); i++) {
            Exam exam = courses.get(i);
            sb.append(escapeCsv(exam.getCode())).append(",").append(escapeCsv(exam.getName()));
            for (int j = 0; j < courses.size(); j++) {
                sb.append(",").append(matrix[i][j]);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
