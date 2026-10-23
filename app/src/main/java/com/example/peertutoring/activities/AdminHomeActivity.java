package com.example.peertutoring.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.adapters.AdminUserAdapter;
import com.example.peertutoring.models.StatusUpdateRequest;
import com.example.peertutoring.models.UserProfile;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.repositories.AuthRepository;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.SessionManager;
import com.example.peertutoring.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

/** Admin only: user numbers, tutor applications to review, and account suspension. */
public class AdminHomeActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private String myUid;

    private AdminUserAdapter applicationsAdapter;
    private AdminUserAdapter accountsAdapter;
    private RecyclerView rvApplications, rvAccounts;
    private View tvEmptyApplications, tvEmptyAccounts;
    private TextView tvStatUsers, tvStatTutors, tvStatPending, tvStatSuspended;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        NavHelper.setup(this, R.id.btnAdmin, false);

        SessionManager session = new SessionManager(this);
        if (!session.isAdmin()) {
            finish();   // the backend refuses non-admins anyway; this just avoids an empty screen
            return;
        }
        myUid = session.getString("uid");
        authRepository = new AuthRepository(this);

        tvStatUsers = findViewById(R.id.tvStatUsers);
        tvStatTutors = findViewById(R.id.tvStatTutors);
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatSuspended = findViewById(R.id.tvStatSuspended);
        tvEmptyApplications = findViewById(R.id.tvEmptyApplications);
        tvEmptyAccounts = findViewById(R.id.tvEmptyAccounts);

        applicationsAdapter = new AdminUserAdapter(AdminUserAdapter.Mode.APPLICATION, new AdminUserAdapter.Listener() {
            @Override
            public void onPrimary(UserProfile user) {
                updateStatus(user, new StatusUpdateRequest("approved", null));
            }

            @Override
            public void onSecondary(UserProfile user) {
                confirm(R.string.admin_reject_title, getString(R.string.admin_reject_message, displayName(user)),
                        R.string.btn_reject, () -> updateStatus(user, new StatusUpdateRequest("rejected", null)));
            }
        });
        accountsAdapter = new AdminUserAdapter(AdminUserAdapter.Mode.ACCOUNT, new AdminUserAdapter.Listener() {
            @Override
            public void onPrimary(UserProfile user) {
                if ("suspended".equals(user.getAccountStatus())) {
                    updateStatus(user, new StatusUpdateRequest(null, "active"));
                } else {
                    confirm(R.string.admin_suspend_title, getString(R.string.admin_suspend_message, displayName(user)),
                            R.string.btn_suspend, () -> updateStatus(user, new StatusUpdateRequest(null, "suspended")));
                }
            }

            @Override
            public void onSecondary(UserProfile user) {
                // not used in ACCOUNT mode
            }
        });

        rvApplications = findViewById(R.id.rvApplications);
        rvApplications.setLayoutManager(new LinearLayoutManager(this));
        rvApplications.setAdapter(applicationsAdapter);
        rvAccounts = findViewById(R.id.rvAccounts);
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvAccounts.setAdapter(accountsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authRepository != null) loadUsers();
    }

    private void loadUsers() {
        authRepository.getAllUsers(new AuthRepository.TutorsCallback() {
            @Override
            public void onSuccess(List<UserProfile> users) {
                if (isFinishing() || isDestroyed()) return;
                showUsers(users == null ? new ArrayList<>() : users);
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(AdminHomeActivity.this, error.getMessage());
            }
        });
    }

    /** Bars under the stat tiles (users, tutors, pending, suspended), scaled to the biggest number. */
    private void setBars(int... values) {
        int[] ids = {R.id.barUsers, R.id.barTutors, R.id.barPending, R.id.barSuspended};
        int max = 1;
        for (int v : values) max = Math.max(max, v);
        float density = getResources().getDisplayMetrics().density;
        int maxPx = (int) (100 * density);
        int minPx = (int) (4 * density);
        for (int i = 0; i < ids.length && i < values.length; i++) {
            View bar = findViewById(ids[i]);
            if (bar == null) continue;
            ViewGroup.LayoutParams params = bar.getLayoutParams();
            params.height = Math.max(minPx, maxPx * values[i] / max);
            bar.setLayoutParams(params);
        }
    }

    private void showUsers(List<UserProfile> users) {
        List<UserProfile> pending = new ArrayList<>();
        List<UserProfile> others = new ArrayList<>();
        int tutors = 0;
        int suspended = 0;
        for (UserProfile user : users) {
            if ("pending".equals(user.getTutorStatus())) pending.add(user);
            if ("approved".equals(user.getTutorStatus())) tutors++;
            if ("suspended".equals(user.getAccountStatus())) suspended++;
            if (user.getUid() != null && !user.getUid().equals(myUid)) others.add(user);   // admins cannot suspend themselves
        }

        tvStatUsers.setText(String.valueOf(users.size()));
        tvStatTutors.setText(String.valueOf(tutors));
        tvStatPending.setText(String.valueOf(pending.size()));
        tvStatSuspended.setText(String.valueOf(suspended));
        setBars(users.size(), tutors, pending.size(), suspended);

        applicationsAdapter.setUsers(pending);
        rvApplications.setVisibility(pending.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptyApplications.setVisibility(pending.isEmpty() ? View.VISIBLE : View.GONE);

        accountsAdapter.setUsers(others);
        rvAccounts.setVisibility(others.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptyAccounts.setVisibility(others.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateStatus(UserProfile user, StatusUpdateRequest request) {
        authRepository.updateUserStatus(user.getUid(), request, new AuthRepository.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(AdminHomeActivity.this, getString(R.string.admin_updated));
                loadUsers();
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing() || isDestroyed()) return;
                ValidationUtils.showToast(AdminHomeActivity.this, error.getMessage());
            }
        });
    }

    private void confirm(int titleRes, String message, int positiveRes, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(positiveRes, (dialog, which) -> onConfirm.run())
                .show();
    }

    private static String displayName(UserProfile user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "This user" : name;
    }
}
