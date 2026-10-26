package edu.ump.timetabling.solver.improvement;

import edu.ump.timetabling.model.*;
import edu.ump.timetabling.solver.ConstraintEvaluator;
import edu.ump.timetabling.solver.HeuristicAlgorithm;

import java.util.*;

/**
 * Step Counting Hill Climbing (SCHC) Metaheuristic.
 * Academic local search algorithm by Bykov & Petrovic.
 * Compares candidate cost with an acceptance bound updated every step-counter period L_s.
 * Supports split room assignments in the same building block.
 */
public class StepCountingHC implements HeuristicAlgorithm {

    @Override
    public String getName() {
        return "SCHC";
    }

    @Override
    public String getDescription() {
        return "Step Counting Hill Climbing (Periodic step threshold acceptance)";
    }

    @Override
    public TimetableSolution solve(ProblemInstance instance, TimetableSolution initialSolution, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        int maxIterations = parameters != null && parameters.containsKey("maxIterations")
                ? ((Number) parameters.get("maxIterations")).intValue() : 15000;
        int stepLength = parameters != null && parameters.containsKey("schcStepLength")
                ? ((Number) parameters.get("schcStepLength")).intValue() : 500;

        TimetableSolution current = new TimetableSolution(initialSolution != null ? initialSolution : new TimetableSolution(instance));
        current.setAlgorithmName("SCHC");

        ConstraintEvaluator.evaluate(current);
        double currentCost = current.getSoftPenalty();
        int currentHard = current.getHardViolations();

        TimetableSolution best = new TimetableSolution(current);
        double bestCost = currentCost;
        int bestHard = currentHard;

        // Step acceptance threshold
        double boundCost = currentCost;
        int boundHard = currentHard;

        int numExams = instance.getNumExams();
        int totalPeriods = instance.getTotalPeriods();
        Random rng = new Random(42);

        int iteration = 0;
        int stepCounter = 0;

        while (iteration < maxIterations) {
            iteration++;
            stepCounter++;

            int examIdx = rng.nextInt(numExams);
            int oldPeriod = current.getPeriod(examIdx);
            List<Integer> oldRooms = new ArrayList<>(current.getRooms(examIdx));

            int newPeriod = rng.nextInt(totalPeriods);
            if (newPeriod == oldPeriod) {
                continue;
            }

            if (ConstraintEvaluator.hasStudentClash(instance, current.getExamPeriods(), examIdx, newPeriod)) {
                continue;
            }

            List<Integer> newRooms = ConstraintEvaluator.findRoomsForExam(instance, current.getExamPeriods(), current.getExamRoomsList(), examIdx, newPeriod);
            if (newRooms == null || newRooms.isEmpty()) {
                continue;
            }

            current.setPeriod(examIdx, newPeriod);
            current.setRooms(examIdx, newRooms);

            EvaluationResult eval = ConstraintEvaluator.evaluate(current);
            double candidateCost = eval.getCompositePenaltyPerStudent();
            int candidateHard = eval.getTotalHardViolations();

            // SCHC Acceptance Criteria
            boolean accept = false;
            if (candidateHard < currentHard) {
                accept = true;
            } else if (candidateHard == currentHard) {
                if (currentHard == 0) {
                    if (candidateCost <= currentCost || candidateCost <= boundCost) {
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
                current.setPeriod(examIdx, oldPeriod);
                current.setRooms(examIdx, oldRooms);
            }

            // Update step bound every L_s steps
            if (stepCounter >= stepLength) {
                boundCost = currentCost;
                boundHard = currentHard;
                stepCounter = 0;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        best.setComputationTimeMs(elapsed);
        best.setIterations(iteration);
        ConstraintEvaluator.evaluate(best);
        return best;
    }
}
