package com.example.studentskillsplacementtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.model.Announcement;

import java.util.ArrayList;
import java.util.List;

/**
 * Read only adapter used by students to view placement announcements
 * (FR-ANN-01).
 *
 * There are deliberately no edit or delete controls and no click handling: the
 * row already shows the full announcement description, so there is no details
 * screen to open.
 */
public class StudentAnnouncementAdapter
        extends RecyclerView.Adapter<StudentAnnouncementAdapter.StudentAnnouncementViewHolder> {

    private final List<Announcement> announcements = new ArrayList<>();

    /**
     * Replaces the displayed announcements.
     */
    public void setAnnouncements(List<Announcement> updatedAnnouncements) {

        announcements.clear();

        if (updatedAnnouncements != null) {
            announcements.addAll(updatedAnnouncements);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentAnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                            int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_announcement, parent, false);
        return new StudentAnnouncementViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentAnnouncementViewHolder holder, int position) {
        holder.bind(announcements.get(position));
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    /**
     * View holder for one read only announcement row.
     */
    static class StudentAnnouncementViewHolder extends RecyclerView.ViewHolder {

        private final TextView studentAnnouncementTitleTextView;
        private final TextView studentAnnouncementCompanyTextView;
        private final TextView studentAnnouncementDateTextView;
        private final TextView studentAnnouncementDescriptionTextView;

        StudentAnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);

            studentAnnouncementTitleTextView =
                    itemView.findViewById(R.id.studentAnnouncementTitleTextView);
            studentAnnouncementCompanyTextView =
                    itemView.findViewById(R.id.studentAnnouncementCompanyTextView);
            studentAnnouncementDateTextView =
                    itemView.findViewById(R.id.studentAnnouncementDateTextView);
            studentAnnouncementDescriptionTextView =
                    itemView.findViewById(R.id.studentAnnouncementDescriptionTextView);
        }

        void bind(Announcement announcement) {

            studentAnnouncementTitleTextView.setText(safe(announcement.getTitle()));

            studentAnnouncementCompanyTextView.setText(
                    itemView.getContext().getString(
                            R.string.student_announcement_item_company,
                            safe(announcement.getCompanyName())
                    )
            );

            studentAnnouncementDateTextView.setText(
                    itemView.getContext().getString(
                            R.string.student_announcement_item_date,
                            Announcement.formatTimestamp(announcement.getDate())
                    )
            );

            studentAnnouncementDescriptionTextView.setText(
                    itemView.getContext().getString(
                            R.string.student_announcement_item_description,
                            safe(announcement.getDescription())
                    )
            );
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
