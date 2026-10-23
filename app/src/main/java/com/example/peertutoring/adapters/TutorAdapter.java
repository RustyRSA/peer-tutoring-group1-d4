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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shows the list of tutors on the Search screen. Call setTutors() whenever the data changes. */
public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.TutorViewHolder> {

    public interface OnTutorClickListener {
        void onViewProfile(UserProfile tutor);
    }

    private final List<UserProfile> tutors = new ArrayList<>();
    private final OnTutorClickListener listener;

    public TutorAdapter(OnTutorClickListener listener) {
        this.listener = listener;
    }

    public void setTutors(List<UserProfile> newTutors) {
        tutors.clear();
        if (newTutors != null) {
            tutors.addAll(newTutors);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TutorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutor, parent, false);
        return new TutorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorViewHolder holder, int position) {
        UserProfile tutor = tutors.get(position);
        Context context = holder.itemView.getContext();

        String first = tutor.getFirstName() == null ? "" : tutor.getFirstName().trim();
        String last = tutor.getLastName() == null ? "" : tutor.getLastName().trim();
        String fullName = (first + " " + last).trim();

        holder.tvInitials.setText(initials(first, last));
        holder.tvName.setText(fullName.isEmpty() ? context.getString(R.string.unknown_name) : fullName);

        List<String> subjects = tutor.getSubjectsOffered();
        holder.tvSubjects.setText(subjects == null || subjects.isEmpty()
                ? context.getString(R.string.no_subjects_listed)
                : TextUtils.join(", ", subjects));

        UserProfile.Rating rating = tutor.getRating();
        holder.tvRating.setText(rating == null || rating.getCount() == 0
                ? context.getString(R.string.no_ratings_yet)
                : String.format(Locale.getDefault(), context.getString(R.string.rating_format),
                rating.getAverage(), rating.getCount()));

        holder.btnViewProfile.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onViewProfile(tutors.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return tutors.size();
    }

    private static String initials(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (!first.isEmpty()) sb.append(Character.toUpperCase(first.charAt(0)));
        if (!last.isEmpty()) sb.append(Character.toUpperCase(last.charAt(0)));
        return sb.length() == 0 ? "?" : sb.toString();
    }

    static class TutorViewHolder extends RecyclerView.ViewHolder {
        final TextView tvInitials;
        final TextView tvSubjects;
        final TextView tvName;
        final TextView tvRating;
        final View btnViewProfile;

        TutorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tvTutorInitials);
            tvSubjects = itemView.findViewById(R.id.tvTutorSubjects);
            tvName = itemView.findViewById(R.id.tvTutorName);
            tvRating = itemView.findViewById(R.id.tvTutorRating);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);

        }
    }
}
