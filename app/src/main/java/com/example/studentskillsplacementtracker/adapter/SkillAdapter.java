package com.example.studentskillsplacementtracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.R;
import com.example.studentskillsplacementtracker.model.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds the authenticated student's skills to the skills list.
 *
 * Edit and delete taps are reported back to the hosting Activity through
 * {@link OnSkillActionListener}, so no Firestore code lives in the adapter.
 */
public class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {

    /**
     * Row actions reported to the hosting Activity.
     */
    public interface OnSkillActionListener {
        void onEditSkill(Skill skill);

        void onDeleteSkill(Skill skill);
    }

    private final List<Skill> skills = new ArrayList<>();
    private final OnSkillActionListener listener;

    public SkillAdapter(OnSkillActionListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the displayed skills.
     */
    public void setSkills(List<Skill> updatedSkills) {

        skills.clear();

        if (updatedSkills != null) {
            skills.addAll(updatedSkills);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SkillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_skill, parent, false);
        return new SkillViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SkillViewHolder holder, int position) {
        holder.bind(skills.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return skills.size();
    }

    /**
     * View holder for one skill row.
     */
    static class SkillViewHolder extends RecyclerView.ViewHolder {

        private final TextView skillNameTextView;
        private final TextView skillProficiencyTextView;
        private final TextView skillProgressTextView;
        private final Button editSkillButton;
        private final Button deleteSkillButton;

        SkillViewHolder(@NonNull View itemView) {
            super(itemView);

            skillNameTextView = itemView.findViewById(R.id.skillNameTextView);
            skillProficiencyTextView = itemView.findViewById(R.id.skillProficiencyTextView);
            skillProgressTextView = itemView.findViewById(R.id.skillProgressTextView);
            editSkillButton = itemView.findViewById(R.id.editSkillButton);
            deleteSkillButton = itemView.findViewById(R.id.deleteSkillButton);
        }

        void bind(Skill skill, OnSkillActionListener listener) {

            skillNameTextView.setText(skill.getSkillName());

            skillProficiencyTextView.setText(
                    itemView.getContext().getString(
                            R.string.skill_item_proficiency,
                            skill.getProficiency()
                    )
            );

            skillProgressTextView.setText(
                    itemView.getContext().getString(
                            R.string.skill_item_progress,
                            skill.getProgress()
                    )
            );

            editSkillButton.setOnClickListener(view -> listener.onEditSkill(skill));
            deleteSkillButton.setOnClickListener(view -> listener.onDeleteSkill(skill));
        }
    }
}
