package edu.ump.timetabling.model;

import java.util.Objects;

/**
 * Represents an Examination Venue / Hall / Room.
 * Includes campus and building block location for building proximity evaluations (S2).
 */
public class Room {
    private final int id;
    private final String code;
    private final String name;
    private final int capacity;
    private final String campus; // "Gambang" or "Pekan"
    private final String block;  // Building block code (e.g. "GB-A", "D", "V", "PK-DSG")

    public Room(int id, String code, String name, int capacity, String campus) {
        this(id, code, name, capacity, campus, deriveBlock(code, campus));
    }

    public Room(int id, String code, String name, int capacity, String campus, String block) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.capacity = capacity;
        this.campus = campus;
        this.block = (block != null && !block.trim().isEmpty()) ? block.trim() : deriveBlock(code, campus);
    }

    private static String deriveBlock(String code, String campus) {
        if (code == null || code.isEmpty()) return "B1";
        if (code.startsWith("GB-") || code.startsWith("PK-")) {
            String[] parts = code.split("-");
            if (parts.length > 1) {
                return parts[1].replaceAll("\\d+$", "");
            }
        }
        return code.replaceAll("\\d+$", "");
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public String getCampus() { return campus; }
    public String getBlock() { return block; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return id == room.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
