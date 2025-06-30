package com.example.collegeproj;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class FullScreenMapActivity extends AppCompatActivity {

    private static final String TAG = "FullScreenMapActivity";
    private MapView fullScreenMapView;
    private GeoPoint pinnedLocation;
    private Marker pinMarker;
    private Button pinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_full_screen_map);

        fullScreenMapView = findViewById(R.id.fullScreenMapView);
        pinButton = findViewById(R.id.pinButton);

        fullScreenMapView.setTileSource(TileSourceFactory.MAPNIK);
        fullScreenMapView.setBuiltInZoomControls(true);
        fullScreenMapView.setMultiTouchControls(true);
        fullScreenMapView.getController().setZoom(15);

        // Get initial location if passed
        double initialLatitude = getIntent().getDoubleExtra("initial_latitude", Double.NaN);
        double initialLongitude = getIntent().getDoubleExtra("initial_longitude", Double.NaN);
        if (!Double.isNaN(initialLatitude) && !Double.isNaN(initialLongitude)) {
            GeoPoint initialPoint = new GeoPoint(initialLatitude, initialLongitude);
            fullScreenMapView.getController().animateTo(initialPoint);
        }

        // Map click listener for pinning
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Log.d(TAG, "onMapClick: lat/lng: " + p.getLatitude() + "/" + p.getLongitude());
                pinLocation(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        MapEventsOverlay OverlayEvents = new MapEventsOverlay(mReceive);
        fullScreenMapView.getOverlays().add(OverlayEvents);

        pinButton.setOnClickListener(v -> {
            if (pinnedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("pinned_latitude", pinnedLocation.getLatitude());
                resultIntent.putExtra("pinned_longitude", pinnedLocation.getLongitude());
                setResult(RESULT_OK, resultIntent);
                finish(); // Go back to UploadRoomActivity
            } else {
                Toast.makeText(this, "Please pin a location on the map.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pinLocation(GeoPoint geoPoint) {
        pinnedLocation = geoPoint;
        // Update the marker
        if (pinMarker != null) {
            fullScreenMapView.getOverlays().remove(pinMarker);
        }
        pinMarker = new Marker(fullScreenMapView);
        pinMarker.setPosition(pinnedLocation);
        pinMarker.setTitle("Pinned Location");
        fullScreenMapView.getOverlays().add(pinMarker);
        fullScreenMapView.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fullScreenMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fullScreenMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fullScreenMapView.onDetach();
    }
}