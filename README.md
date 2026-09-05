# Academic Examination Timetabling Research System (Java Full-Stack)

[![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Donate-orange.svg?style=for-the-badge&logo=buy-me-a-coffee)](https://buymeacoffee.com/selenazara)

A full-stack, research-grade Java web application replicating the classic desktop GUI for academic examination timetabling optimization. The system is built specifically to address and solve real-world, multi-campus benchmark problems based on Universiti Malaysia Pahang (UMP) examination datasets.

---

## Overview

The platform models complex multi-campus examination scheduling across separate campus venues (Gambang Campus, Pekan Campus, and Combined Faculties) over 30 discrete examination time slots (10 days × 3 sessions/day).

It implements a **two-phase optimization framework**:
1. **Initial Phase:** Uses Graph Ordering Heuristics to construct valid, initial feasible solutions.
2. **Improvement Phase:** Applies Metaheuristic Local Search algorithms to minimize soft constraint penalty costs.

---

## Architecture & Project Structure

```text
.
├── pom.xml                                  # Maven configuration
├── run.bat                                  # Quick build & launch script (Windows)
├── run.ps1                                  # PowerShell launcher
├── README.md                                # System documentation
├── src/
│   └── main/
│       ├── java/
│       │   └── edu/ump/timetabling/
│       │       ├── Main.java                # Entrypoint & HTTP Server (Port 8080)
│       │       ├── controller/
│       │       │   ├── ApiHandler.java          # REST routing & JSON endpoints
│       │       │   └── StaticFileHandler.java   # Serves UI assets & i18n
│       │       ├── model/
│       │       │   ├── Exam.java                # Course/exam details & student enrollments
│       │       │   ├── Room.java                # Room ID, name, capacity, campus
│       │       │   ├── ProblemInstance.java     # Dataset container with conflict matrix & degree metrics
│       │       │   ├── TimetableSolution.java   # X_it, Y_ir, Z_rt representations & assignment state
│       │       │   └── EvaluationResult.java    # Hard violations & soft penalty metrics
│       │       ├── dataset/
│       │       │   ├── ConflictMatrixBuilder.java # Builds symmetric conflict matrix [Course.length][Course.length]
│       │       │   └── UmpDataRepository.java   # Internal UMP benchmark datasets
│       │       ├── solver/
│       │       │   ├── ConstraintEvaluator.java # Hard (0-conflict, room cap) & Soft Carter proximity
│       │       │   ├── HeuristicAlgorithm.java  # Common solver interface
│       │       │   ├── initial/                 # Initial Phase Graph Heuristics
│       │       │   │   ├── LargestDegree.java           (LD)
│       │       │   │   ├── LargestEnrollment.java       (LE)
│       │       │   │   ├── LargestWeightedDegree.java   (LWD)
│       │       │   │   └── SaturationDegree.java        (SD LD, SD LE, SD LWD)
│       │       │   └── improvement/             # Improvement Phase Metaheuristics
│       │       │       ├── HillClimbingMove.java        (HC Move)
│       │       │       ├── HillClimbingSwap.java        (HC Swap)
│       │       │       ├── LateAcceptanceHC.java        (LAHC)
│       │       │       └── StepCountingHC.java          (SCHC)
│       │       └── util/
│       │           ├── JsonUtil.java            # Lightweight JSON serializer & parser
│       │           └── I18nManager.java         # Multi-language bundle loader
│       └── resources/
│           ├── i18n/
│           │   ├── messages_en.properties       # English dictionary
│           │   ├── messages_ms.properties       # Bahasa Melayu dictionary
│           │   └── messages_zh.properties       # Chinese dictionary
│           └── web/
│               ├── index.html                   # Vertical scrollable web UI
│               ├── css/style.css                # Desktop GUI styling (bordered fieldsets, bevels)
│               └── js/
│                   ├── app.js                   # UI controllers & AJAX communication
│                   ├── i18n.js                  # Dynamic live language switching
│                   └── timetable-grid.js        # DataGrid renderer for X_it, Y_ir, Z_rt & results
```

---

## Algorithms Implemented

### Graph Heuristics (Initial Phase)

The initial construction phase features **6 distinct graph ordering heuristics** to establish initial feasible solutions:

| Heuristic | Description |
| :--- | :--- |
| **LD** (Largest Degree) | Prioritizes exams involved in the largest number of conflict pairs. |
| **LE** (Largest Enrollment) | Prioritizes exams with the highest number of registered students. |
| **LWD** (Largest Weighted Degree) | Prioritizes exams based on conflicts weighted by total student overlap. |
| **SD LD** (Saturation Degree + LD) | Prioritizes exams with the fewest available valid time slots; breaks ties using LD. |
| **SD LE** (Saturation Degree + LE) | Prioritizes exams with the fewest available valid time slots; breaks ties using LE. |
| **SD LWD** (Saturation Degree + LWD) | Prioritizes exams with the fewest available valid time slots; breaks ties using LWD. |

### Metaheuristics (Improvement Phase)

* **Hill Climbing (HC) — Move Strategy**
* **Hill Climbing (HC) — Swap Strategy**
* **Late Acceptance Hill Climbing (LAHC)**
* **Step-Counting Hill Climbing (SCHC)**

---

## Constraints Framework

### Hard Constraints (Must Be Satisfied)

| ID | Constraint | Description |
| :---: | :--- | :--- |
| **H1** | Clash-Free | No student can be scheduled for two exams simultaneously. |
| **H2** | Single Assignment | Every exam must be assigned exactly once within available time slots. |
| **H3** | Room Capacity | Combined student capacity must fit within assigned exam venues. |
| **H4** | Room Availability | Allocated venue count must not exceed total active campus rooms. |
| **H5** | Campus Allocation | Exams must be assigned to their correct campus location. |
| **H6** | Cross-Campus Sync | Identical shared courses across campuses must share identical timeslots. |
| **H7** | Large Exams Early | High-enrollment exams (>400 students) are restricted to the first 10 slots. |

### Soft Constraints (Penalty Minimization)

| ID | Constraint | Description |
| :---: | :--- | :--- |
| **S1** | Exam Spreading | Distribute student exams evenly across periods to mitigate back-to-back testing. |
| **S2** | Building Proximity | Ensure multi-room split exams utilize adjacent rooms in the same building. |
| **S3** | Minimization of Room Splitting | Discourage dividing single courses across multiple rooms. |
| **S4** | Uniform Duration | Prevent mixing exams of unequal durations in the same room. |

---

## Web Layout Structure (Vertical Scrollable Flow)

### Header Bar
* System title and language selector (English, Bahasa Melayu, Chinese)
* Campus dataset selector (Gambang, Pekan, Combined)

### Section 1: Conflict Matrix Generator & Dataset Management
* **[Generate Conflict Matrix]** — Computes symmetric matrix `conflictMatrix = new int[Course.length][Course.length]`
* **[Export Conflict Matrix (.CSV)]** — Exports the calculated matrix
* **[Upload Dataset (Excel / CSV)]** — Allows importing external instances
* Interactive 2D Conflict Matrix preview grid with conflict statistics

### Section 2: Timetable Optimization Workbench
* **Left Control Panel:** Configurations (Max Iterations, LAHC List Length *L_fa*, SCHC Step Length *L_s*)
* **Initial Phase:** LD, LE, LWD, SD LD, SD LE, SD LWD execution
* **Improvement Phase:** HC Move, HC Swap, LAHC, SCHC optimization
* **Right Canvas:** Real-time execution log terminal, feasibility check, penalty metrics, and runtime statistics

### Section 3: Timetable Action Bar & Decision Matrices
* **[Xit Timetable]** — Binary exam-to-period assignment matrix viewer
* **[Yir Timetable]** — Binary exam-to-room assignment matrix viewer
* **[Zrt Timetable]** — Binary room-to-period utilization matrix viewer
* **[Calculate]** — Evaluates full mathematical objective functions and soft penalty costs

### Section 4: Examination Schedule DataGrid
* Scrollable data grid showing courses, enrollments, assigned periods (*t ∈ 1..30*), room details, day/session breakdowns, and conflict indicators

---

## Algorithm Performance & Results

Benchmark evaluations on internal UMP campus datasets:

| Algorithm / Heuristic | Semester 1 Penalty Cost | Semester 2 Penalty Cost |
| :--- | :---: | :---: |
| **Initial Phase (Best Graph Heuristic)** | *[Internal Data]* | *[Internal Data]* |
| **Hill Climbing (HC) — Move** | *[Internal Data]* | *[Internal Data]* |
| **Hill Climbing (HC) — Swap** | *[Internal Data]* | *[Internal Data]* |
| **Late Acceptance Hill Climbing (LAHC)** | *[Internal Data]* | *[Internal Data]* |
| **Step-Counting Hill Climbing (SCHC)** | *[Internal Data]* | *[Internal Data]* |

---

## How to Build & Run

### Option 1: Using Windows Batch Script
```bat
run.bat
```

### Option 2: Using PowerShell
```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

### Option 3: Using Maven
```bash
mvn compile exec:java -Dexec.mainClass="edu.ump.timetabling.Main"
```

Once running, open your web browser at **[http://localhost:8080](http://localhost:8080)**.

---

## Support

If you find this project useful or would like to support further development:

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Donate-orange.svg?style=for-the-badge&logo=buy-me-a-coffee)](https://buymeacoffee.com/selenazara)

---

## References & Citation

If you use this work or framework in your research, please cite:

```bibtex
@article{shokkan2020solving,
  title={Solving UMP Examination Timetabling Problem Using Metaheuristic Method},
  author={Shokkan, F. N. M. and Kahar, M. N. M.},
  journal={Faculty of Computing, Universiti Malaysia Pahang},
  year={2020}
}
```

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
