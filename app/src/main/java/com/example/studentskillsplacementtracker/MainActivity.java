package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.util.AuthErrorMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar loginProgressBar;
    private TextView registerTextView;

    private FirebaseAuth firebaseAuth;

    // Guards against duplicate login requests
    private boolean isLoggingIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();

        // Already authenticated? Skip the login screen.
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            navigateAfterLogin();
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Connect XML components with Java
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        registerTextView = findViewById(R.id.registerTextView);

        // Login button click
        loginButton.setOnClickListener(view -> loginUser());

        // Open the student registration screen
        registerTextView.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }

    /**
     * Routes an authenticated user to their home screen.
     *
     * Every authenticated user currently lands on StudentHomeActivity. Role based
     * routing is added in a later phase.
     */
    private void navigateAfterLogin() {
        startActivity(new Intent(MainActivity.this, StudentHomeActivity.class));
    }

    private void loginUser() {

        if (isLoggingIn) {
            return;
        }

        String email = emailEditText.getText().toString().trim();
        // The password is never trimmed.
        String password = passwordEditText.getText().toString();

        // Validate email
        if (email.isEmpty()) {
            emailEditText.setError(getString(R.string.err_email_required));
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.err_email_invalid));
            emailEditText.requestFocus();
            return;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordEditText.setError(getString(R.string.err_password_required));
            passwordEditText.requestFocus();
            return;
        }

        setLoadingState(true);

        // Firebase login
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    setLoadingState(false);

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                MainActivity.this,
                                R.string.msg_login_success,
                                Toast.LENGTH_SHORT
                        ).show();

                        // Leave the login screen so Back cannot return to it
                        navigateAfterLogin();
                        finish();

                    } else {

                        Toast.makeText(
                                MainActivity.this,
                                AuthErrorMapper.getMessage(MainActivity.this, task.getException()),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void setLoadingState(boolean loading) {
        isLoggingIn = loading;
        loginProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        registerTextView.setEnabled(!loading);
    }
}