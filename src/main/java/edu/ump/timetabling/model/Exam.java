package edu.ump.timetabling.model;

import java.util.Objects;

/**
 * Represents an Examination (Course).
 */
public class Exam {
    private final int id;
    private final String code;
    private final String name;
    private final int studentCount;
    private final String campus; // "Gambang", "Pekan", or "Combined"
    private final int durationMinutes; // Exam duration in minutes (from timeslot file)

    public Exam(int id, String code, String name, int studentCount, String campus) {
        this(id, code, name, studentCount, campus, 180); // default 3h
    }

    public Exam(int id, String code, String name, int studentCount, String campus, int durationMinutes) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.studentCount = studentCount;
        this.campus = campus;
        this.durationMinutes = durationMinutes;
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getStudentCount() { return studentCount; }
    public String getCampus() { return campus; }
    public int getDurationMinutes() { return durationMinutes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exam exam = (Exam) o;
        return id == exam.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
