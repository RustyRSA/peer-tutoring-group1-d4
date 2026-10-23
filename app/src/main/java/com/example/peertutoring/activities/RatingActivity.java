package com.example.peertutoring.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.peertutoring.R;
import com.example.peertutoring.models.RatingRequest;
import com.example.peertutoring.models.Review;
import com.example.peertutoring.network.ApiCallback;
import com.example.peertutoring.network.ApiClient;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.ValidationUtils;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Lets a student rate the tutor of one completed session. Open it with the session's id and the
 * tutor's name as extras. The backend allows one rating per session.
 */
public class RatingActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_TUTOR_NAME = "tutorName";

    private final ImageView[] stars = new ImageView[5];
    private int selectedStars = 0;
    private String sessionId;
    private TextInputLayout tilComment;
    private View btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        NavHelper.setup(this, R.id.btnHome, true);

        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        if (sessionId == null || sessionId.isEmpty()) {
            finish();   // opened without a session to rate
            return;
        }
        String tutorName = getIntent().getStringExtra(EXTRA_TUTOR_NAME);
        ((TextView) findViewById(R.id.tvRatingSub)).setText(getString(R.string.rating_sub,
                tutorName == null || tutorName.isEmpty() ? getString(R.string.unknown_name) : tutorName));

        int[] starIds = {R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5};
        for (int i = 0; i < stars.length; i++) {
            final int value = i + 1;
            stars[i] = findViewById(starIds[i]);
            stars[i].setOnClickListener(v -> selectStars(value));
        }
        selectStars(0);

        tilComment = findViewById(R.id.tilRatingComment);
        btnSubmit = findViewById(R.id.btnSubmitRating);
        btnSubmit.setOnClickListener(v -> submit());
    }

    /** Colours the first {@code count} stars orange and the rest grey. */
    private void selectStars(int count) {
        selectedStars = count;
        int on = ContextCompat.getColor(this, R.color.icon_profile);
        int off = ContextCompat.getColor(this, R.color.text_secondary);
        for (int i = 0; i < stars.length; i++) {
            stars[i].setColorFilter(i < count ? on : off);
        }
    }

    private void submit() {
        if (selectedStars == 0) {
            ValidationUtils.showToast(this, getString(R.string.rating_pick_stars));
            return;
        }
        String comment = ValidationUtils.text(tilComment.getEditText());
        tilComment.setError(null);

        btnSubmit.setEnabled(false);
        RatingRequest request = new RatingRequest(sessionId, selectedStars, comment.isEmpty() ? null : comment);
        ApiClient.get().submitRating(request).enqueue(new ApiCallback<Review>() {
            @Override
            public void onSuccess(Review review) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(RatingActivity.this, getString(R.string.rating_thanks));
                finish();
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) return;
                btnSubmit.setEnabled(true);
                String commentError = error.getFieldError("comment");
                if (commentError != null) tilComment.setError(commentError);
                ValidationUtils.showToast(RatingActivity.this, error.getMessage());
            }
        });
    }
}
