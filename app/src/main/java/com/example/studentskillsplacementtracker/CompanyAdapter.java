package com.example.studentskillsplacementtracker;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.model.Company;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds company placement requirements to the companies list.
 *
 * Edit and delete taps are reported back to the hosting Activity through
 * {@link OnCompanyActionListener}, so no Firestore code lives in the adapter.
 */
public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.CompanyViewHolder> {

    /**
     * Row actions reported to the hosting Activity.
     */
    public interface OnCompanyActionListener {
        void onEditCompany(Company company);

        void onDeleteCompany(Company company);
    }

    private final List<Company> companies = new ArrayList<>();
    private final OnCompanyActionListener listener;

    public CompanyAdapter(OnCompanyActionListener listener) {
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
    public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company, parent, false);
        return new CompanyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CompanyViewHolder holder, int position) {
        holder.bind(companies.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return companies.size();
    }

    /**
     * View holder for one company row.
     */
    static class CompanyViewHolder extends RecyclerView.ViewHolder {

        private final TextView companyNameTextView;
        private final TextView companyRoleTextView;
        private final TextView companyMinCgpaTextView;
        private final TextView companyMinProjectsTextView;
        private final TextView companyRequiredSkillsTextView;
        private final TextView companyLocationTextView;
        private final Button editCompanyButton;
        private final Button deleteCompanyButton;

        CompanyViewHolder(@NonNull View itemView) {
            super(itemView);

            companyNameTextView = itemView.findViewById(R.id.companyNameTextView);
            companyRoleTextView = itemView.findViewById(R.id.companyRoleTextView);
            companyMinCgpaTextView = itemView.findViewById(R.id.companyMinCgpaTextView);
            companyMinProjectsTextView = itemView.findViewById(R.id.companyMinProjectsTextView);
            companyRequiredSkillsTextView =
                    itemView.findViewById(R.id.companyRequiredSkillsTextView);
            companyLocationTextView = itemView.findViewById(R.id.companyLocationTextView);
            editCompanyButton = itemView.findViewById(R.id.editCompanyButton);
            deleteCompanyButton = itemView.findViewById(R.id.deleteCompanyButton);
        }

        void bind(Company company, OnCompanyActionListener listener) {

            companyNameTextView.setText(safe(company.getCompanyName()));

            companyRoleTextView.setText(
                    itemView.getContext().getString(
                            R.string.company_item_role,
                            safe(company.getRole())
                    )
            );

            companyMinCgpaTextView.setText(
                    itemView.getContext().getString(
                            R.string.company_item_min_cgpa,
                            String.valueOf(company.getMinimumCGPA())
                    )
            );

            companyMinProjectsTextView.setText(
                    itemView.getContext().getString(
                            R.string.company_item_min_projects,
                            company.getMinimumProjects()
                    )
            );

            // Required skills are optional; hide the row when none were configured.
            String requiredSkills = joinRequiredSkills(company.getRequiredSkills());

            if (requiredSkills.isEmpty()) {
                companyRequiredSkillsTextView.setVisibility(View.GONE);
            } else {
                companyRequiredSkillsTextView.setText(
                        itemView.getContext().getString(
                                R.string.company_item_required_skills,
                                requiredSkills
                        )
                );
                companyRequiredSkillsTextView.setVisibility(View.VISIBLE);
            }

            // Location is optional; hide the row when it was not supplied.
            String location = safe(company.getLocation());

            if (location.trim().isEmpty()) {
                companyLocationTextView.setVisibility(View.GONE);
            } else {
                companyLocationTextView.setText(
                        itemView.getContext().getString(
                                R.string.company_item_location,
                                location
                        )
                );
                companyLocationTextView.setVisibility(View.VISIBLE);
            }

            editCompanyButton.setOnClickListener(view -> listener.onEditCompany(company));
            deleteCompanyButton.setOnClickListener(view -> onDelete(company, listener));
        }

        private void onDelete(Company company, OnCompanyActionListener listener) {
            listener.onDeleteCompany(company);
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
