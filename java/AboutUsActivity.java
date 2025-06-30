package com.example.collegeproj;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.Html;


import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AboutUsActivity extends AppCompatActivity {

    private ImageView backButton;
    private TextView aboutUsContentTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about_us);

        // Initialize UI elements
        backButton = findViewById(R.id.backButton);
        aboutUsContentTextView = findViewById(R.id.aboutUsContentTextView);

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
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Handle back press to return to ProfileScreenActivity
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(AboutUsActivity.this, ProfileScreenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0); // No animation
                finish(); // Finish this activity
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Set the About Us content
        String aboutUsText = "<b>Welcome to Find My Room!</b> <br><br>" +
                "We are dedicated to simplifying your search for the perfect room or helping you find the right tenants for your available space. " +
                "Our mission is to connect individuals with suitable living arrangements efficiently and seamlessly.<br><br>" +
                "Key features of Find My Room include:<br>" +
                "- Easy browsing of room listings.<br>" +
                "- Intuitive tools for uploading your own room details.<br>" +
                "- Secure user authentication.<br>" +
                "- Options to manage your uploaded rooms and favorites.<br><br>" +
                "We strive to provide a user-friendly and reliable platform that meets the needs of both room seekers and providers in Nepal. " +
                "Your feedback is invaluable as we continuously work to improve our services.<br><br>" +
                "<b>Our Team:</b><br>" +
                "We are a passionate team committed to building and improving Find My Room. Meet the people behind the project:<br>" +
                "<b>Bikram Kumal: </b><br> - Project Lead, UI/Backend Developer, Tester.<br>" +
                "<b>Binam Pathak: </b><br> - UI Designer, Documentation Supporter & Testing Supporter.<br>" +
                "<b>Nirmaya Tamang: </b><br> - Documentation & Tester.<br><br>" +
                "Thank you for choosing <b>Find My Room!</b><br>";

        aboutUsContentTextView.setText(Html.fromHtml(aboutUsText, Html.FROM_HTML_MODE_LEGACY));
    }
}