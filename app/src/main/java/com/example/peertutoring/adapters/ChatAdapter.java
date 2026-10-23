package com.example.peertutoring.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/** The bubbles on the chat screen: blue on the right for my messages, grey on the left for theirs. */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String myUid;

    public ChatAdapter(String myUid) {
        this.myUid = myUid;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        String sender = messages.get(position).getSenderId();
        return sender != null && sender.equals(myUid) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_SENT ? R.layout.item_message_sent : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        String text = messages.get(position).getText();
        holder.tvText.setText(text == null ? "" : text);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView tvText;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvMessageText);
        }
    }
}
