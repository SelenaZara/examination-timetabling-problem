package edu.ump.timetabling.solver;

import edu.ump.timetabling.model.ProblemInstance;
import edu.ump.timetabling.model.TimetableSolution;
import java.util.Map;

/**
 * Standard interface for Timetabling Heuristic and Metaheuristic Drivers.
 */
public interface HeuristicAlgorithm {
    String getName();
    String getDescription();
    TimetableSolution solve(ProblemInstance instance, TimetableSolution initialSolution, Map<String, Object> parameters);
}
