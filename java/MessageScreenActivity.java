package com.example.collegeproj;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // Import ListenerRegistration
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessageScreenActivity extends AppCompatActivity
        implements ChatListAdaptor.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener { // Implement SwipeRefreshLayout.OnRefreshListener

    private static final String TAG = "MessageScreenActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RecyclerView chatListRecyclerView;
    private ChatListAdaptor chatListAdaptor;
    private List<ChatListItem> chatListItems = new ArrayList<>();
    private TextView noChatsTextView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ImageView homeButton;
    private ImageView favouriteButton;
    private ImageView addButton;
    private TextView messageButton; // TextView for the current active button
    private ImageView profileButton;

    private ListenerRegistration chatListListener; // To manage real-time listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_message_screen);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI elements
        chatListRecyclerView = findViewById(R.id.chatListRecyclerView);
        noChatsTextView = findViewById(R.id.noChatsTextView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Configure RecyclerView
        chatListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatListAdaptor = new ChatListAdaptor(this, chatListItems, this); // Pass 'this' as listener
        chatListRecyclerView.setAdapter(chatListAdaptor);

        // Set SwipeRefreshLayout listener
        swipeRefreshLayout.setOnRefreshListener(this);

        // Set status bar and navigation bar colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.app_background));
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize bottom navigation buttons
        homeButton = findViewById(R.id.homeButton);
        favouriteButton = findViewById(R.id.favouriteButton);
        addButton = findViewById(R.id.addButton);
        messageButton = findViewById(R.id.messageButton); // This is a TextView
        profileButton = findViewById(R.id.profileButton);

        // Set up bottom navigation button listeners
        homeButton.setOnClickListener(v -> {
            startActivity(new Intent(MessageScreenActivity.this, HomeActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        favouriteButton.setOnClickListener(v -> {
            startActivity(new Intent(MessageScreenActivity.this, FavouriteScreenActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        addButton.setOnClickListener(v -> {
            startActivity(new Intent(MessageScreenActivity.this, UploadRoomActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
//        messageButton.setOnClickListener(v -> Toast.makeText(this, "Already on Messages!", Toast.LENGTH_SHORT).show());
        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MessageScreenActivity.this, ProfileScreenActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        // This callback handles the system back button
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Navigate to HomeActivity when back button is pressed from MessageScreenActivity
                Intent intent = new Intent(MessageScreenActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Start fetching chats
        swipeRefreshLayout.setRefreshing(true); // Show spinner on initial load
        fetchChatList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-attach listener in onResume to ensure real-time updates
        // and re-fetch if necessary (e.g., after returning from a chat)
        if (chatListListener == null) {
            swipeRefreshLayout.setRefreshing(true);
            fetchChatList();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detach listener in onPause to prevent memory leaks and unnecessary updates when not visible
        if (chatListListener != null) {
            chatListListener.remove();
            chatListListener = null;
            Log.d(TAG, "Chat list listener removed.");
        }
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "Swipe-to-refresh triggered. Re-fetching chat list.");
        fetchChatList();
    }

    /**
     * Fetches the list of conversations for the current user from Firestore.
     * Sets up a real-time listener for ongoing updates.
     */
    private void fetchChatList() {
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view your messages.", Toast.LENGTH_SHORT).show();
            showEmptyState("Please log in to view your messages.");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String currentUserId = currentUser.getUid();
        Log.d(TAG, "Fetching chat list for user: " + currentUserId);

        // Remove any existing listener before attaching a new one to prevent duplicates
        if (chatListListener != null) {
            chatListListener.remove();
        }

        // Query the 'conversations' collection where the current user is a participant.
        // Order by last message timestamp to show most recent chats first.
        chatListListener = db.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for chat list.", e);
                        Toast.makeText(MessageScreenActivity.this, "Error loading messages.", Toast.LENGTH_SHORT).show();
                        showEmptyState("Error loading messages.");
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }

                    if (snapshots != null) {
                        Log.d(TAG, "Received " + snapshots.size() + " chat list updates. Changes: " + snapshots.getDocumentChanges().size());

                        // Create a new temporary list to build the updated chat list
                        List<ChatListItem> newChatListItems = new ArrayList<>();
                        // No need for conversationIdsToFetchDetails if fetching for all every time for simplicity

                        // Iterate through all documents in the snapshot to build the current state
                        // This is more robust than relying purely on DocumentChange for complex sorting/updates
                        for (DocumentSnapshot doc : snapshots.getDocuments()) { // Iterate through getDocuments() directly
                            String conversationId = doc.getId();
                            List<String> participants = (List<String>) doc.get("participants");

                            String otherUserId = null;
                            if (participants != null && participants.size() == 2) {
                                if (participants.get(0).equals(currentUserId)) {
                                    otherUserId = participants.get(1);
                                } else {
                                    otherUserId = participants.get(0);
                                }
                            }

                            if (otherUserId == null) {
                                Log.w(TAG, "Could not determine other user ID for conversation: " + conversationId);
                                continue; // Skip this conversation
                            }

                            String lastMessageText = doc.getString("lastMessageText");
                            Timestamp lastMessageTimestamp = doc.getTimestamp("lastMessageTimestamp");
                            String associatedRoomId = doc.getString("associatedRoomId");

                            ChatListItem item = new ChatListItem(
                                    conversationId,
                                    otherUserId,
                                    "Loading...", // Placeholder name
                                    null,         // Placeholder image
                                    lastMessageText != null ? lastMessageText : "No messages yet.",
                                    lastMessageTimestamp,
                                    associatedRoomId
                            );
                            newChatListItems.add(item);
                        }

                        // *** Important: Clear the old list and add all new items ***
                        // This ensures the list accurately reflects the current snapshot's order
                        chatListItems.clear();
                        chatListItems.addAll(newChatListItems);

                        // Fetch user details for all items in the new list
                        for (ChatListItem item : chatListItems) {
                            fetchOtherUserDetailsAndAddOrUpdate(item, false); // false for modified/updated scenario
                        }

                        // After processing all documents and updating chatListItems, notify adapter
                        if (chatListItems.isEmpty()) {
                            showEmptyState("You don't have any messages yet.");
                        } else {
                            showRoomsList(); // This calls notifyDataSetChanged()
                        }
                    }
                    swipeRefreshLayout.setRefreshing(false); // Stop refresh animation
                });
    }

    /**
     * Finds the index of a ChatListItem in the list by conversation ID.
     */
    private int findChatListItemIndex(String conversationId) {
        for (int i = 0; i < chatListItems.size(); i++) {
            if (chatListItems.get(i).getConversationId().equals(conversationId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Fetches the other user's details (name, profile image) and updates the ChatListItem.
     * This is crucial because the 'conversations' collection might only store UIDs, not full profile data.
     * @param item The ChatListItem to update.
     * @param isAdded True if this is a newly added item, false if it's an update.
     */
    private void fetchOtherUserDetailsAndAddOrUpdate(ChatListItem item, boolean isAdded) {
        db.collection("users").document(item.getOtherUserId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot userDoc = task.getResult();
//                        QueryDocumentSnapshot userDoc = (QueryDocumentSnapshot) task.getResult(); // Cast to QueryDocumentSnapshot if needed, or just DocumentSnapshot
                        if (userDoc != null && userDoc.exists()) {
                            String firstName = userDoc.getString("firstName");
                            String lastName = userDoc.getString("lastName");
                            String profileImageUrl = userDoc.getString("profileImageUrl");

                            String fullName = "Unknown User";
                            if (firstName != null && !firstName.isEmpty()) {
                                fullName = firstName;
                                if (lastName != null && !lastName.isEmpty()) {
                                    fullName += " " + lastName;
                                }
                            } else if (lastName != null && !lastName.isEmpty()) {
                                fullName = lastName;
                            }

                            // Update the ChatListItem object
                            item.setOtherUserName(fullName);
                            item.setOtherUserProfileImageUrl(profileImageUrl);

                            int index = findChatListItemIndex(item.getConversationId());
                            if (index != -1) {
                                chatListAdaptor.notifyItemChanged(index);
                            } else if (isAdded) {
                                // If it was supposed to be added but not found, notify full data set change.
                                // This is a fallback and indicates potential logic flaw if oldIndex was -1 for ADDED
                                chatListAdaptor.notifyDataSetChanged();
                            }
                        } else {
                            Log.w(TAG, "Other user document not found for ID: " + item.getOtherUserId());
                            item.setOtherUserName("User Not Found");
                            chatListAdaptor.notifyDataSetChanged(); // A full update might be simpler here
                        }
                    } else {
                        Log.e(TAG, "Error fetching other user details for chat list: ", task.getException());
                        item.setOtherUserName("Error Loading User");
                        chatListAdaptor.notifyDataSetChanged(); // A full update might be simpler here
                    }
                });
    }


    private void showEmptyState(String message) {
        runOnUiThread(() -> {
            noChatsTextView.setText(message);
            noChatsTextView.setVisibility(View.VISIBLE);
            chatListRecyclerView.setVisibility(View.GONE);
            chatListAdaptor.notifyDataSetChanged(); // Ensure adapter is aware of the empty list
        });
    }

    private void showRoomsList() { // Renamed from showRoomsList for clarity, applies to chat list
        runOnUiThread(() -> {
            noChatsTextView.setVisibility(View.GONE);
            chatListRecyclerView.setVisibility(View.VISIBLE);
            // Sorting is handled within the snapshot listener, so just notify change
            chatListAdaptor.notifyDataSetChanged();
        });
    }

    @Override
    public void onItemClick(ChatListItem chatListItem) {
        // Handle click on a chat item: navigate to SingleChatActivity
        // Pass necessary info: otherUserId, conversationId, otherUserName, etc.
        Intent intent = new Intent(this, SingleChatActivity.class);
        intent.putExtra("conversationId", chatListItem.getConversationId());
        intent.putExtra("otherUserId", chatListItem.getOtherUserId());
        intent.putExtra("otherUserName", chatListItem.getOtherUserName());
        intent.putExtra("otherUserProfileImageUrl", chatListItem.getOtherUserProfileImageUrl());
        // Optionally pass associatedRoomId if your SingleChatActivity needs it
        intent.putExtra("associatedRoomId", chatListItem.getAssociatedRoomId());
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
