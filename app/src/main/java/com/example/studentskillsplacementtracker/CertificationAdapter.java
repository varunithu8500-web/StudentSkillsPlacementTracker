package com.example.studentskillsplacementtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.model.Certification;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds the authenticated student's certifications to the certifications list.
 *
 * Edit and delete taps are reported back to the hosting Activity through
 * {@link OnCertificationActionListener}, so no Firestore code lives in the
 * adapter.
 */
public class CertificationAdapter
        extends RecyclerView.Adapter<CertificationAdapter.CertificationViewHolder> {

    /**
     * Row actions reported to the hosting Activity.
     */
    public interface OnCertificationActionListener {
        void onEditCertification(Certification certification);

        void onDeleteCertification(Certification certification);
    }

    private final List<Certification> certifications = new ArrayList<>();
    private final OnCertificationActionListener listener;

    public CertificationAdapter(OnCertificationActionListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the displayed certifications.
     */
    public void setCertifications(List<Certification> updatedCertifications) {

        certifications.clear();

        if (updatedCertifications != null) {
            certifications.addAll(updatedCertifications);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CertificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_certification, parent, false);
        return new CertificationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CertificationViewHolder holder, int position) {
        holder.bind(certifications.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return certifications.size();
    }

    /**
     * View holder for one certification row.
     */
    static class CertificationViewHolder extends RecyclerView.ViewHolder {

        private final TextView certificationNameTextView;
        private final TextView certificationOrganizationTextView;
        private final TextView certificationDateTextView;
        private final TextView certificationCredentialTextView;
        private final Button editCertificationButton;
        private final Button deleteCertificationButton;

        CertificationViewHolder(@NonNull View itemView) {
            super(itemView);

            certificationNameTextView = itemView.findViewById(R.id.certificationNameTextView);
            certificationOrganizationTextView =
                    itemView.findViewById(R.id.certificationOrganizationTextView);
            certificationDateTextView = itemView.findViewById(R.id.certificationDateTextView);
            certificationCredentialTextView =
                    itemView.findViewById(R.id.certificationCredentialTextView);
            editCertificationButton = itemView.findViewById(R.id.editCertificationButton);
            deleteCertificationButton = itemView.findViewById(R.id.deleteCertificationButton);
        }

        void bind(Certification certification, OnCertificationActionListener listener) {

            certificationNameTextView.setText(safe(certification.getCertificationName()));

            certificationOrganizationTextView.setText(
                    itemView.getContext().getString(
                            R.string.certification_item_organization,
                            safe(certification.getOrganization())
                    )
            );

            certificationDateTextView.setText(
                    itemView.getContext().getString(
                            R.string.certification_item_date,
                            Certification.formatTimestamp(certification.getDate())
                    )
            );

            // The credential is optional; hide the row when it was not supplied.
            String credential = safe(certification.getCredential());

            if (credential.trim().isEmpty()) {
                certificationCredentialTextView.setVisibility(View.GONE);
            } else {
                certificationCredentialTextView.setText(
                        itemView.getContext().getString(
                                R.string.certification_item_credential,
                                credential
                        )
                );
                certificationCredentialTextView.setVisibility(View.VISIBLE);
            }

            editCertificationButton.setOnClickListener(
                    view -> listener.onEditCertification(certification));
            deleteCertificationButton.setOnClickListener(
                    view -> listener.onDeleteCertification(certification));
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
