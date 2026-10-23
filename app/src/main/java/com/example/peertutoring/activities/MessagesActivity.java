package com.example.peertutoring.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.adapters.ConversationAdapter;
import com.example.peertutoring.models.Conversation;
import com.example.peertutoring.network.ApiCallback;
import com.example.peertutoring.network.ApiClient;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.ValidationUtils;

import java.util.List;

/** The inbox: everyone the user has talked to, latest first. Tapping a row opens the chat. */
public class MessagesActivity extends AppCompatActivity {

    private RecyclerView rvConversations;
    private View tvEmpty;
    private ConversationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        NavHelper.setup(this, R.id.btnMessages, false);

        rvConversations = findViewById(R.id.rvConversations);
        tvEmpty = findViewById(R.id.tvEmptyConversations);
        adapter = new ConversationAdapter(conversation -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_OTHER_UID, conversation.getOtherUid());
            intent.putExtra(ChatActivity.EXTRA_OTHER_NAME, conversation.getOtherName());
            startActivity(intent);
        });
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();   // refresh when coming back from a chat
    }

    private void loadConversations() {
        ApiClient.get().getConversations().enqueue(new ApiCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                if (isFinishing()) return;
                adapter.setConversations(conversations);
                boolean empty = conversations == null || conversations.isEmpty();
                rvConversations.setVisibility(empty ? View.GONE : View.VISIBLE);
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(ApiError error) {
                if (isFinishing()) return;
                ValidationUtils.showToast(MessagesActivity.this, error.getMessage());
            }
        });
    }
}
