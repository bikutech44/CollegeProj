package com.example.collegeproj;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginScreenActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText emailLogin, passwdLogin;
    private MaterialButton loginButton;
    private TextView forgotPasswordTextView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loginscreen);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        emailLogin = findViewById(R.id.emailLogin);
        passwdLogin = findViewById(R.id.passwdLogin);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        TextView notHaveAccountTextView = findViewById(R.id.notHaveAccount);
        String fullText = "Not have an account? Register";

        notHaveAccountTextView.setText(fullText);

        // On Focus Change (for keyboard/TV/stylus navigation)
        notHaveAccountTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                SpannableString underlineSpan = new SpannableString(fullText);
                underlineSpan.setSpan(new UnderlineSpan(), 0, fullText.length(), 0);
                notHaveAccountTextView.setText(underlineSpan);
            } else {
                notHaveAccountTextView.setText(fullText);
            }
        });

        // On Touch (for mobile devices)
        notHaveAccountTextView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    SpannableString underlineSpan = new SpannableString(fullText);
                    underlineSpan.setSpan(new UnderlineSpan(), 0, fullText.length(), 0);
                    notHaveAccountTextView.setText(underlineSpan);
                    return true; // Indicate that the touch down event was handled
                case MotionEvent.ACTION_UP:
                    notHaveAccountTextView.setText(fullText);
                    v.performClick(); // Explicitly trigger the click
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    notHaveAccountTextView.setText(fullText);
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

        notHaveAccountTextView.setOnClickListener(v -> {
            // Create an Intent to open the SignUpscreenActivity
            Intent intent = new Intent(LoginScreenActivity.this, SignUpScreenActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        loginButton.setOnClickListener(v -> {
            loginUser(); // Call the login method
        });

        forgotPasswordTextView.setOnClickListener(v -> {
            // Implement forgot password functionality here (e.g., show a dialog)
            Toast.makeText(LoginScreenActivity.this, "Forgot Password clicked!", Toast.LENGTH_SHORT).show();
            // You would typically show a dialog asking for the user's email
            // and then send a password reset email using Firebase Auth.
        });
    }

    private void loginUser() {
        String email = emailLogin.getText().toString().trim();
        String password = passwdLogin.getText().toString().trim();

        if (email.isEmpty()) {
            emailLogin.setError("Email is required!");
            emailLogin.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwdLogin.setError("Password is required!");
            passwdLogin.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            // Email is verified, proceed to your main app activity
                            Intent intent = new Intent(LoginScreenActivity.this, HomeActivity.class); // Replace HomeActivity
                            startActivity(intent);
                            finish();
                        } else if (user != null) {
                            // Email is not verified
                            Toast.makeText(LoginScreenActivity.this, "Please verify your email address.", Toast.LENGTH_LONG).show();
                        } else {
                            // User is null, which shouldn't happen after successful login, but handle just in case
                            Toast.makeText(LoginScreenActivity.this, "Login successful, but user data not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // If sign in fails, display a message to the user
                        Toast.makeText(LoginScreenActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}