package com.example.collegeproj;  // Replace with your package name

import android.app.Application;
import org.osmdroid.config.Configuration;
import android.app.Application;
import org.osmdroid.config.Configuration;

// Add these imports for Cloudinary and Map
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;


public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize osmdroid configuration once for the entire app
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Cloudinary (initialize only once)
        // Initialize Cloudinary (without checking isInitialized)
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dnlaudxq2");
        config.put("api_key", "396776948922162");
        config.put("api_secret", "rxKOlhA8rOtUKDOZPfFGEQju29A");
        config.put("secure", "true");
        MediaManager.init(this, config);
    }
}