package edu.ump.timetabling.solver.improvement;

import edu.ump.timetabling.model.*;
import edu.ump.timetabling.solver.ConstraintEvaluator;
import edu.ump.timetabling.solver.HeuristicAlgorithm;

import java.util.*;

/**
 * Hill Climbing with Swap Operator (HC Swap).
 * Iteratively swaps periods between two selected exams if it improves soft penalty.
 * Reallocates rooms appropriately in the swapped periods.
 */
public class HillClimbingSwap implements HeuristicAlgorithm {

    @Override
    public String getName() {
        return "HC Swap";
    }

    @Override
    public String getDescription() {
        return "Hill Climbing with Exam Swap Operator";
    }

    @Override
    public TimetableSolution solve(ProblemInstance instance, TimetableSolution initialSolution, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        int maxIterations = parameters != null && parameters.containsKey("maxIterations")
                ? ((Number) parameters.get("maxIterations")).intValue() : 10000;

        TimetableSolution current = new TimetableSolution(initialSolution != null ? initialSolution : new TimetableSolution(instance));
        current.setAlgorithmName("HC Swap");

        ConstraintEvaluator.evaluate(current);
        double currentCost = current.getSoftPenalty();
        int currentHard = current.getHardViolations();

        int numExams = instance.getNumExams();
        Random rng = new Random(42);

        int iteration = 0;
        int noImprovementCount = 0;
        int maxStagnation = Math.min(5000, maxIterations);

        while (iteration < maxIterations && noImprovementCount < maxStagnation) {
            iteration++;

            int e1 = rng.nextInt(numExams);
            int e2 = rng.nextInt(numExams);
            if (e1 == e2) continue;

            int p1 = current.getPeriod(e1);
            List<Integer> r1 = new ArrayList<>(current.getRooms(e1));
            int p2 = current.getPeriod(e2);
            List<Integer> r2 = new ArrayList<>(current.getRooms(e2));

            if (p1 == p2) continue;

            // Check student clashes in swapped periods
            if (ConstraintEvaluator.hasStudentClash(instance, current.getExamPeriods(), e1, p2)) {
                noImprovementCount++;
                continue;
            }
            if (ConstraintEvaluator.hasStudentClash(instance, current.getExamPeriods(), e2, p1)) {
                noImprovementCount++;
                continue;
            }

            // Temporarily clear both periods to find rooms cleanly
            current.setPeriod(e1, -1);
            current.setPeriod(e2, -1);

            List<Integer> newRooms1 = ConstraintEvaluator.findRoomsForExam(instance, current.getExamPeriods(), current.getExamRoomsList(), e1, p2);
            current.setPeriod(e1, p2);
            if (newRooms1 != null) current.setRooms(e1, newRooms1);

            List<Integer> newRooms2 = (newRooms1 != null) ? ConstraintEvaluator.findRoomsForExam(instance, current.getExamPeriods(), current.getExamRoomsList(), e2, p1) : null;

            if (newRooms1 == null || newRooms2 == null) {
                // Revert
                current.setPeriod(e1, p1);
                current.setRooms(e1, r1);
                current.setPeriod(e2, p2);
                current.setRooms(e2, r2);
                noImprovementCount++;
                continue;
            }

            current.setPeriod(e2, p1);
            current.setRooms(e2, newRooms2);

            EvaluationResult eval = ConstraintEvaluator.evaluate(current);

            if (eval.getTotalHardViolations() < currentHard) {
                currentCost = eval.getCompositePenaltyPerStudent();
                currentHard = eval.getTotalHardViolations();
                noImprovementCount = 0;
            } else if (eval.getTotalHardViolations() == currentHard && currentHard == 0 && eval.getCompositePenaltyPerStudent() < currentCost) {
                currentCost = eval.getCompositePenaltyPerStudent();
                currentHard = eval.getTotalHardViolations();
                noImprovementCount = 0;
            } else {
                // Revert swap
                current.setPeriod(e1, p1);
                current.setRooms(e1, r1);
                current.setPeriod(e2, p2);
                current.setRooms(e2, r2);
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
