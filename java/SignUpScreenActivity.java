package com.example.collegeproj;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.MotionEvent;
//import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// for authentication

import com.google.android.material.button.MaterialButton; // Import MaterialButton
import com.google.android.material.textfield.TextInputEditText; // Import TextInputEditText
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Map;


public class SignUpScreenActivity extends AppCompatActivity {

    //    firebase Authentication signup and save user data
    private FirebaseAuth mAuth; // Declare FirebaseAuth instance
    private FirebaseFirestore firestore; // Declare FirebaseFirestore instance

    private TextInputEditText fNameSignUp, lNameSignUp, emailSignUp, passwdSignUp, c_passwdSignup;
    private MaterialButton signUpButton;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_upscreen);

//        firebase code
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        fNameSignUp = findViewById(R.id.fName);
        lNameSignUp = findViewById(R.id.lName);
        emailSignUp = findViewById(R.id.emailSignUp);
        passwdSignUp = findViewById(R.id.passwdSignUp);
        c_passwdSignup = findViewById(R.id.c_passwdSignup);
        signUpButton = findViewById(R.id.signUpButton);



        TextView alreadyHaveAccountTextView = findViewById(R.id.alreadyHaveAccount);
        String fullText = "Already have an account? Login";
        SpannableString spannableString = new SpannableString(fullText);
//        spannableString.setSpan(new UnderlineSpan(), 0, fullText.length(), 0); // Underline the entire text
        alreadyHaveAccountTextView.setText(spannableString);

        // On Focus Change (for keyboard/TV/stylus navigation)
        alreadyHaveAccountTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                SpannableString underlineSpan = new SpannableString(fullText);
                underlineSpan.setSpan(new UnderlineSpan(), 0, fullText.length(), 0);
                alreadyHaveAccountTextView.setText(underlineSpan);
            } else {
                alreadyHaveAccountTextView.setText(fullText);
            }
        });

        // On Touch (for mobile devices)
        alreadyHaveAccountTextView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    SpannableString underlineSpan = new SpannableString(fullText);
                    underlineSpan.setSpan(new UnderlineSpan(), 0, fullText.length(), 0);
                    alreadyHaveAccountTextView.setText(underlineSpan);
                    return true; // Indicate that the touch down event was handled
                case MotionEvent.ACTION_UP:
                    alreadyHaveAccountTextView.setText(fullText);
                    v.performClick(); // Explicitly trigger the click
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    alreadyHaveAccountTextView.setText(fullText);
                    return true;
                default:
                    return false;
            }
        });

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.background2));
        // getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.background2));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        alreadyHaveAccountTextView.setOnClickListener(v -> {
            // Create an Intent to open the LoginscreenActivity
            Intent intent = new Intent(SignUpScreenActivity.this, LoginScreenActivity.class);

            // Start the new activity
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        // Set OnClickListener for the Sign Up button
        signUpButton.setOnClickListener(v -> {
            registerUser(); // Call the registration method
        });
    }
    private void registerUser() {
        String firstName = fNameSignUp.getText().toString().trim();
        String lastName = lNameSignUp.getText().toString().trim();
        String email = emailSignUp.getText().toString().trim();
        String password = passwdSignUp.getText().toString().trim();
        String confirmPassword = c_passwdSignup.getText().toString().trim();

        // Basic input validation
        if (firstName.isEmpty()) {
            fNameSignUp.setError("First Name is required!");
            fNameSignUp.requestFocus();
            return;
        }
        if (lastName.isEmpty()) {
            fNameSignUp.setError("Last Name is required!");
            fNameSignUp.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            emailSignUp.setError("Email is required!");
            emailSignUp.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwdSignUp.setError("Password is required!");
            passwdSignUp.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwdSignUp.setError("Password must be at least 6 characters long!");
            passwdSignUp.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            c_passwdSignup.setError("Passwords do not match!");
            c_passwdSignup.requestFocus();
            return;
        }

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {

                            // Create a Map to store user data
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("firstName", firstName);
                            userData.put("lastName", lastName);
                            userData.put("email", email);


                            firestore.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Data saved to Firestore successfully
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(task1 -> {
                                                    if (task1.isSuccessful()) {
                                                        Toast.makeText(SignUpScreenActivity.this,
                                                                "Registration successful! Please check your email for verification.",
                                                                Toast.LENGTH_LONG).show();
                                                        startActivity(new Intent(SignUpScreenActivity.this, LoginScreenActivity.class));
                                                        finish();
                                                    } else {
                                                        Toast.makeText(SignUpScreenActivity.this,
                                                                "Failed to send verification email: " + task1.getException().getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(SignUpScreenActivity.this,
                                                "Failed to save user data to Firestore: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                        // Optionally, you might want to delete the Firebase Auth user if Firestore save fails
                                        user.delete();
                                    });



                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(SignUpScreenActivity.this, "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

}