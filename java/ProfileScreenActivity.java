package com.example.collegeproj;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView; // Make sure TextView is imported for userNameTextView etc.
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.net.Uri;

import com.bumptech.glide.Glide; // Import Glide for image loading
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; // Import for Firestore document
import com.google.firebase.firestore.FirebaseFirestore; // Import for Firestore


public class ProfileScreenActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ProfileScreenActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Declare Firestore instance

    // UI elements

    private ImageView profileImageView;
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private TextView myUploadsButton; // Changed to TextView to match your XML
    private Button logoutButton;

    // Bottom Navigation buttons
    private ImageView homeButton;
    private ImageView favouriteButton;
    private ImageView addButton;
    private ImageView messageButton;
    private TextView profileButton; // Changed to TextView to match your XML

    private TextView termsPolicy;
    private TextView followUs;
    private TextView contactUs;
    private TextView aboutUs;

    private TextView version;

    public String appVersion= "1.0.1(1)";


    private SwipeRefreshLayout swipeRefreshLayout;


    private TextView savedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profilescreen);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Profile UI elements
        userNameTextView = findViewById(R.id.userNameTextView);
        userEmailTextView = findViewById(R.id.userEmailTextView);
        myUploadsButton = findViewById(R.id.myUploadsButton); // Initialize My Uploads button
        logoutButton = findViewById(R.id.logoutButton);



        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);

        // Set status bar and navigation bar colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.app_background));
        }

        // --- Back Pressed Callback ---
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Finish this activity
                overridePendingTransition(0, 0); // No animation

            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);


        // --- User Profile Data Fetch ---
//        fetchUserProfile();

        // --- My Uploads Button Listener ---
        myUploadsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    Intent intent = new Intent(ProfileScreenActivity.this, MyUploadsActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                } else {
                    Toast.makeText(ProfileScreenActivity.this, "Please log in to view your uploads.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        // --- LOGOUT BUTTON IMPLEMENTATION ---
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut(); // Sign out the user from Firebase

                // Navigate to LoginScreenActivity and clear all activities from the back stack
                Intent intent = new Intent(ProfileScreenActivity.this, LoginScreenActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(0, 0); // Disable transition animation
                finish(); // Finish the current ProfileScreenActivity
            }
        });

        // --- Bottom Navigation Button Listeners ---
        homeButton = findViewById(R.id.homeButton);
        favouriteButton = findViewById(R.id.favouriteButton);
        addButton = findViewById(R.id.addButton);
        messageButton = findViewById(R.id.messageButton);
        profileButton = findViewById(R.id.profileButton);
        savedButton = findViewById(R.id.mySavedButton);


        termsPolicy = findViewById(R.id.termsPolicy);
        followUs = findViewById(R.id.followUs);
        contactUs = findViewById(R.id.contactUs);
        aboutUs = findViewById(R.id.aboutUs);
        version =  findViewById(R.id.version);

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileScreenActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        favouriteButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileScreenActivity.this, FavouriteScreenActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
//            finish();
            // No finish() here, so ProfileScreenActivity stays on the back stack.
            // When FavouriteScreenActivity finishes, it will return to ProfileScreenActivity.
        });

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileScreenActivity.this, UploadRoomActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        messageButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileScreenActivity.this, MessageScreenActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });



        savedButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileScreenActivity.this, FavouriteScreenActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });




        // terms
        termsPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileScreenActivity.this, TermsPoliciesActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // follow us
//        followUs.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String facebookUrl = "https://www.facebook.com/bikram.kumal.02";
//
//                try {
//                    // Attempt to open the URL directly
//                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl));
//                    startActivity(intent);
//                    // Removed overridePendingTransition(0,0) as it was not in your exact snippet
//                } catch (android.content.ActivityNotFoundException e) {
//                    // If no application can handle the intent, catch the exception
//                    Toast.makeText(ProfileScreenActivity.this, "No application available to open the link.", Toast.LENGTH_LONG).show();
//                    Log.e(TAG, "Failed to open URL: " + facebookUrl + ", Error: " + e.getMessage());
//                }
//            }
//        });
        // follow us
        followUs.setOnClickListener(v -> {
            String facebookProfileUrl = "https://www.facebook.com/bikram.kumal.02";

            Log.e(TAG, "Attempting to open URL: " + facebookProfileUrl);

            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookProfileUrl));

                // Check if there's any app that can handle the intent at all
                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                    // Create a chooser intent to force the selection dialog
                    Intent chooserIntent = Intent.createChooser(browserIntent, "Open with...");
                    startActivity(chooserIntent);
                    overridePendingTransition(0, 0);
                } else {
                    Toast.makeText(ProfileScreenActivity.this, "No application available to open the link.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening Facebook link: " + e.getMessage(), e);
                Toast.makeText(ProfileScreenActivity.this, "Failed to open Facebook link.", Toast.LENGTH_LONG).show();
            }
        });

        // Contact Us via WhatsApp
        contactUs.setOnClickListener(v -> {
            String phoneNumber = "9779820271484"; // WhatsApp number without '+' or spaces

            try {
                // URI for opening WhatsApp chat directly
                String whatsappUri = "whatsapp://send?phone=" + phoneNumber;
                Intent whatsappIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUri));

                // Check if WhatsApp app is installed and can handle the intent
                if (whatsappIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(whatsappIntent);
                    overridePendingTransition(0, 0);
                } else {
                    // Fallback if WhatsApp is not installed: try to open in a browser via web.whatsapp.com
                    // This often doesn't work well for direct chats if the app isn't there, but it's a possibility.
                    // A better fallback might be to just show a toast or open a general contact page.
                    String webWhatsappUrl = "https://api.whatsapp.com/send?phone=" + phoneNumber;
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webWhatsappUrl));
                    if (webIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(webIntent);
                        overridePendingTransition(0, 0);
                    } else {
                        Toast.makeText(ProfileScreenActivity.this, "WhatsApp is not installed and no browser can open the web link.", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening WhatsApp: " + e.getMessage(), e);
                Toast.makeText(ProfileScreenActivity.this, "Could not open WhatsApp. Please ensure it's installed.", Toast.LENGTH_LONG).show();
            }
        });


        aboutUs.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileScreenActivity.this, AboutUsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        version.setOnClickListener(v->{
            Toast.makeText(ProfileScreenActivity.this, "App Version: "+appVersion, Toast.LENGTH_LONG).show();
        });




    }



    @Override
    public void onRefresh() {
        Log.d(TAG, "onRefresh: Pull to refresh triggered.");
        fetchUserProfile(); // Re-fetch data on pull-to-refresh
    }

    /**
     * Fetches the current user's profile data (first name and email) from Firestore
     * and updates the UI.
     */
    private void fetchUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String userEmail = currentUser.getEmail(); // Get email directly from FirebaseUser

            // Set email immediately as it's always available if logged in
            if (userEmail != null && !userEmail.isEmpty()) {
                userEmailTextView.setText(userEmail);
            } else {
                userEmailTextView.setText("Email Not Available");
            }


            db.collection("users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String firstName = document.getString("firstName");
                                String lastName = document.getString("lastName");
                                if (firstName != null && !firstName.isEmpty()) {
                                    userNameTextView.setText(firstName);
                                }
                                if (lastName != null && !lastName.isEmpty()) {
                                    if (userNameTextView.length() > 0) {
                                        userNameTextView.append(" ");
                                    }
                                    userNameTextView.append(lastName);
                                }
                                else {
                                    userNameTextView.setText("User"); // Fallback if firstName is null/empty
                                }
                                Log.d(TAG, "User profile data fetched successfully.");
                            } else {
                                Log.d(TAG, "User document not found for UID: " + userId);
                                userNameTextView.setText("User");
                                Toast.makeText(this, "Profile data not found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Error fetching user profile data: " + task.getException().getMessage());
                            Toast.makeText(this, "Error fetching profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            userNameTextView.setText("Error Loading Name");
                        }
                    });
        } else {
            // No user logged in
            userNameTextView.setText("Guest User");
            userEmailTextView.setText("Not Logged In");
            Toast.makeText(this, "Please log in to view your profile.", Toast.LENGTH_LONG).show();
            if (profileImageView != null) { // Check before setting image
                profileImageView.setImageResource(R.drawable.profile_icon);
            }
            swipeRefreshLayout.setRefreshing(false);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile data in onResume in case it was updated (e.g., from an Edit Profile screen)
        fetchUserProfile();
    }
}