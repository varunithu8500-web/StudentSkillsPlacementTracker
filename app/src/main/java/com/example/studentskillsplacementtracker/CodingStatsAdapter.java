package com.example.studentskillsplacementtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.model.CodingStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds the authenticated student's coding practice statistics to the list.
 *
 * Edit and delete taps are reported back to the hosting Activity through
 * {@link OnCodingStatsActionListener}, so no Firestore code lives in the
 * adapter.
 */
public class CodingStatsAdapter
        extends RecyclerView.Adapter<CodingStatsAdapter.CodingStatsViewHolder> {

    /**
     * Row actions reported to the hosting Activity.
     */
    public interface OnCodingStatsActionListener {
        void onEditCodingStats(CodingStats codingStats);

        void onDeleteCodingStats(CodingStats codingStats);
    }

    private final List<CodingStats> codingStatsList = new ArrayList<>();
    private final OnCodingStatsActionListener listener;

    public CodingStatsAdapter(OnCodingStatsActionListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the displayed coding statistics.
     */
    public void setCodingStats(List<CodingStats> updatedCodingStats) {

        codingStatsList.clear();

        if (updatedCodingStats != null) {
            codingStatsList.addAll(updatedCodingStats);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CodingStatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coding_stats, parent, false);
        return new CodingStatsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CodingStatsViewHolder holder, int position) {
        holder.bind(codingStatsList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return codingStatsList.size();
    }

    /**
     * View holder for one coding statistics row.
     */
    static class CodingStatsViewHolder extends RecyclerView.ViewHolder {

        private final TextView codingPlatformTextView;
        private final TextView codingEasyTextView;
        private final TextView codingMediumTextView;
        private final TextView codingHardTextView;
        private final TextView codingTotalTextView;
        private final Button editCodingStatsButton;
        private final Button deleteCodingStatsButton;

        CodingStatsViewHolder(@NonNull View itemView) {
            super(itemView);

            codingPlatformTextView = itemView.findViewById(R.id.codingPlatformTextView);
            codingEasyTextView = itemView.findViewById(R.id.codingEasyTextView);
            codingMediumTextView = itemView.findViewById(R.id.codingMediumTextView);
            codingHardTextView = itemView.findViewById(R.id.codingHardTextView);
            codingTotalTextView = itemView.findViewById(R.id.codingTotalTextView);
            editCodingStatsButton = itemView.findViewById(R.id.editCodingStatsButton);
            deleteCodingStatsButton = itemView.findViewById(R.id.deleteCodingStatsButton);
        }

        void bind(CodingStats codingStats, OnCodingStatsActionListener listener) {

            codingPlatformTextView.setText(
                    itemView.getContext().getString(
                            R.string.coding_item_platform,
                            safe(codingStats.getPlatform())
                    )
            );

            codingEasyTextView.setText(
                    itemView.getContext().getString(
                            R.string.coding_item_easy,
                            codingStats.getEasy()
                    )
            );

            codingMediumTextView.setText(
                    itemView.getContext().getString(
                            R.string.coding_item_medium,
                            codingStats.getMedium()
                    )
            );

            codingHardTextView.setText(
                    itemView.getContext().getString(
                            R.string.coding_item_hard,
                            codingStats.getHard()
                    )
            );

            codingTotalTextView.setText(
                    itemView.getContext().getString(
                            R.string.coding_item_total,
                            codingStats.getTotal()
                    )
            );

            editCodingStatsButton.setOnClickListener(
                    view -> listener.onEditCodingStats(codingStats));
            deleteCodingStatsButton.setOnClickListener(
                    view -> listener.onDeleteCodingStats(codingStats));
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
