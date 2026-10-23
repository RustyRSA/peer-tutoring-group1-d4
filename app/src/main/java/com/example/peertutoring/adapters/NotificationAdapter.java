package com.example.peertutoring.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.models.AppNotification;
import com.example.peertutoring.utils.TimeUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/** The list on the Notifications screen. Unread ones are bold on a light blue card. */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onClick(AppNotification notification);
    }

    private final List<AppNotification> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<AppNotification> newNotifications) {
        notifications.clear();
        if (newNotifications != null) {
            notifications.addAll(newNotifications);
        }
        notifyDataSetChanged();
    }

    /** Redraws after the activity changed a notification's read flag. */
    public void refresh() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification notification = notifications.get(position);

        holder.tvTitle.setText(notification.getTitle() == null ? "" : notification.getTitle());
        holder.tvBody.setText(notification.getBody() == null ? "" : notification.getBody());
        holder.tvTime.setText(TimeUtils.timeAgo(notification.getCreatedAt()));

        boolean unread = !notification.isRead();
        holder.tvTitle.setTypeface(null, unread ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),
                unread ? R.color.brand_blue_light : R.color.white));

        holder.card.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onClick(notifications.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvTitle;
        final TextView tvBody;
        final TextView tvTime;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardNotification);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvBody = itemView.findViewById(R.id.tvNotificationBody);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
        }
    }
}
