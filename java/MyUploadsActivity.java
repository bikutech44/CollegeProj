package com.example.collegeproj;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.collegeproj.MyUploadsAdapter;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyUploadsActivity extends AppCompatActivity implements MyUploadsAdapter.OnItemActionListener {

    private static final String TAG = "MyUploadsActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ImageView backButton;
    private RecyclerView uploadsRecyclerView;
    private MyUploadsAdapter myUploadsAdapter;
    private List<RoomData> uploadedRoomList = new ArrayList<>(); // List to hold fetched room data
    private LinearLayout emptyStateLayout; // Layout to show when no uploads are found
    // private Button uploadNowButton; // REMOVED: Button 'uploadNowButton' as it's no longer in XML

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_uploads);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements by finding them from activity_my_uploads.xml
        backButton = findViewById(R.id.backButton);
        uploadsRecyclerView = findViewById(R.id.uploadsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        // REMOVED: uploadNowButton = findViewById(R.id.uploadNowButton); as it's no longer in XML

        // Configure RecyclerView: Set its layout manager and adapter
        uploadsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Pass 'this' as the listener because MyUploadsActivity implements OnItemActionListener
        myUploadsAdapter = new MyUploadsAdapter(this, uploadedRoomList, this);
        uploadsRecyclerView.setAdapter(myUploadsAdapter);

        // Set status bar color (assuming app_background color is defined in colors.xml)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.app_background)); // Use primaryColor for consistency
        }

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set listeners for buttons
        backButton.setOnClickListener(v -> {
            // Use onBackPressedDispatcher for modern back handling. This triggers the OnBackPressedCallback below.
            getOnBackPressedDispatcher().onBackPressed();
        });

        // REMOVED: uploadNowButton.setOnClickListener(v -> { ... }); as it's no longer in XML

        // Handle back press specifically for this activity
        // This ensures pressing back from MyUploads returns to ProfileScreen, not necessarily Home.
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(MyUploadsActivity.this, ProfileScreenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Clear top activities to ensure smooth back stack
                startActivity(intent);
                overridePendingTransition(0, 0); // No animation
                finish(); // Finish this activity
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    /**
     * Called when the activity is about to become visible.
     * We fetch uploads here to ensure the list is always up-to-date,
     * especially if uploads were made or deleted from other screens.
     */
    @Override
    protected void onResume() {
        super.onResume();
        fetchMyUploads(); // Refresh uploads list every time the activity is resumed
    }

    /**
     * Fetches rooms uploaded by the current user from Firebase Firestore.
     * Clears existing data, fetches new data, and updates the RecyclerView.
     * Manages the visibility of the empty state layout.
     */
    private void fetchMyUploads() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view your uploads.", Toast.LENGTH_SHORT).show();
            showEmptyState(true); // Show empty state if no user is logged in
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Fetching uploads for user: " + userId);

        // Clear existing data to prevent duplicates when refreshing
        uploadedRoomList.clear();
        myUploadsAdapter.notifyDataSetChanged(); // Notify adapter immediately to show empty list/loading state

        // Query 'roomsData' collection where 'userId' matches the current user's ID
        // Order by 'uploadTimestamp' in descending order to show newest uploads first
        db.collection("roomsData").document(userId).collection("rooms")
                .orderBy("uploadTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Check if any documents were returned
                        if (task.getResult().isEmpty()) {
                            Log.d(TAG, "No uploads found for user: " + userId);
                            showEmptyState(true); // Show empty state if no uploads
                        } else {
                            // Iterate through the results and convert each document to a RoomData object
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                RoomData room = document.toObject(RoomData.class);
                                if (room != null) {
                                    // Store the Firestore document ID in the RoomData object
                                    // This ID is crucial for later operations like updating or deleting
                                    room.setDocumentId(document.getId());
                                    uploadedRoomList.add(room);
                                }
                            }
                            Log.d(TAG, "Fetched " + uploadedRoomList.size() + " uploads for user: " + userId);
                            showEmptyState(false); // Hide empty state, show RecyclerView
                            myUploadsAdapter.notifyDataSetChanged(); // Notify adapter to display the new data
                        }
                    } else {
                        Log.e(TAG, "Error fetching user uploads: ", task.getException());
                        Toast.makeText(MyUploadsActivity.this, "Failed to load your uploads.", Toast.LENGTH_SHORT).show();
                        showEmptyState(true); // Show empty state on error
                    }
                });
    }

    /**
     * Toggles the visibility of the empty state layout versus the RecyclerView.
     * @param show true to show empty state (and hide RecyclerView), false to hide empty state (and show RecyclerView).
     */
    private void showEmptyState(boolean show) {
        if (show) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            uploadsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            uploadsRecyclerView.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onItemClick(RoomData room) {
        // Handle general item click, e.g., navigate to a detailed view of the room
//        Toast.makeText(this, "Clicked: " + room.getTitle(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MyUploadsActivity.this, MyUploadDetailsActivity.class);
        intent.putExtra("roomDetails", room); // Pass the entire RoomData object to the details activity
        startActivity(intent);
        overridePendingTransition(0, 0); // No animation
    }

    @Override
    public void onEditClick(RoomData room) {
        // Handle edit icon click
        Toast.makeText(this, "Edit: this function will work soon " , Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(RoomData room) {
        // Handle delete icon click. Show a confirmation dialog before deleting.
        new AlertDialog.Builder(this)
                .setTitle("Delete Upload")
                .setMessage("Are you sure you want to delete '" + room.getTitle() + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteRoom(room)) // Call deleteRoom method on confirmation
                .setNegativeButton("Cancel", null) // Do nothing on cancel
                .show();
    }


    private void deleteRoom(RoomData room) {
        if (room.getDocumentId() == null || room.getDocumentId().isEmpty()) {
            Toast.makeText(this, "Error: Cannot delete room, ID not found.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Attempted to delete room with null/empty documentId.");
            return;
        }
        // Ensure userId is available in the RoomData object for deletion path
        if (room.getUserId() == null || room.getUserId().isEmpty()) {
            Toast.makeText(this, "Error: Cannot delete room, User ID not found.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Attempted to delete room with null/empty userId.");
            return;
        }

        //delete garne path
        db.collection("roomsData")
                .document(room.getUserId())
                .collection("rooms")
                .document(room.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Room document deleted from Firestore: " + room.getDocumentId() + " for user: " + room.getUserId());
                    Toast.makeText(MyUploadsActivity.this, "Room deleted successfully!", Toast.LENGTH_SHORT).show();

                    // Remove the deleted room from our local list and update the RecyclerView
                    uploadedRoomList.remove(room);
                    myUploadsAdapter.notifyDataSetChanged();

                    // If the list becomes empty after deletion, show the empty state
                    if (uploadedRoomList.isEmpty()) {
                        showEmptyState(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting room from Firestore: " + e.getMessage(), e);
                    Toast.makeText(MyUploadsActivity.this, "Failed to delete room.", Toast.LENGTH_SHORT).show();
                });
    }
}
