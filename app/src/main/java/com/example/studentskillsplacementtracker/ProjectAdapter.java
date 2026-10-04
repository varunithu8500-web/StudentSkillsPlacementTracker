package com.example.studentskillsplacementtracker;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.model.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds the authenticated student's projects to the projects list.
 *
 * Edit and delete taps are reported back to the hosting Activity through
 * {@link OnProjectActionListener}, so no Firestore code lives in the adapter.
 */
public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    /**
     * Row actions reported to the hosting Activity.
     */
    public interface OnProjectActionListener {
        void onEditProject(Project project);

        void onDeleteProject(Project project);
    }

    private final List<Project> projects = new ArrayList<>();
    private final OnProjectActionListener listener;

    public ProjectAdapter(OnProjectActionListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the displayed projects.
     */
    public void setProjects(List<Project> updatedProjects) {

        projects.clear();

        if (updatedProjects != null) {
            projects.addAll(updatedProjects);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        holder.bind(projects.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    /**
     * View holder for one project row.
     */
    static class ProjectViewHolder extends RecyclerView.ViewHolder {

        private final TextView projectNameTextView;
        private final TextView projectStatusTextView;
        private final TextView projectDescriptionTextView;
        private final TextView projectTechnologiesTextView;
        private final TextView projectRepositoryTextView;
        private final Button editProjectButton;
        private final Button deleteProjectButton;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);

            projectNameTextView = itemView.findViewById(R.id.projectNameTextView);
            projectStatusTextView = itemView.findViewById(R.id.projectStatusTextView);
            projectDescriptionTextView = itemView.findViewById(R.id.projectDescriptionTextView);
            projectTechnologiesTextView = itemView.findViewById(R.id.projectTechnologiesTextView);
            projectRepositoryTextView = itemView.findViewById(R.id.projectRepositoryTextView);
            editProjectButton = itemView.findViewById(R.id.editProjectButton);
            deleteProjectButton = itemView.findViewById(R.id.deleteProjectButton);
        }

        void bind(Project project, OnProjectActionListener listener) {

            projectNameTextView.setText(project.getProjectName());

            projectStatusTextView.setText(
                    itemView.getContext().getString(
                            R.string.project_item_status,
                            safe(project.getStatus())
                    )
            );

            bindOptional(
                    projectDescriptionTextView,
                    R.string.project_item_description,
                    project.getDescription()
            );

            bindOptional(
                    projectTechnologiesTextView,
                    R.string.project_item_technologies,
                    joinTechnologies(project.getTechnologies())
            );

            bindOptional(
                    projectRepositoryTextView,
                    R.string.project_item_repository,
                    project.getRepositoryLink()
            );

            editProjectButton.setOnClickListener(view -> listener.onEditProject(project));
            deleteProjectButton.setOnClickListener(view -> listener.onDeleteProject(project));
        }

        /**
         * Shows {@code label} + {@code value} only when there is a value.
         */
        private void bindOptional(TextView textView, int labelResId, String value) {

            if (value == null || value.trim().isEmpty()) {
                textView.setVisibility(View.GONE);
                return;
            }

            textView.setText(itemView.getContext().getString(labelResId, value));
            textView.setVisibility(View.VISIBLE);
        }

        private String joinTechnologies(List<String> technologies) {

            if (technologies == null || technologies.isEmpty()) {
                return "";
            }

            return TextUtils.join(", ", technologies);
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
