package com.example.peertutoring.models;

import java.util.List;

/** A user profile as returned by the backend. Field names match the JSON exactly. */
public class UserProfile {

    /** Average star rating and how many ratings it is based on. */
    public static class Rating {
        private double average;
        private int count;

        public double getAverage() {
            return average;
        }

        public int getCount() {
            return count;
        }
    }

    private String uid;
    private String email;
    private String firstName;
    private String lastName;
    private String role;             // "student", "tutor" or "admin"
    private String university;
    private Integer yearOfStudy;
    private String programme;
    private List<String> subjectsNeeded;
    private List<String> subjectsOffered;
    private String credentials;
    private String tutorStatus;      // "none", "pending", "approved" or "rejected"
    private String accountStatus;    // "active" or "suspended"
    private Rating rating;

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getRole() {
        return role;
    }

    public String getUniversity() {
        return university;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public String getProgramme() {
        return programme;
    }

    public List<String> getSubjectsNeeded() {
        return subjectsNeeded;
    }

    public List<String> getSubjectsOffered() {
        return subjectsOffered;
    }

    public String getCredentials() {
        return credentials;
    }

    public String getTutorStatus() {
        return tutorStatus;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public Rating getRating() {
        return rating;
    }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setUniversity(String university) { this.university = university; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public void setProgramme(String programme) { this.programme = programme; }
    public void setSubjectsNeeded(List<String> subjectsNeeded) { this.subjectsNeeded = subjectsNeeded; }
    public void setSubjectsOffered(List<String> subjectsOffered) { this.subjectsOffered = subjectsOffered; }
}
