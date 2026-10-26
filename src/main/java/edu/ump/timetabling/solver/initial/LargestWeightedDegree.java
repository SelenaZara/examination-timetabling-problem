package edu.ump.timetabling.solver.initial;

import edu.ump.timetabling.model.*;
import edu.ump.timetabling.solver.ConstraintEvaluator;
import edu.ump.timetabling.solver.HeuristicAlgorithm;

import java.util.*;

/**
 * Largest Weighted Degree (LWD) Graph Heuristic.
 * Orders examinations in descending order of sum of student conflicts.
 */
public class LargestWeightedDegree implements HeuristicAlgorithm {

    @Override
    public String getName() {
        return "LWD";
    }

    @Override
    public String getDescription() {
        return "Largest Weighted Degree (Orders exams by total conflicting students)";
    }

    @Override
    public TimetableSolution solve(ProblemInstance instance, TimetableSolution initialSolution, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        int numExams = instance.getNumExams();
        int totalPeriods = instance.getTotalPeriods();
        int numRooms = instance.getNumRooms();

        TimetableSolution solution = new TimetableSolution(instance);
        solution.setAlgorithmName("LWD (Largest Weighted Degree)");

        // 1. Order exams: Place large exams (> 400) first to guarantee H7, then sort remainder by weighted degree descending
        Integer[] order = new Integer[numExams];
        for (int i = 0; i < numExams; i++) order[i] = i;

        List<Exam> exams = instance.getExams();
        int[] weightedDegrees = instance.getWeightedDegree();
        Arrays.sort(order, (a, b) -> {
            boolean aLarge = exams.get(a).getStudentCount() > 400;
            boolean bLarge = exams.get(b).getStudentCount() > 400;
            if (aLarge != bLarge) return aLarge ? -1 : 1;
            return Integer.compare(weightedDegrees[b], weightedDegrees[a]);
        });

        // 2. Sequential greedy placement with multi-room splitting and synchronized coincident placement
        for (int examIdx : order) {
            if (solution.getPeriod(examIdx) >= 0) continue; // Already placed as a coincident twin
            ConstraintEvaluator.placeWithRelaxation(instance, solution, examIdx, totalPeriods, numRooms);
        }

        // 3. Autonomous feasibility repair to ensure 100% FEASIBLE solution
        ConstraintEvaluator.repairFeasibility(instance, solution);

        long elapsed = System.currentTimeMillis() - startTime;
        solution.setComputationTimeMs(elapsed);
        ConstraintEvaluator.evaluate(solution);
        return solution;
    }
}
