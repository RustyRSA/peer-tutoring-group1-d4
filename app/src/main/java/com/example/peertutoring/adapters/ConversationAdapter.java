package com.example.peertutoring.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.models.Conversation;

import java.util.ArrayList;
import java.util.List;

/** The inbox list on the Messages screen. */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onOpen(Conversation conversation);
    }

    private final List<Conversation> conversations = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ConversationAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setConversations(List<Conversation> newConversations) {
        conversations.clear();
        if (newConversations != null) {
            conversations.addAll(newConversations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        String name = conversation.getOtherName() == null ? "" : conversation.getOtherName().trim();

        holder.tvInitials.setText(TutorNames.initials(name));
        holder.tvName.setText(name.isEmpty() ? holder.itemView.getContext().getString(R.string.unknown_name) : name);
        holder.tvLastMessage.setText(conversation.getLastMessage() == null ? "" : conversation.getLastMessage());

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onOpen(conversations.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        final TextView tvInitials;
        final TextView tvName;
        final TextView tvLastMessage;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tvConversationInitials);
            tvName = itemView.findViewById(R.id.tvConversationName);
            tvLastMessage = itemView.findViewById(R.id.tvConversationLast);
        }
    }
}
