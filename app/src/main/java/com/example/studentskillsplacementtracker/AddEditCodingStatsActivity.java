package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.CodingStatsRepository;
import com.example.studentskillsplacementtracker.model.CodingStats;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Add / edit form for a single coding statistics entry (FR-COD-01).
 *
 * The form works in "edit" mode when a coding id is supplied as an Intent
 * extra. Only the coding id is ever read from the Intent: the owning uid always
 * comes from the authenticated Firebase user, never from the UI layer. The
 * total is derived from the three difficulty counts and is not editable.
 */
public class AddEditCodingStatsActivity extends AppCompatActivity {

    /** Extra carrying the id of the coding statistics being edited. */
    public static final String EXTRA_CODING_ID = "extra_coding_id";

    private FirebaseAuth firebaseAuth;
    private CodingStatsRepository codingStatsRepository;

    private TextView codingStatsFormTitleTextView;
    private EditText platformEditText;
    private EditText easyEditText;
    private EditText mediumEditText;
    private EditText hardEditText;
    private ProgressBar codingStatsFormProgressBar;
    private Button saveCodingStatsButton;
    private Button cancelCodingStatsButton;

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

    /** Id of the coding statistics being edited, or {@code null} when adding. */
    private String codingId;

    // Guards against duplicate save requests
    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();
        codingStatsRepository = new CodingStatsRepository();

        // Navigation guard: this screen must never be shown without a session
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        // Ownership always comes from the authenticated user, never an Intent
        uid = currentUser.getUid();

        // Only the coding id may travel through the Intent
        codingId = getIntent().getStringExtra(EXTRA_CODING_ID);

        setContentView(R.layout.activity_add_edit_coding_stats);

        // Connect XML components with Java
        codingStatsFormTitleTextView = findViewById(R.id.codingStatsFormTitleTextView);
        platformEditText = findViewById(R.id.platformEditText);
        easyEditText = findViewById(R.id.easyEditText);
        mediumEditText = findViewById(R.id.mediumEditText);
        hardEditText = findViewById(R.id.hardEditText);
        codingStatsFormProgressBar = findViewById(R.id.codingStatsFormProgressBar);
        saveCodingStatsButton = findViewById(R.id.saveCodingStatsButton);
        cancelCodingStatsButton = findViewById(R.id.cancelCodingStatsButton);

        saveCodingStatsButton.setOnClickListener(view -> saveCodingStats());
        cancelCodingStatsButton.setOnClickListener(view -> finish());

        if (isEditMode()) {
            codingStatsFormTitleTextView.setText(R.string.coding_stats_edit_title);
            loadCodingStats();
        } else {
            codingStatsFormTitleTextView.setText(R.string.coding_stats_add_title);
        }
    }

    private boolean isEditMode() {
        return codingId != null && !codingId.isEmpty();
    }

    /**
     * Loads the coding statistics being edited and fills the form.
     */
    private void loadCodingStats() {

        setLoadingState(true);

        codingStatsRepository.getCodingStat(uid, codingId,
                codingStats -> {
                    setLoadingState(false);

                    if (codingStats == null) {
                        Toast.makeText(
                                AddEditCodingStatsActivity.this,
                                R.string.err_coding_stats_load_failed,
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    } else {
                        bindCodingStats(codingStats);
                    }
                },
                exception -> {
                    setLoadingState(false);
                    Toast.makeText(
                            AddEditCodingStatsActivity.this,
                            R.string.err_coding_stats_load_failed,
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    private void bindCodingStats(CodingStats codingStats) {

        platformEditText.setText(codingStats.getPlatform());
        easyEditText.setText(String.valueOf(codingStats.getEasy()));
        mediumEditText.setText(String.valueOf(codingStats.getMedium()));
        hardEditText.setText(String.valueOf(codingStats.getHard()));
    }

    private void saveCodingStats() {

        if (isSaving) {
            return;
        }

        if (!isFormValid()) {
            return;
        }

        // isFormValid() guarantees the three counts parse and are >= 0.
        String platform = platformEditText.getText().toString().trim();
        int easy = Integer.parseInt(easyEditText.getText().toString().trim());
        int medium = Integer.parseInt(mediumEditText.getText().toString().trim());
        int hard = Integer.parseInt(hardEditText.getText().toString().trim());

        // The total is always derived inside the model.
        CodingStats codingStats = new CodingStats(codingId, uid, platform, easy, medium, hard);

        setLoadingState(true);

        if (isEditMode()) {
            updateExistingCodingStats(codingStats);
        } else {
            addNewCodingStats(codingStats);
        }
    }

    /**
     * Creates a new coding statistics entry. The repository generates the
     * document id and stores it as {@code codingId}; the authenticated uid is
     * stored as {@code userId}.
     */
    private void addNewCodingStats(CodingStats codingStats) {

        codingStatsRepository.addCodingStats(uid, codingStats, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditCodingStatsActivity.this,
                        R.string.msg_coding_stats_added,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditCodingStatsActivity.this,
                        R.string.err_coding_stats_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Updates the existing coding statistics entry. {@code codingId},
     * {@code userId} and {@code createdAt} are preserved.
     */
    private void updateExistingCodingStats(CodingStats codingStats) {

        codingStatsRepository.updateCodingStats(uid, codingStats, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditCodingStatsActivity.this,
                        R.string.msg_coding_stats_updated,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditCodingStatsActivity.this,
                        R.string.err_coding_stats_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Validates the form. The platform is required, each difficulty count must
     * be a whole number of 0 or more, and the total is never entered by hand.
     */
    private boolean isFormValid() {

        String platform = platformEditText.getText().toString().trim();
        String easy = easyEditText.getText().toString().trim();
        String medium = mediumEditText.getText().toString().trim();
        String hard = hardEditText.getText().toString().trim();

        // Platform
        if (platform.isEmpty()) {
            return showFieldError(platformEditText, R.string.err_coding_platform_required);
        }

        // Easy
        if (easy.isEmpty()) {
            return showFieldError(easyEditText, R.string.err_coding_easy_required);
        }
        if (!isCountValid(easy)) {
            return showFieldError(easyEditText, R.string.err_coding_easy_invalid);
        }

        // Medium
        if (medium.isEmpty()) {
            return showFieldError(mediumEditText, R.string.err_coding_medium_required);
        }
        if (!isCountValid(medium)) {
            return showFieldError(mediumEditText, R.string.err_coding_medium_invalid);
        }

        // Hard
        if (hard.isEmpty()) {
            return showFieldError(hardEditText, R.string.err_coding_hard_required);
        }
        if (!isCountValid(hard)) {
            return showFieldError(hardEditText, R.string.err_coding_hard_invalid);
        }

        return true;
    }

    private boolean showFieldError(EditText field, int messageResId) {
        field.setError(getString(messageResId));
        field.requestFocus();
        return false;
    }

    /**
     * A count is valid when it is a whole number of 0 or more. There is no
     * artificial maximum.
     */
    private boolean isCountValid(String rawCount) {
        try {
            int value = Integer.parseInt(rawCount);
            return value >= 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void setLoadingState(boolean loading) {
        isSaving = loading;
        codingStatsFormProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveCodingStatsButton.setEnabled(!loading);
        cancelCodingStatsButton.setEnabled(!loading);
        platformEditText.setEnabled(!loading);
        easyEditText.setEnabled(!loading);
        mediumEditText.setEnabled(!loading);
        hardEditText.setEnabled(!loading);
    }

    private void goToLogin() {
        Intent intent = new Intent(AddEditCodingStatsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
