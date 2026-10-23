package com.example.peertutoring.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peertutoring.R;
import com.example.peertutoring.adapters.ChatAdapter;
import com.example.peertutoring.models.ChatMessage;
import com.example.peertutoring.models.SendMessageRequest;
import com.example.peertutoring.network.ApiCallback;
import com.example.peertutoring.network.ApiClient;
import com.example.peertutoring.network.ApiError;
import com.example.peertutoring.utils.NavHelper;
import com.example.peertutoring.utils.SessionManager;
import com.example.peertutoring.utils.ValidationUtils;

import java.util.List;

/** One conversation with one person. New messages from them appear within a few seconds. */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_OTHER_UID = "otherUid";
    public static final String EXTRA_OTHER_NAME = "otherName";

    private static final long REFRESH_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            loadThread(false);
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    private String otherUid;
    private ChatAdapter adapter;
    private RecyclerView rvChat;
    private View tvEmptyChat;
    private EditText etMessage;
    private View btnSend;
    private int shownCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        NavHelper.setup(this, R.id.btnMessages, true);

        otherUid = getIntent().getStringExtra(EXTRA_OTHER_UID);
        String otherName = getIntent().getStringExtra(EXTRA_OTHER_NAME);
        if (otherUid == null || otherUid.isEmpty()) {
            finish();   // opened without anyone to talk to
            return;
        }

        ((TextView) findViewById(R.id.tvChatName)).setText(
                otherName == null || otherName.isEmpty() ? getString(R.string.unknown_name) : otherName);

        rvChat = findViewById(R.id.rvChat);
        tvEmptyChat = findViewById(R.id.tvEmptyChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        adapter = new ChatAdapter(new SessionManager(this).getString("uid"));
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (otherUid != null) {
            handler.post(refresh);   // loads now, then every few seconds while the screen is open
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresh);
    }

    private void loadThread(boolean forceScroll) {
        ApiClient.get().getThread(otherUid).enqueue(new ApiCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                if (isFinishing()) return;
                int count = messages == null ? 0 : messages.size();
                boolean changed = count != shownCount;
                if (changed) {
                    adapter.setMessages(messages);
                    shownCount = count;
                    tvEmptyChat.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                }
                if ((changed || forceScroll) && count > 0) {
                    rvChat.scrollToPosition(count - 1);
                }
            }

            @Override
            public void onError(ApiError error) {
                // Stay quiet on refresh failures; the next refresh tries again
            }
        });
    }

    private void sendMessage() {
        String text = ValidationUtils.text(etMessage);
        if (text.isEmpty()) return;

        btnSend.setEnabled(false);
        ApiClient.get().sendMessage(new SendMessageRequest(otherUid, text)).enqueue(new ApiCallback<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage message) {
                btnSend.setEnabled(true);
                if (isFinishing()) return;
                etMessage.setText("");
                loadThread(true);
            }

            @Override
            public void onError(ApiError error) {
                btnSend.setEnabled(true);
                if (isFinishing()) return;
                ValidationUtils.showToast(ChatActivity.this, error.getMessage());   // the text stays so it can be resent
            }
        });
    }
}
