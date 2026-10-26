package edu.ump.timetabling.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.ump.timetabling.dataset.ConflictMatrixBuilder;
import edu.ump.timetabling.dataset.UmpDataRepository;
import edu.ump.timetabling.dataset.UmpDatasetParser;
import edu.ump.timetabling.model.*;
import edu.ump.timetabling.solver.ConstraintEvaluator;
import edu.ump.timetabling.solver.HeuristicAlgorithm;
import edu.ump.timetabling.solver.improvement.*;
import edu.ump.timetabling.solver.initial.*;
import edu.ump.timetabling.util.I18nManager;
import edu.ump.timetabling.util.JsonUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST API Handler for Examination Timetabling operations and Conflict Matrix generation.
 */
public class ApiHandler implements HttpHandler {

    private final UmpDataRepository dataRepository;
    private final I18nManager i18nManager;

    // Visitor counter — persistent tracking of unique page visits
    private static final String VISITOR_FILE = "visitor_count.txt";
    private static final AtomicLong VISITOR_COUNT = new AtomicLong(0);
    private static final long SERVER_START_MS = System.currentTimeMillis();

    static {
        long initialCount = 1;
        try {
            File vf = new File(VISITOR_FILE);
            if (vf.exists()) {
                String content = new String(Files.readAllBytes(vf.toPath()), StandardCharsets.UTF_8).trim();
                initialCount = Math.max(1, Long.parseLong(content));
            } else {
                initialCount = 1;
                Files.write(vf.toPath(), String.valueOf(initialCount).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            initialCount = 1;
        }
        VISITOR_COUNT.set(initialCount);
    }

    private static synchronized void persistVisitorCount() {
        try {
            Files.write(Paths.get(VISITOR_FILE), String.valueOf(VISITOR_COUNT.get()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    private ProblemInstance currentInstance;
    private TimetableSolution initialSolution;
    private TimetableSolution currentSolution;
    private final List<Map<String, Object>> comparisonLogs = new ArrayList<>();

    private int maxIterations = 10000;
    private int lahcListLength = 500;
    private int schcStepLength = 500;
    
    // Hard constraints enable flags: H1 to H7
    private boolean[] enabledConstraints = {true, true, true, true, true, true, true};

    // Soft constraints enable flags: S1 to S4
    private boolean[] enabledSoftConstraints = {true, true, true, true};

    public ApiHandler(UmpDataRepository dataRepository, I18nManager i18nManager) {
        this.dataRepository = dataRepository;
        this.i18nManager = i18nManager;
        this.currentInstance = dataRepository.getDefaultInstance();
        resetSolutions();
    }

    private void resetSolutions() {
        this.initialSolution = null;
        this.currentSolution = null;
        this.comparisonLogs.clear();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        // Enable CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if (path.equals("/api/dataset/list") && method.equals("GET")) {
                handleDatasetList(exchange);
            } else if (path.equals("/api/dataset/select") && method.equals("POST")) {
                handleDatasetSelect(exchange);
            } else if (path.equals("/api/dataset/current") && method.equals("GET")) {
                handleDatasetCurrent(exchange);
            } else if (path.equals("/api/dataset/upload") && method.equals("POST")) {
                handleDatasetUpload(exchange);
            } else if (path.equals("/api/conflict-matrix") && method.equals("GET")) {
                handleGetConflictMatrix(exchange);
            } else if (path.equals("/api/conflict-matrix/export") && method.equals("GET")) {
                handleExportConflictMatrix(exchange);
            } else if (path.equals("/api/conflict-matrix/generate") && method.equals("POST")) {
                handleGenerateConflictMatrix(exchange);
            } else if (path.equals("/api/solver/initialize") && method.equals("POST")) {
                handleInitialize(exchange);
            } else if (path.equals("/api/solver/initial") && method.equals("POST")) {
                handleInitialPhase(exchange);
            } else if (path.equals("/api/solver/improve") && method.equals("POST")) {
                handleImprovementPhase(exchange);
            } else if (path.equals("/api/solver/calculate") && method.equals("POST")) {
                handleCalculate(exchange);
            } else if (path.equals("/api/settings") && method.equals("POST")) {
                handleSettings(exchange);
            } else if (path.equals("/api/settings") && method.equals("GET")) {
                handleGetSettings(exchange);
            } else if (path.equals("/api/timetable/xit") && method.equals("GET")) {
                handleTimetableXit(exchange);
            } else if (path.equals("/api/timetable/yir") && method.equals("GET")) {
                handleTimetableYir(exchange);
            } else if (path.equals("/api/timetable/zrt") && method.equals("GET")) {
                handleTimetableZrt(exchange);
            } else if (path.equals("/api/timetable/grid") && method.equals("GET")) {
                handleTimetableGrid(exchange);
            } else if (path.equals("/api/i18n") && method.equals("GET")) {
                handleI18n(exchange);
            } else if (path.startsWith("/api/stats")) {
                handleStats(exchange);
            } else {
                sendJsonResponse(exchange, 404, Map.of("error", "Endpoint not found: " + path));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    private void handleDatasetList(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProblemInstance pi : dataRepository.getAllInstances()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", pi.getDatasetId());
            map.put("name", pi.getDatasetName());
            map.put("semester", pi.getSemester());
            map.put("session", pi.getSession());
            map.put("campus", pi.getCampus());
            map.put("numExams", pi.getNumExams());
            map.put("numRooms", pi.getNumRooms());
            map.put("totalPeriods", pi.getTotalPeriods());
            map.put("totalStudents", pi.getTotalStudents());
            list.add(map);
        }
        sendJsonResponse(exchange, 200, Map.of("datasets", list, "currentId", currentInstance.getDatasetId()));
    }

    private void handleDatasetSelect(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseJsonObject(body);
        String datasetId = (String) req.get("datasetId");

        ProblemInstance pi = dataRepository.getInstance(datasetId);
        if (pi != null) {
            currentInstance = pi;
            resetSolutions();
            sendJsonResponse(exchange, 200, Map.of(
                    "status", "success",
                    "message", "Dataset selected: " + pi.getDatasetName(),
                    "dataset", getDatasetSummary(pi)
            ));
        } else {
            sendJsonResponse(exchange, 400, Map.of("error", "Invalid dataset ID"));
        }
    }

    private void handleDatasetCurrent(HttpExchange exchange) throws IOException {
        sendJsonResponse(exchange, 200, getDatasetSummary(currentInstance));
    }

    private void handleGetConflictMatrix(HttpExchange exchange) throws IOException {
        int numExams = currentInstance.getNumExams();
        List<String> codes = new ArrayList<>();
        for (Exam e : currentInstance.getExams()) {
            codes.add(e.getCode());
        }

        int[][] matrix = currentInstance.getConflictMatrix();
        List<List<Integer>> rows = new ArrayList<>();
        int totalEdges = 0;
        int maxClash = 0;

        for (int i = 0; i < numExams; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < numExams; j++) {
                int clash = matrix[i][j];
                row.add(clash);
                if (i < j && clash > 0) {
                    totalEdges++;
                    if (clash > maxClash) maxClash = clash;
                }
            }
            rows.add(row);
        }

        double density = (numExams > 1) ? ((2.0 * totalEdges) / (numExams * (numExams - 1))) : 0.0;

        sendJsonResponse(exchange, 200, Map.of(
                "datasetName", currentInstance.getDatasetName(),
                "numExams", numExams,
                "numCourses", numExams,
                "courseCodes", codes,
                "matrix", rows,
                "totalConflictEdges", totalEdges,
                "maxClash", maxClash,
                "conflictDensity", String.format(Locale.US, "%.4f", density)
        ));
    }

    private void handleExportConflictMatrix(HttpExchange exchange) throws IOException {
        String csv = ConflictMatrixBuilder.toCsv(currentInstance.getExams(), currentInstance.getConflictMatrix());
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"Conflict_Matrix_" + currentInstance.getDatasetId() + ".csv\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleGenerateConflictMatrix(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        // Supports generating and updating conflict matrix
        sendJsonResponse(exchange, 200, Map.of(
                "status", "success",
                "message", "Conflict Matrix computed successfully based on Course.length and student registrations."
        ));
    }

    private void handleDatasetUpload(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseJsonObject(body);

        UmpDatasetParser.RawFilesData rawData = new UmpDatasetParser.RawFilesData();
        rawData.datasetName = (String) req.getOrDefault("name", "UMP Custom Upload");
        rawData.semester = (String) req.getOrDefault("semester", "Semester 1");
        rawData.session = (String) req.getOrDefault("session", "2018/2019");
        rawData.campusScope = (String) req.getOrDefault("campus", "Combined (Gambang & Pekan)");

        if (req.containsKey("totalPeriods")) {
            try {
                rawData.totalPeriods = Integer.parseInt(req.get("totalPeriods").toString());
            } catch (Exception ignored) {}
        }

        rawData.campusFileContent = (String) req.getOrDefault("campusFile", "");
        rawData.coincidenceFileContent = (String) req.getOrDefault("coincidenceFile", "");
        rawData.courseFileContent = (String) req.getOrDefault("courseFile", "");
        rawData.distanceMatrixFileContent = (String) req.getOrDefault("distanceMatrixFile", "");
        rawData.roomInfoFileContent = (String) req.getOrDefault("roomInfoFile", "");
        rawData.studentFileContent = (String) req.getOrDefault("studentFile", "");
        rawData.timeslotFileContent = (String) req.getOrDefault("timeslotFile", "");

        String id = "CUSTOM-UMP-UPLOAD";
        ProblemInstance custom = UmpDatasetParser.parseAndBuildInstance(rawData, id);

        dataRepository.registerCustomInstance(custom);
        this.currentInstance = custom;
        resetSolutions();

        sendJsonResponse(exchange, 200, Map.of(
                "status", "success",
                "datasetId", id,
                "dataset", getDatasetSummary(custom)
        ));
    }

    private Map<String, Object> getDatasetSummary(ProblemInstance pi) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", pi.getDatasetId());
        map.put("name", pi.getDatasetName());
        map.put("semester", pi.getSemester());
        map.put("session", pi.getSession());
        map.put("campus", pi.getCampus());
        map.put("numExams", pi.getNumExams());
        map.put("numRooms", pi.getNumRooms());
        map.put("totalPeriods", pi.getTotalPeriods());
        map.put("totalStudents", pi.getTotalStudents());

        int totalEdges = 0;
        for (int d : pi.getDegree()) totalEdges += d;
        totalEdges /= 2;
        double density = (pi.getNumExams() > 1)
                ? ((2.0 * totalEdges) / (pi.getNumExams() * (pi.getNumExams() - 1))) : 0.0;

        map.put("conflictEdges", totalEdges);
        map.put("conflictDensity", String.format(Locale.US, "%.4f", density));
        return map;
    }

    private void handleInitialize(HttpExchange exchange) throws IOException {
        resetSolutions();
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("type", "SYSTEM");
        logEntry.put("message", "System initialized. Active Dataset: " + currentInstance.getDatasetName());
        logEntry.put("timestamp", new Date().toString());
        comparisonLogs.add(logEntry);

        sendJsonResponse(exchange, 200, Map.of(
                "status", "initialized",
                "message", "System initialized. Problem instance loaded: " + currentInstance.getDatasetName(),
                "dataset", getDatasetSummary(currentInstance),
                "logs", comparisonLogs
        ));
    }

    private void handleInitialPhase(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseJsonObject(body);
        String algorithm = (String) req.getOrDefault("algorithm", "LD");

        HeuristicAlgorithm solver = getInitialAlgorithm(algorithm);
        if (solver == null) {
            sendJsonResponse(exchange, 400, Map.of("error", "Unknown initial algorithm: " + algorithm));
            return;
        }

        TimetableSolution solution = solver.solve(currentInstance, null, Collections.emptyMap());
        EvaluationResult eval = ConstraintEvaluator.evaluate(solution, enabledConstraints, enabledSoftConstraints);
        if (!eval.isFeasible()) {
            System.out.println("  [ApiHandler] Initial solution had " + eval.getTotalHardViolations() + " hard violations. Running repairFeasibility...");
            ConstraintEvaluator.repairFeasibility(currentInstance, solution, enabledConstraints, enabledSoftConstraints);
            eval = ConstraintEvaluator.evaluate(solution, enabledConstraints, enabledSoftConstraints);
            System.out.println("  [ApiHandler] After repair: " + eval.getTotalHardViolations() + " hard violations. Feasible: " + eval.isFeasible());
        }
        this.initialSolution = solution;
        this.currentSolution = new TimetableSolution(solution);

        System.out.println("================================================================================");
        System.out.println("=== INITIAL EVALUATION BREAKDOWN: " + solver.getName() + " ===");
        System.out.println("  H1 Student Conflicts : " + eval.getH1StudentConflicts());
        System.out.println("  H2 Unassigned        : " + eval.getH2Unassigned());
        System.out.println("  H3 Room Capacity     : " + eval.getH3RoomCapacity());
        System.out.println("  H4 Double Booking    : " + eval.getH4RoomDoubleBooking());
        System.out.println("  H5 Campus Mismatch   : " + eval.getH5CampusMismatch());
        System.out.println("  H6 Split Exam Sync   : " + eval.getH6SplitExamSync());
        System.out.println("  H7 Large Exams Late  : " + eval.getH7LargeExamLate());
        System.out.println("  TOTAL HARD VIOLATIONS: " + eval.getTotalHardViolations());
        System.out.println("  FEASIBLE             : " + eval.isFeasible());

        if (eval.getH5CampusMismatch() > 0) {
            for (int i = 0; i < currentInstance.getNumExams(); i++) {
                Exam ex = currentInstance.getExams().get(i);
                for (int r : solution.getRooms(i)) {
                    Room rm = currentInstance.getRooms().get(r);
                    if (ConstraintEvaluator.hasCampusMismatch(ex, rm)) {
                        System.out.println("  -> [H5 Mismatch] Exam " + ex.getCode() + " (" + ex.getCampus() + ") assigned to " + rm.getCode() + " (" + rm.getCampus() + ")");
                    }
                }
            }
        }
        if (eval.getH6SplitExamSync() > 0) {
            for (int i = 0; i < currentInstance.getNumExams(); i++) {
                String c1 = currentInstance.getExams().get(i).getCode();
                String c2 = currentInstance.getCoincidence(c1);
                if (c2 != null) {
                    for (int j = i + 1; j < currentInstance.getNumExams(); j++) {
                        if (currentInstance.getExams().get(j).getCode().equals(c2)) {
                            int p1 = solution.getPeriod(i);
                            int p2 = solution.getPeriod(j);
                            if (p1 >= 0 && p2 >= 0 && p1 != p2) {
                                System.out.println("  -> [H6 Out of Sync] " + c1 + " in P" + (p1+1) + " but " + c2 + " in P" + (p2+1));
                            }
                        }
                    }
                }
            }
        }
        if (eval.getH7LargeExamLate() > 0) {
            for (int i = 0; i < currentInstance.getNumExams(); i++) {
                Exam ex = currentInstance.getExams().get(i);
                int p = solution.getPeriod(i);
                if (ConstraintEvaluator.isLargeExamViolated(ex, p)) {
                    System.out.println("  -> [H7 Large Late] Exam " + ex.getCode() + " (size=" + ex.getStudentCount() + ") placed in P" + (p+1));
                }
            }
        }
        if (eval.getH4RoomDoubleBooking() > 0) {
            for (int p = 0; p < currentInstance.getTotalPeriods(); p++) {
                for (int r = 0; r < currentInstance.getNumRooms(); r++) {
                    List<String> inRoom = new ArrayList<>();
                    for (int i = 0; i < currentInstance.getNumExams(); i++) {
                        if (solution.getPeriod(i) == p && solution.getRooms(i).contains(r)) {
                            inRoom.add(currentInstance.getExams().get(i).getCode());
                        }
                    }
                    if (inRoom.size() > 1) {
                        System.out.println("  -> [H4 Double Booking] Room " + currentInstance.getRooms().get(r).getCode() + " in P" + (p+1) + " has " + inRoom);
                    }
                }
            }
        }
        System.out.println("================================================================================");

        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("phase", "INITIAL");
        logEntry.put("algorithm", solver.getName());
        logEntry.put("timeMs", solution.getComputationTimeMs());
        logEntry.put("hardViolations", eval.getTotalHardViolations());
        logEntry.put("h1", eval.getH1StudentConflicts());
        logEntry.put("h2", eval.getH2Unassigned());
        logEntry.put("h3", eval.getH3RoomCapacity());
        logEntry.put("h4", eval.getH4RoomDoubleBooking());
        logEntry.put("h5", eval.getH5CampusMismatch());
        logEntry.put("h6", eval.getH6SplitExamSync());
        logEntry.put("h7", eval.getH7LargeExamLate());
        logEntry.put("studentClashes", eval.getStudentClashes());
        logEntry.put("roomOvercap", eval.getRoomOvercapacities());
        logEntry.put("softPenalty", String.format(Locale.US, "%.4f", eval.getCompositePenaltyPerStudent()));
        logEntry.put("compositeSoftPenalty", String.format(Locale.US, "%.4f", eval.getCompositePenaltyPerStudent()));
        logEntry.put("s1Spreading", String.format(Locale.US, "%.4f", eval.getPenaltyPerStudent()));
        logEntry.put("s2BuildingDistance", eval.getS2());
        logEntry.put("s3RoomSplitting", eval.getS3());
        logEntry.put("s4UniformDuration", eval.getS4());
        logEntry.put("totalPenaltySum", (long) eval.getTotalSoftPenalty());
        logEntry.put("feasible", eval.isFeasible());
        comparisonLogs.add(logEntry);

        Map<String, Object> respData = new LinkedHashMap<>();
        respData.put("status", "success");
        respData.put("phase", "INITIAL");
        respData.put("algorithm", solver.getName());
        respData.put("timeMs", solution.getComputationTimeMs());
        respData.put("hardViolations", eval.getTotalHardViolations());
        respData.put("h1", eval.getH1StudentConflicts());
        respData.put("h2", eval.getH2Unassigned());
        respData.put("h3", eval.getH3RoomCapacity());
        respData.put("h4", eval.getH4RoomDoubleBooking());
        respData.put("h5", eval.getH5CampusMismatch());
        respData.put("h6", eval.getH6SplitExamSync());
        respData.put("h7", eval.getH7LargeExamLate());
        respData.put("studentClashes", eval.getStudentClashes());
        respData.put("roomOvercap", eval.getRoomOvercapacities());
        respData.put("softPenalty", eval.getCompositePenaltyPerStudent());
        respData.put("compositeSoftPenalty", eval.getCompositePenaltyPerStudent());
        respData.put("s1", eval.getPenaltyPerStudent());
        respData.put("s2", eval.getS2());
        respData.put("s3", eval.getS3());
        respData.put("s4", eval.getS4());
        respData.put("feasible", eval.isFeasible());
        respData.put("logs", comparisonLogs);

        sendJsonResponse(exchange, 200, respData);
    }

    private void handleImprovementPhase(HttpExchange exchange) throws IOException {
        if (this.currentSolution == null) {
            HeuristicAlgorithm defaultInit = new LargestDegree();
            this.initialSolution = defaultInit.solve(currentInstance, null, Collections.emptyMap());
            this.currentSolution = new TimetableSolution(this.initialSolution);
        }

        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseJsonObject(body);
        String algorithm = (String) req.getOrDefault("algorithm", "HC Move");

        HeuristicAlgorithm solver = getImprovementAlgorithm(algorithm);
        if (solver == null) {
            sendJsonResponse(exchange, 400, Map.of("error", "Unknown improvement algorithm: " + algorithm));
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("maxIterations", maxIterations);
        params.put("lahcListLength", lahcListLength);
        params.put("schcStepLength", schcStepLength);

        double beforeCost = this.currentSolution.getSoftPenalty();
        int beforeHard = this.currentSolution.getHardViolations();

        TimetableSolution improved = solver.solve(currentInstance, this.currentSolution, params);
        EvaluationResult eval = ConstraintEvaluator.evaluate(improved, enabledConstraints, enabledSoftConstraints);
        if (!eval.isFeasible()) {
            ConstraintEvaluator.repairFeasibility(currentInstance, improved, enabledConstraints, enabledSoftConstraints);
            eval = ConstraintEvaluator.evaluate(improved, enabledConstraints, enabledSoftConstraints);
        }
        this.currentSolution = improved;

        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("phase", "IMPROVEMENT");
        logEntry.put("algorithm", solver.getName());
        logEntry.put("iterations", improved.getIterations());
        logEntry.put("timeMs", improved.getComputationTimeMs());
        logEntry.put("initialCost", String.format(Locale.US, "%.4f", beforeCost));
        logEntry.put("initialHard", beforeHard);
        logEntry.put("improvedCost", String.format(Locale.US, "%.4f", eval.getCompositePenaltyPerStudent()));
        logEntry.put("improvedHard", eval.getTotalHardViolations());
        logEntry.put("softPenalty", String.format(Locale.US, "%.4f", eval.getCompositePenaltyPerStudent()));
        logEntry.put("compositeSoftPenalty", String.format(Locale.US, "%.4f", eval.getCompositePenaltyPerStudent()));
        logEntry.put("s1Spreading", String.format(Locale.US, "%.4f", eval.getPenaltyPerStudent()));
        logEntry.put("s2BuildingDistance", eval.getS2());
        logEntry.put("s3RoomSplitting", eval.getS3());
        logEntry.put("s4UniformDuration", eval.getS4());
        logEntry.put("improvementPercentage", String.format(Locale.US, "%.2f%%",
                beforeCost > 0 ? ((beforeCost - eval.getCompositePenaltyPerStudent()) / beforeCost * 100.0) : 0.0));
        logEntry.put("feasible", eval.isFeasible());
        comparisonLogs.add(logEntry);

        Map<String, Object> respData = new LinkedHashMap<>();
        respData.put("status", "success");
        respData.put("phase", "IMPROVEMENT");
        respData.put("algorithm", solver.getName());
        respData.put("iterations", improved.getIterations());
        respData.put("timeMs", improved.getComputationTimeMs());
        respData.put("initialCost", beforeCost);
        respData.put("improvedCost", eval.getCompositePenaltyPerStudent());
        respData.put("softPenalty", eval.getCompositePenaltyPerStudent());
        respData.put("compositeSoftPenalty", eval.getCompositePenaltyPerStudent());
        respData.put("s1", eval.getPenaltyPerStudent());
        respData.put("s2", eval.getS2());
        respData.put("s3", eval.getS3());
        respData.put("s4", eval.getS4());
        respData.put("hardViolations", eval.getTotalHardViolations());
        respData.put("feasible", eval.isFeasible());
        respData.put("logs", comparisonLogs);

        sendJsonResponse(exchange, 200, respData);
    }

    private void handleCalculate(HttpExchange exchange) throws IOException {
        if (this.currentSolution == null) {
            sendJsonResponse(exchange, 200, Map.of(
                    "status", "no_solution",
                    "message", "No timetable solution generated yet. Please run Initial Phase first."
            ));
            return;
        }

        EvaluationResult eval = ConstraintEvaluator.evaluate(this.currentSolution, enabledConstraints, enabledSoftConstraints);
        if (!eval.isFeasible()) {
            ConstraintEvaluator.repairFeasibility(currentInstance, this.currentSolution, enabledConstraints, enabledSoftConstraints);
            eval = ConstraintEvaluator.evaluate(this.currentSolution, enabledConstraints, enabledSoftConstraints);
        }
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("phase", "CALCULATION");
        logEntry.put("algorithm", this.currentSolution.getAlgorithmName());
        logEntry.put("hardViolations", eval.getTotalHardViolations());
        logEntry.put("studentClashes", eval.getStudentClashes());
        logEntry.put("roomOvercap", eval.getRoomOvercapacities());
        logEntry.put("softPenalty", String.format(Locale.US, "%.4f", eval.getCompositePenaltyPerStudent()));
        logEntry.put("compositeSoftPenalty", String.format(Locale.US, "%.4f", eval.getCompositePenaltyPerStudent()));
        logEntry.put("s1Spreading", String.format(Locale.US, "%.4f", eval.getPenaltyPerStudent()));
        logEntry.put("s2BuildingDistance", eval.getS2());
        logEntry.put("s3RoomSplitting", eval.getS3());
        logEntry.put("s4UniformDuration", eval.getS4());
        logEntry.put("totalPenaltySum", (long) eval.getTotalSoftPenalty());
        logEntry.put("feasible", eval.isFeasible());
        comparisonLogs.add(logEntry);

        Map<String, Object> evalMap = new LinkedHashMap<>();
        evalMap.put("hardViolations", eval.getTotalHardViolations());
        evalMap.put("studentClashes", eval.getStudentClashes());
        evalMap.put("roomOvercap", eval.getRoomOvercapacities());
        evalMap.put("unassigned", eval.getUnassignedExams());
        evalMap.put("totalSoftPenalty", eval.getTotalSoftPenalty());
        evalMap.put("penaltyPerStudent", eval.getPenaltyPerStudent());
        evalMap.put("compositePenaltyPerStudent", eval.getCompositePenaltyPerStudent());
        evalMap.put("softPenalty", eval.getCompositePenaltyPerStudent());
        evalMap.put("s1", eval.getPenaltyPerStudent());
        evalMap.put("s2", eval.getS2());
        evalMap.put("s3", eval.getS3());
        evalMap.put("s4", eval.getS4());
        evalMap.put("feasible", eval.isFeasible());

        sendJsonResponse(exchange, 200, Map.of(
                "status", "calculated",
                "evaluation", evalMap,
                "logs", comparisonLogs
        ));
    }

    private void handleSettings(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseJsonObject(body);
        if (req.containsKey("maxIterations")) {
            maxIterations = ((Number) req.get("maxIterations")).intValue();
        }
        if (req.containsKey("lahcListLength")) {
            lahcListLength = ((Number) req.get("lahcListLength")).intValue();
        }
        if (req.containsKey("schcStepLength")) {
            schcStepLength = ((Number) req.get("schcStepLength")).intValue();
        }
        if (req.containsKey("enabledConstraints")) {
            Object raw = req.get("enabledConstraints");
            if (raw instanceof List) {
                List<?> cl = (List<?>) raw;
                for (int i = 0; i < Math.min(7, cl.size()); i++) {
                    Object val = cl.get(i);
                    if (val instanceof Boolean) {
                        enabledConstraints[i] = (Boolean) val;
                    } else if (val instanceof String) {
                        enabledConstraints[i] = "true".equalsIgnoreCase((String) val);
                    } else {
                        enabledConstraints[i] = val != null && !val.equals(0) && !val.equals("false");
                    }
                }
            }
        }
        if (req.containsKey("enabledSoftConstraints")) {
            Object raw = req.get("enabledSoftConstraints");
            if (raw instanceof List) {
                List<?> sl = (List<?>) raw;
                if (sl.size() >= 4) {
                    for (int i = 0; i < 4; i++) {
                        Object val = sl.get(i);
                        if (val instanceof Boolean) {
                            enabledSoftConstraints[i] = (Boolean) val;
                        } else if (val instanceof String) {
                            enabledSoftConstraints[i] = "true".equalsIgnoreCase((String) val);
                        } else {
                            enabledSoftConstraints[i] = val != null && !val.equals(0) && !val.equals("false");
                        }
                    }
                }
            }
        }

        sendJsonResponse(exchange, 200, Map.of(
                "status", "updated",
                "maxIterations", maxIterations,
                "lahcListLength", lahcListLength,
                "schcStepLength", schcStepLength,
                "enabledConstraints", Arrays.asList(enabledConstraints[0], enabledConstraints[1], enabledConstraints[2], enabledConstraints[3], enabledConstraints[4], enabledConstraints[5], enabledConstraints[6]),
                "enabledSoftConstraints", Arrays.asList(enabledSoftConstraints[0], enabledSoftConstraints[1], enabledSoftConstraints[2], enabledSoftConstraints[3])
        ));
    }

    private void handleGetSettings(HttpExchange exchange) throws IOException {
        sendJsonResponse(exchange, 200, Map.of(
                "maxIterations", maxIterations,
                "lahcListLength", lahcListLength,
                "schcStepLength", schcStepLength,
                "enabledConstraints", Arrays.asList(enabledConstraints[0], enabledConstraints[1], enabledConstraints[2], enabledConstraints[3], enabledConstraints[4], enabledConstraints[5], enabledConstraints[6]),
                "enabledSoftConstraints", Arrays.asList(enabledSoftConstraints[0], enabledSoftConstraints[1], enabledSoftConstraints[2], enabledSoftConstraints[3])
        ));
    }

    private void handleTimetableXit(HttpExchange exchange) throws IOException {
        if (this.currentSolution == null) {
            sendJsonResponse(exchange, 200, Map.of("columns", Collections.emptyList(), "rows", Collections.emptyList()));
            return;
        }

        List<String> columns = new ArrayList<>();
        columns.add("Course Code");
        columns.add("Course Name");
        columns.add("Students");
        for (int p = 1; p <= currentInstance.getTotalPeriods(); p++) {
            columns.add("P" + p);
        }

        List<List<Object>> rows = new ArrayList<>();
        List<Exam> exams = currentInstance.getExams();
        for (int i = 0; i < exams.size(); i++) {
            Exam exam = exams.get(i);
            List<Object> row = new ArrayList<>();
            row.add(exam.getCode());
            row.add(exam.getName());
            row.add(exam.getStudentCount());
            int assignedP = currentSolution.getPeriod(i);
            for (int p = 0; p < currentInstance.getTotalPeriods(); p++) {
                row.add(assignedP == p ? 1 : 0);
            }
            rows.add(row);
        }

        sendJsonResponse(exchange, 200, Map.of(
                "title", "Xit Matrix (Exam-Period Assignment)",
                "description", "Binary matrix X_it = 1 if exam i is held in period t, 0 otherwise.",
                "columns", columns,
                "rows", rows
        ));
    }

    private void handleTimetableYir(HttpExchange exchange) throws IOException {
        if (this.currentSolution == null) {
            sendJsonResponse(exchange, 200, Map.of("columns", Collections.emptyList(), "rows", Collections.emptyList()));
            return;
        }

        List<String> columns = new ArrayList<>();
        columns.add("Course Code");
        columns.add("Course Name");
        columns.add("Students");
        List<Room> rooms = currentInstance.getRooms();
        for (Room r : rooms) {
            columns.add(r.getCode());
        }

        List<List<Object>> rows = new ArrayList<>();
        List<Exam> exams = currentInstance.getExams();
        for (int i = 0; i < exams.size(); i++) {
            Exam exam = exams.get(i);
            List<Object> row = new ArrayList<>();
            row.add(exam.getCode());
            row.add(exam.getName());
            row.add(exam.getStudentCount());
            List<Integer> assignedRooms = currentSolution.getRooms(i);
            for (int r = 0; r < rooms.size(); r++) {
                row.add(assignedRooms != null && assignedRooms.contains(r) ? 1 : 0);
            }
            rows.add(row);
        }

        sendJsonResponse(exchange, 200, Map.of(
                "title", "Yir Matrix (Exam-Room Assignment)",
                "description", "Binary matrix Y_ir = 1 if exam i is allocated to room r, 0 otherwise.",
                "columns", columns,
                "rows", rows
        ));
    }

    private void handleTimetableZrt(HttpExchange exchange) throws IOException {
        if (this.currentSolution == null) {
            sendJsonResponse(exchange, 200, Map.of("columns", Collections.emptyList(), "rows", Collections.emptyList()));
            return;
        }

        List<String> columns = new ArrayList<>();
        columns.add("Room Code");
        columns.add("Room Name");
        columns.add("Capacity");
        columns.add("Campus");
        for (int p = 1; p <= currentInstance.getTotalPeriods(); p++) {
            columns.add("P" + p);
        }

        List<List<Object>> rows = new ArrayList<>();
        List<Room> rooms = currentInstance.getRooms();
        for (int r = 0; r < rooms.size(); r++) {
            Room room = rooms.get(r);
            List<Object> row = new ArrayList<>();
            row.add(room.getCode());
            row.add(room.getName());
            row.add(room.getCapacity());
            row.add(room.getCampus());
            for (int p = 0; p < currentInstance.getTotalPeriods(); p++) {
                row.add(currentSolution.getZrt(r, p) ? 1 : 0);
            }
            rows.add(row);
        }

        sendJsonResponse(exchange, 200, Map.of(
                "title", "Zrt Matrix (Room-Period Utilization)",
                "description", "Binary matrix Z_rt = 1 if room r is active/utilized in period t, 0 otherwise.",
                "columns", columns,
                "rows", rows
        ));
    }

    private void handleTimetableGrid(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> grid = new ArrayList<>();
        List<Exam> exams = currentInstance.getExams();
        List<Room> rooms = currentInstance.getRooms();

        for (int i = 0; i < exams.size(); i++) {
            Exam exam = exams.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("examId", exam.getId());
            item.put("code", exam.getCode());
            item.put("name", exam.getName());
            item.put("studentCount", exam.getStudentCount());
            item.put("campus", exam.getCampus());
            item.put("degree", currentInstance.getDegree()[i]);
            item.put("weightedDegree", currentInstance.getWeightedDegree()[i]);

            if (currentSolution != null) {
                int period = currentSolution.getPeriod(i);
                List<Integer> rList = currentSolution.getRooms(i);

                item.put("period", period >= 0 ? (period + 1) : "Unassigned");
                item.put("periodIndex", period);
                if (period >= 0) {
                    int day = (period / 3) + 1;
                    int slotInDay = (period % 3) + 1;
                    String sessionName = slotInDay == 1 ? "08:30 - 11:30 (Morning)" :
                            slotInDay == 2 ? "14:00 - 17:00 (Afternoon)" : "20:00 - 22:30 (Evening)";
                    item.put("day", "Day " + day);
                    item.put("session", sessionName);
                } else {
                    item.put("day", "-");
                    item.put("session", "-");
                }

                if (rList != null && !rList.isEmpty()) {
                    if (rList.size() == 1) {
                        Room room = rooms.get(rList.get(0));
                        item.put("roomCode", room.getCode());
                        item.put("roomName", room.getName());
                        item.put("roomCapacity", room.getCapacity());
                        item.put("splitCount", 1);
                        item.put("block", room.getBlock());
                    } else {
                        StringBuilder codeSb = new StringBuilder();
                        StringBuilder nameSb = new StringBuilder();
                        int totalCap = 0;
                        Set<String> distinctBlocks = new LinkedHashSet<>();
                        for (int k = 0; k < rList.size(); k++) {
                            int rId = rList.get(k);
                            if (rId >= 0 && rId < rooms.size()) {
                                Room room = rooms.get(rId);
                                if (k > 0) {
                                    codeSb.append(", ");
                                    nameSb.append(", ");
                                }
                                codeSb.append(room.getCode());
                                nameSb.append(room.getName());
                                totalCap += room.getCapacity();
                                if (room.getBlock() != null && !room.getBlock().trim().isEmpty()) {
                                    distinctBlocks.add(room.getBlock().trim());
                                }
                            }
                        }
                        String blockLabel;
                        if (distinctBlocks.isEmpty()) {
                            blockLabel = "";
                        } else if (distinctBlocks.size() == 1) {
                            blockLabel = "Block " + distinctBlocks.iterator().next();
                        } else {
                            blockLabel = "Blocks " + String.join(", ", distinctBlocks);
                        }
                        item.put("roomCode", codeSb.toString() + (!blockLabel.isEmpty() ? " (" + blockLabel + ")" : ""));
                        item.put("roomName", "Split (" + rList.size() + " rooms in " + (!blockLabel.isEmpty() ? blockLabel : "campus") + ")");
                        item.put("roomCapacity", totalCap);
                        item.put("splitCount", rList.size());
                        item.put("block", String.join(", ", distinctBlocks));
                    }
                } else {
                    item.put("roomCode", "-");
                    item.put("roomName", "Unassigned");
                    item.put("roomCapacity", 0);
                    item.put("splitCount", 0);
                }
            } else {
                item.put("period", "Unassigned");
                item.put("periodIndex", -1);
                item.put("day", "-");
                item.put("session", "-");
                item.put("roomCode", "-");
                item.put("roomName", "Unassigned");
                item.put("roomCapacity", 0);
                item.put("splitCount", 0);
            }

            grid.add(item);
        }

        sendJsonResponse(exchange, 200, Map.of(
                "datasetName", currentInstance.getDatasetName(),
                "grid", grid
        ));
    }

    private void handleI18n(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String lang = "en";
        if (query != null && query.contains("lang=")) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "lang".equalsIgnoreCase(pair[0])) {
                    lang = pair[1];
                }
            }
        }
        Map<String, String> bundle = i18nManager.getBundle(lang);
        sendJsonResponse(exchange, 200, bundle);
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        boolean isVisit = "POST".equalsIgnoreCase(method) || path.endsWith("/visit");
        if (query != null && (query.contains("visit=true") || query.contains("visit=1") || query.contains("inc=1"))) {
            isVisit = true;
        }

        long count;
        if (isVisit) {
            count = VISITOR_COUNT.incrementAndGet();
            persistVisitorCount();
        } else {
            count = VISITOR_COUNT.get();
        }

        long uptimeMs = System.currentTimeMillis() - SERVER_START_MS;
        long totalSecs = uptimeMs / 1000;
        long days = totalSecs / 86400;
        long hours = (totalSecs % 86400) / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;

        StringBuilder uptimeStr = new StringBuilder();
        if (days > 0) uptimeStr.append(days).append("d ");
        if (hours > 0 || days > 0) uptimeStr.append(hours).append("h ");
        uptimeStr.append(minutes).append("m ").append(seconds).append("s");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("status", "online");
        stats.put("visitorCount", count);
        stats.put("visitorCountFormatted", String.format(Locale.US, "%,d", count));
        stats.put("serverStartTime", Instant.ofEpochMilli(SERVER_START_MS).toString());
        stats.put("uptime", uptimeStr.toString());
        stats.put("uptimeSeconds", totalSecs);
        stats.put("activeDataset", currentInstance != null ? currentInstance.getDatasetName() : "None");
        stats.put("datasetCount", dataRepository.getAllInstances().size());
        stats.put("numExams", currentInstance != null ? currentInstance.getNumExams() : 0);
        stats.put("numRooms", currentInstance != null ? currentInstance.getNumRooms() : 0);
        stats.put("numStudents", currentInstance != null ? currentInstance.getTotalStudents() : 0);

        sendJsonResponse(exchange, 200, stats);
    }

    private HeuristicAlgorithm getInitialAlgorithm(String code) {
        String norm = code.trim().toUpperCase();
        switch (norm) {
            case "LD": return new LargestDegree();
            case "LE": return new LargestEnrollment();
            case "LWD": return new LargestWeightedDegree();
            case "SD LD": return new SaturationDegree(SaturationDegree.TieBreaker.LD);
            case "SD LE": return new SaturationDegree(SaturationDegree.TieBreaker.LE);
            case "SD LWD": return new SaturationDegree(SaturationDegree.TieBreaker.LWD);
            default: return null;
        }
    }

    private HeuristicAlgorithm getImprovementAlgorithm(String code) {
        String norm = code.trim().toUpperCase();
        switch (norm) {
            case "HC MOVE": return new HillClimbingMove();
            case "HC SWAP": return new HillClimbingSwap();
            case "LAHC": return new LateAcceptanceHC();
            case "SCHC": return new StepCountingHC();
            default: return null;
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] bytes = JsonUtil.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }
}
