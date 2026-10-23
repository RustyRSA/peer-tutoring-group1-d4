package com.example.peertutoring.models;

public class StatusUpdateRequest {
    private String tutorStatus;    // approved, rejected, ect
    private String accountStatus;  //active, suspended

    public StatusUpdateRequest(String tutorStatus, String accountStatus) {
        this.tutorStatus = tutorStatus;
        this.accountStatus = accountStatus;

    }
}