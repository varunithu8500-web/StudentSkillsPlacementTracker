package com.example.studentskillsplacementtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.model.Announcement;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds placement announcements to the coordinator announcements list
 * (FR-ADM-01).
 *
 * Edit and delete taps are reported back to the hosting Activity through
 * {@link OnAnnouncementActionListener}, so no Firestore code lives in the
 * adapter.
 */
public class AnnouncementAdapter
        extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    /**
     * Row actions reported to the hosting Activity.
     */
    public interface OnAnnouncementActionListener {
        void onEditAnnouncement(Announcement announcement);

        void onDeleteAnnouncement(Announcement announcement);
    }

    private final List<Announcement> announcements = new ArrayList<>();
    private final OnAnnouncementActionListener listener;

    public AnnouncementAdapter(OnAnnouncementActionListener listener) {
        this.listener = listener;
    }

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
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement, parent, false);
        return new AnnouncementViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        holder.bind(announcements.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    /**
     * View holder for one announcement row.
     */
    static class AnnouncementViewHolder extends RecyclerView.ViewHolder {

        private final TextView announcementTitleTextView;
        private final TextView announcementCompanyTextView;
        private final TextView announcementDateTextView;
        private final TextView announcementDescriptionTextView;
        private final Button editAnnouncementButton;
        private final Button deleteAnnouncementButton;

        AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);

            announcementTitleTextView = itemView.findViewById(R.id.announcementTitleTextView);
            announcementCompanyTextView = itemView.findViewById(R.id.announcementCompanyTextView);
            announcementDateTextView = itemView.findViewById(R.id.announcementDateTextView);
            announcementDescriptionTextView =
                    itemView.findViewById(R.id.announcementDescriptionTextView);
            editAnnouncementButton = itemView.findViewById(R.id.editAnnouncementButton);
            deleteAnnouncementButton = itemView.findViewById(R.id.deleteAnnouncementButton);
        }

        void bind(Announcement announcement, OnAnnouncementActionListener listener) {

            announcementTitleTextView.setText(safe(announcement.getTitle()));

            announcementCompanyTextView.setText(
                    itemView.getContext().getString(
                            R.string.announcement_item_company,
                            safe(announcement.getCompanyName())
                    )
            );

            announcementDateTextView.setText(
                    itemView.getContext().getString(
                            R.string.announcement_item_date,
                            Announcement.formatTimestamp(announcement.getDate())
                    )
            );

            announcementDescriptionTextView.setText(
                    itemView.getContext().getString(
                            R.string.announcement_item_description,
                            safe(announcement.getDescription())
                    )
            );

            editAnnouncementButton.setOnClickListener(
                    view -> listener.onEditAnnouncement(announcement));
            deleteAnnouncementButton.setOnClickListener(
                    view -> listener.onDeleteAnnouncement(announcement));
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
