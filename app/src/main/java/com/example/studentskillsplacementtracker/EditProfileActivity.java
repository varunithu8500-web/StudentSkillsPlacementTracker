package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.StudentRepository;
import com.example.studentskillsplacementtracker.model.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private StudentRepository studentRepository;

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText departmentEditText;
    private EditText yearEditText;
    private EditText cgpaEditText;

    private Button saveButton;
    private Button cancelButton;
    private ProgressBar progressBar;

    private String currentUid;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        studentRepository = new StudentRepository();

        nameEditText = findViewById(R.id.editProfileNameEditText);
        emailEditText = findViewById(R.id.editProfileEmailEditText);
        departmentEditText = findViewById(R.id.editProfileDepartmentEditText);
        yearEditText = findViewById(R.id.editProfileYearEditText);
        cgpaEditText = findViewById(R.id.editProfileCgpaEditText);

        saveButton = findViewById(R.id.saveProfileButton);
        cancelButton = findViewById(R.id.cancelProfileButton);
        progressBar = findViewById(R.id.editProfileProgressBar);

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Please log in to edit your profile.",
                    Toast.LENGTH_LONG
            ).show();

            goToLogin();
            return;
        }

        currentUid = currentUser.getUid();

        emailEditText.setText(currentUser.getEmail());

        saveButton.setOnClickListener(view -> saveProfile());

        cancelButton.setOnClickListener(view -> finish());

        loadProfile();
    }

    // Load the existing profile from Firestore.
    private void loadProfile() {

        setLoading(true);

        studentRepository.getProfile(
                currentUid,
                student -> {

                    setLoading(false);

                    if (student == null) {
                        Toast.makeText(
                                this,
                                "Student profile was not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        saveButton.setEnabled(false);
                        return;
                    }

                    nameEditText.setText(safe(student.getName()));
                    emailEditText.setText(safe(student.getEmail()));
                    departmentEditText.setText(
                            safe(student.getDepartment())
                    );
                    yearEditText.setText(
                            String.valueOf(student.getYear())
                    );
                    cgpaEditText.setText(
                            String.valueOf(student.getCgpa())
                    );
                },
                error -> {

                    setLoading(false);

                    Toast.makeText(
                            this,
                            "Unable to load profile. Check your connection.",
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    // Validate and save the edited profile.
    private void saveProfile() {

        if (isLoading) {
            return;
        }

        String name = nameEditText.getText().toString().trim();
        String department = departmentEditText.getText()
                .toString().trim();
        String yearText = yearEditText.getText().toString().trim();
        String cgpaText = cgpaEditText.getText().toString().trim();

        // Validate name.
        if (name.isEmpty()) {
            nameEditText.setError("Name is required.");
            nameEditText.requestFocus();
            return;
        }

        // Validate department.
        if (department.isEmpty()) {
            departmentEditText.setError("Department is required.");
            departmentEditText.requestFocus();
            return;
        }

        // Validate year.
        if (yearText.isEmpty()) {
            yearEditText.setError("Year is required.");
            yearEditText.requestFocus();
            return;
        }

        int year;

        try {
            year = Integer.parseInt(yearText);
        } catch (NumberFormatException e) {
            yearEditText.setError("Enter a valid year.");
            yearEditText.requestFocus();
            return;
        }

        if (year < 1 || year > 4) {
            yearEditText.setError("Year must be between 1 and 4.");
            yearEditText.requestFocus();
            return;
        }

        // Validate CGPA.
        if (cgpaText.isEmpty()) {
            cgpaEditText.setError("CGPA is required.");
            cgpaEditText.requestFocus();
            return;
        }

        double cgpa;

        try {
            cgpa = Double.parseDouble(cgpaText);
        } catch (NumberFormatException e) {
            cgpaEditText.setError("Enter a valid CGPA.");
            cgpaEditText.requestFocus();
            return;
        }

        if (Double.isNaN(cgpa)
                || Double.isInfinite(cgpa)
                || cgpa < 0
                || cgpa > 10) {

            cgpaEditText.setError("CGPA must be between 0 and 10.");
            cgpaEditText.requestFocus();
            return;
        }

        // Save the validated values.
        setLoading(true);

        studentRepository.updateProfile(
                currentUid,
                name,
                department,
                year,
                cgpa,
                task -> {

                    setLoading(false);

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                EditProfileActivity.this,
                                "Profile updated successfully.",
                                Toast.LENGTH_SHORT
                        ).show();

                        setResult(RESULT_OK);
                        finish();

                    } else {

                        Toast.makeText(
                                EditProfileActivity.this,
                                "Profile update failed. Please try again.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // Show or hide the loading indicator.
    private void setLoading(boolean loading) {

        isLoading = loading;

        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        nameEditText.setEnabled(!loading);
        departmentEditText.setEnabled(!loading);
        yearEditText.setEnabled(!loading);
        cgpaEditText.setEnabled(!loading);

        saveButton.setEnabled(!loading);
        cancelButton.setEnabled(!loading);
    }

    // Avoid displaying null values.
    private String safe(String value) {
        return value == null ? "" : value;
    }

    // Return to the login screen if the session has expired.
    private void goToLogin() {

        Intent intent = new Intent(
                EditProfileActivity.this,
                MainActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}