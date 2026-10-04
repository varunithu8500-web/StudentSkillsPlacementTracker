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

import com.example.studentskillsplacementtracker.adapter.SkillAdapter;
import com.example.studentskillsplacementtracker.data.SkillRepository;
import com.example.studentskillsplacementtracker.model.Skill;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Skills list screen (FR-SKL-01).
 *
 * Lists the skills of the authenticated student and offers add, edit and
 * delete. Skills always live under the authenticated user's uid; the uid is
 * never taken from an Intent. This class contains no Firestore code of its
 * own - all access goes through {@link SkillRepository}.
 */
public class SkillsActivity extends AppCompatActivity
        implements SkillAdapter.OnSkillActionListener {

    private FirebaseAuth firebaseAuth;
    private SkillRepository skillRepository;

    private RecyclerView skillsRecyclerView;
    private SkillAdapter skillAdapter;
    private ProgressBar skillsProgressBar;
    private TextView skillsMessageTextView;
    private Button addSkillButton;

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();
        skillRepository = new SkillRepository();

        // Navigation guard: this screen must never be shown without a session
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        // Ownership always comes from the authenticated user, never an Intent
        uid = currentUser.getUid();

        setContentView(R.layout.activity_skills);

        // Connect XML components with Java
        skillsRecyclerView = findViewById(R.id.skillsRecyclerView);
        skillsProgressBar = findViewById(R.id.skillsProgressBar);
        skillsMessageTextView = findViewById(R.id.skillsMessageTextView);
        addSkillButton = findViewById(R.id.addSkillButton);

        skillAdapter = new SkillAdapter(this);
        skillsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        skillsRecyclerView.setAdapter(skillAdapter);

        addSkillButton.setOnClickListener(view -> openAddSkillForm());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload every time the screen becomes visible again, e.g. after the
        // add / edit form finishes.
        if (uid != null) {
            loadSkills();
        }
    }

    /**
     * Loads the authenticated student's skills.
     */
    private void loadSkills() {

        showLoadingState();

        skillRepository.listSkills(uid,
                skills -> {

                    skillsProgressBar.setVisibility(View.GONE);
                    skillAdapter.setSkills(skills);

                    if (skills.isEmpty()) {
                        showMessage(R.string.skills_empty);
                    } else {
                        skillsMessageTextView.setVisibility(View.GONE);
                        skillsRecyclerView.setVisibility(View.VISIBLE);
                    }
                },
                exception -> {
                    skillsProgressBar.setVisibility(View.GONE);
                    skillAdapter.setSkills(null);
                    showMessage(R.string.skills_load_error);
                });
    }

    private void showLoadingState() {
        skillsProgressBar.setVisibility(View.VISIBLE);
        skillsRecyclerView.setVisibility(View.GONE);
        skillsMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        skillsRecyclerView.setVisibility(View.GONE);
        skillsMessageTextView.setText(messageResId);
        skillsMessageTextView.setVisibility(View.VISIBLE);
    }

    private void openAddSkillForm() {
        startActivity(new Intent(SkillsActivity.this, AddEditSkillActivity.class));
    }

    @Override
    public void onEditSkill(Skill skill) {

        // Only the skill id travels through the Intent
        Intent intent = new Intent(SkillsActivity.this, AddEditSkillActivity.class);
        intent.putExtra(AddEditSkillActivity.EXTRA_SKILL_ID, skill.getSkillId());
        startActivity(intent);
    }

    @Override
    public void onDeleteSkill(Skill skill) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_skill_title)
                .setMessage(R.string.dialog_delete_skill_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> deleteSkill(skill))
                .show();
    }

    /**
     * Deletes the skill once the user has confirmed, then reloads the list.
     */
    private void deleteSkill(Skill skill) {

        showLoadingState();

        skillRepository.deleteSkill(uid, skill.getSkillId(), task -> {

            if (task.isSuccessful()) {
                Toast.makeText(
                        SkillsActivity.this,
                        R.string.msg_skill_deleted,
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        SkillsActivity.this,
                        R.string.err_skill_delete_failed,
                        Toast.LENGTH_LONG
                ).show();
            }

            loadSkills();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(SkillsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
