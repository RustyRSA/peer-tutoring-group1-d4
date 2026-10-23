package com.example.peertutoring.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peertutoring.R;
import com.example.peertutoring.models.CreateSessionRequest;
import com.example.peertutoring.models.TutorSession;
import com.example.peertutoring.network.ApiCallback;
import com.example.peertutoring.network.ApiClient;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.ValidationUtils;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/** A student asks one tutor for a session. Opened from the Tutor profile screen. */
public class BookingRequestActivity extends AppCompatActivity {

    /** Extras the Tutor profile screen passes along when the student taps "Request Session". */
    public static final String EXTRA_TUTOR_ID = "tutorId";
    public static final String EXTRA_TUTOR_NAME = "tutorName";
    public static final String EXTRA_SUBJECTS = "subjects";   // ArrayList<String>

    private String tutorId;

    private TextInputLayout tilSubject, tilDate, tilTime, tilTopic;
    private MaterialAutoCompleteTextView actSubject;
    private TextInputEditText etDate, etTime, etTopic;
    private View btnSend;

    // Chosen date and time (null until the student picks them)
    private Integer year, month, day, hour, minute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_request);

        // Keep "Search" highlighted on the bottom nav, with a back button
        NavHelper.setup(this, R.id.btnSearch, true);

        tutorId = getIntent().getStringExtra(EXTRA_TUTOR_ID);
        if (tutorId == null || tutorId.isEmpty()) {
            finish();   // opened without a tutor to book
            return;
        }
        String tutorName = getIntent().getStringExtra(EXTRA_TUTOR_NAME);
        ((TextView) findViewById(R.id.tvBookingTutor)).setText(getString(R.string.booking_with,
                tutorName == null || tutorName.isEmpty() ? getString(R.string.unknown_name) : tutorName));

        tilSubject = findViewById(R.id.tilBookingSubject);
        tilDate = findViewById(R.id.tilBookingDate);
        tilTime = findViewById(R.id.tilBookingTime);
        tilTopic = findViewById(R.id.tilBookingTopic);
        actSubject = findViewById(R.id.actBookingSubject);
        etDate = findViewById(R.id.etBookingDate);
        etTime = findViewById(R.id.etBookingTime);
        etTopic = findViewById(R.id.etBookingTopic);
        btnSend = findViewById(R.id.btnSendRequest);

        setUpSubjects(getIntent().getStringArrayListExtra(EXTRA_SUBJECTS));
        etDate.setOnClickListener(v -> pickDate());
        etTime.setOnClickListener(v -> pickTime());
        btnSend.setOnClickListener(v -> sendRequest());
    }

    /** The subject is chosen from what the tutor offers; if the list is empty it can be typed. */
    private void setUpSubjects(ArrayList<String> subjects) {
        if (subjects != null && !subjects.isEmpty()) {
            actSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjects));
            if (subjects.size() == 1) actSubject.setText(subjects.get(0), false);
        } else {
            actSubject.setInputType(InputType.TYPE_CLASS_TEXT);
            tilSubject.setEndIconMode(TextInputLayout.END_ICON_NONE);
        }
    }

    private void pickDate() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (picker, y, m, d) -> {
            year = y;
            month = m;
            day = d;
            Calendar chosen = Calendar.getInstance();
            chosen.set(y, m, d);
            etDate.setText(new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(chosen.getTime()));
            tilDate.setError(null);
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);   // no dates in the past
        dialog.show();
    }

    private void pickTime() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (picker, h, min) -> {
            hour = h;
            minute = min;
            etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min));
            tilTime.setError(null);
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
    }

    private void sendRequest() {
        clearErrors();

        String subject = ValidationUtils.text(actSubject);
        String topic = ValidationUtils.text(etTopic);
        boolean valid = true;
        if (subject.isEmpty()) {
            tilSubject.setError(getString(R.string.booking_error_subject));
            valid = false;
        }
        if (year == null) {
            tilDate.setError(getString(R.string.booking_error_date));
            valid = false;
        }
        if (hour == null) {
            tilTime.setError(getString(R.string.booking_error_time));
            valid = false;
        }
        if (topic.isEmpty()) {
            tilTopic.setError(getString(R.string.booking_error_topic));
            valid = false;
        }
        if (valid) {
            Calendar when = Calendar.getInstance();
            when.set(year, month, day, hour, minute, 0);
            if (when.before(Calendar.getInstance())) {
                tilTime.setError(getString(R.string.booking_error_past));
                valid = false;
            }
        }
        if (!valid) return;

        String preferredTime = ValidationUtils.text(etDate) + ", " + ValidationUtils.text(etTime);

        btnSend.setEnabled(false);
        ApiClient.get().createSession(new CreateSessionRequest(tutorId, subject, topic, preferredTime))
                .enqueue(new ApiCallback<TutorSession>() {
                    @Override
                    public void onSuccess(TutorSession session) {
                        if (isFinishing() || isDestroyed()) return;
                        ValidationUtils.showToast(BookingRequestActivity.this, getString(R.string.booking_sent));
                        finish();   // back to the tutor; the request now shows on Home
                    }

                    @Override
                    public void onError(ApiError error) {
                        if (isFinishing() || isDestroyed()) return;
                        btnSend.setEnabled(true);
                        showFieldError(tilSubject, error.getFieldError("subject"));
                        showFieldError(tilTime, error.getFieldError("preferredTime"));
                        showFieldError(tilTopic, error.getFieldError("topic"));
                        ValidationUtils.showToast(BookingRequestActivity.this, error.getMessage());
                    }
                });
    }

    private void clearErrors() {
        tilSubject.setError(null);
        tilDate.setError(null);
        tilTime.setError(null);
        tilTopic.setError(null);
    }

    private static void showFieldError(TextInputLayout layout, String message) {
        if (message != null) layout.setError(message);
    }
}
