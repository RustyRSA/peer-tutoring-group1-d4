package com.example.peertutoring.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.models.TutorSession;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The session list on the Home screen. Each row offers the buttons that make sense for the viewer:
 * a student can cancel an open request or rate a completed one; a tutor can accept, decline or
 * complete a request. Call setSessions() whenever the data changes.
 */
public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    /** What the buttons on a row do. */
    public enum Action { NONE, CANCEL, RATE, ACCEPT_OR_DECLINE, COMPLETE }

    public interface Listener {
        /** The row's main button (Cancel, Rate, Accept or Complete). */
        void onPrimary(TutorSession session, Action action);

        /** The row's second button (Decline). */
        void onSecondary(TutorSession session, Action action);
    }

    private final List<TutorSession> sessions = new ArrayList<>();
    private final String myUid;
    private final Listener listener;

    public SessionAdapter(String myUid, Listener listener) {
        this.myUid = myUid;
        this.listener = listener;
    }

    public void setSessions(List<TutorSession> newSessions) {
        sessions.clear();
        if (newSessions != null) {
            sessions.addAll(newSessions);
        }
        notifyDataSetChanged();
    }

    /** Which buttons a session gets for the signed-in user. */
    public static Action actionFor(TutorSession session, String myUid) {
        String status = session.getStatus() == null ? "" : session.getStatus();
        boolean iAmStudent = myUid != null && myUid.equals(session.getStudentId());
        boolean iAmTutor = myUid != null && myUid.equals(session.getTutorId());

        if (iAmStudent) {
            if ("pending".equals(status) || "accepted".equals(status)) return Action.CANCEL;
            if ("completed".equals(status) && !session.isRated()) return Action.RATE;
        } else if (iAmTutor) {
            if ("pending".equals(status)) return Action.ACCEPT_OR_DECLINE;
            if ("accepted".equals(status)) return Action.COMPLETE;
        }
        return Action.NONE;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        TutorSession session = sessions.get(position);
        Context context = holder.itemView.getContext();

        // Show the other person: the tutor for a student, the student for a tutor
        boolean iAmTutor = myUid != null && myUid.equals(session.getTutorId());
        String other = iAmTutor ? session.getStudentName() : session.getTutorName();
        other = other == null ? "" : other.trim();

        holder.tvInitials.setText(TutorNames.initials(other));
        holder.tvSubject.setText(session.getSubject() == null ? "" : session.getSubject());
        holder.tvWith.setText(context.getString(R.string.with_tutor,
                other.isEmpty() ? context.getString(R.string.unknown_name) : other));
        holder.tvTime.setText(session.getPreferredTime() == null ? "" : session.getPreferredTime());
        holder.tvStatus.setText(capitalise(session.getStatus()));

        Action action = actionFor(session, myUid);
        holder.btnPrimary.setVisibility(action == Action.NONE ? View.GONE : View.VISIBLE);
        holder.btnSecondary.setVisibility(action == Action.ACCEPT_OR_DECLINE ? View.VISIBLE : View.GONE);
        switch (action) {
            case CANCEL:
                holder.btnPrimary.setText(R.string.btn_cancel_session);
                break;
            case RATE:
                holder.btnPrimary.setText(R.string.btn_rate_tutor);
                break;
            case ACCEPT_OR_DECLINE:
                holder.btnPrimary.setText(R.string.btn_accept);
                holder.btnSecondary.setText(R.string.btn_decline);
                break;
            case COMPLETE:
                holder.btnPrimary.setText(R.string.btn_mark_completed);
                break;
            default:
                break;
        }

        holder.btnPrimary.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                TutorSession clicked = sessions.get(adapterPosition);
                listener.onPrimary(clicked, actionFor(clicked, myUid));
            }
        });
        holder.btnSecondary.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                TutorSession clicked = sessions.get(adapterPosition);
                listener.onSecondary(clicked, actionFor(clicked, myUid));
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    private static String capitalise(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        final TextView tvInitials;
        final TextView tvSubject;
        final TextView tvWith;
        final TextView tvTime;
        final TextView tvStatus;
        final MaterialButton btnPrimary;
        final MaterialButton btnSecondary;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tvSessionInitials);
            tvSubject = itemView.findViewById(R.id.tvSessionSubject);
            tvWith = itemView.findViewById(R.id.tvSessionWith);
            tvTime = itemView.findViewById(R.id.tvSessionTime);
            tvStatus = itemView.findViewById(R.id.tvSessionStatus);
            btnPrimary = itemView.findViewById(R.id.btnSessionPrimary);
            btnSecondary = itemView.findViewById(R.id.btnSessionSecondary);
        }
    }
}
