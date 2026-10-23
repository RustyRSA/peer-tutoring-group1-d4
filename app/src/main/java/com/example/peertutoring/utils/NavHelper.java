package com.example.peertutoring.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.peertutoring.R;
import com.example.peertutoring.activities.AdminHomeActivity;
import com.example.peertutoring.activities.LoginActivity;
import com.example.peertutoring.activities.MessagesActivity;
import com.example.peertutoring.activities.NotificationsActivity;
import com.example.peertutoring.activities.ProfileActivity;
import com.example.peertutoring.activities.StudentHomeActivity;
import com.example.peertutoring.activities.TutorSearchActivity;
import com.example.peertutoring.repositories.AuthRepository;

import java.util.Calendar;

/** One call wires the shared header + bottom navigation for any screen that includes them. */
public class NavHelper {

    /** The user the role popup was last shown for, so Home only shows it once per sign-in. */
    private static String roleShownFor = null;

    public static void setup(Activity a, int selectedNavId, boolean showBack) {
        // Every screen with the shared header needs a logged-in user
        if (!AuthRepository.isLoggedIn()) {
            sendToLogin(a);
            return;
        }
        SessionManager session = new SessionManager(a);

        BottomNavHelper.updateSelection(a, selectedNavId);

        go(a, R.id.btnHome, StudentHomeActivity.class);
        go(a, R.id.btnSearch, TutorSearchActivity.class);
        go(a, R.id.btnMessages, MessagesActivity.class);
        go(a, R.id.btnProfile, ProfileActivity.class);
        go(a, R.id.btnNotifications, NotificationsActivity.class);

        // Admin entry points are only for admins
        View adminTab = a.findViewById(R.id.btnAdmin);
        View adminBtn = a.findViewById(R.id.btnHeaderAdmin);
        // Both are hidden in the layout, so they only ever appear here, for admins
        if (session.isAdmin()) {
            if (adminTab != null) adminTab.setVisibility(View.VISIBLE);
            if (adminBtn != null) adminBtn.setVisibility(View.VISIBLE);
            go(a, R.id.btnAdmin, AdminHomeActivity.class);
            go(a, R.id.btnHeaderAdmin, AdminHomeActivity.class);
        }

        View back = a.findViewById(R.id.btnBack);
        if (back != null && showBack) {
            back.setVisibility(View.VISIBLE);
            back.setOnClickListener(v -> a.finish());
        }

        TextView greeting = a.findViewById(R.id.tvGreeting);
        if (greeting != null) {
            greeting.setText(a.getString(R.string.greeting_format,
                    greetingPart(a), session.getDisplayName()));
            greeting.setOnClickListener(v -> showRoleDialog(a));   // tap the greeting to see your role
        }
    }

    /** A basic popup showing who is signed in and which role the app thinks they have. */
    public static void showRoleDialog(Activity a) {
        SessionManager session = new SessionManager(a);
        String role = session.getRole();
        new AlertDialog.Builder(a)
                .setTitle(R.string.role_popup_title)
                .setMessage(a.getString(R.string.role_popup_message,
                        session.getDisplayName(),
                        session.getString("email"),
                        role.isEmpty() ? "(none)" : role,
                        a.getString(session.isAdmin() ? R.string.role_popup_yes : R.string.role_popup_no)))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /** Shows the role popup the first time Home opens after signing in (once per user per app start). */
    public static void showRoleDialogOnce(Activity a) {
        String uid = new SessionManager(a).getString("uid");
        if (uid.isEmpty() || uid.equals(roleShownFor)) return;
        roleShownFor = uid;
        showRoleDialog(a);
    }

    /** Opens the login screen and clears the back stack, so Back cannot return to a private screen. */
    public static void sendToLogin(Activity a) {
        Intent intent = new Intent(a, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(intent);
        a.finish();
    }

    private static String greetingPart(Activity a) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return a.getString(R.string.greeting_part_morning);
        if (hour < 18) return a.getString(R.string.greeting_part_afternoon);
        return a.getString(R.string.greeting_part_evening);
    }

    private static void go(Activity a, int viewId, Class<?> target) {
        View v = a.findViewById(viewId);
        if (v == null) return;
        v.setOnClickListener(x -> {
            if (a.getClass().equals(target)) return;      // already here
            Intent i = new Intent(a, target);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            a.startActivity(i);                           // no back-stack pile-up
        });
    }
}
