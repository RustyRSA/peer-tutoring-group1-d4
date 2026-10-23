package com.example.peertutoring.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.peertutoring.R;
import com.example.peertutoring.models.Review;
import com.example.peertutoring.models.UserProfile;
import com.example.peertutoring.network.ApiCallback;
import com.example.peertutoring.network.ApiClient;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.repositories.AuthRepository;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One tutor's public profile, with buttons to request a session or send a message. */
public class TutorProfileActivity extends AppCompatActivity {

    public static final String EXTRA_TUTOR_ID = "tutorId";

    private String tutorId;
    private String tutorName = "";
    private List<String> subjects = new ArrayList<>();

    private TextView tvInitials, tvName, tvRating, tvYear, tvStudies, tvCredentials, tvSubjects, tvNoReviews;
    private LinearLayout reviewsContainer;
    private View btnRequest, btnMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_profile);

        NavHelper.setup(this, R.id.btnSearch, true);

        tutorId = getIntent().getStringExtra(EXTRA_TUTOR_ID);
        if (tutorId == null || tutorId.isEmpty()) {
            finish();   // opened without a tutor to show
            return;
        }

        tvInitials = findViewById(R.id.tvTutorProfileInitials);
        tvName = findViewById(R.id.tvTutorProfileName);
        tvRating = findViewById(R.id.tvTutorProfileRating);
        tvYear = findViewById(R.id.tvTutorProfileYear);
        tvStudies = findViewById(R.id.tvTutorProfileStudies);
        tvCredentials = findViewById(R.id.tvTutorCredentials);
        tvSubjects = findViewById(R.id.tvTutorSubjectsOffered);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        reviewsContainer = findViewById(R.id.reviewsContainer);
        btnRequest = findViewById(R.id.btnRequestSession);
        btnMessage = findViewById(R.id.btnMessageTutor);

        btnRequest.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingRequestActivity.class);
            intent.putExtra(BookingRequestActivity.EXTRA_TUTOR_ID, tutorId);
            intent.putExtra(BookingRequestActivity.EXTRA_TUTOR_NAME, tutorName);
            intent.putStringArrayListExtra(BookingRequestActivity.EXTRA_SUBJECTS, new ArrayList<>(subjects));
            startActivity(intent);
        });
        btnMessage.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_OTHER_UID, tutorId);
            intent.putExtra(ChatActivity.EXTRA_OTHER_NAME, tutorName);
            startActivity(intent);
        });

        loadTutor();
        loadReviews();
    }

    private void loadTutor() {
        new AuthRepository(this).getUserById(tutorId, new AuthRepository.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile tutor) {
                if (isFinishing() || isDestroyed() || tutor == null) return;
                showTutor(tutor);
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(TutorProfileActivity.this, error.getMessage());
            }
        });
    }

    private void showTutor(UserProfile tutor) {
        String first = tutor.getFirstName() == null ? "" : tutor.getFirstName().trim();
        String last = tutor.getLastName() == null ? "" : tutor.getLastName().trim();
        tutorName = (first + " " + last).trim();
        subjects = tutor.getSubjectsOffered() == null ? new ArrayList<>() : tutor.getSubjectsOffered();

        StringBuilder initials = new StringBuilder();
        if (!first.isEmpty()) initials.append(Character.toUpperCase(first.charAt(0)));
        if (!last.isEmpty()) initials.append(Character.toUpperCase(last.charAt(0)));
        tvInitials.setText(initials.length() == 0 ? "?" : initials.toString());
        tvName.setText(tutorName.isEmpty() ? getString(R.string.unknown_name) : tutorName);

        UserProfile.Rating rating = tutor.getRating();
        tvRating.setText(rating == null || rating.getCount() == 0
                ? getString(R.string.tutor_profile_no_rating)
                : String.format(Locale.getDefault(), getString(R.string.tutor_profile_rating_format),
                rating.getAverage(), rating.getCount()));

        if (tutor.getYearOfStudy() != null) {
            tvYear.setText(getString(R.string.tutor_profile_year_format, tutor.getYearOfStudy()));
            tvYear.setVisibility(View.VISIBLE);
        } else {
            tvYear.setVisibility(View.GONE);
        }

        List<String> studies = new ArrayList<>();
        if (tutor.getProgramme() != null && !tutor.getProgramme().isEmpty()) studies.add(tutor.getProgramme());
        if (tutor.getUniversity() != null && !tutor.getUniversity().isEmpty()) studies.add(tutor.getUniversity());
        tvStudies.setText(TextUtils.join(", ", studies));
        tvStudies.setVisibility(studies.isEmpty() ? View.GONE : View.VISIBLE);

        String credentials = tutor.getCredentials();
        tvCredentials.setText(credentials == null || credentials.isEmpty()
                ? getString(R.string.no_credentials) : credentials);
        tvSubjects.setText(subjects.isEmpty() ? getString(R.string.no_subjects_listed) : TextUtils.join(", ", subjects));

        btnRequest.setEnabled(true);
        btnMessage.setEnabled(true);
    }

    private void loadReviews() {
        ApiClient.get().getTutorReviews(tutorId).enqueue(new ApiCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> reviews) {
                if (isFinishing() || isDestroyed()) return;
                reviewsContainer.removeAllViews();
                boolean empty = reviews == null || reviews.isEmpty();
                tvNoReviews.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (empty) return;
                for (Review review : reviews) {
                    addReview(review);
                }
            }

            @Override
            public void onError(ApiError error) {
                // The profile is still useful without reviews
            }
        });
    }

    private void addReview(Review review) {
        String student = review.getStudentName() == null || review.getStudentName().isEmpty()
                ? getString(R.string.unknown_name) : review.getStudentName();

        TextView header = new TextView(this);
        header.setText(getString(R.string.review_header_format, review.getStars(), student));
        header.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        reviewsContainer.addView(header);

        TextView body = new TextView(this);
        body.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        if (review.getComment() != null && !review.getComment().isEmpty()) {
            body.setText(review.getComment());
        } else {
            body.setVisibility(View.GONE);
        }
        reviewsContainer.addView(body);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(1, Math.round(12 * getResources().getDisplayMetrics().density)));
        reviewsContainer.addView(spacer);
    }
}
