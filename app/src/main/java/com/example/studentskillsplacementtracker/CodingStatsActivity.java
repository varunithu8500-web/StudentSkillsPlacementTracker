package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.data.CodingStatsRepository;
import com.example.studentskillsplacementtracker.model.CodingStats;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Coding practice statistics list screen (FR-COD-01).
 *
 * Lists the self reported coding statistics of the authenticated student and
 * offers add, edit and delete. Statistics always live under the authenticated
 * user's uid; the uid is never taken from an Intent. This class contains no
 * Firestore code of its own - all access goes through
 * {@link CodingStatsRepository}.
 */
public class CodingStatsActivity extends AppCompatActivity
        implements CodingStatsAdapter.OnCodingStatsActionListener {

    private FirebaseAuth firebaseAuth;
    private CodingStatsRepository codingStatsRepository;

    private RecyclerView codingStatsRecyclerView;
    private CodingStatsAdapter codingStatsAdapter;
    private ProgressBar codingStatsProgressBar;
    private TextView codingStatsMessageTextView;
    private Button addCodingStatsButton;

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

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

        setContentView(R.layout.activity_coding_stats);

        // Connect XML components with Java
        codingStatsRecyclerView = findViewById(R.id.codingStatsRecyclerView);
        codingStatsProgressBar = findViewById(R.id.codingStatsProgressBar);
        codingStatsMessageTextView = findViewById(R.id.codingStatsMessageTextView);
        addCodingStatsButton = findViewById(R.id.addCodingStatsButton);

        codingStatsAdapter = new CodingStatsAdapter(this);
        codingStatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        codingStatsRecyclerView.setAdapter(codingStatsAdapter);

        addCodingStatsButton.setOnClickListener(view -> openAddCodingStatsForm());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload every time the screen becomes visible again, e.g. after the
        // add / edit form finishes.
        if (uid != null) {
            loadCodingStats();
        }
    }

    /**
     * Loads the authenticated student's coding statistics.
     */
    private void loadCodingStats() {

        showLoadingState();

        codingStatsRepository.getCodingStats(uid,
                codingStatsList -> {

                    codingStatsProgressBar.setVisibility(View.GONE);
                    codingStatsAdapter.setCodingStats(codingStatsList);

                    if (codingStatsList.isEmpty()) {
                        showMessage(R.string.coding_stats_empty);
                    } else {
                        codingStatsMessageTextView.setVisibility(View.GONE);
                        codingStatsRecyclerView.setVisibility(View.VISIBLE);
                    }
                },
                exception -> {
                    codingStatsProgressBar.setVisibility(View.GONE);
                    codingStatsAdapter.setCodingStats(null);
                    showMessage(R.string.coding_stats_load_error);
                });
    }

    private void showLoadingState() {
        codingStatsProgressBar.setVisibility(View.VISIBLE);
        codingStatsRecyclerView.setVisibility(View.GONE);
        codingStatsMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        codingStatsRecyclerView.setVisibility(View.GONE);
        codingStatsMessageTextView.setText(messageResId);
        codingStatsMessageTextView.setVisibility(View.VISIBLE);
    }

    private void openAddCodingStatsForm() {
        startActivity(new Intent(CodingStatsActivity.this, AddEditCodingStatsActivity.class));
    }

    @Override
    public void onEditCodingStats(CodingStats codingStats) {

        // Only the coding id travels through the Intent
        Intent intent = new Intent(CodingStatsActivity.this, AddEditCodingStatsActivity.class);
        intent.putExtra(
                AddEditCodingStatsActivity.EXTRA_CODING_ID,
                codingStats.getCodingId()
        );
        startActivity(intent);
    }

    @Override
    public void onDeleteCodingStats(CodingStats codingStats) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_coding_stats_title)
                .setMessage(R.string.dialog_delete_coding_stats_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(
                        R.string.btn_delete,
                        (dialog, which) -> deleteCodingStats(codingStats))
                .show();
    }

    /**
     * Deletes the coding statistics document once the user has confirmed, then
     * reloads the list.
     */
    private void deleteCodingStats(CodingStats codingStats) {

        showLoadingState();

        codingStatsRepository.deleteCodingStats(
                uid,
                codingStats.getCodingId(),
                task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                CodingStatsActivity.this,
                                R.string.msg_coding_stats_deleted,
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(
                                CodingStatsActivity.this,
                                R.string.err_coding_stats_delete_failed,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    loadCodingStats();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(CodingStatsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
