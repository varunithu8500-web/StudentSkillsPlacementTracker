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

import com.example.studentskillsplacementtracker.data.SkillRepository;
import com.example.studentskillsplacementtracker.model.Skill;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Add / edit form for a single skill (FR-SKL-01).
 *
 * The form works in "edit" mode when a skill id is supplied as an Intent extra.
 * Only the skill id is ever read from the Intent: the owning uid always comes
 * from the authenticated Firebase user, never from the UI layer.
 */
public class AddEditSkillActivity extends AppCompatActivity {

    /** Extra carrying the id of the skill being edited. */
    public static final String EXTRA_SKILL_ID = "extra_skill_id";

    private FirebaseAuth firebaseAuth;
    private SkillRepository skillRepository;

    private TextView skillFormTitleTextView;
    private EditText skillNameEditText;
    private EditText proficiencyEditText;
    private EditText progressEditText;
    private ProgressBar skillFormProgressBar;
    private Button saveSkillButton;
    private Button cancelSkillButton;

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

    /** Id of the skill being edited, or {@code null} when adding. */
    private String skillId;

    // Guards against duplicate save requests
    private boolean isSaving = false;

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

        // Only the skill id may travel through the Intent
        skillId = getIntent().getStringExtra(EXTRA_SKILL_ID);

        setContentView(R.layout.activity_add_edit_skill);

        // Connect XML components with Java
        skillFormTitleTextView = findViewById(R.id.skillFormTitleTextView);
        skillNameEditText = findViewById(R.id.skillNameEditText);
        proficiencyEditText = findViewById(R.id.proficiencyEditText);
        progressEditText = findViewById(R.id.progressEditText);
        skillFormProgressBar = findViewById(R.id.skillFormProgressBar);
        saveSkillButton = findViewById(R.id.saveSkillButton);
        cancelSkillButton = findViewById(R.id.cancelSkillButton);

        saveSkillButton.setOnClickListener(view -> saveSkill());
        cancelSkillButton.setOnClickListener(view -> finish());

        if (isEditMode()) {
            skillFormTitleTextView.setText(R.string.skill_edit_title);
            loadSkill();
        } else {
            skillFormTitleTextView.setText(R.string.skill_add_title);
        }
    }

    private boolean isEditMode() {
        return skillId != null && !skillId.isEmpty();
    }

    /**
     * Loads the skill being edited and fills the form.
     */
    private void loadSkill() {

        setLoadingState(true);

        skillRepository.getSkill(uid, skillId,
                skill -> {
                    setLoadingState(false);

                    if (skill == null) {
                        Toast.makeText(
                                AddEditSkillActivity.this,
                                R.string.err_skill_load_failed,
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    } else {
                        bindSkill(skill);
                    }
                },
                exception -> {
                    setLoadingState(false);
                    Toast.makeText(
                            AddEditSkillActivity.this,
                            R.string.err_skill_load_failed,
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    private void bindSkill(Skill skill) {
        skillNameEditText.setText(skill.getSkillName());
        proficiencyEditText.setText(String.valueOf(skill.getProficiency()));
        progressEditText.setText(String.valueOf(skill.getProgress()));
    }

    private void saveSkill() {

        if (isSaving) {
            return;
        }

        if (!isFormValid()) {
            return;
        }

        // The skill name is trimmed; the numeric fields were validated above.
        String skillName = skillNameEditText.getText().toString().trim();
        int proficiency = Integer.parseInt(proficiencyEditText.getText().toString().trim());
        int progress = Integer.parseInt(progressEditText.getText().toString().trim());

        Skill skill = new Skill(skillId, uid, skillName, proficiency, progress);

        setLoadingState(true);

        if (isEditMode()) {
            updateExistingSkill(skill);
        } else {
            addNewSkill(skill);
        }
    }

    /**
     * Creates a new skill. The repository generates the document id and stores
     * it as {@code skillId}; the authenticated uid is stored as {@code userId}.
     */
    private void addNewSkill(Skill skill) {

        skillRepository.addSkill(uid, skill, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditSkillActivity.this,
                        R.string.msg_skill_added,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditSkillActivity.this,
                        R.string.err_skill_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Updates the existing skill. {@code skillId}, {@code userId} and
     * {@code createdAt} are preserved.
     */
    private void updateExistingSkill(Skill skill) {

        skillRepository.updateSkill(uid, skill, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditSkillActivity.this,
                        R.string.msg_skill_updated,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditSkillActivity.this,
                        R.string.err_skill_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private boolean isFormValid() {

        String skillName = skillNameEditText.getText().toString().trim();
        String proficiency = proficiencyEditText.getText().toString().trim();
        String progress = progressEditText.getText().toString().trim();

        // Skill name
        if (skillName.isEmpty()) {
            return showFieldError(skillNameEditText, R.string.err_skill_name_required);
        }

        // Proficiency
        if (proficiency.isEmpty()) {
            return showFieldError(proficiencyEditText, R.string.err_skill_proficiency_required);
        }
        if (!isProficiencyValid(proficiency)) {
            return showFieldError(proficiencyEditText, R.string.err_skill_proficiency_invalid);
        }

        // Progress
        if (progress.isEmpty()) {
            return showFieldError(progressEditText, R.string.err_skill_progress_required);
        }
        if (!isProgressValid(progress)) {
            return showFieldError(progressEditText, R.string.err_skill_progress_invalid);
        }

        return true;
    }

    private boolean showFieldError(EditText field, int messageResId) {
        field.setError(getString(messageResId));
        field.requestFocus();
        return false;
    }

    private boolean isProficiencyValid(String proficiency) {
        try {
            int value = Integer.parseInt(proficiency);
            return value >= Skill.PROFICIENCY_MIN && value <= Skill.PROFICIENCY_MAX;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isProgressValid(String progress) {
        try {
            int value = Integer.parseInt(progress);
            return value >= Skill.PROGRESS_MIN && value <= Skill.PROGRESS_MAX;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void setLoadingState(boolean loading) {
        isSaving = loading;
        skillFormProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveSkillButton.setEnabled(!loading);
        cancelSkillButton.setEnabled(!loading);
        skillNameEditText.setEnabled(!loading);
        proficiencyEditText.setEnabled(!loading);
        progressEditText.setEnabled(!loading);
    }

    private void goToLogin() {
        Intent intent = new Intent(AddEditSkillActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
