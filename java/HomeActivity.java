package com.example.collegeproj;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Handler;
import android.os.Looper;


public class HomeActivity extends AppCompatActivity
        implements RecentUploadsAdapter.OnItemClickListener, NearbyRoomsAdapter.OnItemClickListener,
        SwipeRefreshLayout.OnRefreshListener, SearchView.OnQueryTextListener {

    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE_NEARBY = 101;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private TextView firstNameTextView;

    private RecyclerView nearbyRoomsRecyclerView;
    private NearbyRoomsAdapter nearbyRoomsAdapter;
    private List<NearbyRoom> nearbyRoomsList = new ArrayList<>();

    private RecyclerView recentUploadsRecyclerView;
    private RecentUploadsAdapter recentUploadsAdapter;
    private List<RoomData> recentUploadsList = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;

    private TextView noNearbyRoomsText;
    private TextView loadingNearbyRoomsText;

    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isInitialLoadDone = false;
    private int completedFetches = 0;
    private final int TOTAL_FETCHES_EXPECTED = 3;

    private Set<String> favoriteRoomIds = new HashSet<>();

    private TextView homeButton;

    private ImageView favouriteButton;

    private ImageView addButton;

    private ImageView messageButton;

    private ImageView profileButton;

    private SearchView searchView;
    private String currentSearchQuery = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called. isInitialLoadDone: " + isInitialLoadDone);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        firstNameTextView = findViewById(R.id.firstNameTextView);
        noNearbyRoomsText = findViewById(R.id.noNearbyRoomsText);
        loadingNearbyRoomsText = findViewById(R.id.loadingNearbyRoomsText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);

        searchView = findViewById(R.id.searchView);
        if (searchView != null) {
            // Set hint text
            searchView.setQueryHint("Search here..");

            // Set query text listener (assuming 'this' implements SearchView.OnQueryTextListener)
            searchView.setOnQueryTextListener(this);

            // Ensure the SearchView is expanded (no need for extra click listener)
            searchView.setIconifiedByDefault(false);
        }



        recentUploadsRecyclerView = findViewById(R.id.recentUploadsRecyclerView);
        recentUploadsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentUploadsAdapter = new RecentUploadsAdapter(this, recentUploadsList, this);
        recentUploadsRecyclerView.setAdapter(recentUploadsAdapter);

        nearbyRoomsRecyclerView = findViewById(R.id.nearbyRoomsRecyclerView);
        LinearLayoutManager nearbyLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        nearbyRoomsRecyclerView.setLayoutManager(nearbyLayoutManager);
        nearbyRoomsRecyclerView.setHasFixedSize(true);
        nearbyRoomsAdapter = new NearbyRoomsAdapter(this, nearbyRoomsList, this);
        nearbyRoomsRecyclerView.setAdapter(nearbyRoomsAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.app_background));
        }

        homeButton = findViewById(R.id.homeButton);
        favouriteButton = findViewById(R.id.favouriteButton);
        addButton = findViewById(R.id.addButton);
        messageButton = findViewById(R.id.messageButton);
        profileButton = findViewById(R.id.profileButton);

//        homeButton.setOnClickListener(v -> Toast.makeText(this, "Already on Home!", Toast.LENGTH_SHORT).show());
        favouriteButton.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FavouriteScreenActivity.class));
            overridePendingTransition(0, 0);
        });
        addButton.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, UploadRoomActivity.class));
            overridePendingTransition(0, 0);
        });
        messageButton.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MessageScreenActivity.class));
            overridePendingTransition(0, 0);
        });
        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileScreenActivity.class));
            overridePendingTransition(0, 0);
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(0, 0);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        swipeRefreshLayout.setRefreshing(true);
        loadAllHomeData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");

        // --- NEW CODE STARTS HERE ---
        // Clear the search view and collapse it when returning to HomeActivity
        if (searchView != null) {
            searchView.setQuery("", false); // Clears text, 'false' means don't submit query
            searchView.setIconified(true); // Collapses the search view
            currentSearchQuery = ""; // Also clear the internal tracking variable
        }
        // --- NEW CODE ENDS HERE ---

        if (!swipeRefreshLayout.isRefreshing()) { // Only refresh if not already refreshing
            swipeRefreshLayout.setRefreshing(true);
            loadAllHomeData();
        } else {
            // If already refreshing, ensure current data is re-applied (e.g. favorite status)
            // This is primarily for cases where a quick resume happens before initial load finishes.
            recentUploadsAdapter.notifyDataSetChanged();
            nearbyRoomsAdapter.notifyDataSetChanged();
        }
    }

    private void loadAllHomeData() {
        completedFetches = 0;

        recentUploadsList.clear();
        nearbyRoomsList.clear();
        favoriteRoomIds.clear();

        recentUploadsAdapter.notifyDataSetChanged();
        nearbyRoomsAdapter.notifyDataSetChanged();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserFavorites(currentUser.getUid(), () -> {
                fetchUserData(currentUser.getUid());
                fetchRecentUploads(currentSearchQuery);
                getCurrentLocationForNearbyRooms(currentSearchQuery);
            });
        } else {
            firstNameTextView.setText("Guest");
            fetchRecentUploads(currentSearchQuery);
            getCurrentLocationForNearbyRooms(currentSearchQuery);
            completedFetches++;
            checkRefreshCompletion();
        }
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "Pull-to-refresh triggered. Initiating full data load.");
        loadAllHomeData();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d(TAG, "Search query submitted: " + query);
        // --- MODIFIED CODE START ---
        // Instead of reloading all data in HomeActivity, launch SearchResultsActivity
        if (query != null && !query.trim().isEmpty()) {
            Intent intent = new Intent(HomeActivity.this, SearchResultsActivity.class);
            intent.putExtra(SearchResultsActivity.EXTRA_SEARCH_QUERY, query);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else {
            Toast.makeText(HomeActivity.this, "Please enter a search term.", Toast.LENGTH_SHORT).show();
        }
        // It's good practice to clear the current search and collapse it when navigating away
        // However, since we are moving to a new activity, we can clear it in onResume.
        // For immediate clearing, you could add:
        // searchView.setQuery("", false);
        // searchView.setIconified(true);
        // currentSearchQuery = "";
        searchView.clearFocus(); // Hide keyboard
        return true;
        // --- MODIFIED CODE END ---
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public void expandSearchView(View view) {
        if (searchView != null) {
            searchView.setIconified(false);
        }
    }

    @Override
    public void onItemClick(RoomData roomData) {
        Intent intent = new Intent(this, RoomDetailsActivity.class);
        intent.putExtra("roomDetails", roomData);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onItemClick(NearbyRoom room) {
        Log.d(TAG, "Clicked on NearbyRoom: " + room.getPrice() + ", ID: " + room.getDocumentId() + ", Uploader: " + room.getUploaderUserId());

        if (room.getDocumentId() != null && room.getUploaderUserId() != null) {
            firestore.collection("roomsData")
                    .document(room.getUploaderUserId())
                    .collection("rooms")
                    .document(room.getDocumentId())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                RoomData fullRoomData = document.toObject(RoomData.class);
                                if (fullRoomData != null) {
                                    fullRoomData.setDocumentId(document.getId());
                                    Log.d(TAG, "Fetched full RoomData for NearbyRoom: " + fullRoomData.getTitle());
                                    Intent intent = new Intent(HomeActivity.this, RoomDetailsActivity.class);
                                    intent.putExtra("roomDetails", fullRoomData);
                                    startActivity(intent);
                                    overridePendingTransition(0, 0);
                                } else {
                                    Log.e(TAG, "Failed to parse full RoomData from document.");
                                    Toast.makeText(HomeActivity.this, "Error loading room details.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.d(TAG, "Nearby Room document not found: " + room.getDocumentId());
                                Toast.makeText(HomeActivity.this, "Room not found or has been removed.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Error fetching full RoomData for NearbyRoom: ", task.getException());
                            Toast.makeText(HomeActivity.this, "Error fetching room details.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Room ID or Uploader ID missing for nearby room.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Room ID or Uploader ID is null for nearby room click.");
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
                        Log.d(TAG, "Fetched " + favoriteRoomIds.size() + " favorite rooms.");
                    } else {
                        Log.e(TAG, "Error fetching user favorites: ", task.getException());
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    private void fetchUserData(String uid) {
        Log.d(TAG, "Fetching user data...");
        firestore.collection("users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String firstName = document.getString("firstName");
                            if (firstName != null && !firstName.isEmpty()) {
                                firstNameTextView.setText("Hi, " + firstName);
                            } else {
                                firstNameTextView.setText("User");
                            }
                            Log.d(TAG, "User data fetched successfully.");
                        } else {
                            Log.d(TAG, "User document not found for uid: " + uid);
                            firstNameTextView.setText("User");
                        }
                    } else {
                        Log.e(TAG, "Error fetching user data: " + task.getException().getMessage());
                        Toast.makeText(HomeActivity.this, "Error fetching user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        firstNameTextView.setText("Error Loading Name");
                    }
                    checkRefreshCompletion();
                });
    }

    private void fetchRecentUploads(String query) {
        Log.d(TAG, "Fetching recent uploads with query: '" + (query != null ? query : "null") + "'");

        Query baseQuery = FirebaseFirestore.getInstance().collectionGroup("rooms")
                .orderBy("uploadTimestamp", Query.Direction.DESCENDING);

        baseQuery.get()
                .addOnCompleteListener(task -> {
                    recentUploadsList.clear();
                    if (task.isSuccessful()) {
                        List<RoomData> fetchedRooms = new ArrayList<>();
                        String lowerCaseQuery = (query != null) ? query.toLowerCase() : "";

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            RoomData room = document.toObject(RoomData.class);
                            if (room != null) {
                                room.setDocumentId(document.getId());
                                room.setFavorite(favoriteRoomIds.contains(document.getId()));

                                if (lowerCaseQuery.isEmpty() ||
                                        (room.getTitle() != null && room.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                                        (room.getDescription() != null && room.getDescription().toLowerCase().contains(lowerCaseQuery)) ||
                                        (room.getPlace() != null && room.getPlace().toLowerCase().contains(lowerCaseQuery)) ||
                                        (room.getRoomType() != null && room.getRoomType().toLowerCase().contains(lowerCaseQuery))) {
                                    fetchedRooms.add(room);
                                }
                            }
                        }
                        recentUploadsList.addAll(fetchedRooms);
                        Log.d(TAG, "Fetched " + fetchedRooms.size() + " recent rooms (after search filter).");
                        recentUploadsAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Error getting recent uploads: ", task.getException());
                        Toast.makeText(this, "Failed to load recent uploads: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                    checkRefreshCompletion();
                });
    }

    private void getCurrentLocationForNearbyRooms(String query) {
        Log.d(TAG, "Getting current location for nearby rooms with query: '" + (query != null ? query : "null") + "'");
        showLoadingState();
        nearbyRoomsList.clear();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            Log.d(TAG, "Current location retrieved: " + location.getLatitude() + ", " + location.getLongitude());
                            fetchNearbyRoomsFromFirestore(location.getLatitude(), location.getLongitude(), 3.0, query);
                        } else {
                            Log.w(TAG, "Could not get last known location for nearby rooms. Displaying empty state.");
                            showEmptyState("No nearby rooms (location unavailable)");
                            Toast.makeText(this, "Could not retrieve current location for nearby rooms.", Toast.LENGTH_SHORT).show();
                            checkRefreshCompletion();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting last location for nearby rooms: " + e.getMessage());
                        showEmptyState("No nearby rooms (location error)");
                        Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        checkRefreshCompletion();
                    });
        } else {
            Log.w(TAG, "Location permission not granted. Requesting permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE_NEARBY);
            showEmptyState("Location permission needed for nearby rooms");
            checkRefreshCompletion();
        }
    }

    private void fetchNearbyRoomsFromFirestore(double userLatitude, double userLongitude, double radiusKm, String query) {
        Log.d(TAG, "Fetching nearby rooms from Firestore with query: '" + (query != null ? query : "null") + "'");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            firestore.collectionGroup("rooms")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<NearbyRoom> tempNearbyRooms = new ArrayList<>();
                            String lowerCaseQuery = (query != null) ? query.toLowerCase() : "";

                            if (task.getResult().isEmpty()) {
                                Log.d(TAG, "No rooms found in database for nearby calculation.");
                            } else {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    RoomData roomData = document.toObject(RoomData.class);
                                    if (roomData != null) {
                                        Double roomLatitude = roomData.getLatitude();
                                        Double roomLongitude = roomData.getLongitude();

                                        if (roomLatitude != null && roomLongitude != null) {
                                            double distance = calculateDistance(userLatitude, userLongitude,
                                                    roomLatitude, roomLongitude);

                                            if (distance <= radiusKm) {
                                                if (lowerCaseQuery.isEmpty() ||
                                                        (roomData.getTitle() != null && roomData.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                                                        (roomData.getDescription() != null && roomData.getDescription().toLowerCase().contains(lowerCaseQuery)) ||
                                                        (roomData.getPlace() != null && roomData.getPlace().toLowerCase().contains(lowerCaseQuery)) ||
                                                        (roomData.getRoomType() != null && roomData.getRoomType().toLowerCase().contains(lowerCaseQuery))) {

                                                    String imageUrl = (roomData.getImageUrls() != null && !roomData.getImageUrls().isEmpty())
                                                            ? roomData.getImageUrls().get(0) : null;

                                                    String roomId = document.getId();
                                                    String uploaderId = roomData.getUserId();
                                                    boolean isFav = favoriteRoomIds.contains(roomId);

                                                    tempNearbyRooms.add(new NearbyRoom(
                                                            imageUrl,
                                                            roomData.getPrice(),
                                                            roomData.getRoomType(),
                                                            formatDistance(distance),
                                                            isFav,
                                                            roomId,
                                                            uploaderId
                                                    ));
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!tempNearbyRooms.isEmpty()) {
                                Collections.sort(tempNearbyRooms, (r1, r2) -> {
                                    try {
                                        double dist1 = Double.parseDouble(r1.getDistance().split(" ")[0]);
                                        double dist2 = Double.parseDouble(r2.getDistance().split(" ")[0]);
                                        return Double.compare(dist1, dist2);
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Error parsing distance for sorting: " + e.getMessage());
                                        return 0;
                                    }
                                });
                                nearbyRoomsList.addAll(tempNearbyRooms);
                                showRoomsList();
                                Log.d(TAG, "Fetched " + nearbyRoomsList.size() + " nearby rooms (filtered by distance and search).");
                            } else {
                                showEmptyState("No nearby rooms found within " + radiusKm + " km for \"" + query + "\"");
                                Log.d(TAG, "No nearby rooms found within radius or matching search.");
                            }
                        } else {
                            showEmptyState("Failed to load rooms");
                            Log.e(TAG, "Error loading rooms for nearby calculation: ", task.getException());
                        }
                        checkRefreshCompletion();
                    });
        }, 100);
    }

    private void showLoadingState() {
        runOnUiThread(() -> {
            if (loadingNearbyRoomsText != null) loadingNearbyRoomsText.setVisibility(View.VISIBLE);
            if (noNearbyRoomsText != null) noNearbyRoomsText.setVisibility(View.GONE);
            if (nearbyRoomsRecyclerView != null) nearbyRoomsRecyclerView.setVisibility(View.GONE);
        });
    }

    private void showEmptyState(String message) {
        runOnUiThread(() -> {
            if (loadingNearbyRoomsText != null) loadingNearbyRoomsText.setVisibility(View.GONE);
            if (noNearbyRoomsText != null) {
                noNearbyRoomsText.setText(message);
                noNearbyRoomsText.setVisibility(View.VISIBLE);
            }
            if (nearbyRoomsRecyclerView != null) nearbyRoomsRecyclerView.setVisibility(View.GONE);
            nearbyRoomsAdapter.notifyDataSetChanged();
        });
    }

    private void showRoomsList() {
        runOnUiThread(() -> {
            if (loadingNearbyRoomsText != null) loadingNearbyRoomsText.setVisibility(View.GONE);
            if (noNearbyRoomsText != null) noNearbyRoomsText.setVisibility(View.GONE);
            if (nearbyRoomsRecyclerView != null) {
                nearbyRoomsRecyclerView.setVisibility(View.VISIBLE);
                nearbyRoomsAdapter.notifyDataSetChanged();
            }
        });
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String formatDistance(double distance) {
        return String.format(Locale.getDefault(), "%.1f km away", distance);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE_NEARBY) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted after request.");
                getCurrentLocationForNearbyRooms(currentSearchQuery);
            } else {
                Log.w(TAG, "Location permission denied after request.");
                Toast.makeText(this, "Location permission denied. Cannot show nearby rooms.", Toast.LENGTH_SHORT).show();
                showEmptyState("Location permission denied");
            }
        }
    }

    private void checkRefreshCompletion() {
        completedFetches++;
        Log.d(TAG, "Completed fetch: " + completedFetches + "/" + TOTAL_FETCHES_EXPECTED);
        if (completedFetches >= TOTAL_FETCHES_EXPECTED) {
            runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));
            completedFetches = 0;
            Log.d(TAG, "All fetches completed. Refresh animation stopped.");
        }
    }
}
