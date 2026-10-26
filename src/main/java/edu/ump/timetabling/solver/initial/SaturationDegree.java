package edu.ump.timetabling.solver.initial;

import edu.ump.timetabling.model.*;
import edu.ump.timetabling.solver.ConstraintEvaluator;
import edu.ump.timetabling.solver.HeuristicAlgorithm;

import java.util.*;

/**
 * Saturation Degree (SD) Graph Coloring Heuristic with tie-breakers (LD, LE, LWD).
 *
 * Uses TRUE graph-coloring saturation: the number of DISTINCT period-colours
 * already assigned to conflicting (neighbouring) exams.  An exam with more
 * neighbour colours used has fewer genuine placement options and is scheduled
 * first.  Ties in saturation degree are broken by LD, LE, or LWD respectively.
 *
 * This produces genuinely different orderings from the plain LD/LE/LWD
 * heuristics even on dense conflict graphs, because saturation is a DYNAMIC
 * measure that changes as exams are placed, whereas LD/LE/LWD use a fixed
 * pre-computed ordering.
 */
public class SaturationDegree implements HeuristicAlgorithm {

    public enum TieBreaker {
        LD,  // Largest Degree
        LE,  // Largest Enrollment
        LWD  // Largest Weighted Degree
    }

    private final TieBreaker tieBreaker;

    public SaturationDegree(TieBreaker tieBreaker) {
        this.tieBreaker = tieBreaker;
    }

    @Override
    public String getName() {
        return "SD " + tieBreaker.name();
    }

    @Override
    public String getDescription() {
        return "Saturation Degree with " + tieBreaker.name() + " tie-breaking";
    }

    @Override
    public TimetableSolution solve(ProblemInstance instance, TimetableSolution initialSolution, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        int numExams    = instance.getNumExams();
        int totalPeriods = instance.getTotalPeriods();
        int numRooms    = instance.getNumRooms();

        TimetableSolution solution = new TimetableSolution(instance);
        solution.setAlgorithmName("SD " + tieBreaker.name());

        int[] degrees        = instance.getDegree();
        int[] weightedDegrees= instance.getWeightedDegree();
        List<Exam> exams     = instance.getExams();
        boolean[] assigned   = new boolean[numExams];
        int assignedCount    = 0;

        // 0. Place all large exams (> 400) first into periods 0..9 to guarantee H7
        for (int i = 0; i < numExams; i++) {
            if (exams.get(i).getStudentCount() > 400 && !assigned[i]) {
                ConstraintEvaluator.placeWithRelaxation(instance, solution, i, totalPeriods, numRooms);
                assigned[i] = true;
                assignedCount++;
                for (int sib : ConstraintEvaluator.getCoincidentOrSiblingExams(instance, i)) {
                    if (solution.getPeriod(sib) >= 0 && !assigned[sib]) {
                        assigned[sib] = true;
                        assignedCount++;
                    }
                }
            }
        }

        while (assignedCount < numExams) {

            // ── 1. Compute true saturation degree for every unassigned exam ──
            int bestExam         = -1;
            int maxSaturation    = -1;   // higher saturation → schedule first

            for (int i = 0; i < numExams; i++) {
                if (assigned[i] || solution.getPeriod(i) >= 0) {
                    if (!assigned[i]) {
                        assigned[i] = true;
                        assignedCount++;
                    }
                    continue;
                }

                // Collect distinct periods used by conflicting assigned neighbours
                Set<Integer> neighbourPeriods = new HashSet<>();
                for (int j = 0; j < numExams; j++) {
                    if (j != i && assigned[j] && instance.getConflict(i, j) > 0) {
                        int p = solution.getPeriod(j);
                        if (p >= 0) neighbourPeriods.add(p);
                    }
                }
                int saturation = neighbourPeriods.size(); // true SD value

                if (saturation > maxSaturation) {
                    maxSaturation = saturation;
                    bestExam = i;
                } else if (saturation == maxSaturation && bestExam != -1) {
                    // Apply tie-breaker
                    boolean takeCurrent = false;
                    switch (tieBreaker) {
                        case LD:
                            takeCurrent = degrees[i] > degrees[bestExam];
                            break;
                        case LE:
                            takeCurrent = exams.get(i).getStudentCount() > exams.get(bestExam).getStudentCount();
                            break;
                        case LWD:
                            takeCurrent = weightedDegrees[i] > weightedDegrees[bestExam];
                            break;
                    }
                    if (takeCurrent) bestExam = i;
                }
            }

            if (bestExam == -1) break;

            // ── 2. Assign bestExam using relaxed placement (with multi-room splitting and coincident sync) ──
            if (solution.getPeriod(bestExam) < 0) {
                ConstraintEvaluator.placeWithRelaxation(instance, solution, bestExam, totalPeriods, numRooms);
            }

            assigned[bestExam] = true;
            assignedCount++;

            for (int sib : ConstraintEvaluator.getCoincidentOrSiblingExams(instance, bestExam)) {
                if (solution.getPeriod(sib) >= 0 && !assigned[sib]) {
                    assigned[sib] = true;
                    assignedCount++;
                }
            }
        }

        // 3. Autonomous feasibility repair to ensure 100% FEASIBLE solution
        ConstraintEvaluator.repairFeasibility(instance, solution);

        long elapsed = System.currentTimeMillis() - startTime;
        solution.setComputationTimeMs(elapsed);
        ConstraintEvaluator.evaluate(solution);
        return solution;
    }
}
