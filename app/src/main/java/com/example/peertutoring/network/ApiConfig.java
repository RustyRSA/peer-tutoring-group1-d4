package com.example.peertutoring.network;

/** Settings that depend on where the backend runs. Change them here only. */
public final class ApiConfig {

    /**
     * Backend address as seen from the Android emulator (10.0.2.2 is the computer running the
     * emulator). Must end with "/". For a real phone use the computer's local network address.
     */
    public static final String BASE_URL = "http://10.0.2.2:3000/";

    /**
     * Only emails ending with this are accepted at sign-up. Keep it equal to UNIVERSITY_DOMAIN in
     * backend/.env, or leave it empty to allow any email.
     */
    public static final String UNIVERSITY_EMAIL_DOMAIN = "@vossie.net";

    private ApiConfig() {
    }
}
