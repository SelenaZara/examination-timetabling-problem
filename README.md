# Academic Examination Timetabling Research System

[![Java Version](https://img.shields.io/badge/Java-11%2B-blue.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36.svg?logo=apachemaven)](https://maven.apache.org/)
[![Version](https://img.shields.io/badge/Version-2.4.0-informational.svg)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Donate-orange.svg?logo=buy-me-a-coffee)](https://buymeacoffee.com/selenazara)

A full-stack, research-grade Java web application for academic examination timetabling optimization. The system is built specifically to address and solve real-world, multi-campus benchmark problems based on **Universiti Malaysia Pahang (UMP)** examination datasets from Session 2018/2019.

---

## Overview

The platform models complex multi-campus examination scheduling across two separate campus venues — **Gambang Campus** and **Pekan Campus** — as well as a combined view, over **30 discrete examination time slots** (10 days × 3 sessions per day: Morning, Afternoon, Evening).

It implements a **two-phase optimization framework**:

1. **Initial Phase** — Uses Graph Ordering Heuristics to construct valid, feasible timetable solutions.
2. **Improvement Phase** — Applies Metaheuristic Local Search algorithms to minimize soft constraint penalty costs.

No external libraries are required — the entire system is built on the **Java standard library only**, including the built-in `com.sun.net.httpserver.HttpServer` for serving the web interface.

---

## Prerequisites

| Requirement | Version |
| :--- | :--- |
| **Java JDK** | 11 or higher |
| **Apache Maven** | 3.6+ *(only for Maven build option)* |

---

## Architecture & Project Structure

```text
.
├── pom.xml                                  # Maven configuration (v2.4.0)
├── run.bat                                  # Quick build & launch script (Windows CMD)
├── run.ps1                                  # Quick build & launch script (PowerShell)
├── README.md                                # System documentation
└── src/
    └── main/
        ├── java/
        │   └── edu/ump/timetabling/
        │       ├── Main.java                # Entrypoint & HTTP Server (Port 8080)
        │       ├── controller/
        │       │   ├── ApiHandler.java          # REST routing & JSON endpoints
        │       │   └── StaticFileHandler.java   # Serves web UI assets
        │       ├── model/
        │       │   ├── Exam.java                # Course/exam details & student enrollments
        │       │   ├── Room.java                # Room ID, name, capacity, campus, building
        │       │   ├── ProblemInstance.java     # Dataset container with conflict matrix & degree metrics
        │       │   ├── TimetableSolution.java   # X_it, Y_ir, Z_rt representations & assignment state
        │       │   └── EvaluationResult.java    # Hard violations & soft penalty metrics
        │       ├── dataset/
        │       │   ├── ConflictMatrixBuilder.java  # Builds symmetric conflict matrix
        │       │   ├── UmpDataRepository.java      # 6 built-in UMP benchmark dataset instances
        │       │   └── UmpDatasetParser.java        # Parser for user-uploaded datasets (Excel/CSV)
        │       ├── solver/
        │       │   ├── ConstraintEvaluator.java    # Hard (clash-free, room cap) & Soft (Carter proximity)
        │       │   ├── HeuristicAlgorithm.java     # Common solver interface
        │       │   ├── initial/                    # Initial Phase — Graph Heuristics
        │       │   │   ├── LargestDegree.java              (LD)
        │       │   │   ├── LargestEnrollment.java          (LE)
        │       │   │   ├── LargestWeightedDegree.java      (LWD)
        │       │   │   └── SaturationDegree.java           (SD LD, SD LE, SD LWD)
        │       │   └── improvement/                # Improvement Phase — Metaheuristics
        │       │       ├── HillClimbingMove.java           (HC Move)
        │       │       ├── HillClimbingSwap.java           (HC Swap)
        │       │       ├── LateAcceptanceHC.java           (LAHC)
        │       │       └── StepCountingHC.java             (SCHC)
        │       └── util/
        │           ├── JsonUtil.java            # Lightweight JSON serializer & parser (zero dependencies)
        │           └── I18nManager.java         # Multi-language bundle loader
        └── resources/
            ├── i18n/
            │   ├── messages_en.properties       # English
            │   ├── messages_ms.properties       # Bahasa Melayu
            │   └── messages_zh.properties       # Chinese (Simplified)
            └── web/
                ├── index.html                   # Single-page scrollable web UI
                ├── css/style.css                # Desktop GUI styling
                └── js/
                    ├── app.js                   # UI controllers & AJAX communication
                    ├── i18n.js                  # Live language switching
                    └── timetable-grid.js        # DataGrid renderer for X_it, Y_ir, Z_rt & results
```

---

## Built-in Datasets

Six benchmark instances from UMP Session **2018/2019** are pre-loaded:

| Dataset ID | Description | Exams | Rooms | Total Students |
| :--- | :--- | :---: | :---: | :---: |
| `UMP-20182019-1-GAMBANG` | Sem 1 — Gambang Campus | 45 | 12 | 1,850 |
| `UMP-20182019-1-PEKAN` | Sem 1 — Pekan Campus | 52 | 14 | 2,100 |
| `UMP-20182019-1-ALL` | Sem 1 — Combined (Gambang & Pekan) | 97 | 26 | 3,950 |
| `UMP-20182019-2-GAMBANG` | Sem 2 — Gambang Campus | 48 | 12 | 1,920 |
| `UMP-20182019-2-PEKAN` | Sem 2 — Pekan Campus | 55 | 14 | 2,250 |
| `UMP-20182019-2-ALL` | Sem 2 — Combined (Gambang & Pekan) | 103 | 26 | 4,170 |

Custom datasets can also be uploaded via the UI (Excel / CSV format).

---

## Algorithms Implemented

### Graph Heuristics — Initial Phase

| Heuristic | Description |
| :--- | :--- |
| **LD** (Largest Degree) | Prioritizes exams with the largest number of conflict pairs. |
| **LE** (Largest Enrollment) | Prioritizes exams with the highest number of registered students. |
| **LWD** (Largest Weighted Degree) | Prioritizes exams based on conflicts weighted by total student overlap. |
| **SD LD** (Saturation Degree + LD) | Fewest available valid timeslots first; ties broken by LD. |
| **SD LE** (Saturation Degree + LE) | Fewest available valid timeslots first; ties broken by LE. |
| **SD LWD** (Saturation Degree + LWD) | Fewest available valid timeslots first; ties broken by LWD. |

### Metaheuristics — Improvement Phase

| Algorithm | Description |
| :--- | :--- |
| **HC Move** | Hill Climbing with single-exam period move neighbourhood. |
| **HC Swap** | Hill Climbing with pairwise exam period swap neighbourhood. |
| **LAHC** | Late Acceptance Hill Climbing — accepts moves based on a historical cost list of length *L_fa*. |
| **SCHC** | Step-Counting Hill Climbing — accepts moves until a step-count threshold *L_s* is reached. |

---

## Constraints Framework

### Hard Constraints (Must Be Satisfied)

| ID | Constraint | Description |
| :---: | :--- | :--- |
| **H1** | Clash-Free | No student can sit two exams at the same time slot. |
| **H2** | Single Assignment | Every exam must be assigned exactly once. |
| **H3** | Room Capacity | Combined student count must not exceed venue capacity. |
| **H4** | Room Availability | Allocated rooms must not exceed total available rooms per period. |
| **H5** | Campus Allocation | Exams must be assigned only to rooms on their designated campus. |
| **H6** | Cross-Campus Sync | Shared common courses across campuses must share the same timeslot. |
| **H7** | Large Exams Early | High-enrollment exams (>400 students) are restricted to slots 1–10. |

### Soft Constraints (Penalty Minimization)

| ID | Constraint | Description |
| :---: | :--- | :--- |
| **S1** | Exam Spreading | Distribute student exams evenly to reduce back-to-back scheduling. |
| **S2** | Building Proximity | Multi-room split exams should use adjacent rooms in the same building. |
| **S3** | Minimize Room Splitting | Discourage splitting a single course across multiple rooms. |
| **S4** | Uniform Duration | Avoid mixing exams of different durations in the same room. |

---

## Web Interface

The UI is a single-page, vertically-scrollable web application served locally at `http://localhost:8080`.

### Header Bar
- System title and **live language switcher** (English / Bahasa Melayu / Chinese)
- Campus & dataset selector

### Section 1 — Conflict Matrix Generator
- **[Generate Conflict Matrix]** — Computes symmetric matrix `int[numExams][numExams]`
- **[Export Conflict Matrix (.CSV)]** — Downloads the computed matrix
- **[Upload Dataset (Excel / CSV)]** — Import external problem instances
- Interactive 2D conflict matrix preview grid with statistics

### Section 2 — Timetable Optimization Workbench
- **Left Panel:** Algorithm configuration (Max Iterations, LAHC list length *L_fa*, SCHC step size *L_s*)
- **Initial Phase controls:** LD, LE, LWD, SD LD, SD LE, SD LWD
- **Improvement Phase controls:** HC Move, HC Swap, LAHC, SCHC
- **Right Panel:** Real-time execution log, feasibility status, penalty metrics, and runtime statistics

### Section 3 — Decision Matrix Viewers
- **[X_it Timetable]** — Binary exam-to-period assignment matrix
- **[Y_ir Timetable]** — Binary exam-to-room assignment matrix
- **[Z_rt Timetable]** — Binary room-to-period utilization matrix
- **[Calculate]** — Full objective function evaluation with soft penalty breakdown

### Section 4 — Examination Schedule Grid
- Scrollable data grid: course code, enrollment, assigned period (*t ∈ 1..30*), room(s), day, session, and conflict indicators

---

## How to Build & Run

### Option 1: Windows Batch Script *(Recommended — no Maven required)*
```bat
run.bat
```

### Option 2: PowerShell
```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

### Option 3: Maven
```bash
mvn package -q
java -jar target/examination-timetabling-problem-2.4.0.jar
```

### Option 4: Plain `javac` / `java`
```bash
mkdir bin
javac -encoding UTF-8 -d bin -sourcepath src/main/java src/main/java/edu/ump/timetabling/Main.java ...
java -cp bin edu.ump.timetabling.Main 8080
```

Once running, open your browser at **[http://localhost:8080](http://localhost:8080)**.

> **Custom port:** Pass a port number as the first argument, e.g. `java -jar ... 9090`.

---

## Support

If you find this project useful or would like to support further development:

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Donate-orange.svg?style=for-the-badge&logo=buy-me-a-coffee)](https://buymeacoffee.com/selenazara)

---

## About This Project

This system was developed during my postgraduate studies at Universiti Malaysia Pahang (UMP). Due to personal circumstances, I was unable to complete my postgraduate programme — but rather than leaving this work behind, I have chosen to release it publicly so it can benefit researchers, students, and developers around the world.

I hope this project serves as a useful reference or foundation for anyone working on examination timetabling, combinatorial optimization, or related academic scheduling problems.

---

## Acknowledgements

A heartfelt thank you to my supervisor **Profesor Madya Dr. Mohd Nizam Bin Mohmad Kahar** from Universiti Malaysia Pahang (UMP) for his invaluable guidance, mentorship, and patience throughout this research. His expertise in the domain and generous sharing of the UMP examination dataset made this project possible.

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
