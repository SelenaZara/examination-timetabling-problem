# UMP Examination Timetabling Optimization

[![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

An optimization framework written in Java to solve real-world, multi-campus **University Examination Timetabling Problems (UETP)** at Universiti Malaysia Pahang (UMP) using graph heuristics and local search metaheuristics.

---

## Overview

The UMP examination timetabling problem involves scheduling examination sessions across two separate campuses located **57 km apart** (Gambang and Pekan) while satisfying both hard operational constraints and soft quality requirements.

This repository implements a **two-phase approach**:
1. **Initial Phase:** Uses Graph Heuristics (e.g., Largest Degree, Saturation Degree) to construct initial feasible solutions.
2. **Improvement Phase:** Applies Local Search algorithms (Hill Climbing, Late Acceptance Hill Climbing, Step-Counting Hill Climbing) to minimize soft constraint penalty costs.

---

## Key Features & Constraints

### Hard Constraints (Must be satisfied)
* **H1 (Clash-Free):** Students must not be scheduled to sit more than one exam at the same timeslot.
* **H2 (Single Assignment):** Each exam must be scheduled exactly once across 30 available timeslots (3-week period, 2 slots/day, excluding weekends).
* **H3 (Room Capacity):** Total assigned student seats must not exceed the capacity of assigned examination rooms.
* **H4 (Room Availability):** Number of rooms used in a timeslot cannot exceed total available rooms (Gambang: 22, Pekan: 17).
* **H5 (Campus Allocation):** Exams must be assigned to the appropriate campus location.
* **H6 (Cross-Campus Synchronization):** Identical general course exams across different campuses must occur in the same timeslot.
* **H7 (Large Exams Early):** Large exams (>400 students) must be scheduled within the first 10 timeslots.

### Soft Constraints (Penalty Minimization)
* **S1 (Exam Spreading):** Spread exams evenly for students across the period to prevent back-to-back testing.
* **S2 (Building Proximity):** Ensure split exams occur in adjacent/nearby rooms in the same building.
* **S3 (Minimization of Room Splitting):** Discourage splitting single exam sessions across multiple rooms.
* **S4 (Uniform Duration):** Minimize mixing exams with different time durations in the same room.

---

## Algorithm Performance & Results

Benchmark evaluations on UMP **Semester 1** and **Semester 2 (2018/2019)** datasets produced the following penalty costs:

| Algorithm / Heuristic | Sem 1 2018/2019 Cost | Sem 2 2018/2019 Cost |
| :--- | :---: | :---: |
| **Initial Phase (Best Graph Heuristic)** | 41.36 *(SD LWD)* | 12.38 *(LD)* |
| **Hill Climbing (HC) - Move** | **38.30** | **9.23** |
| **Hill Climbing (HC) - Swap** | 38.31 | 9.25 |
| **Late Acceptance Hill Climbing (LAHC)** | 39.48 | 9.45 |
| **Step-Counting Hill Climbing (SCHC)** | 39.41 | 10.64 |

*Note: **HC Move** yielded the lowest penalty costs across both academic semester datasets.*

---

## Getting Started

### Prerequisites
* **Java Development Kit (JDK):** Version 8 or higher
* **Apache Maven** or **Gradle** (Optional build tool)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/ump-examination-timetabling.git
   cd ump-examination-timetabling
   ```

2. Compile the project:
   ```bash
   mvn compile
   # or manually
   javac -d bin src/**/*.java
   ```

3. Run the solver:
   ```bash
   mvn exec:java -Dexec.mainClass="edu.ump.timetabling.Main"
   # or using the provided script
   ./run.bat   # Windows
   ./run.ps1   # PowerShell
   ```

---

## Project Structure

```
ump-examination-timetabling/
├── src/
│   └── main/
│       └── java/edu/ump/timetabling/
│           ├── Main.java
│           ├── model/          # Data models (Exam, Room, Timeslot, etc.)
│           ├── dataset/        # Dataset loaders & conflict matrix
│           └── solver/         # Heuristic & metaheuristic algorithms
├── README.md
└── pom.xml
```

---

## Algorithms Implemented

### Graph Heuristics (Initial Phase)
| Heuristic | Description |
| :--- | :--- |
| **LD** (Largest Degree) | Prioritize exams with the most conflicts |
| **SD** (Saturation Degree) | Prioritize exams with the fewest remaining color options |
| **LWD** (Largest Weighted Degree) | Weighted version using student counts |
| **SD LWD** | Saturation Degree with Largest Weighted Degree tie-breaking |

### Local Search (Improvement Phase)
| Algorithm | Description |
| :--- | :--- |
| **HC Move** | Hill Climbing using single-exam move operations |
| **HC Swap** | Hill Climbing using exam-swap operations |
| **LAHC** | Late Acceptance Hill Climbing with history list |
| **SCHC** | Step-Counting Hill Climbing with step counter |

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## Author

Developed as part of research on examination timetabling optimization at **Universiti Malaysia Pahang (UMP)**.
