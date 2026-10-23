package com.example.peertutoring.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.adapters.NotificationAdapter;
import com.example.peertutoring.models.AppNotification;
import com.example.peertutoring.network.ApiCallback;
import com.example.peertutoring.network.ApiClient;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.ValidationUtils;

import java.util.List;

/** Session updates and other news for the signed-in user. Tapping one marks it as read. */
public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private View tvEmpty;
    private View tvMarkAllRead;
    private NotificationAdapter adapter;
    private List<AppNotification> current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        NavHelper.setup(this, R.id.btnHome, true);

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmptyNotifications);
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead);

        adapter = new NotificationAdapter(this::markRead);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        tvMarkAllRead.setOnClickListener(v -> markAllRead());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ApiClient.get().getNotifications().enqueue(new ApiCallback<List<AppNotification>>() {
            @Override
            public void onSuccess(List<AppNotification> notifications) {
                if (isFinishing()) return;
                current = notifications;
                adapter.setNotifications(notifications);
                boolean empty = notifications == null || notifications.isEmpty();
                rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                tvMarkAllRead.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing()) return;
                ValidationUtils.showToast(NotificationsActivity.this, error.getMessage());
            }
        });
    }

    private void markRead(AppNotification notification) {
        if (notification.isRead()) return;
        ApiClient.get().markNotificationRead(notification.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void body) {
                notification.setRead(true);
                adapter.refresh();
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing()) return;
                ValidationUtils.showToast(NotificationsActivity.this, error.getMessage());
            }
        });
    }

    private void markAllRead() {
        ApiClient.get().markAllNotificationsRead().enqueue(new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void body) {
                if (current != null) {
                    for (AppNotification notification : current) notification.setRead(true);
                }
                adapter.refresh();
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing()) return;
                ValidationUtils.showToast(NotificationsActivity.this, error.getMessage());
            }
        });
    }
}
