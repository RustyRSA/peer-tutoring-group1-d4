package com.example.peertutoring.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.peertutoring.models.UserProfile;

import java.util.List;

/**
 * Small copy of the signed-in user's profile kept on the phone so screens can show the name and
 * decide what to display without a network call. The backend stays the source of truth and the
 * copy is replaced on every login. Passwords are never stored here (Firebase handles those).
 */
public class SessionManager {

    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_TUTOR = "tutor";
    public static final String ROLE_ADMIN = "admin";

    private static final String PREFS = "peer_tutoring_session";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Replaces everything stored with the profile the backend just returned. */
    public void saveUser(UserProfile profile) {
        String first = orEmpty(profile.getFirstName());
        String last = orEmpty(profile.getLastName());
        String role = orEmpty(profile.getRole());

        // Students list the subjects they need help with, tutors the ones they offer
        List<String> subjects = ROLE_TUTOR.equals(role)
                ? profile.getSubjectsOffered() : profile.getSubjectsNeeded();

        prefs.edit()
                .clear()
                .putString("uid", orEmpty(profile.getUid()))
                .putString("email", orEmpty(profile.getEmail()))
                .putString("role", role)
                .putString("name", (first + " " + last).trim())
                .putString("profile_first", first)
                .putString("profile_last", last)
                .putString("profile_university", orEmpty(profile.getUniversity()))
                .putString("profile_year", profile.getYearOfStudy() == null
                        ? "" : String.valueOf(profile.getYearOfStudy()))
                .putString("profile_programme", orEmpty(profile.getProgramme()))
                .putString("profile_credentials", orEmpty(profile.getCredentials()))
                .putString("tutor_status", orEmpty(profile.getTutorStatus()))
                .putString("profile_subjects", subjects == null ? "" : TextUtils.join(", ", subjects))
                .apply();
    }

    /** Forgets everything (used on logout). */
    public void clear() {
        prefs.edit().clear().apply();
    }

    public String getRole() {
        return prefs.getString("role", ROLE_STUDENT);
    }

    /** Only decides what the app shows; the backend checks the role again on every request. */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(getRole());
    }

    public String getString(String key) {
        return prefs.getString(key, "");
    }

    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    /** Name shown in the header greeting. */
    public String getDisplayName() {
        String first = prefs.getString("profile_first", "");
        if (!first.isEmpty()) return first;
        String name = prefs.getString("name", "").trim();
        if (!name.isEmpty()) return name.split("\\s+")[0];
        String email = prefs.getString("email", "");
        int at = email.indexOf('@');
        if (at > 0) return Character.toUpperCase(email.charAt(0)) + email.substring(1, at);
        return "there";
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
