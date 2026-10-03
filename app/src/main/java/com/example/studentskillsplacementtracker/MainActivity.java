package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect XML components with Java
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();

        // Login button click
        loginButton.setOnClickListener(view -> loginUser());

        // Open the student registration screen
        TextView registerTextView = findViewById(R.id.registerTextView);
        registerTextView.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }

    private void loginUser() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            emailEditText.setError("Enter your email");
            emailEditText.requestFocus();
            return;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordEditText.setError("Enter your password");
            passwordEditText.requestFocus();
            return;
        }

        // Firebase login
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                MainActivity.this,
                                "Login successful",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        String errorMessage = "Login failed";

                        if (task.getException() != null) {
                            errorMessage = "Login failed: "
                                    + task.getException().getMessage();
                        }

                        Toast.makeText(
                                MainActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}