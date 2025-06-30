package com.example.collegeproj;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TermsPoliciesActivity extends AppCompatActivity {

    private ImageView backButton;
    private WebView termsWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_terms_policies);

        // Initialize UI elements
        backButton = findViewById(R.id.backButton);
        termsWebView = findViewById(R.id.termsWebView);

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
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed()); // FIX: Use getOnBackPressedDispatcher()

        // Handle back press to return to ProfileScreenActivity
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(TermsPoliciesActivity.this, ProfileScreenActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0); // No animation
                finish(); // Finish this activity
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);


        // Configure WebView settings (optional, but good for security and performance)
        termsWebView.getSettings().setJavaScriptEnabled(false); // Generally, no JS needed for static terms
        termsWebView.getSettings().setDomStorageEnabled(false);
        termsWebView.setWebViewClient(new WebViewClient()); // Keep navigation within the WebView

        // Load the HTML content. I'm using an HTML string directly for simplicity.
        // You can convert the Markdown content into HTML.
        String termsHtmlContent = getTermsAndPoliciesHtml();
        termsWebView.loadDataWithBaseURL(null, termsHtmlContent, "text/html", "UTF-8", null);
    }

    private String getTermsAndPoliciesHtml() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Terms &amp; Policies</title>" +
                "<style>" +
                "body { font-family: sans-serif; margin: 14px; line-height: 1.6; color: #333; background-color: #f8f8f8; }" +
                "h1 { color: #000000; font-size: 20px; margin-bottom: 0.2px; }" +
                "h2 { color: #424242; font-size: 16px; margin-top: 22px; margin-bottom: 8px; border-bottom: 1px solid #eee; padding-bottom: 3px; }" +
                "p { margin-bottom: 14px; font-size: 14px;}" +
                "h3 {margin-bottom: 8px; color: #555555; font-size: 12px; }"+
                "ul { list-style-type: disc; margin-left: -4px; margin-bottom: 10px; }" +
                "li { margin-bottom: 5px; }" +
                "strong { font-weight: bold; }" +
                "a { color: #2196F3; text-decoration: none; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<h1>Terms &amp; Policies</h1>" +
                "<h3>Last Updated: June 28, 2025</h3>" +
                "<p>Welcome to Find My Room! These Terms and Policies govern your access to and use of our mobile application, services, features, content, &amp; .etc. By accessing or using the Service, you agree to be bound by these Terms and our Privacy Policy. If you do not agree to these Terms, you may not access or use the Service.</p>" +
                "<h2>1. Acceptance of Terms</h2>" +
                "<p>By creating an account, accessing, or using Find My Room, you confirm that you have read, understood, and agree to be bound by these Terms, and you represent that you are of legal age to form a binding contract. If you are using the Service on behalf of an organization, you agree to these Terms on behalf of that organization.</p>" +
                "<h2>2. Your Responsibilities</h2>" +
                "<p>You are responsible for your use of the Service and for any content you provide, including compliance with applicable laws, rules, and regulations. You agree not to misuse the Service or assist anyone else to do so. This includes, but is not limited to:</p>" +
                "<ul>" +
                "<li>Providing accurate and up-to-date information.</li>" +
                "<li>Maintaining the confidentiality of your account credentials.</li>" +
                "<li>Not uploading or sharing any content that is illegal, offensive, harmful, defamatory, infringing, or violates the rights of others.</li>" +
                "<li>Not interfering with or disrupting the integrity or performance of the Service.</li>" +
                "<li>Respecting the intellectual property rights of Find My Room and third parties.</li>" +
                "</ul>" +
                "<h2>3. User-Generated Content</h2>" +
                "<p>Find My Room allows you to upload, share, and manage content related to &quot;rooms.&quot; By uploading any content (such as images, descriptions, or comments), you grant Find My Room a worldwide, non-exclusive, royalty-free, transferable, and sublicensable license to use, reproduce, modify, adapt, publish, distribute, and display such content in connection with the Service. You represent and warrant that you have all necessary rights to grant us this license for any content you submit. We reserve the right to remove any content that violates these Terms or is otherwise objectionable.</p>" +
                "<h2>4. Intellectual Property</h2>" +
                "<p>All content, features, and functionality of Find My Room, including but not limited to text, graphics, logos, icons, images, and software, are the exclusive property of Find My Room and its licensors and are protected by copyright, trademark, and other intellectual property laws. You may not reproduce, distribute, modify, create derivative works of, publicly display, publicly perform, republish, download, store, or transmit any of the material on our Service, except as generally permitted for your personal, non-commercial use.</p>" +
                "<h2>5. Privacy</h2>" +
                "<p>Your privacy is very important to us. Please review our separate <strong>Privacy Policy</strong> linked within our app, which explains how we collect, use, and disclose information about you. By using the Service, you consent to our data practices as described in the Privacy Policy.</p>" +
                "<h2>6. Disclaimers and Limitation of Liability</h2>" +
                "<p>The Service is provided on an &quot;AS IS&quot; and &quot;AS AVAILABLE&quot; basis. Find My Room makes no warranties, express or implied, regarding the Service. To the maximum extent permitted by law, Find My Room shall not be liable for any indirect, incidental, special, consequential, or punitive damages, or any loss of profits or revenues, whether incurred directly or indirectly, or any loss of data, use, goodwill, or other intangible losses, resulting from (a) your access to or use of or inability to access or use the Service; (b) any conduct or content of any third party on the Service; (c) any content obtained from the Service; and (d) unauthorized access, use, or alteration of your transmissions or content.</p>" +
                "<h2>7. Termination</h2>" +
                "<p>We may terminate or suspend your access to all or part of the Service immediately, without prior notice or liability, for any reason whatsoever, including without limitation if you breach these Terms. Upon termination, your right to use the Service will immediately cease.</p>" +
                "<h2>8. Governing Law</h2>" +
                "<p>These Terms shall be governed and construed in accordance with the laws of Nepal, without regard to its conflict of law provisions.</p>" +
                "<h2>9. Changes to Terms</h2>" +
                "<p>We reserve the right, at our sole discretion, to modify or replace these Terms at any time. If a revision is material, we will provide at least 30 days, notice prior to any new terms taking effect. By continuing to access or use our Service after those revisions become effective, you agree to be bound by the revised terms.</p>" +
                "<h2>10. Contact Us</h2>" +
                "<p>If you have any questions about these Terms, please contact us at <i><u>bikutech44@gmail.com</u></i>.</p>" +
                "</body>" +
                "</html>";
    }
}
