package com.example.peertutoring.models;

import java.util.List;

/**
 * Body of POST /users. Null fields are left out of the JSON. A tutor who sends subjectsOffered
 * starts with tutorStatus "pending", which is what puts them in the admin's Review Applications.
 */
public class CreateUserRequest {

    private final String firstName;
    private final String lastName;
    private final String role;                    // "student" or "tutor"
    private final List<String> subjectsOffered;   // tutors only
    private final String credentials;             // tutors only, optional

    public CreateUserRequest(String firstName, String lastName, String role) {
        this(firstName, lastName, role, null, null);
    }

    public CreateUserRequest(String firstName, String lastName, String role,
                             List<String> subjectsOffered, String credentials) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.subjectsOffered = subjectsOffered;
        this.credentials = credentials;
    }
}
