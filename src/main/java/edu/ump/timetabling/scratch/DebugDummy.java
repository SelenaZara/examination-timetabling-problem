package edu.ump.timetabling.scratch;

import edu.ump.timetabling.dataset.UmpDataRepository;
import edu.ump.timetabling.model.*;
import edu.ump.timetabling.solver.ConstraintEvaluator;
import edu.ump.timetabling.solver.initial.*;

import java.util.*;

public class DebugDummy {
    public static void main(String[] args) {
        UmpDataRepository repo = new UmpDataRepository();
        ProblemInstance instance = repo.getDefaultInstance();
        System.out.println("Default instance: " + instance.getDatasetName());
        System.out.println("Num exams: " + instance.getNumExams() + ", Num rooms: " + instance.getNumRooms());
        System.out.println("Exams campus: " + instance.getCampus());
        for (int i = 0; i < Math.min(10, instance.getNumExams()); i++) {
            Exam e = instance.getExams().get(i);
            System.out.println("  Exam " + i + ": " + e.getCode() + ", cap=" + e.getStudentCount() + ", camp=" + e.getCampus());
        }
        for (int r = 0; r < instance.getNumRooms(); r++) {
            Room rm = instance.getRooms().get(r);
            System.out.println("  Room " + r + ": " + rm.getCode() + ", cap=" + rm.getCapacity() + ", camp=" + rm.getCampus() + ", blk=" + rm.getBlock());
        }

        TimetableSolution sol = new TimetableSolution(instance);
        Integer[] order = new Integer[instance.getNumExams()];
        for (int i = 0; i < instance.getNumExams(); i++) order[i] = i;
        int[] degrees = instance.getDegree();
        Arrays.sort(order, (a, b) -> Integer.compare(degrees[b], degrees[a]));

        for (int examIdx : order) {
            int retLevel = ConstraintEvaluator.placeWithRelaxation(instance, sol, examIdx, instance.getTotalPeriods(), instance.getNumRooms());
            if (retLevel >= 15) {
                System.out.println("Exam " + examIdx + " (" + instance.getExams().get(examIdx).getCode() + ") RELAXED with level " + retLevel + " to Period " + sol.getPeriod(examIdx));
            }
        }
        for (int i = 0; i < sol.getExamPeriods().length; i++) {
            System.out.println("  Exam " + i + " -> Period " + sol.getPeriod(i) + ", Rooms: " + sol.getRooms(i));
        }
        for (int i = 0; i < instance.getNumExams(); i++) {
            for (int j = i + 1; j < instance.getNumExams(); j++) {
                if (sol.getPeriod(i) == sol.getPeriod(j) && instance.getConflict(i, j) > 0) {
                    System.out.println("  CLASH in P" + sol.getPeriod(i) + ": Exam " + i + " and Exam " + j + " (" + instance.getConflict(i, j) + " students)");
                }
            }
        }
        EvaluationResult eval = ConstraintEvaluator.evaluate(sol);
        System.out.println("\nEVALUATION:");
        System.out.println("  H1: " + eval.getH1StudentConflicts());
        System.out.println("  H2: " + eval.getH2Unassigned());
        System.out.println("  H3: " + eval.getH3RoomCapacity());
        System.out.println("  H4: " + eval.getH4RoomDoubleBooking());
        System.out.println("  H5: " + eval.getH5CampusMismatch());
        System.out.println("  H6: " + eval.getH6SplitExamSync());
        System.out.println("  H7: " + eval.getH7LargeExamLate());
    }
}
