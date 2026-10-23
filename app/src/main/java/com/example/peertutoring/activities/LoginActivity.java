package com.example.peertutoring.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.models.UserProfile;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.repositories.AuthRepository;
import com.example.peertutoring.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

//Login screen. Also the first screen after app start: a remembered user skips straight to Home.
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private EditText etEmail;
    private EditText etPassword;
    private MaterialButton btnLogin;
    private View tvRegisterLink;
    private View tvForgotPassword;
    private View progressBar;

    private AuthRepository authRepository;

    //Shared by a normal login and by restoring a remembered session.
    private final AuthRepository.ProfileCallback loginCallback = new AuthRepository.ProfileCallback() {
        @Override
        public void onSuccess(UserProfile profile) {
            if (isFinishing() || isDestroyed()) return;
            openHome();
        }

        @Override
        public void onError(ApiError error) {
            if (isFinishing() || isDestroyed()) return;
            setLoading(false);
            if (ApiError.CODE_INVALID_CREDENTIALS.equals(error.getCode())) {
                tilPassword.setError(error.getMessage());
            } else {
                ValidationUtils.showToast(LoginActivity.this, error.getMessage());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository(this);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));


        //Temporarely commented out for testing.
        // Firebase remembers the login between app starts; only the profile has to be loaded again
        // if (AuthRepository.isLoggedIn()) {
        //     setLoading(true);
        //     authRepository.restoreSession(loginCallback);
        //}
    }

    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = ValidationUtils.text(etEmail);
        String password = ValidationUtils.rawText(etPassword);

        boolean valid = true;
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            valid = false;
        } else if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            valid = false;
        }
        if (!valid) return;

        setLoading(true);
        authRepository.login(email, password, loginCallback);
    }

    private void sendPasswordReset() {
        tilEmail.setError(null);
        String email = ValidationUtils.text(etEmail);
        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError(getString(email.isEmpty()
                    ? R.string.error_email_required : R.string.error_email_invalid));
            return;
        }

        setLoading(true);
        authRepository.sendPasswordReset(email, new AuthRepository.ResultCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                ValidationUtils.showToast(LoginActivity.this, getString(R.string.msg_reset_sent));
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                ValidationUtils.showToast(LoginActivity.this, error.getMessage());
            }
        });
    }

    //Opens Home and clears the back stack, so Back cannot return to the login form.
    private void openHome() {
        Intent intent = new Intent(this, StudentHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Shows the spinner and blocks the buttons while a request is running.
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        tvRegisterLink.setEnabled(!loading);
        tvForgotPassword.setEnabled(!loading);
    }
}
