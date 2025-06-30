package com.example.collegeproj;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
//import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;
//import android.annotation.SuppressLint;
//
public class SplashScreenActivity extends AppCompatActivity {

    private static final String TAG = "SplashscreenActivity";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Optional: Hide status and navigation bars for a full-screen splash
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //         WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.background));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.background));
        }

        setContentView(R.layout.activity_splashscreen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Check if the user is currently logged in
                FirebaseUser currentUser = mAuth.getCurrentUser();
                // --- ADDED LOGGING HERE ---
                if (currentUser != null) {
                    Log.d(TAG, "User is logged in. UID: " + currentUser.getUid());
                    Log.d(TAG, "Email Verified: " + currentUser.isEmailVerified());
                } else {
                    Log.d(TAG, "No user is currently logged in.");
                }
                // --- END ADDED LOGGING ---

                if (currentUser != null) {
                    if (currentUser.isEmailVerified()) {
                        // User is logged in AND email is verified, go to HomeActivity
                        startActivity(new Intent(SplashScreenActivity.this, HomeActivity.class));
                    } else {
                        startActivity(new Intent(SplashScreenActivity.this, LoginScreenActivity.class));
                        Log.d(TAG, "User email not verified, redirected to Login.");
                    }
                } else {
                    // User is not logged in, go to LoginscreenActivity
                    startActivity(new Intent(SplashScreenActivity.this, LoginScreenActivity.class));
                }
                finish();
            }
        }, 1800);
    }
}