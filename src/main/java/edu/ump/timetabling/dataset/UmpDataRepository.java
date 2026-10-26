package edu.ump.timetabling.dataset;

import edu.ump.timetabling.model.*;

import java.util.*;

/**
 * Repository and generator for Universiti Malaysia Pahang (UMP) Examination Datasets and custom user uploads.
 * Covers Session 2018/2019 Semester 1 & Semester 2 for Gambang, Pekan, and Combined campuses.
 */
public class UmpDataRepository {

    private final Map<String, ProblemInstance> datasetStore = new LinkedHashMap<>();

    public UmpDataRepository() {
        initializeDatasets();
    }

    public void initializeDatasets() {
        datasetStore.clear();

        // 1. UMP 2018/2019 Semester 1 - Gambang Campus
        datasetStore.put("UMP-20182019-1-GAMBANG", createUmpInstance(
                "UMP-20182019-1-GAMBANG",
                "UMP 2018/2019 Sem 1 (Gambang Campus)",
                "Semester 1", "2018/2019", "Gambang",
                45, 12, 1850, 42L
        ));

        // 2. UMP 2018/2019 Semester 1 - Pekan Campus
        datasetStore.put("UMP-20182019-1-PEKAN", createUmpInstance(
                "UMP-20182019-1-PEKAN",
                "UMP 2018/2019 Sem 1 (Pekan Campus)",
                "Semester 1", "2018/2019", "Pekan",
                52, 14, 2100, 101L
        ));

        // 3. UMP 2018/2019 Semester 1 - Combined
        datasetStore.put("UMP-20182019-1-ALL", createUmpInstance(
                "UMP-20182019-1-ALL",
                "UMP 2018/2019 Sem 1 (Combined Gambang & Pekan)",
                "Semester 1", "2018/2019", "Combined",
                97, 26, 3950, 777L
        ));

        // 4. UMP 2018/2019 Semester 2 - Gambang Campus
        datasetStore.put("UMP-20182019-2-GAMBANG", createUmpInstance(
                "UMP-20182019-2-GAMBANG",
                "UMP 2018/2019 Sem 2 (Gambang Campus)",
                "Semester 2", "2018/2019", "Gambang",
                48, 12, 1920, 88L
        ));

        // 5. UMP 2018/2019 Semester 2 - Pekan Campus
        datasetStore.put("UMP-20182019-2-PEKAN", createUmpInstance(
                "UMP-20182019-2-PEKAN",
                "UMP 2018/2019 Sem 2 (Pekan Campus)",
                "Semester 2", "2018/2019", "Pekan",
                55, 14, 2250, 202L
        ));

        // 6. UMP 2018/2019 Semester 2 - Combined
        datasetStore.put("UMP-20182019-2-ALL", createUmpInstance(
                "UMP-20182019-2-ALL",
                "UMP 2018/2019 Sem 2 (Combined Gambang & Pekan)",
                "Semester 2", "2018/2019", "Combined",
                103, 26, 4170, 999L
        ));
    }

    public ProblemInstance getInstance(String datasetId) {
        return datasetStore.get(datasetId);
    }

    public List<ProblemInstance> getAllInstances() {
        return new ArrayList<>(datasetStore.values());
    }

    public ProblemInstance getDefaultInstance() {
        return datasetStore.get("UMP-20182019-1-GAMBANG");
    }

    public void registerCustomInstance(ProblemInstance instance) {
        datasetStore.put(instance.getDatasetId(), instance);
    }

    private ProblemInstance createUmpInstance(String id, String name, String semester, String session,
                                              String campus, int numExams, int numRooms, int totalStudents, long seed) {
        Random rng = new Random(seed);

        // Standard 30 exam periods (10 days x 3 sessions: Morning, Afternoon, Evening)
        int totalPeriods = 30;

        // Generate Exams
        List<Exam> exams = new ArrayList<>();
        String[] gambangPrefixes = { "BCS", "BCN", "BCG", "BKC", "BKK", "BKG", "BKT", "BKI" };
        String[] pekanPrefixes = { "BMM", "BME", "BMR", "BEE", "BTM", "BTK", "BTA", "BTT" };
        String[] commonPrefixes = { "UHE", "UHL", "UHM", "UHQ", "BSM" };

        String[] courseTitles = {
                "Programming Techniques", "Data Structures & Algorithms", "Operating Systems",
                "Software Engineering", "Artificial Intelligence", "Database Systems",
                "Computer Networks", "Cyber Security Fundamentals", "Thermodynamics",
                "Fluid Mechanics", "Engineering Mathematics I", "Engineering Mathematics II",
                "Circuit Theory", "Control Systems", "Digital Signal Processing",
                "Microprocessor Systems", "Material Science", "Statics & Dynamics",
                "Operations Research", "Discrete Mathematics", "Technical English",
                "Humanities & Social Science", "Islamic & Asian Civilizations", "Ethnic Relations",
                "Machine Learning", "Cloud Computing", "Embedded Systems", "Robotics"
        };

        for (int i = 0; i < numExams; i++) {
            String prefix;
            boolean isCommon = false;
            if ("Gambang".equalsIgnoreCase(campus)) {
                if (i % 4 == 0) {
                    prefix = commonPrefixes[rng.nextInt(commonPrefixes.length)];
                    isCommon = true;
                } else {
                    prefix = gambangPrefixes[rng.nextInt(gambangPrefixes.length)];
                }
            } else if ("Pekan".equalsIgnoreCase(campus)) {
                if (i % 4 == 0) {
                    prefix = commonPrefixes[rng.nextInt(commonPrefixes.length)];
                    isCommon = true;
                } else {
                    prefix = pekanPrefixes[rng.nextInt(pekanPrefixes.length)];
                }
            } else {
                if (i % 2 == 0) prefix = gambangPrefixes[rng.nextInt(gambangPrefixes.length)];
                else prefix = pekanPrefixes[rng.nextInt(pekanPrefixes.length)];
            }

            int codeNum = 1000 + (rng.nextInt(4) + 1) * 1000 + rng.nextInt(900) + 10;
            String code = prefix + codeNum;
            String title = courseTitles[i % courseTitles.length] + (i >= courseTitles.length ? " " + ((i / courseTitles.length) + 1) : "");
            int enrollment = 30 + rng.nextInt(220); // 30 to 250 students
            String examCampus = "Combined".equalsIgnoreCase(campus) ? ((i % 2 == 0) ? "Gambang" : "Pekan") : campus;

            // Duration: 120 min for common humanities/language courses, 180 min for engineering/core courses
            int durationMinutes = isCommon ? 120 : (i % 3 == 0 ? 150 : 180);

            exams.add(new Exam(i, code, title, enrollment, examCampus, durationMinutes));
        }

        // Generate Rooms with building blocks
        List<Room> rooms = new ArrayList<>();
        int roomIdx = 0;

        if ("Gambang".equalsIgnoreCase(campus) || "Combined".equalsIgnoreCase(campus)) {
            rooms.add(new Room(roomIdx++, "GB-ASTAKA", "Dewan Astaka Gambang", 500, "Gambang", "ASTAKA"));
            rooms.add(new Room(roomIdx++, "GB-DK1", "Dewan Kuliah 1 Gambang", 200, "Gambang", "DK"));
            rooms.add(new Room(roomIdx++, "GB-DK2", "Dewan Kuliah 2 Gambang", 200, "Gambang", "DK"));
            rooms.add(new Room(roomIdx++, "GB-DK3", "Dewan Kuliah 3 Gambang", 180, "Gambang", "DK"));
            rooms.add(new Room(roomIdx++, "GB-BK01", "Bilik Kuliah 1 Gambang", 80, "Gambang", "BK"));
            rooms.add(new Room(roomIdx++, "GB-BK02", "Bilik Kuliah 2 Gambang", 80, "Gambang", "BK"));
            rooms.add(new Room(roomIdx++, "GB-BK03", "Bilik Kuliah 3 Gambang", 80, "Gambang", "BK"));
            rooms.add(new Room(roomIdx++, "GB-MK01", "Makmal Komputer 1", 60, "Gambang", "MK"));
            rooms.add(new Room(roomIdx++, "GB-MK02", "Makmal Komputer 2", 60, "Gambang", "MK"));
            rooms.add(new Room(roomIdx++, "GB-MK03", "Makmal Komputer 3", 60, "Gambang", "MK"));
            rooms.add(new Room(roomIdx++, "GB-AUD", "Auditorium Gambang", 250, "Gambang", "AUD"));
            rooms.add(new Room(roomIdx++, "GB-SEMINAR", "Bilik Seminar Gambang", 100, "Gambang", "SEM"));
        }

        if ("Pekan".equalsIgnoreCase(campus) || "Combined".equalsIgnoreCase(campus)) {
            rooms.add(new Room(roomIdx++, "PK-DSG", "Dewan Serbaguna Pekan", 600, "Pekan", "DSG"));
            rooms.add(new Room(roomIdx++, "PK-DKP1", "Dewan Kuliah Pekan 1", 250, "Pekan", "DKP"));
            rooms.add(new Room(roomIdx++, "PK-DKP2", "Dewan Kuliah Pekan 2", 250, "Pekan", "DKP"));
            rooms.add(new Room(roomIdx++, "PK-DKP3", "Dewan Kuliah Pekan 3", 200, "Pekan", "DKP"));
            rooms.add(new Room(roomIdx++, "PK-BK01", "Bilik Kuliah Pekan 1", 90, "Pekan", "BK"));
            rooms.add(new Room(roomIdx++, "PK-BK02", "Bilik Kuliah Pekan 2", 90, "Pekan", "BK"));
            rooms.add(new Room(roomIdx++, "PK-BK03", "Bilik Kuliah Pekan 3", 90, "Pekan", "BK"));
            rooms.add(new Room(roomIdx++, "PK-BK04", "Bilik Kuliah Pekan 4", 90, "Pekan", "BK"));
            rooms.add(new Room(roomIdx++, "PK-CAD1", "Makmal CAD/CAM Pekan", 70, "Pekan", "CAD"));
            rooms.add(new Room(roomIdx++, "PK-CAD2", "Makmal Robotik Pekan", 70, "Pekan", "CAD"));
            rooms.add(new Room(roomIdx++, "PK-AUD", "Auditorium Perpustakaan Pekan", 300, "Pekan", "AUD"));
            rooms.add(new Room(roomIdx++, "PK-SEM1", "Bilik Seminar Bestari Pekan", 120, "Pekan", "SEM"));
            rooms.add(new Room(roomIdx++, "PK-SEM2", "Bilik Seminar Gemilang Pekan", 120, "Pekan", "SEM"));
            rooms.add(new Room(roomIdx++, "PK-LAB-EE", "Makmal Elektrikal Pekan", 80, "Pekan", "LAB"));
        }

        // Generate Student Registrations & Build Conflict Matrix
        List<String[]> studentRegistrations = new ArrayList<>();
        int studentIdCounter = 10001;

        for (int i = 0; i < numExams; i++) {
            Exam e1 = exams.get(i);
            int enroll = e1.getStudentCount();
            for (int s = 0; s < enroll; s++) {
                String studId = "ST" + (studentIdCounter + s);
                studentRegistrations.add(new String[]{ studId, e1.getCode() });

                // Also enroll student into another course with reasonable probability
                if (rng.nextDouble() < 0.22 && i + 1 < numExams) {
                    int otherIdx = i + 1 + rng.nextInt(Math.min(5, numExams - i - 1));
                    studentRegistrations.add(new String[]{ studId, exams.get(otherIdx).getCode() });
                }
            }
            studentIdCounter += Math.max(1, (int)(enroll * 0.65));
        }

        int[][] conflictMatrix = ConflictMatrixBuilder.buildConflictMatrix(exams, studentRegistrations);

        return new ProblemInstance(id, name, semester, session, campus, totalPeriods, exams, rooms, conflictMatrix, totalStudents);
    }
}
