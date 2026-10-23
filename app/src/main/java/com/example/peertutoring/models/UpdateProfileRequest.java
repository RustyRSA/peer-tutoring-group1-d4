package com.example.peertutoring.models;

import java.util.List;

/**
 * Body of PUT /users/me. A null field is left out of the JSON (so the backend keeps its value);
 * an empty string or empty list clears the field on the backend.
 */
public class UpdateProfileRequest {

    private final String firstName;
    private final String lastName;
    private final String university;
    private final String yearOfStudy;
    private final String programme;
    private final List<String> subjectsNeeded;   // students
    private final List<String> subjectsOffered;  // tutors
    private final String credentials;            // tutors

    public UpdateProfileRequest(String firstName, String lastName, String university,
                                String yearOfStudy, String programme,
                                List<String> subjectsNeeded, List<String> subjectsOffered,
                                String credentials) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.university = university;
        this.yearOfStudy = yearOfStudy;
        this.programme = programme;
        this.subjectsNeeded = subjectsNeeded;
        this.subjectsOffered = subjectsOffered;
        this.credentials = credentials;
    }
}
