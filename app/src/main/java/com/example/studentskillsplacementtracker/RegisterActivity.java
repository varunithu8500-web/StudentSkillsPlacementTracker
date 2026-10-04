package com.example.studentskillsplacementtracker;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.StudentRepository;
import com.example.studentskillsplacementtracker.model.Student;
import com.example.studentskillsplacementtracker.util.AuthErrorMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Student registration screen.
 *
 * Creates the Firebase Authentication account and then persists the student
 * profile to Cloud Firestore. Registration is only considered complete once
 * both steps succeed.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MIN_YEAR = 1;
    private static final int MAX_YEAR = 4;
    private static final double MIN_CGPA = 0.0;
    private static final double MAX_CGPA = 10.0;

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private EditText departmentEditText;
    private EditText yearEditText;
    private EditText cgpaEditText;

    private Button registerButton;
    private ProgressBar registerProgressBar;
    private TextView loginLinkTextView;

    private FirebaseAuth firebaseAuth;
    private StudentRepository studentRepository;

    // Guards against duplicate registration requests
    private boolean isRegistering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Connect XML components with Java
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        departmentEditText = findViewById(R.id.departmentEditText);
        yearEditText = findViewById(R.id.yearEditText);
        cgpaEditText = findViewById(R.id.cgpaEditText);

        registerButton = findViewById(R.id.registerButton);
        registerProgressBar = findViewById(R.id.registerProgressBar);
        loginLinkTextView = findViewById(R.id.loginLinkTextView);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();

        // Single point of access for the student profile in Cloud Firestore
        studentRepository = new StudentRepository();

        registerButton.setOnClickListener(view -> registerUser());

        // Back to the login screen
        loginLinkTextView.setOnClickListener(view -> finish());
    }

    /**
     * Validates every field. Only plain text fields are trimmed - the password
     * is always used exactly as typed.
     */
    private boolean isFormValid() {

        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String department = departmentEditText.getText().toString().trim();
        String year = yearEditText.getText().toString().trim();
        String cgpa = cgpaEditText.getText().toString().trim();

        // Name
        if (name.isEmpty()) {
            return showFieldError(nameEditText, R.string.err_name_required);
        }

        // Email
        if (email.isEmpty()) {
            return showFieldError(emailEditText, R.string.err_email_required);
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return showFieldError(emailEditText, R.string.err_email_invalid);
        }

        // Password
        if (password.isEmpty()) {
            return showFieldError(passwordEditText, R.string.err_password_required);
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return showFieldError(passwordEditText, R.string.err_password_short);
        }

        // Confirm password
        if (confirmPassword.isEmpty()) {
            return showFieldError(
                    confirmPasswordEditText,
                    R.string.err_confirm_password_required
            );
        }
        if (!password.equals(confirmPassword)) {
            return showFieldError(confirmPasswordEditText, R.string.err_password_mismatch);
        }

        // Department
        if (department.isEmpty()) {
            return showFieldError(departmentEditText, R.string.err_department_required);
        }

        // Year
        if (year.isEmpty()) {
            return showFieldError(yearEditText, R.string.err_year_required);
        }
        if (!isYearValid(year)) {
            return showFieldError(yearEditText, R.string.err_year_invalid);
        }

        // CGPA
        if (cgpa.isEmpty()) {
            return showFieldError(cgpaEditText, R.string.err_cgpa_required);
        }
        if (!isCgpaValid(cgpa)) {
            return showFieldError(cgpaEditText, R.string.err_cgpa_invalid);
        }

        return true;
    }

    private boolean showFieldError(EditText field, int messageResId) {
        field.setError(getString(messageResId));
        field.requestFocus();
        return false;
    }

    private boolean isYearValid(String year) {
        try {
            int value = Integer.parseInt(year);
            return value >= MIN_YEAR && value <= MAX_YEAR;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isCgpaValid(String cgpa) {
        try {
            double value = Double.parseDouble(cgpa);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return false;
            }
            return value >= MIN_CGPA && value <= MAX_CGPA;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void registerUser() {

        if (isRegistering) {
            return;
        }

        if (!isFormValid()) {
            return;
        }

        // Only the email is trimmed. The password is never trimmed and is never
        // written to Firestore.
        final String name = nameEditText.getText().toString().trim();
        final String email = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString();
        final String department = departmentEditText.getText().toString().trim();
        // isFormValid() guarantees both values parse and fall within range.
        final int year = Integer.parseInt(yearEditText.getText().toString().trim());
        final double cgpa = Double.parseDouble(cgpaEditText.getText().toString().trim());

        setLoadingState(true);

        // Step 1: create the Firebase Authentication account.
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        setLoadingState(false);
                        Toast.makeText(
                                RegisterActivity.this,
                                AuthErrorMapper.getMessage(
                                        RegisterActivity.this,
                                        task.getException()
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    FirebaseUser user = (task.getResult() != null)
                            ? task.getResult().getUser()
                            : null;

                    if (user == null) {
                        setLoadingState(false);
                        Toast.makeText(
                                RegisterActivity.this,
                                R.string.err_registration_failed,
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    // Step 2: persist the student profile keyed by the Auth UID.
                    Student student = new Student(
                            user.getUid(),
                            name,
                            email,
                            department,
                            year,
                            cgpa,
                            Student.ROLE_STUDENT
                    );

                    saveProfileAndFinish(student);
                });
    }

    /**
     * Writes the student profile to Firestore. Registration is only considered
     * complete once this write succeeds.
     */
    private void saveProfileAndFinish(Student student) {

        studentRepository.createProfile(student, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {

                Toast.makeText(
                        RegisterActivity.this,
                        R.string.msg_registration_success,
                        Toast.LENGTH_LONG
                ).show();

                // Firebase signs the new account in automatically. Sign out again
                // so the user returns to Login and signs in explicitly.
                firebaseAuth.signOut();
                finish();

            } else {

                // The profile could not be saved, so registration is NOT complete.
                // Roll the half-created account back and report the failure.
                rollbackRegistration();
            }
        });
    }

    /**
     * Deletes the just-created (and still signed in) account so a failed profile
     * write cannot leave an orphaned authentication user behind.
     */
    private void rollbackRegistration() {

        Toast.makeText(
                RegisterActivity.this,
                R.string.err_profile_save_failed,
                Toast.LENGTH_LONG
        ).show();

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        user.delete().addOnCompleteListener(deleteTask -> {
            firebaseAuth.signOut();
            finish();
        });
    }

    private void setLoadingState(boolean loading) {
        isRegistering = loading;
        registerProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
        loginLinkTextView.setEnabled(!loading);
    }
}
