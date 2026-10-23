package com.example.peertutoring.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.adapters.SessionAdapter;
import com.example.peertutoring.models.TutorSession;
import com.example.peertutoring.models.UpdateSessionRequest;
import com.example.peertutoring.network.ApiCallback;
import com.example.peertutoring.network.ApiClient;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.SessionManager;
import com.example.peertutoring.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;


public class StudentHomeActivity extends AppCompatActivity {

    private RecyclerView rvSessions;
    private View tvEmptySessions;
    private SessionAdapter sessionAdapter;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        //Header, bottom navigation and greeting. The Admin buttons are hidden unless the user is an admin.
        NavHelper.setup(this, R.id.btnHome, false);
        if (isFinishing()) return;   // not logged in: sent to Login
        NavHelper.showRoleDialogOnce(this);   // popup: which role am I signed in as?

        //Quick actions grid
        findViewById(R.id.btnQuickSearch).setOnClickListener(v ->
                startActivity(new Intent(this, TutorSearchActivity.class)));
        findViewById(R.id.btnQuickMessages).setOnClickListener(v ->
                startActivity(new Intent(this, MessagesActivity.class)));
        findViewById(R.id.btnQuickProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        View quickAdmin = findViewById(R.id.btnQuickAdmin);
        if (new SessionManager(this).isAdmin()) {   // hidden in the layout; only admins see it
            quickAdmin.setVisibility(View.VISIBLE);
            quickAdmin.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminHomeActivity.class)));
        }

        //Upcoming sessions list
        myUid = new SessionManager(this).getString("uid");
        rvSessions = findViewById(R.id.rvSessions);
        tvEmptySessions = findViewById(R.id.tvEmptySessions);
        sessionAdapter = new SessionAdapter(myUid, new SessionAdapter.Listener() {
            @Override
            public void onPrimary(TutorSession session, SessionAdapter.Action action) {
                switch (action) {
                    case CANCEL:
                        confirmCancel(session);
                        break;
                    case RATE:
                        Intent intent = new Intent(StudentHomeActivity.this, RatingActivity.class);
                        intent.putExtra(RatingActivity.EXTRA_SESSION_ID, session.getId());
                        intent.putExtra(RatingActivity.EXTRA_TUTOR_NAME, session.getTutorName());
                        startActivity(intent);
                        break;
                    case ACCEPT_OR_DECLINE:
                        updateStatus(session, "accepted");
                        break;
                    case COMPLETE:
                        updateStatus(session, "completed");
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onSecondary(TutorSession session, SessionAdapter.Action action) {
                if (action == SessionAdapter.Action.ACCEPT_OR_DECLINE) {
                    updateStatus(session, "declined");
                }
            }
        });
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        rvSessions.setAdapter(sessionAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSessions();   // refresh after booking, cancelling or rating
    }

    /** Upcoming sessions: open requests, plus completed ones the student can still rate. */
    private void loadSessions() {
        ApiClient.get().getSessions().enqueue(new ApiCallback<List<TutorSession>>() {
            @Override
            public void onSuccess(List<TutorSession> sessions) {
                if (isFinishing() || isDestroyed()) return;
                List<TutorSession> shown = new ArrayList<>();
                if (sessions != null) {
                    for (TutorSession session : sessions) {
                        String status = session.getStatus();
                        boolean open = "pending".equals(status) || "accepted".equals(status);
                        boolean rateable = SessionAdapter.actionFor(session, myUid) == SessionAdapter.Action.RATE;
                        if (open || rateable) shown.add(session);
                    }
                }
                showSessions(shown);
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(StudentHomeActivity.this, error.getMessage());
            }
        });
    }

    /** Puts a list of sessions on screen, or the empty message when the list is empty. */
    private void showSessions(List<TutorSession> sessions) {
        sessionAdapter.setSessions(sessions);
        boolean empty = sessions == null || sessions.isEmpty();
        rvSessions.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmptySessions.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void confirmCancel(TutorSession session) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.session_cancel_title)
                .setMessage(getString(R.string.session_cancel_message,
                        session.getSubject() == null ? "" : session.getSubject(),
                        session.getTutorName() == null ? getString(R.string.unknown_name) : session.getTutorName()))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_cancel_session, (dialog, which) -> cancelSession(session))
                .show();
    }

    private void cancelSession(TutorSession session) {
        ApiClient.get().cancelSession(session.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void body) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(StudentHomeActivity.this, getString(R.string.session_cancelled_toast));
                loadSessions();
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(StudentHomeActivity.this, error.getMessage());
            }
        });
    }

    private void updateStatus(TutorSession session, String status) {
        ApiClient.get().updateSession(session.getId(), new UpdateSessionRequest(status)).enqueue(new ApiCallback<TutorSession>() {
            @Override
            public void onSuccess(TutorSession updated) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(StudentHomeActivity.this, getString(R.string.session_updated_toast));
                loadSessions();
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(StudentHomeActivity.this, error.getMessage());
            }
        });
    }
}
