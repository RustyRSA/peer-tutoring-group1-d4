package com.example.peertutoring.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.models.UpdateProfileRequest;
import com.example.peertutoring.models.UserProfile;
import com.example.peertutoring.network.ApiCallback;
import com.example.peertutoring.network.ApiClient;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.repositories.AuthRepository;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.SessionManager;
import com.example.peertutoring.utils.ValidationUtils;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private AuthRepository authRepository;
    private EditText etFirst, etLast, etUniversity, etYear, etProgramme, etSubjects, etCredentials;
    private TextInputLayout tilFirst, tilLast, tilUniversity, tilYear, tilProgramme, tilSubjects, tilCredentials;
    private TextView tvTutorStatus;
    private TextView tvAvatar;
    private View btnSave;
    private boolean populating = false;   // true while the code (not the user) fills the form
    private boolean edited = false;       // true once the user has typed something

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        NavHelper.setup(this, R.id.btnProfile, false);
        session = new SessionManager(this);
        authRepository = new AuthRepository(this);

        etFirst = findViewById(R.id.etFirstName);
        etLast = findViewById(R.id.etLastName);
        etUniversity = findViewById(R.id.etUniversity);
        etYear = findViewById(R.id.etYears);
        etProgramme = findViewById(R.id.etProgramme);
        etSubjects = findViewById(R.id.etSubjects);
        etCredentials = findViewById(R.id.etCredentials);
        tilFirst = findViewById(R.id.tilFirstName);
        tilLast = findViewById(R.id.tilLastName);
        tilUniversity = findViewById(R.id.tilUniversity);
        tilYear = findViewById(R.id.tilYear);
        tilProgramme = findViewById(R.id.tilProgramme);
        tilSubjects = findViewById(R.id.tilSubjects);
        tilCredentials = findViewById(R.id.tilCredentials);
        tvTutorStatus = findViewById(R.id.tvTutorStatus);
        tvAvatar = findViewById(R.id.tvAvatar);
        btnSave = findViewById(R.id.btnSaveChanges);

        TextWatcher editWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!populating) edited = true;
            }
        };
        for (EditText field : new EditText[]{etFirst, etLast, etUniversity, etYear, etProgramme, etSubjects, etCredentials}) {
            field.addTextChangedListener(editWatcher);
        }

        showSavedProfile();
        refreshFromServer();
        btnSave.setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());
    }

    /** Fills the form from the copy stored on the phone, so the screen is never empty. */
    private void showSavedProfile() {
        // Fall back to the name given at registration
        String[] regName = session.getString("name").split("\\s+", 2);
        String first = session.getString("profile_first");
        String last = session.getString("profile_last");
        if (first.isEmpty() && regName.length > 0) first = regName[0];
        if (last.isEmpty() && regName.length > 1) last = regName[1];

        populating = true;
        etFirst.setText(first);
        etLast.setText(last);
        etUniversity.setText(session.getString("profile_university"));
        etYear.setText(session.getString("profile_year"));
        etProgramme.setText(session.getString("profile_programme"));
        etSubjects.setText(session.getString("profile_subjects"));
        etCredentials.setText(session.getString("profile_credentials"));
        populating = false;
        showTutorApplication();
        edited = false;
        updateAvatar(first);
    }

    /** Tutors also see their credentials and where their application stands. */
    private void showTutorApplication() {
        boolean isTutor = SessionManager.ROLE_TUTOR.equals(session.getRole());
        tilCredentials.setVisibility(isTutor ? View.VISIBLE : View.GONE);
        tvTutorStatus.setVisibility(isTutor ? View.VISIBLE : View.GONE);
        if (!isTutor) return;

        String status = session.getString("tutor_status");
        int message;
        switch (status) {
            case "pending":
                message = R.string.tutor_status_pending;
                break;
            case "approved":
                message = R.string.tutor_status_approved;
                break;
            case "rejected":
                message = R.string.tutor_status_rejected;
                break;
            default:
                message = R.string.tutor_status_none;
                break;
        }
        tvTutorStatus.setText(message);
    }

    /** Quietly loads the latest profile from the backend (GET /users/me) and shows it. */
    private void refreshFromServer() {
        ApiClient.get().getMe().enqueue(new ApiCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (isFinishing() || profile == null) return;
                session.saveUser(profile);
                if (!edited) showSavedProfile();   // never overwrite what the user is typing
            }

            @Override
            public void onError(ApiError error) {
                // Keep showing the saved copy; the user can still edit and save
            }
        });
    }

    private void saveProfile() {
        clearErrors();

        String first = ValidationUtils.text(etFirst);
        if (first.isEmpty()) {
            tilFirst.setError(getString(R.string.profile_first_name_required));
            return;
        }

        List<String> subjects = ValidationUtils.splitList(ValidationUtils.text(etSubjects));
        boolean isTutor = SessionManager.ROLE_TUTOR.equals(session.getRole());

        UpdateProfileRequest request = new UpdateProfileRequest(
                first,
                ValidationUtils.text(etLast),
                ValidationUtils.text(etUniversity),
                ValidationUtils.text(etYear),
                ValidationUtils.text(etProgramme),
                isTutor ? null : subjects,   // students list subjects they need help with
                isTutor ? subjects : null,   // tutors list the subjects they offer
                isTutor ? ValidationUtils.text(etCredentials) : null);

        btnSave.setEnabled(false);
        authRepository.updateProfile(request, new AuthRepository.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (isFinishing() || isDestroyed()) return;
                btnSave.setEnabled(true);
                showSavedProfile();   // the repository has already stored the fresh profile on the phone
                NavHelper.setup(ProfileActivity.this, R.id.btnProfile, false);   // refresh the header greeting

                boolean pendingTutor = profile != null && isTutor
                        && "pending".equals(profile.getTutorStatus());
                ValidationUtils.showToast(ProfileActivity.this, getString(pendingTutor
                        ? R.string.profile_saved_tutor_pending : R.string.profile_saved));
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) return;
                btnSave.setEnabled(true);
                showFieldError(tilFirst, error.getFieldError("firstName"));
                showFieldError(tilLast, error.getFieldError("lastName"));
                showFieldError(tilUniversity, error.getFieldError("university"));
                showFieldError(tilYear, error.getFieldError("yearOfStudy"));
                showFieldError(tilProgramme, error.getFieldError("programme"));
                String subjectError = error.getFieldError("subjectsNeeded") != null
                        ? error.getFieldError("subjectsNeeded") : error.getFieldError("subjectsOffered");
                showFieldError(tilSubjects, subjectError);
                showFieldError(tilCredentials, error.getFieldError("credentials"));
                ValidationUtils.showToast(ProfileActivity.this, error.getMessage());
            }
        });
    }

    private void clearErrors() {
        tilFirst.setError(null);
        tilLast.setError(null);
        tilUniversity.setError(null);
        tilYear.setError(null);
        tilProgramme.setError(null);
        tilSubjects.setError(null);
        tilCredentials.setError(null);
    }

    private static void showFieldError(TextInputLayout layout, String message) {
        if (message != null) layout.setError(message);
    }

    /** Asks first, then signs out and returns to Login with an empty back stack. */
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_logout, (dialog, which) -> {
                    new AuthRepository(this).logout();
                    NavHelper.sendToLogin(this);
                })
                .show();
    }

    private void updateAvatar(String first) {
        tvAvatar.setText(first.isEmpty() ? "?" : first.substring(0, 1).toUpperCase());
    }
}
