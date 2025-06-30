package com.example.collegeproj;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button; // Still keep Button import if you have other buttons, but prev/next are removed
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class RoomDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RoomDetailsActivity";

    private TextView detailsTitleTextView;
    private TextView detailsPriceTextView;
    private TextView detailsPlaceTextView;
    private TextView detailsRoomTypeTextView;
    private TextView detailsFurnishingStatusTextView;
    private TextView detailsDescriptionTextView;
    // Removed: private TextView detailsLatitudeTextView; // No longer needed
    // Removed: private TextView detailsLongitudeTextView; // No longer needed
    private TextView detailsUploadTimestampTextView; // Now correctly in XML and initialized in Java

    private TextView self;

    private ViewPager2 imageViewPager;
    private ImagePagerAdapter imageSliderAdapter;
    private LinearLayout dotsIndicator;
    // Removed: private Button prevButton;
    // Removed: private Button nextButton;

    private MapView detailsMapView;
    private TextView mapCoordinatesTextView;

    private ImageView posterProfileImageView;
    private TextView posterNameTextView;
    private MaterialCardView messageOwnerButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private RoomData currentRoomData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_room_details);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        detailsTitleTextView = findViewById(R.id.detailsTitleTextView);
        detailsPriceTextView = findViewById(R.id.detailsPriceTextView);
        detailsPlaceTextView = findViewById(R.id.detailsPlaceTextView);
        detailsRoomTypeTextView = findViewById(R.id.detailsRoomTypeTextView);
        detailsFurnishingStatusTextView = findViewById(R.id.detailsFurnishingStatusTextView);
        detailsDescriptionTextView = findViewById(R.id.detailsDescriptionTextView);
        detailsUploadTimestampTextView = findViewById(R.id.detailsUploadTimestampTextView); // Now correctly initialized

        imageViewPager = findViewById(R.id.imageViewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);
        // Removed: prevButton = findViewById(R.id.prevButton);
        // Removed: nextButton = findViewById(R.id.nextButton);

        self = findViewById(R.id.self);

        detailsMapView = findViewById(R.id.detailsMapView);
        mapCoordinatesTextView = findViewById(R.id.mapCoordinatesTextView);

        posterProfileImageView = findViewById(R.id.posterProfileImageView);
        posterNameTextView = findViewById(R.id.posterNameTextView);
        messageOwnerButton = findViewById(R.id.messageOwnerButton);

        // Configure osmdroid MapView
        detailsMapView.setTileSource(TileSourceFactory.MAPNIK);
        detailsMapView.setBuiltInZoomControls(true);
        detailsMapView.setMultiTouchControls(true);
        detailsMapView.getController().setZoom(15);


        // Get RoomData from intent
        currentRoomData = getIntent().getParcelableExtra("roomDetails");
        String passedRoomId = getIntent().getStringExtra("roomId");
        String passedUploaderId = getIntent().getStringExtra("uploaderId");

        if (currentRoomData != null) {
            Log.d(TAG, "Received RoomData Parcelable.");
            displayRoomDetails(currentRoomData);
        } else if (passedRoomId != null && passedUploaderId != null) {
            Log.d(TAG, "Received RoomId and UploaderId. Fetching full RoomData.");
            fetchRoomDetailsFromFirestore(passedRoomId, passedUploaderId);
        } else {
            Toast.makeText(this, "Room details not found.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No room details or IDs found in intent. Cannot display details.");
            finish();
        }


        // Status bar and Navigation bar colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.app_background));
        }

        // Removed: prevButton.setOnClickListener and nextButton.setOnClickListener
        // The ViewPager2's onPageSelected callback still needed to update dots
        imageViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Removed: updateNavigationButtonVisibility(position);
                updateDotsIndicator(position); // Update dots on page change
            }
        });

        // Message to Owner button click listener
        messageOwnerButton.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                if (currentRoomData != null && currentRoomData.getUserId() != null && !currentRoomData.getUserId().isEmpty()) {
                    if (user.getUid().equals(currentRoomData.getUserId())) {
                        Toast.makeText(RoomDetailsActivity.this, "You cannot message yourself.", Toast.LENGTH_SHORT).show();
                    }
                    else{
//                        Toast.makeText(RoomDetailsActivity.this, "hahaha.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RoomDetailsActivity.this, "Cannot message owner: User ID not available.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(RoomDetailsActivity.this, "Please log in to message the owner.", Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent(RoomDetailsActivity.this, LoginScreenActivity.class);
                startActivity(loginIntent);
            }
        });

        findViewById(R.id.backButton).setOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void fetchRoomDetailsFromFirestore(String roomId, String uploaderId) {
        firestore.collection("roomsData")
                .document(uploaderId)
                .collection("rooms")
                .document(roomId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            currentRoomData = document.toObject(RoomData.class);
                            if (currentRoomData != null) {
                                currentRoomData.setDocumentId(document.getId());
                                displayRoomDetails(currentRoomData);
                            } else {
                                Log.e(TAG, "Failed to parse RoomData from document: " + roomId);
                                Toast.makeText(RoomDetailsActivity.this, "Error loading room details.", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Log.d(TAG, "Room document not found for ID: " + roomId + ", Uploader: " + uploaderId);
                            Toast.makeText(RoomDetailsActivity.this, "Room not found or has been removed.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.e(TAG, "Error fetching room details from Firestore: ", task.getException());
                        Toast.makeText(RoomDetailsActivity.this, "Error fetching room details.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayRoomDetails(RoomData roomData) {
        detailsTitleTextView.setText(roomData.getTitle());
        detailsPriceTextView.setText(String.format(Locale.getDefault(), "Nrs. %s/month", roomData.getPrice()));
        detailsPlaceTextView.setText(String.format(Locale.getDefault(), "Location: %s", roomData.getPlace()));
        detailsRoomTypeTextView.setText(String.format(Locale.getDefault(), "Room Type: %s", roomData.getRoomType()));
        detailsFurnishingStatusTextView.setText(String.format(Locale.getDefault(), "Furnishing: %s", roomData.getFurnishingStatus()));
        detailsDescriptionTextView.setText(String.format(Locale.getDefault(), "Description: \n%s", roomData.getDescription()));

        if (roomData.getUploadTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.getDefault());
            detailsUploadTimestampTextView.setText(String.format(Locale.getDefault(), "Posted on: %s", sdf.format(roomData.getUploadTimestamp().toDate())));
            detailsUploadTimestampTextView.setVisibility(View.VISIBLE); // Ensure visibility
        } else {
            detailsUploadTimestampTextView.setVisibility(View.GONE);
        }

        // Image Slider setup
        List<String> imageUrls = roomData.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            imageSliderAdapter = new ImagePagerAdapter(this, imageUrls);
            imageViewPager.setAdapter(imageSliderAdapter);
            setupDotsIndicator(imageUrls.size());
            // Removed: Initial button visibility update (as buttons are removed)
        } else {
            Log.w(TAG, "No image URLs found for this room. Hiding ViewPager and dots.");
            setupDotsIndicator(0);
            imageViewPager.setVisibility(View.GONE);
            // Removed: prevButton.setVisibility and nextButton.setVisibility
        }

        // Map setup
        double latitude = roomData.getLatitude();
        double longitude = roomData.getLongitude();

        if (latitude != 0.0 && longitude != 0.0) {
            GeoPoint roomLocationPoint = new GeoPoint(latitude, longitude);
            detailsMapView.getController().setCenter(roomLocationPoint);
            detailsMapView.getController().setZoom(15.0);

            Marker roomMarker = new Marker(detailsMapView);
            roomMarker.setPosition(roomLocationPoint);
            roomMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            roomMarker.setTitle(roomData.getTitle());
            detailsMapView.getOverlays().add(roomMarker);
            detailsMapView.invalidate();

            mapCoordinatesTextView.setText(String.format(Locale.getDefault(), "Latitude: %.4f, Longitude: %.4f", latitude, longitude));
            detailsMapView.setVisibility(View.VISIBLE);
            mapCoordinatesTextView.setVisibility(View.VISIBLE);
        } else {
            mapCoordinatesTextView.setText("Location not available.");
            detailsMapView.setVisibility(View.GONE);
            mapCoordinatesTextView.setVisibility(View.GONE);
        }

        fetchRoomPosterDetails(roomData.getUserId());
    }

    // Removed: updateNavigationButtonVisibility method, as prev/next buttons are removed.

    private void setupDotsIndicator(int count) {
        dotsIndicator.removeAllViews();
        if (count <= 1) {
            dotsIndicator.setVisibility(View.GONE);
            return;
        } else {
            dotsIndicator.setVisibility(View.VISIBLE);
        }

        ImageView[] dots = new ImageView[count];
        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_selector));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsIndicator.addView(dots[i], params);
        }

        if (count > 0) {
            dots[0].setSelected(true);
        }
    }

    private void updateDotsIndicator(int position) {
        int childCount = dotsIndicator.getChildCount();
        for (int i = 0; i < childCount; i++) {
            dotsIndicator.getChildAt(i).setSelected(i == position);
        }
    }

    private void fetchRoomPosterDetails(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID for poster is null or empty.");
            posterNameTextView.setText("Unknown User");
            posterProfileImageView.setImageResource(R.drawable.profile_icon);
            return;
        }

        firestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String firstName = document.getString("firstName");
                            String lastName = document.getString("lastName");
                            String profileImageUrl = document.getString("profileImageUrl");

                            FirebaseUser user = mAuth.getCurrentUser();

                            String fullName = "Unknown User";
                            if (firstName != null && !firstName.isEmpty()) {
                                fullName = firstName;
                                if (lastName != null && !lastName.isEmpty()) {
                                    fullName += " " + lastName;
                                }
                                if (user.getUid().equals(currentRoomData.getUserId())){
                                    self.setVisibility(View.VISIBLE);
                                }
                            } else if (lastName != null && !lastName.isEmpty()) {
                                fullName = lastName;
                            }
                            posterNameTextView.setText(fullName);

                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profileImageUrl)
                                        .apply(new RequestOptions()
                                                .placeholder(R.drawable.profile_icon)
                                                .error(R.drawable.profile_icon)
                                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                .centerCrop())
                                        .into(posterProfileImageView);
                            } else {
                                Log.d(TAG, "Profile image URL is null or empty. Setting default icon.");
                                posterProfileImageView.setImageResource(R.drawable.profile_icon);
                            }
                        } else {
                            Log.d(TAG, "User document not found for userId: " + userId);
                            posterNameTextView.setText("User Not Found");
                            posterProfileImageView.setImageResource(R.drawable.profile_icon);
                        }
                    } else {
                        Log.e(TAG, "Error loading poster info: ", task.getException());
                        Toast.makeText(this, "Error loading poster info: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        posterNameTextView.setText("Error Loading User");
                        posterProfileImageView.setImageResource(R.drawable.profile_icon);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (detailsMapView != null) {
            detailsMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (detailsMapView != null) {
            detailsMapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detailsMapView != null) {
            detailsMapView.onDetach();
        }
    }
}
