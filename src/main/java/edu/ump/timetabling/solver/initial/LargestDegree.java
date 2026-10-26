package edu.ump.timetabling.solver.initial;

import edu.ump.timetabling.model.*;
import edu.ump.timetabling.solver.ConstraintEvaluator;
import edu.ump.timetabling.solver.HeuristicAlgorithm;

import java.util.*;

/**
 * Largest Degree (LD) Graph Coloring Heuristic.
 * Orders examinations in descending order of conflict degree (number of conflicting courses).
 */
public class LargestDegree implements HeuristicAlgorithm {

    @Override
    public String getName() {
        return "LD";
    }

    @Override
    public String getDescription() {
        return "Largest Degree (Orders exams by number of conflict edges)";
    }

    @Override
    public TimetableSolution solve(ProblemInstance instance, TimetableSolution initialSolution, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        int numExams = instance.getNumExams();
        int totalPeriods = instance.getTotalPeriods();
        int numRooms = instance.getNumRooms();

        TimetableSolution solution = new TimetableSolution(instance);
        solution.setAlgorithmName("LD (Largest Degree)");

        // 1. Order exams: Place large exams (> 400) first to guarantee H7 early placement, then sort remainder by degree descending
        Integer[] order = new Integer[numExams];
        for (int i = 0; i < numExams; i++) order[i] = i;

        List<Exam> exams = instance.getExams();
        int[] degrees = instance.getDegree();
        Arrays.sort(order, (a, b) -> {
            boolean aLarge = exams.get(a).getStudentCount() > 400;
            boolean bLarge = exams.get(b).getStudentCount() > 400;
            if (aLarge != bLarge) return aLarge ? -1 : 1;
            return Integer.compare(degrees[b], degrees[a]);
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
