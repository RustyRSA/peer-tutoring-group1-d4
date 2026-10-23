package com.example.peertutoring.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.models.CreateUserRequest;
import com.example.peertutoring.models.UserProfile;
import com.example.peertutoring.network.ApiConfig;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.repositories.AuthRepository;
import com.example.peertutoring.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;

/** Sign-up: creates the Firebase account and the backend profile, then goes straight to Home. */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Spinner spinnerRole;
    private View tutorFields;
    private TextInputLayout tilTutorSubjects;
    private TextInputLayout tilTutorCredentials;
    private EditText etTutorSubjects;
    private EditText etTutorCredentials;
    private MaterialButton btnRegister;
    private View progressBar;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository(this);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        tutorFields = findViewById(R.id.tutorFields);
        tilTutorSubjects = findViewById(R.id.tilTutorSubjects);
        tilTutorCredentials = findViewById(R.id.tilTutorCredentials);
        etTutorSubjects = findViewById(R.id.etTutorSubjects);
        etTutorCredentials = findViewById(R.id.etTutorCredentials);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.roles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Tutors apply with the subjects they can teach; an admin reviews the application
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tutorFields.setVisibility(isTutorSelected() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnRegister.setOnClickListener(v -> attemptRegistration());
    }

    private boolean isTutorSelected() {
        Object selected = spinnerRole.getSelectedItem();
        return selected != null && "tutor".equals(selected.toString().toLowerCase(Locale.ROOT));
    }

    private void attemptRegistration() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilTutorSubjects.setError(null);
        tilTutorCredentials.setError(null);

        String fullName = ValidationUtils.text(etName);
        String email = ValidationUtils.text(etEmail);
        String password = ValidationUtils.rawText(etPassword);
        String confirm = ValidationUtils.rawText(etConfirmPassword);

        // "Sam van der Berg" -> first name "Sam", last name "van der Berg"; a single name has no last name
        String[] parts = fullName.split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : null;

        boolean valid = true;
        if (fullName.isEmpty()) {
            tilName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!ValidationUtils.isValidName(firstName)
                || (lastName != null && !ValidationUtils.isValidName(lastName))) {
            tilName.setError(getString(R.string.error_name_invalid));
            valid = false;
        }

        String domain = ApiConfig.UNIVERSITY_EMAIL_DOMAIN;
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            valid = false;
        } else if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        } else if (!domain.isEmpty() && !email.toLowerCase(Locale.ROOT).endsWith(domain)) {
            tilEmail.setError(getString(R.string.error_email_domain, domain));
            valid = false;
        }

        if (!ValidationUtils.isPasswordValid(password)) {
            tilPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }
        if (!ValidationUtils.arePasswordsMatching(password, confirm)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_mismatch));
            valid = false;
        }

        boolean isTutor = isTutorSelected();
        List<String> subjectsOffered = ValidationUtils.splitList(ValidationUtils.text(etTutorSubjects));
        String credentials = ValidationUtils.text(etTutorCredentials);
        if (isTutor && subjectsOffered.isEmpty()) {
            tilTutorSubjects.setError(getString(R.string.error_tutor_subjects_required));
            valid = false;
        }
        if (!valid) return;

        // The spinner shows "Student" / "Tutor"; the backend expects lower case
        String role = spinnerRole.getSelectedItem().toString().toLowerCase(Locale.ROOT);

        setLoading(true);
        authRepository.register(email, password, isTutor
                        ? new CreateUserRequest(firstName, lastName, role, subjectsOffered,
                                credentials.isEmpty() ? null : credentials)
                        : new CreateUserRequest(firstName, lastName, role),
                new AuthRepository.ProfileCallback() {
                    @Override
                    public void onSuccess(UserProfile profile) {
                        if (isFinishing() || isDestroyed()) return;
                        ValidationUtils.showToast(RegisterActivity.this, getString(R.string.msg_account_created));
                        openHome();
                    }

                    @Override
                    public void onError(ApiError error) {
                        if (isFinishing() || isDestroyed()) return;
                        setLoading(false);
                        showError(error);
                    }
                });
    }

    /** Puts a message under the input it belongs to, or shows it as a toast when it belongs to none. */
    private void showError(ApiError error) {
        String code = error.getCode();
        String nameError = error.getFieldError("firstName") != null
                ? error.getFieldError("firstName") : error.getFieldError("lastName");

        if (ApiError.CODE_EMAIL_IN_USE.equals(code) || "INVALID_EMAIL_DOMAIN".equals(code)) {
            tilEmail.setError(error.getMessage());
        } else if (ApiError.CODE_WEAK_PASSWORD.equals(code)) {
            tilPassword.setError(error.getMessage());
        } else if (error.getFieldError("subjectsOffered") != null) {
            tilTutorSubjects.setError(error.getFieldError("subjectsOffered"));
        } else if (error.getFieldError("credentials") != null) {
            tilTutorCredentials.setError(error.getFieldError("credentials"));
        } else if (nameError != null) {
            tilName.setError(nameError);
        } else {
            ValidationUtils.showToast(this, error.getMessage());
        }
    }

    /** Opens Home and clears the back stack, so Back cannot return to the sign-up form. */
    private void openHome() {
        Intent intent = new Intent(this, StudentHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
