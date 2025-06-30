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

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FavouriteScreenActivity extends AppCompatActivity
        implements RecentUploadsAdapter.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "FavouriteActivity";
    private static final String KEY_IS_DATA_LOADED = "isDataLoaded"; // Key for saving state

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private RecyclerView favoritesRecyclerView;
    private RecentUploadsAdapter favoritesAdapter;
    private List<RoomData> favoriteRoomList = new ArrayList<>();
    private TextView noFavoritesText;

    private ImageView homeButton;
    private TextView favouriteButton;
    private ImageView addButton;
    private ImageView messageButton;
    private ImageView profileButton;

    private SwipeRefreshLayout swipeRefreshLayout;

    private Map<String, Timestamp> favoriteTimestampsMap = new HashMap<>();

    // Flag to track if data has been loaded at least once
    private boolean isDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favourite_screen);

        // Retrieve saved state if activity is being recreated
        if (savedInstanceState != null) {
            isDataLoaded = savedInstanceState.getBoolean(KEY_IS_DATA_LOADED, false);
            Log.d(TAG, "onCreate: Restored isDataLoaded from savedInstanceState: " + isDataLoaded);
        } else {
            Log.d(TAG, "onCreate: savedInstanceState is null. First time creation.");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);
        noFavoritesText = findViewById(R.id.noFavoritesText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);

        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesAdapter = new RecentUploadsAdapter(this, favoriteRoomList, this);
        favoritesRecyclerView.setAdapter(favoritesAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Status bar and Navigation bar colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.app_background));
        }


        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(0, 0);

            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // >>>>>>>>>>>>> NAVIGATION BUTTON SETUP (UNCHANGED) <<<<<<<<<<<<<

        homeButton = findViewById(R.id.homeButton);
        favouriteButton = findViewById(R.id.favouriteButton);
        addButton = findViewById(R.id.addButton);
        messageButton = findViewById(R.id.messageButton);
        profileButton = findViewById(R.id.profileButton);

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(FavouriteScreenActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

//        favouriteButton.setOnClickListener(v -> {
//            Toast.makeText(this, "Already on Favorites!", Toast.LENGTH_SHORT).show();
//            // If the user taps the 'Favorites' button while already on this screen,
//            // they probably want to refresh, so we'll trigger a manual refresh.
//            onRefresh();
//        });

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(FavouriteScreenActivity.this, UploadRoomActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        messageButton.setOnClickListener(v -> {
            Intent intent = new Intent(FavouriteScreenActivity.this, MessageScreenActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(FavouriteScreenActivity.this, ProfileScreenActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // >>>>>>>>>>>>> NAVIGATION BUTTON SETUP ENDS <<<<<<<<<<<<<

        // Initial data load for onCreate only, respecting savedInstanceState
        if (!isDataLoaded) {
            Log.d(TAG, "onCreate: Data not loaded. Triggering initial fetch.");
            // Show the spinner immediately
            swipeRefreshLayout.post(() -> {
                if (!swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(true);
                }
            });
            fetchFavorites();
            // isDataLoaded will be set to true in fetchFavorites() on success
        } else {
            Log.d(TAG, "onCreate: Data already loaded and restored. Skipping initial fetch.");
            // If data was loaded and restored, ensure UI is updated if needed
            favoritesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // REMOVED AUTOMATIC fetchFavorites() here.
        // Data will only refresh on onCreate (first creation/recreation) or explicit pull-to-refresh.
        Log.d(TAG, "onResume: Skipping automatic data fetch. isDataLoaded: " + isDataLoaded);
        // Ensure adapter is notified in case list content (favorites state of rooms) changed in HomeActivity
        // while this activity was paused.
        // If your HomeActivity doesn't affect favouriteRoomList directly, this notify may not be strictly needed,
        // but it's a safe practice to ensure the RecyclerView reflects any underlying data changes.
        favoritesAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current state of isDataLoaded
        outState.putBoolean(KEY_IS_DATA_LOADED, isDataLoaded);
        Log.d(TAG, "onSaveInstanceState: Saving isDataLoaded: " + isDataLoaded);
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "Pull-to-refresh triggered. Forcing data reload.");
        // When user explicitly pulls, force a reload regardless of `isDataLoaded` state.
        // Reset flag to false so fetchFavorites treats it as a fresh load for this cycle.
        isDataLoaded = false;
        fetchFavorites();
    }

    /**
     * Fetches the list of favorited room references and their favorite timestamps
     * for the current user from Firestore.
     */
    private void fetchFavorites() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view favorites.", Toast.LENGTH_SHORT).show();
            noFavoritesText.setText("Please log in to view your favorite rooms.");
            noFavoritesText.setVisibility(View.VISIBLE);
            favoritesRecyclerView.setVisibility(View.GONE);
            favoriteRoomList.clear();
            favoritesAdapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false); // Stop refresh animation
            isDataLoaded = false; // Data is not loaded if user is not logged in
            Log.d(TAG, "Not logged in, cannot fetch favorites. Stopping refresh.");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Fetching favorites for user: " + userId);

        favoriteRoomList.clear();
        favoriteTimestampsMap.clear();
        noFavoritesText.setVisibility(View.GONE);
        favoritesRecyclerView.setVisibility(View.GONE);

        db.collection("users").document(userId).collection("favorites")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentReference> roomRefs = new ArrayList<>();
                        for (QueryDocumentSnapshot favoriteDoc : task.getResult()) {
                            Object roomRefObj = favoriteDoc.get("roomRef");
                            Timestamp favTimestamp = favoriteDoc.getTimestamp("timestamp");

                            if (roomRefObj instanceof DocumentReference && favTimestamp != null) {
                                roomRefs.add((DocumentReference) roomRefObj);
                                favoriteTimestampsMap.put(favoriteDoc.getId(), favTimestamp);
                            } else {
                                Log.w(TAG, "Favorite document " + favoriteDoc.getId() + " is missing 'roomRef' or 'timestamp'. Skipping.");
                            }
                        }

                        if (roomRefs.isEmpty()) {
                            noFavoritesText.setText("You haven't favorited any rooms yet.");
                            noFavoritesText.setVisibility(View.VISIBLE);
                            favoritesRecyclerView.setVisibility(View.GONE);
                            favoritesAdapter.notifyDataSetChanged();
                            Log.d(TAG, "No favorite room references found for user " + userId + ".");
                            swipeRefreshLayout.setRefreshing(false); // Stop refresh animation
                            isDataLoaded = true; // Mark as loaded even if empty
                            return;
                        }

                        fetchRoomDetailsFromReferences(roomRefs);

                    } else {
                        Log.e(TAG, "Error fetching favorite references: ", task.getException());
                        Toast.makeText(this, "Error loading favorites.", Toast.LENGTH_SHORT).show();
                        noFavoritesText.setText("Error loading your favorite rooms.");
                        noFavoritesText.setVisibility(View.VISIBLE);
                        favoritesRecyclerView.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false); // Stop refresh animation
                        isDataLoaded = false; // If there's an error, data isn't considered loaded
                    }
                });
    }

    /**
     * Fetches the full RoomData objects based on the provided DocumentReferences and applies
     * their corresponding favorite timestamps from the cached map.
     *
     * @param roomRefs A list of DocumentReferences to the actual room documents.
     */
    private void fetchRoomDetailsFromReferences(List<DocumentReference> roomRefs) {
        List<RoomData> fetchedRooms = new ArrayList<>();
        final int[] count = {0};

        Log.d(TAG, "Fetching details for " + roomRefs.size() + " favorited rooms.");

        if (roomRefs.isEmpty()) {
            favoriteRoomList.clear();
            favoritesAdapter.notifyDataSetChanged();
            noFavoritesText.setVisibility(View.VISIBLE);
            favoritesRecyclerView.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            isDataLoaded = true;
            return;
        }

        for (DocumentReference roomRef : roomRefs) {
            roomRef.get().addOnCompleteListener(roomTask -> {
                count[0]++;
                if (roomTask.isSuccessful()) {
                    DocumentSnapshot roomDoc = roomTask.getResult();
                    if (roomDoc.exists()) {
                        RoomData room = roomDoc.toObject(RoomData.class);
                        if (room != null) {
                            String roomId = roomDoc.getId();
                            room.setDocumentId(roomId);
                            room.setFavorite(true);

                            Timestamp currentFavoriteTimestamp = favoriteTimestampsMap.get(roomId);
                            room.setFavoriteTimestamp(currentFavoriteTimestamp);

                            fetchedRooms.add(room);
                            Log.d(TAG, "Fetched favorite room details: " + room.getTitle() + " (ID: " + room.getDocumentId() + ") Favorited: " + currentFavoriteTimestamp);
                        }
                    } else {
                        Log.w(TAG, "Favorite room document not found (might have been deleted): " + roomRef.getPath());
                    }
                } else {
                    Log.e(TAG, "Error fetching favorite room details for ref: " + roomRef.getPath(), roomTask.getException());
                }

                if (count[0] == roomRefs.size()) {
                    favoriteRoomList.clear();
                    favoriteRoomList.addAll(fetchedRooms);

                    Collections.sort(favoriteRoomList, (r1, r2) -> {
                        if (r1.getFavoriteTimestamp() == null || r2.getFavoriteTimestamp() == null) {
                            return 0;
                        }
                        return r2.getFavoriteTimestamp().compareTo(r1.getFavoriteTimestamp());
                    });

                    if (favoriteRoomList.isEmpty()) {
                        noFavoritesText.setText("You haven't favorited any rooms yet.");
                        noFavoritesText.setVisibility(View.VISIBLE);
                        favoritesRecyclerView.setVisibility(View.GONE);
                    } else {
                        favoritesRecyclerView.setVisibility(View.VISIBLE);
                        noFavoritesText.setVisibility(View.GONE);
                    }
                    favoritesAdapter.notifyDataSetChanged();
                    Log.d(TAG, "All favorite room details processed and sorted. Displaying " + favoriteRoomList.size() + " rooms.");
                    swipeRefreshLayout.setRefreshing(false); // Stop refresh animation
                    isDataLoaded = true; // Data successfully loaded, set flag to true
                }
            });
        }
    }

    @Override
    public void onItemClick(RoomData roomData) {
        Intent intent = new Intent(this, RoomDetailsActivity.class);
        intent.putExtra("roomDetails", roomData);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}