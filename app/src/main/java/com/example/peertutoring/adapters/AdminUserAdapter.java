package com.example.peertutoring.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.models.UserProfile;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rows of users on the Admin screen. In APPLICATION mode each row has Approve and Reject buttons;
 * in ACCOUNT mode it has one Suspend or Reactivate button.
 */
public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    public enum Mode { APPLICATION, ACCOUNT }

    public interface Listener {
        /** Approve (applications) or Suspend / Reactivate (accounts). */
        void onPrimary(UserProfile user);

        /** Reject (applications only). */
        void onSecondary(UserProfile user);
    }

    private final List<UserProfile> users = new ArrayList<>();
    private final Mode mode;
    private final Listener listener;

    public AdminUserAdapter(Mode mode, Listener listener) {
        this.mode = mode;
        this.listener = listener;
    }

    public void setUsers(List<UserProfile> newUsers) {
        users.clear();
        if (newUsers != null) {
            users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserProfile user = users.get(position);
        Context context = holder.itemView.getContext();

        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String name = (first + " " + last).trim();
        holder.tvName.setText(name.isEmpty() ? context.getString(R.string.unknown_name) : name);

        if (mode == Mode.APPLICATION) {
            List<String> subjects = user.getSubjectsOffered();
            String subjectText = subjects == null || subjects.isEmpty()
                    ? context.getString(R.string.no_subjects_listed) : TextUtils.join(", ", subjects);
            String credentials = user.getCredentials();
            holder.tvDetails.setText(credentials == null || credentials.isEmpty()
                    ? subjectText : subjectText + "\n" + credentials);

            holder.btnPrimary.setText(R.string.btn_approve);
            holder.btnSecondary.setText(R.string.btn_reject);
            holder.btnSecondary.setVisibility(View.VISIBLE);
        } else {
            boolean suspended = "suspended".equals(user.getAccountStatus());
            holder.tvDetails.setText(context.getString(R.string.admin_account_details,
                    capitalise(user.getRole()), user.getEmail() == null ? "" : user.getEmail(),
                    capitalise(user.getAccountStatus())));

            holder.btnPrimary.setText(suspended ? R.string.btn_reactivate : R.string.btn_suspend);
            holder.btnSecondary.setVisibility(View.GONE);
        }

        holder.btnPrimary.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onPrimary(users.get(adapterPosition));
            }
        });
        holder.btnSecondary.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onSecondary(users.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    private static String capitalise(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvDetails;
        final MaterialButton btnPrimary;
        final MaterialButton btnSecondary;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAdminUserName);
            tvDetails = itemView.findViewById(R.id.tvAdminUserDetails);
            btnPrimary = itemView.findViewById(R.id.btnAdminPrimary);
            btnSecondary = itemView.findViewById(R.id.btnAdminSecondary);
        }
    }
}
