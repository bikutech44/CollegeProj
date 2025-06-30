package com.example.collegeproj;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser
import com.google.firebase.firestore.CollectionReference; // Import CollectionReference
import com.google.firebase.firestore.DocumentReference; // Import DocumentReference
import com.google.firebase.firestore.FirebaseFirestore; // Import FirebaseFirestore
import com.google.firebase.firestore.SetOptions; // Import SetOptions for merging data
import com.google.firebase.firestore.FieldValue;


import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.callback.UploadResult;
import com.cloudinary.android.callback.ErrorInfo;


import java.util.concurrent.atomic.AtomicInteger;




public class UploadRoomActivity extends AppCompatActivity {

    private static final String TAG = "UploadRoomActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int PIN_LOCATION_REQUEST_CODE = 200; // Request code for FullScreenMapActivity
    private static final int MAX_IMAGES = 5;

    private ImageView uploadImageView;
    private TextView uploadText;
    private LinearLayout selectedImagesContainer;
    private List<Uri> imageUris = new ArrayList<>();
    private Spinner roomTypeSpinner;
    private TextInputLayout otherRoomTypeInputLayout;
    private TextInputEditText otherRoomTypeEditText;
    private RadioGroup furnishingRadioGroup;
    private Button uploadRoomButton;
    private TextView locationTextView;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint selectedGeoPoint;
    private Marker selectedLocationMarker;
    private TextView fullscreenPinButton; // Changed from Button to TextView
    private TextInputEditText uploadTitleEditText;
    private TextInputEditText uploadPriceEditText;
    private TextInputEditText uploadPlaceEditText;
    private TextInputEditText uploadDescriptionEditText;


    private String selectedRoomType;
    private String selectedFurnishingStatus = "";

    // cloudanary list
    private List<String> cloudinaryImageUrls = new ArrayList<>();

    // Firebase instances
    private FirebaseAuth mAuth; // Add FirebaseAuth instance
    private FirebaseFirestore db; // Add FirebaseFirestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Important: Initialize Configuration before setContentView
//        Configuration.getInstance().setUserAgentValue(getPackageName());  // this lines removed because My application java class handel it
        setContentView(R.layout.activity_upload_room);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

// this is removed because initilized in MYApplication class
//        // cloudanary part
//        Map<String, String> config = new HashMap<>();
//        config.put("cloud_name", "dnlaudxq2"); // Cloudinary cloud name
//        config.put("api_key", "396776948922162");       // Cloudinary API key
//        config.put("api_secret", "rxKOlhA8rOtUKDOZPfFGEQju29A"); // Cloudinary API secret
//        config.put("secure", "true");
//
//        MediaManager.init(this, config);


        // Initialize the EditText fields
        uploadTitleEditText = findViewById(R.id.uploadTitleEditText);
        uploadPriceEditText = findViewById(R.id.uploadPriceEditText);
        uploadPlaceEditText = findViewById(R.id.uploadPlaceEditText);
        uploadDescriptionEditText = findViewById(R.id.uploadDescriptionEditText);



        // Initialize UI elements
        uploadImageView = findViewById(R.id.uploadImageView);
        uploadText = findViewById(R.id.uploadImg);
        selectedImagesContainer = findViewById(R.id.selectedImagesContainer);
        roomTypeSpinner = findViewById(R.id.roomTypeSpinner);
        otherRoomTypeInputLayout = findViewById(R.id.otherRoomTypeInputLayout);
        otherRoomTypeEditText = findViewById(R.id.otherRoomTypeEditText);
        furnishingRadioGroup = findViewById(R.id.furnishingRadioGroup);
        uploadRoomButton = findViewById(R.id.uploadRoomButton);
        locationTextView = findViewById(R.id.locationTextView);
        mapView = findViewById(R.id.mapView);
        fullscreenPinButton = findViewById(R.id.fullscreenPinButton); // Initialize the TextView button

        // Configure osmdroid MapView
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Choose a tile source
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15); // Initial zoom level

        // Initialize FusedLocationProviderClient for current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set listeners for image upload
        View.OnClickListener imageClickListener = v -> openFileChooser();
        uploadImageView.setOnClickListener(imageClickListener);
        uploadText.setOnClickListener(imageClickListener);

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up listener for Room Type Spinner
        roomTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                if (selectedItem.equals("Others")) {
                    otherRoomTypeInputLayout.setVisibility(View.VISIBLE);
                } else {
                    otherRoomTypeInputLayout.setVisibility(View.GONE);
                    otherRoomTypeEditText.setText("");
                    selectedRoomType = selectedItem;
                    checkIfAllFieldsFilled();
                }
                Log.d(TAG, "Selected Room Type: " + selectedRoomType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRoomType = "";
                checkIfAllFieldsFilled();
            }
        });

        // Set up listener for Other Room Type EditText
        otherRoomTypeEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && roomTypeSpinner.getSelectedItemPosition() == roomTypeSpinner.getAdapter().getCount() - 1) {
                selectedRoomType = otherRoomTypeEditText.getText().toString().trim();
                Log.d(TAG, "Other Room Type entered: " + selectedRoomType);
                checkIfAllFieldsFilled();
            }
        });

        // Set up listener for Furnishing Status RadioGroup
        furnishingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton checkedRadioButton = findViewById(checkedId);
            if (checkedRadioButton != null) {
                selectedFurnishingStatus = checkedRadioButton.getText().toString();
                Log.d(TAG, "Selected Furnishing Status: " + selectedFurnishingStatus);
            } else {
                selectedFurnishingStatus = "";
            }
            checkIfAllFieldsFilled();
        });

        // Set OnClickListener for the Upload Room button
        uploadRoomButton.setOnClickListener(v -> {
            if (isAllFieldsFilled() && selectedGeoPoint != null  && !imageUris.isEmpty()) {
                uploadRoomButton.setEnabled(false);
                Toast.makeText(this, "Uploading room data and images...", Toast.LENGTH_SHORT).show();

                cloudinaryImageUrls.clear(); // Clear previous URLs

                uploadImagesToCloudinaryAndThenSaveDataToFirestore(); // Call the function to upload images and then save data

            } else {
                String message = "Please fill in all the required fields";
                if (imageUris.isEmpty()) {
                    message += " and upload at least one image";
                }
                if (selectedGeoPoint == null) {
                    message += " and select a location on the map";
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Set up click listener for the fullscreen pin TextView
        fullscreenPinButton.setOnClickListener(v -> {
            Intent intent = new Intent(UploadRoomActivity.this, FullScreenMapActivity.class);
            // Pass the current map center as initial location (optional)
            intent.putExtra("initial_latitude", mapView.getMapCenter().getLatitude());
            intent.putExtra("initial_longitude", mapView.getMapCenter().getLongitude());
            startActivityForResult(intent, PIN_LOCATION_REQUEST_CODE);
        });

        // Set up map click listener for pinning location on the small map
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Log.d(TAG, "onMapClick: lat/lng: " + p.getLatitude() + "/" + p.getLongitude());
                selectLocation(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false; // Not handling long press for now
            }
        };
        MapEventsOverlay OverlayEvents = new MapEventsOverlay(mReceive);
        mapView.getOverlays().add(OverlayEvents);

        // Enable my location and initially check form fields
        enableMyLocation();
        checkIfAllFieldsFilled();
    }




    private void uploadImagesToCloudinaryAndThenSaveDataToFirestore() {
        if (imageUris.isEmpty()) {
            saveRoomDataToFirestore(new ArrayList<>());
            return;
        }

        // Reset URLs list for new uploads
        cloudinaryImageUrls.clear();

        // Track failed uploads
        AtomicInteger failedUploads = new AtomicInteger(0);
        AtomicInteger successfulUploads = new AtomicInteger(0); // Track successes too


        for (Uri imageUri : imageUris) {
            try {
                MediaManager.get().upload(imageUri)
//                        .unsigned("roomPic")
                        .callback(new UploadCallback() {
                            @Override
                            public void onStart(String requestId) {
                                Log.d(TAG, "Upload started for request: " + requestId);
                            }

                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {
                                double progress = (double) bytes / totalBytes * 100;
                                Log.d(TAG, "Upload progress for request " + requestId + ": " + progress + "%");
                            }

                            @Override
                            public void onSuccess(String requestId, Map resultData) {
                                Log.d(TAG, "Upload successful for request " + requestId + ": " + resultData);
                                String publicImageUrl = (String) resultData.get("secure_url");
                                cloudinaryImageUrls.add(publicImageUrl);

                                if (cloudinaryImageUrls.size() == imageUris.size()) {
                                    saveRoomDataToFirestore(cloudinaryImageUrls);
                                }
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                Log.e(TAG, "Upload error for request " + requestId + ": " + error.getDescription());
                                uploadRoomButton.setEnabled(true);
                                Toast.makeText(UploadRoomActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onReschedule(String requestId, ErrorInfo error) {
                                Log.i(TAG, "Upload rescheduled for request " + requestId + ": " + error.getDescription());
                                // You might want to add specific handling for rescheduling here,
                                // such as showing a message to the user.
                            }

                            public void onComplete(String requestId, Map resultData) {
                                Log.i(TAG, "Upload completed for requRest: " + requestId + ", Result: " + resultData);
                                // Optional: Handle completion event
                            }
                        }).dispatch();

            } catch (Exception e) {
                Log.e(TAG, "Error preparing upload: " + e.getMessage(), e);
                uploadRoomButton.setEnabled(true);
                Toast.makeText(this, "Error preparing image upload.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void saveRoomDataToFirestore(List<String> imageUrls) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            DocumentReference roomDocument = db.collection("roomsData").document(userId).collection("rooms").document();

            String title = uploadTitleEditText.getText().toString().trim();
            String price = uploadPriceEditText.getText().toString().trim();
            String place = uploadPlaceEditText.getText().toString().trim();
            String description = uploadDescriptionEditText.getText().toString().trim();
            double latitude = selectedGeoPoint.getLatitude();
            double longitude = selectedGeoPoint.getLongitude();

            Map<String, Object> roomData = new HashMap<>();
            roomData.put("title", title);
            roomData.put("roomType", selectedRoomType);
            roomData.put("furnishingStatus", selectedFurnishingStatus);
            roomData.put("price", price);
            roomData.put("place", place);
            roomData.put("description", description);
            roomData.put("latitude", latitude);
            roomData.put("longitude", longitude);
            roomData.put("userId", userId);
            roomData.put("uploadTimestamp", FieldValue.serverTimestamp());
            roomData.put("imageUrls", imageUrls); // Save the list of image URLs here

            roomDocument.set(roomData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Room data saved to Firestore with ID: " + roomDocument.getId());
                        Toast.makeText(UploadRoomActivity.this, "Room data uploaded successfully!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(UploadRoomActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                        uploadRoomButton.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving room data to Firestore: " + e.getMessage(), e);
                        Toast.makeText(UploadRoomActivity.this, "Failed to upload room data.", Toast.LENGTH_SHORT).show();
                        uploadRoomButton.setEnabled(true);
                    });
        } else {
            Log.e(TAG, "User not authenticated.");
            Toast.makeText(this, "User not authenticated. Cannot save room data.", Toast.LENGTH_SHORT).show();
            uploadRoomButton.setEnabled(true);
        }
    }


    //     Method to open file chooser for image selection
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Enable multiple image selection
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }


    // Handles results from other activities (image picker, full-screen map)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            boolean imageAdded = false;
            if (data.getClipData() != null) { // Multiple images selected
                int clipDataCount = data.getClipData().getItemCount();
                for (int i = 0; i < clipDataCount; i++) {
                    if (imageUris.size() < MAX_IMAGES) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        if (!imageUris.contains(imageUri)) { // Prevent adding duplicates
                            addImageToPreview(imageUri);
                            imageAdded = true;
                        }
                    } else {
                        Toast.makeText(this, "You can select a maximum of " + MAX_IMAGES + " images.", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            } else if (data.getData() != null) { // Single image selected
                if (imageUris.size() < MAX_IMAGES) {
                    Uri imageUri = data.getData();
                    if (!imageUris.contains(imageUri)) { // Prevent adding duplicates
                        addImageToPreview(imageUri);
                        imageAdded = true;
                    }
                } else {
                    Toast.makeText(this, "You can select a maximum of " + MAX_IMAGES + " images.", Toast.LENGTH_SHORT).show();
                }
            }
            // Always ensure the initial upload option is visible if we haven't reached the limit
            if (imageUris.size() < MAX_IMAGES) {
                uploadImageView.setVisibility(View.VISIBLE);
                uploadText.setVisibility(View.VISIBLE);
            } else {
                uploadImageView.setVisibility(View.GONE);
                uploadText.setVisibility(View.GONE);
            }
            checkIfAllFieldsFilled();
        } else if (requestCode == PIN_LOCATION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Received pinned location from FullScreenMapActivity
            double latitude = data.getDoubleExtra("pinned_latitude", Double.NaN);
            double longitude = data.getDoubleExtra("pinned_longitude", Double.NaN);
            if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                GeoPoint pinnedLocation = new GeoPoint(latitude, longitude);
                selectLocation(pinnedLocation); // Update selected location and marker
                mapView.getController().animateTo(pinnedLocation); // Center the small map on the returned location
            } else {
                Toast.makeText(this, "No location pinned in the full-screen map.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addImageToPreview(Uri uri) {
        imageUris.add(uri);
        ImageView imageView = new ImageView(this);
        // Increase these pixel values to make the images larger
        int desiredWidth = 538; // Example: Increase width to 250 pixels
        int desiredHeight = 360; // Example: Increase height to 170 pixels
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(desiredWidth, desiredHeight);
        layoutParams.setMargins(0, 0, 8, 0); // Add some margin between images
        imageView.setLayoutParams(layoutParams);
        imageView.setBackgroundResource(R.drawable.rounded_bg4);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY); // Use FIT_XY to avoid cropping
        try {
            imageView.setImageBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), uri));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
        selectedImagesContainer.addView(imageView);

        // Hide the initial upload image/text after the first image is added
        if (imageUris.size() > 0) {
            uploadImageView.setVisibility(View.GONE);
            uploadText.setVisibility(View.GONE);
        }
    }

    // Checks if all required form fields are filled
    private boolean isAllFieldsFilled() {
        String price = ((TextInputEditText) findViewById(R.id.uploadPriceEditText)).getText().toString().trim();
        String place = ((TextInputEditText) findViewById(R.id.uploadPlaceEditText)).getText().toString().trim();
        String description = ((TextInputEditText) findViewById(R.id.uploadDescriptionEditText)).getText().toString().trim();

        return imageUris != null && !imageUris.isEmpty() &&
                selectedRoomType != null && !selectedRoomType.isEmpty() &&
                !selectedFurnishingStatus.isEmpty() &&
                !price.isEmpty() &&
                !place.isEmpty() &&
                !description.isEmpty();
    }

    // Enables/disables the upload button based on form completion
    private void checkIfAllFieldsFilled() {
        uploadRoomButton.setEnabled(isAllFieldsFilled());
    }

    // Selects a location on the map and places a marker
    private void selectLocation(GeoPoint geoPoint) {
        selectedGeoPoint = geoPoint;
        locationTextView.setText("Latitude: " + selectedGeoPoint.getLatitude() + ", Longitude: " + selectedGeoPoint.getLongitude());

        // Add a marker at the selected location
        if (selectedLocationMarker != null) {
            mapView.getOverlays().remove(selectedLocationMarker); // Remove the previous marker
        }
        selectedLocationMarker = new Marker(mapView);
        selectedLocationMarker.setPosition(selectedGeoPoint);
        selectedLocationMarker.setTitle("Selected Location");
        mapView.getOverlays().add(selectedLocationMarker);
        mapView.invalidate(); // Refresh the map to show the new marker
    }

    // Enables "My Location" dot and centers map on current location
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            GeoPoint currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapView.getController().animateTo(currentGeoPoint);
                            // Optionally, automatically pin the initial location
                            selectLocation(currentGeoPoint);
                        } else {
                            Log.w(TAG, "No last known location found.");
                            Toast.makeText(UploadRoomActivity.this, "Could not retrieve current location.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Request location permission if
            // Request location permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }



    // Handles permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation(); // Permission granted, try enabling location again
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // MapView lifecycle methods (important for osmdroid)
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume(); // Call osmdroid's onResume
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause(); // Call osmdroid's onPause
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach(); // Call osmdroid's onDetach to release resources
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // osmdroid doesn't have a direct onLowMemory method on MapView.
        // Resources are handled by onPause/onDetach.
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save map center and zoom level if the map is initialized
        if (mapView != null) {
            outState.putDouble("map_latitude", mapView.getMapCenter().getLatitude());
            outState.putDouble("map_longitude", mapView.getMapCenter().getLongitude());
            outState.putDouble("map_zoom", mapView.getZoomLevelDouble());
        }
        // Save other form data as needed
    }
    @Override
     protected void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore map center and zoom level if savedInstanceState is not null
         if (mapView != null && savedInstanceState != null) {
             double latitude = savedInstanceState.getDouble("map_latitude");
             double longitude = savedInstanceState.getDouble("map_longitude");
             double zoom = savedInstanceState.getDouble("map_zoom");
             mapView.getController().setCenter(new GeoPoint(latitude, longitude));
             mapView.getController().setZoom(zoom);
         }
         // Restore other form data as needed
     }
}