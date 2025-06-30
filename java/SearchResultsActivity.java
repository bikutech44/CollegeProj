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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections; // Not explicitly needed for sorting if data is already sorted by query
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchResultsActivity extends AppCompatActivity
        implements RecentUploadsAdapter.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "SearchResultsActivity";
    public static final String EXTRA_SEARCH_QUERY = "extra_search_query";

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private TextView screenTitleTextView;
    private RecyclerView searchResultsRecyclerView;
    private TextView noResultsTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageView backButton;

    private RecentUploadsAdapter searchResultsAdapter;
    private List<RoomData> searchResultsList = new ArrayList<>();
    private String currentSearchQuery;

    private Set<String> favoriteRoomIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_results);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI elements
        screenTitleTextView = findViewById(R.id.screenTitle);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        noResultsTextView = findViewById(R.id.noResultsTextView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        backButton = findViewById(R.id.backButton);

        swipeRefreshLayout.setOnRefreshListener(this);

        // Setup RecyclerView
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsAdapter = new RecentUploadsAdapter(this, searchResultsList, this);
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);

        // Get search query from Intent
        if (getIntent().hasExtra(EXTRA_SEARCH_QUERY)) {
            currentSearchQuery = getIntent().getStringExtra(EXTRA_SEARCH_QUERY);
            screenTitleTextView.setText("Search Results for: \"" + currentSearchQuery + "\"");
        } else {
            currentSearchQuery = "";
            screenTitleTextView.setText("Search Results");
            Toast.makeText(this, "No search query provided.", Toast.LENGTH_SHORT).show();
        }

        // Set up back button
        backButton.setOnClickListener(v -> onBackPressed());

        // Handle back press
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(0, 0);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Status bar color (consistent with your other activities)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.app_background));
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.app_background2));
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initial fetch of search results
        swipeRefreshLayout.setRefreshing(true); // Start refreshing animation
        loadSearchResults(); // Centralized loading
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");
        // Re-fetch in onResume to ensure latest data (e.g., if a room was favorited/unfavorited)
        // Only if not already refreshing (from onCreate or onRefresh) to avoid redundant calls
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
            loadSearchResults();
        }
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "Swipe-to-refresh triggered in SearchResultsActivity.");
        loadSearchResults(); // Centralized loading
    }

    /**
     * Centralized method to load search results, including fetching favorites.
     * Ensures list is cleared and UI states are correctly managed.
     */
    private void loadSearchResults() {
        Log.d(TAG, "loadSearchResults called for query: " + currentSearchQuery);
        // Always clear the list at the beginning of a load cycle
        searchResultsList.clear();
        searchResultsAdapter.notifyDataSetChanged(); // Notify adapter that the list is empty

        noResultsTextView.setVisibility(View.GONE); // Hide empty state
        searchResultsRecyclerView.setVisibility(View.GONE); // Hide RecyclerView

        if (currentSearchQuery == null || currentSearchQuery.trim().isEmpty()) {
            noResultsTextView.setText("Enter a search term to find rooms.");
            noResultsTextView.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Fetch favorites first, then proceed with search results fetching
        if (currentUser != null) {
            fetchUserFavorites(currentUser.getUid(), () -> performRoomSearch(currentSearchQuery));
        } else {
            favoriteRoomIds.clear(); // Guest user has no favorites
            performRoomSearch(currentSearchQuery);
        }
    }

    private void fetchUserFavorites(String userId, Runnable onComplete) {
        if (userId == null || userId.isEmpty()) {
            favoriteRoomIds.clear();
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        firestore.collection("users").document(userId).collection("favorites")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        favoriteRoomIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            favoriteRoomIds.add(document.getId());
                        }
                        Log.d(TAG, "Fetched " + favoriteRoomIds.size() + " favorite rooms for search results.");
                    } else {
                        Log.e(TAG, "Error fetching user favorites for search results: ", task.getException());
                        Toast.makeText(SearchResultsActivity.this, "Error fetching favorites.", Toast.LENGTH_SHORT).show();
                    }
                    if (onComplete != null) {
                        onComplete.run(); // Ensure this always runs, even on failure
                    }
                });
    }

    private void performRoomSearch(String query) {
        Log.d(TAG, "performRoomSearch: Starting Firestore query for '" + query + "'");

        final String lowerCaseQuery = query.toLowerCase();

        firestore.collectionGroup("rooms")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<RoomData> fetchedAndFilteredRooms = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            RoomData room = document.toObject(RoomData.class);
                            if (room != null) {
                                // Apply client-side filtering
                                String title = room.getTitle() != null ? room.getTitle().toLowerCase() : "";
                                String description = room.getDescription() != null ? room.getDescription().toLowerCase() : "";
                                String place = room.getPlace() != null ? room.getPlace().toLowerCase() : "";
                                String roomType = room.getRoomType() != null ? room.getRoomType().toLowerCase() : "";

                                if (title.contains(lowerCaseQuery) ||
                                        description.contains(lowerCaseQuery) ||
                                        place.contains(lowerCaseQuery) ||
                                        roomType.contains(lowerCaseQuery)) {

                                    room.setDocumentId(document.getId());
                                    room.setFavorite(favoriteRoomIds.contains(document.getId()));
                                    fetchedAndFilteredRooms.add(room);
                                }
                            }
                        }

                        // Update the RecyclerView's data list on the main thread
                        runOnUiThread(() -> {
                            searchResultsList.addAll(fetchedAndFilteredRooms);
                            if (searchResultsList.isEmpty()) {
                                noResultsTextView.setText("No results found for \"" + query + "\".");
                                noResultsTextView.setVisibility(View.VISIBLE);
                                searchResultsRecyclerView.setVisibility(View.GONE);
                                Log.d(TAG, "No search results found for: " + query);
                            } else {
                                searchResultsRecyclerView.setVisibility(View.VISIBLE);
                                noResultsTextView.setVisibility(View.GONE);
                                searchResultsAdapter.notifyDataSetChanged(); // Notify after data is added
                                Log.d(TAG, "Displaying " + searchResultsList.size() + " search results for: " + query);
                            }
                            swipeRefreshLayout.setRefreshing(false); // Stop refresh animation here
                        });

                    } else {
                        Log.e(TAG, "Error getting search results: ", task.getException());
                        runOnUiThread(() -> {
                            Toast.makeText(SearchResultsActivity.this, "Failed to load search results: " +
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            noResultsTextView.setText("Error loading search results.");
                            noResultsTextView.setVisibility(View.VISIBLE);
                            searchResultsRecyclerView.setVisibility(View.GONE);
                            swipeRefreshLayout.setRefreshing(false); // Stop refresh animation on error
                        });
                    }
                });
    }

    @Override
    public void onItemClick(RoomData roomData) {
        Intent intent = new Intent(this, RoomDetailsActivity.class);
        intent.putExtra("roomDetails", roomData);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
