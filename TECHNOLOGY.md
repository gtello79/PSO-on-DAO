# Technology Stack

This document describes all technologies present in the PSO-on-DAO project.

---

## Programming Languages

### Java
- **Version required:** Java 11
- **Role:** Core implementation language for the PSO (Particle Swarm Optimization) algorithm and the DAO (Direct Aperture Optimization) problem model.
- **Source location:** `src/`
- **Key Java features used:**
  - `java.util.concurrent` (ExecutorService, Executors, Callable) — multi-threaded swarm evaluation
  - `javafx.util.Pair` — used throughout for coordinate pairs
  - `java.util.ArrayList`, `java.util.Vector`, `java.util.HashMap`, `java.util.Hashtable` — standard data structures
  - `java.io.File`, `java.util.Scanner` — file I/O for reading problem instances

### Python
- **Version:** Python 3 (standard library + third-party packages)
- **Role:** Experiment orchestration, data processing, and result visualization.
- **Source files:**
  - `runnerExperiment.py` — compiles and runs the Java program, manages experiment iterations, and aggregates results
  - `instancesTest.py` — runs PSO over a range of problem instances
  - `process_results.py` — parses and computes statistics over experiment output files
  - `plotter.ipynb` — Jupyter Notebook for visualizing results

---

## Frameworks & Libraries

### Gurobi Optimizer
- **Type:** Commercial mathematical optimization solver
- **Version referenced:** Gurobi 11.0.1 (`gurobi1101`)
- **JAR path:** `/opt/gurobi1101/linux64/lib/gurobi.jar`
- **Role:** Mixed-Integer Linear Programming (MILP) solver used as a **repair function** within PSO to re-optimize beam aperture intensities. Wrapped in `src/Utils/Gurobi_Solver.java`.
- **API packages used:** `com.gurobi.gurobi.*` (GRBEnv, GRBModel, GRBVar, GRBLinExpr, GRBQuadExpr, GRBException)
- **Requirement:** A valid Gurobi license must be active; without it, the algorithm runs in PSO-only mode (no repair function).

### Seaborn
- **Type:** Python data visualization library
- **Role:** Used in `process_results.py` and `plotter.ipynb` for statistical plot generation.

### Matplotlib
- **Type:** Python plotting library
- **Role:** Used alongside Seaborn in `plotter.ipynb` and `process_results.py` for rendering charts and graphs of experiment results.

### NumPy
- **Type:** Python numerical computing library
- **Role:** Used in `plotter.ipynb` for array operations during result analysis.

### Pandas
- **Type:** Python data analysis library
- **Role:** Used in `plotter.ipynb` (via `pandas.DataFrame`) for structured data handling during visualization.

---

## Development Tools & Environment

### Jupyter Notebook
- **File:** `plotter.ipynb`
- **Role:** Interactive environment for exploring and visualizing PSO experiment results. Uses Python kernel with Seaborn, Matplotlib, NumPy, and Pandas.

### Visual Studio Code
- **Config directory:** `.vscode/` (gitignored)
- **Role:** Recommended IDE for development (inferred from `.vscode` directory presence).

### javac / java (JDK 11)
- **Role:** Java compiler and runtime. The Python runner scripts invoke `javac` to compile the Java source before each experiment run and `java` to execute it.
- **Compile command pattern:** `javac --class-path src:<gurobi.jar> src/com/company/Main.java`
- **Run command pattern:** `java -classpath ./src:<gurobi.jar> com.company.Main <params>`

---

## Project Architecture Overview

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Algorithm Core | Java 11 | PSO engine, aperture/intensity velocity/movement calculations |
| Optimization Solver | Gurobi (Java API) | Intensity repair via MILP |
| Concurrency | Java `ExecutorService` | Parallel particle evaluation across threads |
| Experiment Runner | Python 3 | Compile, execute, and collect experiment results |
| Data Analysis | Python 3 + Pandas/NumPy | Parse and aggregate result files |
| Visualization | Seaborn + Matplotlib (Jupyter) | Plot convergence curves and beam descriptors |

---

## Input Data Formats

- **Instance files (`.txt`):** Text files describing beam angles, organ volumes, and dose deposition matrices (DDM).
- **Coordinate files (`.txt`):** Define collimator geometry per instance.
- **Plotter data files (`.dat`):** Located in `plotter/`, contain organ-level data for visualization (e.g., `organ_xls0.dat`, `organ_xls1.dat`, `organ_xls2.dat`).
- **Index file (`data/index_instances.txt`):** Maps instance IDs to their data folders.

---

## Output Files

- **Experiment files (`ExperimentsFiles/`):** Per-run PSO output (gitignored).
- **Final results (`FinalResults/`):** Aggregated best results per experiment batch (gitignored).
- **Log files (`.log`, `.lp`):** Gurobi solver logs and LP model exports (gitignored).
