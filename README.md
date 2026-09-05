# Academic Examination Timetabling Research System (Java Full-Stack)

A full-stack, research-grade Java web application replicating the classic desktop GUI for academic examination timetabling optimization.

This platform models the **Universiti Malaysia Pahang (UMP)** examination datasets for **Session 2018/2019 (Semester 1 & Semester 2)** across **Gambang Campus**, **Pekan Campus**, and **Combined Faculties** over 30 discrete examination time slots (10 days $\times$ 3 sessions/day).

---

## 1. System Architecture & Components

```
d:\Coding\Java\examination-timetabling-problem/
├── pom.xml                                      # Maven configuration
├── run.bat                                      # Quick build & launch script (Windows)
├── run.ps1                                      # PowerShell launcher
├── README.md                                    # System documentation
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── edu/ump/timetabling/
│   │   │       ├── Main.java                    # Entrypoint & HTTP Server (Port 8080)
│   │   │       ├── controller/
│   │   │       │   ├── ApiHandler.java          # REST routing & JSON endpoints
│   │   │       │   └── StaticFileHandler.java   # Serves UI assets & i18n
│   │   │       ├── model/
│   │   │       │   ├── Exam.java                # Course/exam details & student enrollments
│   │   │       │   ├── Room.java                # Room ID, name, capacity, campus
│   │   │       │   ├── ProblemInstance.java     # Dataset container with conflict matrix & degree metrics
│   │   │       │   ├── TimetableSolution.java   # X_it, Y_ir, Z_rt representations & assignment state
│   │   │       │   └── EvaluationResult.java    # Hard violations & soft penalty metrics
│   │   │       ├── dataset/
│   │   │       │   ├── ConflictMatrixBuilder.java # Builds symmetric conflict matrix [Course.length][Course.length]
│   │   │       │   └── UmpDataRepository.java   # UMP 2018/2019 Sem 1 & Sem 2 datasets
│   │   │       ├── solver/
│   │   │       │   ├── ConstraintEvaluator.java # Hard (0-conflict, room cap) & Soft Carter proximity
│   │   │       │   ├── HeuristicAlgorithm.java  # Common solver interface
│   │   │       │   ├── initial/                 # Initial Phase Graph Heuristics
│   │   │       │   │   ├── LargestDegree.java           (LD)
│   │   │       │   │   ├── LargestEnrollment.java       (LE)
│   │   │       │   │   ├── LargestWeightedDegree.java   (LWD)
│   │   │       │   │   └── SaturationDegree.java        (SD LD, SD LE, SD LWD)
│   │   │       │   └── improvement/             # Improvement Phase Metaheuristics
│   │   │       │       ├── HillClimbingMove.java        (HC Move)
│   │   │       │       ├── HillClimbingSwap.java        (HC Swap)
│   │   │       │       ├── LateAcceptanceHC.java        (LAHC)
│   │   │       │       └── StepCountingHC.java          (SCHC)
│   │   │       └── util/
│   │   │           ├── JsonUtil.java            # Lightweight JSON serializer & parser
│   │   │           └── I18nManager.java         # Multi-language bundle loader
│   │   └── resources/
│   │       ├── i18n/
│   │       │   ├── messages_en.properties       # English dictionary
│   │       │   ├── messages_ms.properties       # Bahasa Melayu dictionary
│   │       │   └── messages_zh.properties       # Chinese dictionary
│   │       └── web/
│   │           ├── index.html                   # Vertical scrollable web UI with section dividers
│   │           ├── css/style.css                # Classic desktop GUI styling (bordered fieldsets, bevels)
│   │           └── js/
│   │               ├── app.js                   # UI controllers, event handlers, AJAX communication
│   │               ├── i18n.js                  # Dynamic live language switching
│   │               └── timetable-grid.js        # DataGrid renderer for X_it, Y_ir, Z_rt & results
```

---

## 2. Web Layout Structure (Vertical Scrollable Flow)

1. **Header Bar**:
   - System title and subtitle.
   - Dataset selector (UMP 2018/2019 Sem 1/2 Gambang, Pekan, Combined).
   - Language selector (English, Bahasa Melayu, Chinese).

2. **Section 1: Conflict Matrix Generator & Dataset Management**:
   - `[Generate Conflict Matrix]`: Computes symmetric matrix `conflictMatrix = new int[Course.length][Course.length]`.
   - `[Export Conflict Matrix (.CSV)]`: Exports the matrix to standard `.csv`.
   - `[Upload Dataset (Excel / CSV)]`: Allows researchers to upload their own datasets.
   - Interactive 2D Conflict Matrix preview grid with clash statistics.

3. **Section 2: Timetable Optimization Workbench**:
   - Left Control Panel: `Initialize` & `Setting` (Max Iterations, LAHC List Length $L_{fa}$, SCHC Step Length $L_s$).
   - Initial Phase: `LD`, `LE`, `LWD`, `SD LD`, `SD LE`, `SD LWD` + `[Generate]`.
   - Improvement Phase: `HC Move`, `HC Swap`, `LAHC`, `SCHC` + `[Generate]`.
   - Right Canvas: Real-time comparison terminal log, feasibility status, soft penalties, and time.

4. **Section 3: Timetable Action Bar & Decision Matrices**:
   - `[Xit Timetable]`: Binary exam-to-period assignment matrix viewer with CSV export.
   - `[Yir Timetable]`: Binary exam-to-room assignment matrix viewer with CSV export.
   - `[Zrt Timetable]`: Binary room-to-period utilization matrix viewer with CSV export.
   - `[Calculate]`: Evaluates the full mathematical objective function and constraint violations.

5. **Section 4: Examination Schedule DataGrid**:
   - Full-width scrollable data grid showing courses, names, enrollments, periods ($t \in 1..30$), rooms ($r$), day/session breakdown, and conflict status.

---

## 3. How to Build & Run

### Method 1: Using Windows Batch Script (Zero-Dependency)
```cmd
run.bat
```

### Method 2: Using PowerShell
```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

### Method 3: Using Maven
```bash
mvn compile exec:java -Dexec.mainClass="edu.ump.timetabling.Main"
```

Open your browser at **`http://localhost:8080`**.
