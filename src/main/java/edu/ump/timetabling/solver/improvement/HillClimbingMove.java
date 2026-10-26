package edu.ump.timetabling.solver.improvement;

import edu.ump.timetabling.model.*;
import edu.ump.timetabling.solver.ConstraintEvaluator;
import edu.ump.timetabling.solver.HeuristicAlgorithm;

import java.util.*;

/**
 * Hill Climbing with Move Operator (HC Move).
 * Iteratively attempts to move an examination to another timeslot and room allocation if it maintains
 * feasibility and strictly improves the soft penalty.
 * Supports split room assignments in the same building block.
 */
public class HillClimbingMove implements HeuristicAlgorithm {

    @Override
    public String getName() {
        return "HC Move";
    }

    @Override
    public String getDescription() {
        return "Hill Climbing with Single Exam Move Operator";
    }

    @Override
    public TimetableSolution solve(ProblemInstance instance, TimetableSolution initialSolution, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        int maxIterations = parameters != null && parameters.containsKey("maxIterations")
                ? ((Number) parameters.get("maxIterations")).intValue() : 10000;

        TimetableSolution current = new TimetableSolution(initialSolution != null ? initialSolution : new TimetableSolution(instance));
        current.setAlgorithmName("HC Move");

        // Ensure current evaluation
        ConstraintEvaluator.evaluate(current);
        double currentCost = current.getSoftPenalty();
        int currentHard = current.getHardViolations();

        int numExams = instance.getNumExams();
        int totalPeriods = instance.getTotalPeriods();
        Random rng = new Random(42);

        int iteration = 0;
        int noImprovementCount = 0;
        int maxStagnation = Math.min(5000, maxIterations);

        while (iteration < maxIterations && noImprovementCount < maxStagnation) {
            iteration++;

            // Pick a random exam
            int examIdx = rng.nextInt(numExams);
            int oldPeriod = current.getPeriod(examIdx);
            List<Integer> oldRooms = new ArrayList<>(current.getRooms(examIdx));

            int newPeriod = rng.nextInt(totalPeriods);
            if (newPeriod == oldPeriod) {
                continue;
            }

            if (ConstraintEvaluator.hasStudentClash(instance, current.getExamPeriods(), examIdx, newPeriod)) {
                noImprovementCount++;
                continue;
            }

            List<Integer> newRooms = ConstraintEvaluator.findRoomsForExam(instance, current.getExamPeriods(), current.getExamRoomsList(), examIdx, newPeriod);
            if (newRooms == null || newRooms.isEmpty()) {
                noImprovementCount++;
                continue;
            }

            // Tentatively move
            current.setPeriod(examIdx, newPeriod);
            current.setRooms(examIdx, newRooms);

            EvaluationResult eval = ConstraintEvaluator.evaluate(current);

            // Only accept if hard violations decrease, or stay 0 with lower soft penalty
            if (eval.getTotalHardViolations() < currentHard) {
                currentCost = eval.getCompositePenaltyPerStudent();
                currentHard = eval.getTotalHardViolations();
                noImprovementCount = 0;
            } else if (eval.getTotalHardViolations() == currentHard && currentHard == 0 && eval.getCompositePenaltyPerStudent() < currentCost) {
                currentCost = eval.getCompositePenaltyPerStudent();
                currentHard = eval.getTotalHardViolations();
                noImprovementCount = 0;
            } else {
                // Revert move
                current.setPeriod(examIdx, oldPeriod);
                current.setRooms(examIdx, oldRooms);
                noImprovementCount++;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        current.setComputationTimeMs(elapsed);
        current.setIterations(iteration);
        ConstraintEvaluator.evaluate(current);
        return current;
    }
}
