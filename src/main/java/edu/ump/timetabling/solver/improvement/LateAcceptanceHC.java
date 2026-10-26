package edu.ump.timetabling.solver.improvement;

import edu.ump.timetabling.model.*;
import edu.ump.timetabling.solver.ConstraintEvaluator;
import edu.ump.timetabling.solver.HeuristicAlgorithm;

import java.util.*;

/**
 * Late Acceptance Hill Climbing (LAHC) Metaheuristic.
 * Academic local search algorithm by Burke & Bykov for Examination Timetabling.
 * Compares candidate cost with an acceptance history list of length L_fa.
 * Supports split room assignments in the same building block.
 */
public class LateAcceptanceHC implements HeuristicAlgorithm {

    @Override
    public String getName() {
        return "LAHC";
    }

    @Override
    public String getDescription() {
        return "Late Acceptance Hill Climbing (Burke & Bykov list acceptance)";
    }

    @Override
    public TimetableSolution solve(ProblemInstance instance, TimetableSolution initialSolution, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        int maxIterations = parameters != null && parameters.containsKey("maxIterations")
                ? ((Number) parameters.get("maxIterations")).intValue() : 15000;
        int listLength = parameters != null && parameters.containsKey("lahcListLength")
                ? ((Number) parameters.get("lahcListLength")).intValue() : 500;

        TimetableSolution current = new TimetableSolution(initialSolution != null ? initialSolution : new TimetableSolution(instance));
        current.setAlgorithmName("LAHC");

        ConstraintEvaluator.evaluate(current);
        double currentCost = current.getSoftPenalty();
        int currentHard = current.getHardViolations();

        TimetableSolution best = new TimetableSolution(current);
        double bestCost = currentCost;
        int bestHard = currentHard;

        // Initialize history list
        double[] history = new double[listLength];
        Arrays.fill(history, currentCost);

        int numExams = instance.getNumExams();
        int totalPeriods = instance.getTotalPeriods();
        Random rng = new Random(42);

        int iteration = 0;

        while (iteration < maxIterations) {
            int historyIndex = iteration % listLength;

            // Generate neighbor
            int examIdx = rng.nextInt(numExams);
            int oldPeriod = current.getPeriod(examIdx);
            List<Integer> oldRooms = new ArrayList<>(current.getRooms(examIdx));

            int newPeriod = rng.nextInt(totalPeriods);
            if (newPeriod == oldPeriod) {
                iteration++;
                continue;
            }

            if (ConstraintEvaluator.hasStudentClash(instance, current.getExamPeriods(), examIdx, newPeriod)) {
                iteration++;
                continue;
            }

            List<Integer> newRooms = ConstraintEvaluator.findRoomsForExam(instance, current.getExamPeriods(), current.getExamRoomsList(), examIdx, newPeriod);
            if (newRooms == null || newRooms.isEmpty()) {
                iteration++;
                continue;
            }

            current.setPeriod(examIdx, newPeriod);
            current.setRooms(examIdx, newRooms);

            EvaluationResult eval = ConstraintEvaluator.evaluate(current);
            double candidateCost = eval.getCompositePenaltyPerStudent();
            int candidateHard = eval.getTotalHardViolations();

            // LAHC Acceptance Criteria
            boolean accept = false;
            if (candidateHard < currentHard) {
                accept = true;
            } else if (candidateHard == currentHard) {
                if (currentHard == 0) {
                    if (candidateCost <= currentCost || candidateCost <= history[historyIndex]) {
                        accept = true;
                    }
                } else {
                    accept = true;
                }
            }

            if (accept) {
                currentCost = candidateCost;
                currentHard = candidateHard;

                if (currentHard < bestHard || (currentHard == bestHard && currentCost < bestCost)) {
                    best = new TimetableSolution(current);
                    bestCost = currentCost;
                    bestHard = currentHard;
                }
            } else {
                // Revert
                current.setPeriod(examIdx, oldPeriod);
                current.setRooms(examIdx, oldRooms);
            }

            history[historyIndex] = currentCost;
            iteration++;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        best.setComputationTimeMs(elapsed);
        best.setIterations(iteration);
        ConstraintEvaluator.evaluate(best);
        return best;
    }
}
