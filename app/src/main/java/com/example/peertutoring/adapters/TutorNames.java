package com.example.peertutoring.adapters;

/** Small helper shared by the adapters: turns "Thabo Mokoena" into "TM". */
final class TutorNames {

    private TutorNames() {}

    static String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        return sb.toString();
    }
}
