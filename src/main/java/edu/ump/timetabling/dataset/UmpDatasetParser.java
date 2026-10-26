package edu.ump.timetabling.dataset;

import edu.ump.timetabling.model.Exam;
import edu.ump.timetabling.model.ProblemInstance;
import edu.ump.timetabling.model.Room;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Universal Parser for Universiti Malaysia Pahang (UMP) Malaysia Examination Datasets.
 * Fully dynamic: handles variable lengths of courses, students, rooms, distance matrices, 
 * campus allocations, coincidence constraints, and exam timeslots without hardcoded array limits.
 */
public class UmpDatasetParser {

    public static class RawFilesData {
        public String campusFileContent = "";
        public String coincidenceFileContent = "";
        public String courseFileContent = "";
        public String distanceMatrixFileContent = "";
        public String roomInfoFileContent = "";
        public String studentFileContent = "";
        public String timeslotFileContent = "";
        public String datasetName = "UMP Custom Upload";
        public String semester = "Semester 1";
        public String session = "2018/2019";
        public String campusScope = "Combined (Gambang & Pekan)";
        public int totalPeriods = 30;
    }

    public static ProblemInstance parseAndBuildInstance(RawFilesData data, String instanceId) throws IOException {
        // 1. Parse Timeslot File (Exam Durations: Subject -> Duration in minutes)
        Map<String, Integer> durationMap = new HashMap<>();
        if (data.timeslotFileContent != null && !data.timeslotFileContent.isBlank()) {
            try (BufferedReader br = new BufferedReader(new StringReader(data.timeslotFileContent))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.toLowerCase().startsWith("subject")) continue;
                    String[] tokens = line.split("[,;\\t\\s]+");
                    if (tokens.length >= 2) {
                        String code = tokens[0].trim().toUpperCase();
                        try {
                            durationMap.put(code, Integer.parseInt(tokens[1].trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        // 2. Parse Campus Mapping (Subject -> Campus: "G" -> Gambang, "P" -> Pekan)
        Map<String, String> campusMap = new HashMap<>();
        if (data.campusFileContent != null && !data.campusFileContent.isBlank()) {
            try (BufferedReader br = new BufferedReader(new StringReader(data.campusFileContent))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.toLowerCase().startsWith("subject")) continue;
                    String[] tokens = line.split("[,;\\t\\s]+");
                    if (tokens.length >= 2) {
                        String sub = tokens[0].trim().toUpperCase();
                        String campCode = tokens[1].trim().toUpperCase();
                        String fullCamp = campCode.startsWith("G") ? "Gambang" : (campCode.startsWith("P") ? "Pekan" : campCode);
                        campusMap.put(sub, fullCamp);
                    }
                }
            }
        }

        // 2b. Parse Coincidence Mapping (Synchronized Split Exams across campuses, e.g. G0002 P0002)
        Map<String, String> coincidenceMap = new HashMap<>();
        if (data.coincidenceFileContent != null && !data.coincidenceFileContent.isBlank()) {
            try (BufferedReader br = new BufferedReader(new StringReader(data.coincidenceFileContent))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.toLowerCase().startsWith("subject")) continue;
                    String[] tokens = line.split("[,;\\t\\s]+");
                    if (tokens.length >= 2) {
                        String s1 = tokens[0].trim().toUpperCase();
                        String s2 = tokens[1].trim().toUpperCase();
                        coincidenceMap.put(s1, s2);
                        coincidenceMap.put(s2, s1);
                    }
                }
            }
        }

        // 3. Parse Course List (Subject -> Total Students)
        List<Exam> exams = new ArrayList<>();
        Map<String, Integer> courseIndexMap = new LinkedHashMap<>();
        if (data.courseFileContent != null && !data.courseFileContent.isBlank()) {
            try (BufferedReader br = new BufferedReader(new StringReader(data.courseFileContent))) {
                String line;
                int idx = 0;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.toLowerCase().startsWith("subject") || line.toLowerCase().startsWith("course")) continue;
                    String[] tokens = line.split("[,;\\t]+");
                    if (tokens.length == 1) {
                        tokens = line.split("\\s+");
                    }
                    if (tokens.length >= 2) {
                        String code = tokens[0].trim().toUpperCase();
                        int totalStud = 0;
                        try {
                            totalStud = Integer.parseInt(tokens[1].trim());
                        } catch (NumberFormatException ignored) {}

                        String camp = campusMap.getOrDefault(code, code.startsWith("P") ? "Pekan" : "Gambang");
                        String courseName = "Course " + code;
                        int duration = durationMap.getOrDefault(code, 180);
                        exams.add(new Exam(idx, code, courseName, totalStud, camp, duration));
                        courseIndexMap.put(code, idx);
                        idx++;
                    }
                }
            }
        }

        // 4. Parse Room Info (Block, Campus, Capacity, Room Name)
        List<Room> rooms = new ArrayList<>();
        Map<String, Integer> roomIndexMap = new LinkedHashMap<>();
        if (data.roomInfoFileContent != null && !data.roomInfoFileContent.isBlank()) {
            try (BufferedReader br = new BufferedReader(new StringReader(data.roomInfoFileContent))) {
                String line;
                int rIdx = 0;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.toLowerCase().startsWith("block") || line.toLowerCase().startsWith("room")) continue;
                    String[] tokens = line.split("[,;\\t]+");
                    if (tokens.length < 3) {
                        tokens = line.split("\\s+");
                    }
                    if (tokens.length >= 3) {
                        String roomName;
                        int capacity = 100;
                        String camp = "Gambang";
                        String block = "B1";

                        if (tokens.length >= 4) {
                            block = tokens[0].trim().toUpperCase();
                            String campCode = tokens[1].trim().toUpperCase();
                            camp = campCode.startsWith("P") ? "Pekan" : "Gambang";
                            try { capacity = Integer.parseInt(tokens[2].trim()); } catch (Exception ignored) {}
                            roomName = tokens[3].trim().toUpperCase();
                            if (block.isEmpty() || block.equals("-")) {
                                block = roomName.replaceAll("[0-9]", "");
                                if (block.isEmpty()) block = "DEFAULT";
                            }
                        } else {
                            int cap1 = -1;
                            try { cap1 = Integer.parseInt(tokens[1].trim()); } catch (Exception ignored) {}
                            if (cap1 > 0) {
                                capacity = cap1;
                                String t0 = tokens[0].trim().toUpperCase();
                                String t2 = tokens[2].trim().toUpperCase();
                                if (t0.equals("P") || t0.equals("G") || t0.startsWith("PEK") || t0.startsWith("GAM")) {
                                    camp = t0.startsWith("P") ? "Pekan" : "Gambang";
                                    roomName = t2;
                                } else {
                                    roomName = t0;
                                    camp = t2.startsWith("P") ? "Pekan" : (t2.startsWith("G") ? "Gambang" : t2);
                                }
                            } else {
                                roomName = tokens[0].trim().toUpperCase();
                                try { capacity = Integer.parseInt(tokens[1].trim()); } catch (Exception ignored) {}
                                if (tokens.length >= 3) {
                                    String cCode = tokens[2].trim().toUpperCase();
                                    camp = cCode.startsWith("P") ? "Pekan" : (cCode.startsWith("G") ? "Gambang" : tokens[2].trim());
                                }
                            }
                            block = roomName.replaceAll("[0-9]", "");
                            if (block.isEmpty()) block = "DEFAULT";
                        }

                        rooms.add(new Room(rIdx, roomName, roomName, capacity, camp, block));
                        roomIndexMap.put(roomName, rIdx);
                        rIdx++;
                    }
                }
            }
        }

        // 5. Parse Student Registrations (Student ID -> Registered Subjects)
        Map<String, List<String>> studentCourseMap = new LinkedHashMap<>();
        Set<String> uniqueStudents = new HashSet<>();
        if (data.studentFileContent != null && !data.studentFileContent.isBlank()) {
            try (BufferedReader br = new BufferedReader(new StringReader(data.studentFileContent))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.toLowerCase().startsWith("student")) continue;
                    String[] tokens = line.split("[,;\\t]+");
                    if (tokens.length == 1) {
                        tokens = line.split("\\s+");
                    }
                    if (tokens.length >= 2) {
                        String studId = tokens[0].trim().toUpperCase();
                        String subCode = tokens[1].trim().toUpperCase();

                        uniqueStudents.add(studId);
                        studentCourseMap.computeIfAbsent(studId, k -> new ArrayList<>()).add(subCode);

                        // If course was not in course file, add dynamically
                        if (!courseIndexMap.containsKey(subCode)) {
                            int newIdx = exams.size();
                            String camp = campusMap.getOrDefault(subCode, subCode.startsWith("P") ? "Pekan" : "Gambang");
                            int duration = durationMap.getOrDefault(subCode, 180);
                            exams.add(new Exam(newIdx, subCode, "Course " + subCode, 0, camp, duration));
                            courseIndexMap.put(subCode, newIdx);
                        }
                    }
                }
            }
        }

        // 6. Build Conflict Matrix based on Student Enrollments
        int numExams = exams.size();
        int[][] conflictMatrix = new int[numExams][numExams];

        // Count actual per-exam enrollment from student registrations
        int[] examEnrollments = new int[numExams];

        for (Map.Entry<String, List<String>> entry : studentCourseMap.entrySet()) {
            List<String> list = entry.getValue();
            int n = list.size();
            for (int i = 0; i < n; i++) {
                Integer idxA = courseIndexMap.get(list.get(i));
                if (idxA == null) continue;

                // Count this student as enrolled in exam idxA
                examEnrollments[idxA]++;

                for (int j = i + 1; j < n; j++) {
                    Integer idxB = courseIndexMap.get(list.get(j));
                    if (idxB == null || idxA.equals(idxB)) continue;

                    conflictMatrix[idxA][idxB]++;
                    conflictMatrix[idxB][idxA]++;
                }
            }
        }

        // Rebuild the exams list with correct student counts derived from actual registrations.
        // If course file already had enrollment data AND registrations agree, registrations take priority.
        List<Exam> finalExams = new ArrayList<>(exams.size());
        for (int i = 0; i < exams.size(); i++) {
            Exam original = exams.get(i);
            // Prefer the enrollment count from actual student registrations if available
            int actualEnrollment = examEnrollments[i];
            int finalEnrollment = actualEnrollment > 0 ? actualEnrollment : original.getStudentCount();
            finalExams.add(new Exam(
                    original.getId(),
                    original.getCode(),
                    original.getName(),
                    finalEnrollment,
                    original.getCampus(),
                    original.getDurationMinutes()
            ));
        }
        exams = finalExams;

        // Fallback default rooms if room file was empty
        if (rooms.isEmpty()) {
            rooms.add(new Room(0, "DBP", "Dewan Bahasa dan Pustaka (DBP)", 1019, "Gambang", "D"));
            rooms.add(new Room(1, "ASTAKA", "Dewan Astaka Gambang", 355, "Gambang", "A"));
            rooms.add(new Room(2, "VDK01", "Dewan Kuliah VDK01", 75, "Gambang", "V"));
            rooms.add(new Room(3, "ZDK02", "Dewan Kuliah ZDK02", 40, "Gambang", "Z"));
            rooms.add(new Room(4, "DSG", "Dewan Serbaguna Pekan", 600, "Pekan", "P"));
            for (int i = 0; i < rooms.size(); i++) {
                roomIndexMap.put(rooms.get(i).getCode(), i);
            }
        }

        // 7. Parse Distance Matrix (Room1, Room2, Distance)
        int numRooms = rooms.size();
        int[][] distanceMatrix = new int[numRooms][numRooms];
        if (data.distanceMatrixFileContent != null && !data.distanceMatrixFileContent.isBlank()) {
            try (BufferedReader br = new BufferedReader(new StringReader(data.distanceMatrixFileContent))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.toLowerCase().startsWith("room")) continue;
                    String[] tokens = line.split("[,;\\t\\s]+");
                    if (tokens.length >= 3) {
                        String r1Name = tokens[0].trim().toUpperCase();
                        String r2Name = tokens[1].trim().toUpperCase();
                        Integer idx1 = roomIndexMap.get(r1Name);
                        Integer idx2 = roomIndexMap.get(r2Name);
                        if (idx1 != null && idx2 != null) {
                            try {
                                int dist = Integer.parseInt(tokens[2].trim());
                                distanceMatrix[idx1][idx2] = dist;
                                distanceMatrix[idx2][idx1] = dist;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        } else {
            distanceMatrix = null; // Let ProblemInstance compute defaults based on blocks
        }

        // Total students = unique student IDs found in the student registration file
        int totalStudents = uniqueStudents.size();


        return new ProblemInstance(
                instanceId,
                data.datasetName,
                data.semester,
                data.session,
                data.campusScope,
                data.totalPeriods,
                exams,
                rooms,
                conflictMatrix,
                distanceMatrix,
                coincidenceMap,
                totalStudents
        );
    }
}
