package com.example.collegeproj;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View; // Import View for View.GONE/VISIBLE

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.collegeproj.ImagePagerAdapter;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.SimpleDateFormat; // Import for date formatting
import java.util.List;
import java.util.Locale;
import java.util.Date; // Import for Date object

public class MyUploadDetailsActivity extends AppCompatActivity {

    private static final String TAG = "MyUploadDetailsActivity";

    private ImageView backButton;
    private TextView screenTitle;
    private ViewPager2 roomImageViewPager;
    private LinearLayout dotsIndicator;
    private TextView roomTitle;
    private TextView roomPrice;
    private TextView roomLocation;
    private TextView roomType;
    private TextView roomFurnishing;
    private TextView roomDescription;
    private TextView detailsUploadTimestampTextView; // Declare the new TextView

    private MapView myUploadDetailsMapView;
    private TextView mapCoordinatesTextView;

    private ImagePagerAdapter imageSliderAdapter;
    private RoomData currentRoomData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_upload_details);

        // Initialize UI elements
        backButton = findViewById(R.id.backButton);
        screenTitle = findViewById(R.id.screenTitle);
        roomImageViewPager = findViewById(R.id.roomImageViewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);
        roomTitle = findViewById(R.id.roomTitle);
        roomPrice = findViewById(R.id.roomPrice);
        roomLocation = findViewById(R.id.roomLocation);
        roomType = findViewById(R.id.roomType);
        roomFurnishing = findViewById(R.id.roomFurnishing);
        roomDescription = findViewById(R.id.roomDescription);
        detailsUploadTimestampTextView = findViewById(R.id.detailsUploadTimestampTextView); // Initialize the TextView

        myUploadDetailsMapView = findViewById(R.id.myUploadDetailsMapView);
        mapCoordinatesTextView = findViewById(R.id.mapCoordinatesTextView);

        // Configure osmdroid MapView
        Configuration.getInstance().setUserAgentValue(getPackageName());
        myUploadDetailsMapView.setTileSource(TileSourceFactory.MAPNIK);
        myUploadDetailsMapView.setBuiltInZoomControls(true);
        myUploadDetailsMapView.setMultiTouchControls(true);
        myUploadDetailsMapView.getController().setZoom(15);

        // Get RoomData from Intent
        currentRoomData = getIntent().getParcelableExtra("roomDetails");

        if (currentRoomData != null) {
            displayRoomDetails(currentRoomData);
        } else {
            Toast.makeText(this, "Room details not found.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "RoomDetails is null. Cannot display details.");
            finish();
        }

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.app_background));
        }

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up back button
        backButton.setOnClickListener(v -> onBackPressed());

        // Handle back press to return to MyUploadsActivity
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(MyUploadDetailsActivity.this, MyUploadsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    /**
     * Displays the details of the room on the UI.
     * @param roomData The RoomData object to display.
     */
    private void displayRoomDetails(RoomData roomData) {
        // Image Slider
        List<String> imageUrls = roomData.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            imageSliderAdapter = new ImagePagerAdapter(this, imageUrls);
            roomImageViewPager.setAdapter(imageSliderAdapter);
            setupDotsIndicator(imageUrls.size());
        } else {
            Log.w(TAG, "No image URLs found for this room. Setting default placeholder.");
            setupDotsIndicator(0);
        }

        // Text Details
        roomTitle.setText(roomData.getTitle());
        roomPrice.setText(String.format(Locale.getDefault(), "Nrs. %s/month", roomData.getPrice()));
        roomLocation.setText(roomData.getPlace());
        roomType.setText(String.format(Locale.getDefault(), "Room Type: %s", roomData.getRoomType()));
        roomFurnishing.setText(String.format(Locale.getDefault(), "Furnishing Status: %s", roomData.getFurnishingStatus()));
        roomDescription.setText(String.format(Locale.getDefault(), "Description: \n%s", roomData.getDescription()));

        // Set the upload timestamp
        if (roomData.getUploadTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String formattedTimestamp = sdf.format(roomData.getUploadTimestamp().toDate());
            detailsUploadTimestampTextView.setText(String.format(Locale.getDefault(), "Posted on: %s", formattedTimestamp));
            detailsUploadTimestampTextView.setVisibility(View.VISIBLE); // Ensure visibility
        } else {
            detailsUploadTimestampTextView.setVisibility(View.GONE); // Hide if no timestamp
            Log.w(TAG, "Upload timestamp is null for room: " + roomData.getDocumentId());
        }

        // Map setup
        double latitude = roomData.getLatitude();
        double longitude = roomData.getLongitude();

        if (latitude != 0.0 && longitude != 0.0) {
            GeoPoint roomLocationPoint = new GeoPoint(latitude, longitude);
            myUploadDetailsMapView.getController().setCenter(roomLocationPoint);
            myUploadDetailsMapView.getController().setZoom(15.0);

            Marker roomMarker = new Marker(myUploadDetailsMapView);
            roomMarker.setPosition(roomLocationPoint);
            roomMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            roomMarker.setTitle(roomData.getTitle());
            myUploadDetailsMapView.getOverlays().add(roomMarker);
            myUploadDetailsMapView.invalidate();

            mapCoordinatesTextView.setText(String.format(Locale.getDefault(), "Latitude: %.4f, Longitude: %.4f", latitude, longitude));
            myUploadDetailsMapView.setVisibility(View.VISIBLE);
            mapCoordinatesTextView.setVisibility(View.VISIBLE);
        } else {
            mapCoordinatesTextView.setText("Location not available.");
            myUploadDetailsMapView.setVisibility(View.GONE);
            mapCoordinatesTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up the dots indicator for the ViewPager2.
     * @param count The number of dots (equal to the number of images).
     */
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
        // Listener to update dots on page change
        roomImageViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                for (int i = 0; i < count; i++) {
                    dots[i].setSelected(i == position);
                }
            }
        });
    }

    // MapView lifecycle methods (important for osmdroid)
    @Override
    protected void onResume() {
        super.onResume();
        if (myUploadDetailsMapView != null) {
            myUploadDetailsMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (myUploadDetailsMapView != null) {
            myUploadDetailsMapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myUploadDetailsMapView != null) {
            myUploadDetailsMapView.onDetach();
        }
    }
}
