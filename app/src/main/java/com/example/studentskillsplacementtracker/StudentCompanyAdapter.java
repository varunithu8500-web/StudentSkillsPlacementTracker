package com.example.studentskillsplacementtracker;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.model.Company;

import java.util.ArrayList;
import java.util.List;

/**
 * Read only adapter used by students to browse company placement requirements.
 *
 * Selecting a row reports the company id back to the hosting Activity, which is
 * the only value forwarded to the eligibility screen. There are deliberately no
 * edit or delete controls here.
 */
public class StudentCompanyAdapter
        extends RecyclerView.Adapter<StudentCompanyAdapter.StudentCompanyViewHolder> {

    /**
     * Reports the selected company to the hosting Activity.
     */
    public interface OnCompanySelectedListener {
        void onCompanySelected(String companyId);
    }

    private final List<Company> companies = new ArrayList<>();
    private final OnCompanySelectedListener listener;

    public StudentCompanyAdapter(OnCompanySelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the displayed companies.
     */
    public void setCompanies(List<Company> updatedCompanies) {

        companies.clear();

        if (updatedCompanies != null) {
            companies.addAll(updatedCompanies);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentCompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_company, parent, false);
        return new StudentCompanyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentCompanyViewHolder holder, int position) {
        holder.bind(companies.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return companies.size();
    }

    /**
     * View holder for one read only company row.
     */
    static class StudentCompanyViewHolder extends RecyclerView.ViewHolder {

        private final TextView studentCompanyNameTextView;
        private final TextView studentCompanyRoleTextView;
        private final TextView studentCompanyMinCgpaTextView;
        private final TextView studentCompanyMinProjectsTextView;
        private final TextView studentCompanyRequiredSkillsTextView;
        private final TextView studentCompanyLocationTextView;

        StudentCompanyViewHolder(@NonNull View itemView) {
            super(itemView);

            studentCompanyNameTextView =
                    itemView.findViewById(R.id.studentCompanyNameTextView);
            studentCompanyRoleTextView =
                    itemView.findViewById(R.id.studentCompanyRoleTextView);
            studentCompanyMinCgpaTextView =
                    itemView.findViewById(R.id.studentCompanyMinCgpaTextView);
            studentCompanyMinProjectsTextView =
                    itemView.findViewById(R.id.studentCompanyMinProjectsTextView);
            studentCompanyRequiredSkillsTextView =
                    itemView.findViewById(R.id.studentCompanyRequiredSkillsTextView);
            studentCompanyLocationTextView =
                    itemView.findViewById(R.id.studentCompanyLocationTextView);
        }

        void bind(Company company, OnCompanySelectedListener listener) {

            studentCompanyNameTextView.setText(safe(company.getCompanyName()));

            studentCompanyRoleTextView.setText(
                    itemView.getContext().getString(
                            R.string.student_company_item_role,
                            safe(company.getRole())
                    )
            );

            studentCompanyMinCgpaTextView.setText(
                    itemView.getContext().getString(
                            R.string.student_company_item_min_cgpa,
                            String.valueOf(company.getMinimumCGPA())
                    )
            );

            studentCompanyMinProjectsTextView.setText(
                    itemView.getContext().getString(
                            R.string.student_company_item_min_projects,
                            company.getMinimumProjects()
                    )
            );

            // Required skills are optional; hide the row when none were configured.
            String requiredSkills = joinRequiredSkills(company.getRequiredSkills());

            if (requiredSkills.isEmpty()) {
                studentCompanyRequiredSkillsTextView.setVisibility(View.GONE);
            } else {
                studentCompanyRequiredSkillsTextView.setText(
                        itemView.getContext().getString(
                                R.string.student_company_item_required_skills,
                                requiredSkills
                        )
                );
                studentCompanyRequiredSkillsTextView.setVisibility(View.VISIBLE);
            }

            // Location is optional; hide the row when it was not supplied.
            String location = safe(company.getLocation());

            if (location.trim().isEmpty()) {
                studentCompanyLocationTextView.setVisibility(View.GONE);
            } else {
                studentCompanyLocationTextView.setText(
                        itemView.getContext().getString(
                                R.string.student_company_item_location,
                                location
                        )
                );
                studentCompanyLocationTextView.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(
                    view -> listener.onCompanySelected(company.getCompanyId()));
        }

        /**
         * Joins the company's configured required skills for display, or returns
         * an empty string when none were supplied.
         */
        private String joinRequiredSkills(List<String> requiredSkills) {

            if (requiredSkills == null || requiredSkills.isEmpty()) {
                return "";
            }

            return TextUtils.join(", ", requiredSkills);
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
