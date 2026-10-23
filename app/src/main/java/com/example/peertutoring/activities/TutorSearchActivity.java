package com.example.peertutoring.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.adapters.TutorAdapter;
import com.example.peertutoring.models.UserProfile;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.repositories.AuthRepository;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.ValidationUtils;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Lists the approved tutors. The search box filters by tutor name or subject, the Subject chip
 * keeps one subject, and the Rating chip keeps tutors rated 4 or higher.
 */
public class TutorSearchActivity extends AppCompatActivity {

    private static final long SEARCH_DELAY_MS = 350;
    private static final double MIN_RATING = 4.0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private int latestRequest = 0;

    private RecyclerView rvTutors;
    private View tvEmptyTutors;
    private View progressBar;
    private EditText etSearch;
    private Chip chipSubject, chipRating;
    private TutorAdapter tutorAdapter;
    private AuthRepository authRepository;

    private String selectedSubject = null;                    // null = all subjects
    private final List<String> knownSubjects = new ArrayList<>();
    private List<UserProfile> lastResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_search);

        authRepository = new AuthRepository(this);

        //Header, bottom navigation and greeting. The Admin buttons are hidden unless the user is an admin.
        NavHelper.setup(this, R.id.btnSearch, false);
        if (isFinishing()) return;   // not logged in: sent to Login

        //Tutor list
        rvTutors = findViewById(R.id.rvTutors);
        tvEmptyTutors = findViewById(R.id.tvEmptyTutors);
        progressBar = findViewById(R.id.progressBar);
        tutorAdapter = new TutorAdapter(tutor -> {
            Intent intent = new Intent(this, TutorProfileActivity.class);
            intent.putExtra(TutorProfileActivity.EXTRA_TUTOR_ID, tutor.getUid());
            startActivity(intent);
        });
        rvTutors.setLayoutManager(new LinearLayoutManager(this));
        rvTutors.setAdapter(tutorAdapter);

        //Search box: wait until the user stops typing, then ask the backend
        etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
                pendingSearch = () -> fetchTutors();
                handler.postDelayed(pendingSearch, SEARCH_DELAY_MS);
            }
        });

        //Filter chips
        chipSubject = findViewById(R.id.chipSubject);
        chipRating = findViewById(R.id.chipRating);
        chipSubject.setOnClickListener(v -> chooseSubject());
        chipRating.setOnCheckedChangeListener((button, checked) -> showTutors(lastResults));

        fetchTutors();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
    }

    /** Asks the backend for tutors matching the search text and the chosen subject. */
    private void fetchTutors() {
        final int requestId = ++latestRequest;   // ignore slow answers to older searches
        final String query = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        final boolean unfiltered = query.isEmpty() && selectedSubject == null;

        progressBar.setVisibility(View.VISIBLE);
        tvEmptyTutors.setVisibility(View.GONE);

        authRepository.searchTutors(query, selectedSubject, new AuthRepository.TutorsCallback() {
            @Override
            public void onSuccess(List<UserProfile> tutors) {
                if (isFinishing() || isDestroyed() || requestId != latestRequest) return;
                progressBar.setVisibility(View.GONE);
                lastResults = tutors == null ? new ArrayList<>() : tutors;
                if (unfiltered) rememberSubjects(lastResults);   // the full list is what the Subject chip offers
                showTutors(lastResults);
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed() || requestId != latestRequest) return;
                progressBar.setVisibility(View.GONE);
                ValidationUtils.showToast(TutorSearchActivity.this, error.getMessage());
                lastResults = new ArrayList<>();
                showTutors(lastResults);
            }
        });
    }

    private void rememberSubjects(List<UserProfile> tutors) {
        TreeSet<String> subjects = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (UserProfile tutor : tutors) {
            if (tutor.getSubjectsOffered() != null) subjects.addAll(tutor.getSubjectsOffered());
        }
        knownSubjects.clear();
        knownSubjects.addAll(subjects);
    }

    /** Lets the student pick one subject (or all) from the subjects the tutors offer. */
    private void chooseSubject() {
        if (knownSubjects.isEmpty()) {
            ValidationUtils.showToast(this, getString(R.string.filter_no_subjects));
            return;
        }
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.filter_all_subjects));
        options.addAll(knownSubjects);

        new AlertDialog.Builder(this)
                .setTitle(R.string.filter_pick_subject)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    selectedSubject = which == 0 ? null : options.get(which);
                    chipSubject.setText(selectedSubject == null
                            ? getString(R.string.filter_subject)
                            : getString(R.string.filter_subject_selected, selectedSubject));
                    fetchTutors();
                })
                .show();
    }

    /** Applies the Rating chip, then puts the list on screen (or the empty message). */
    private void showTutors(List<UserProfile> tutors) {
        List<UserProfile> shown = new ArrayList<>();
        if (tutors != null) {
            for (UserProfile tutor : tutors) {
                UserProfile.Rating rating = tutor.getRating();
                boolean good = rating != null && rating.getCount() > 0 && rating.getAverage() >= MIN_RATING;
                if (!chipRating.isChecked() || good) shown.add(tutor);
            }
        }
        tutorAdapter.setTutors(shown);
        boolean empty = shown.isEmpty();
        rvTutors.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmptyTutors.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
