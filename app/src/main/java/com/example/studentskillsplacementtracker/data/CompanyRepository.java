package com.example.studentskillsplacementtracker.data;

import com.example.studentskillsplacementtracker.model.Company;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single point of access to Cloud Firestore for company placement requirements.
 *
 * Companies live in the top level {@code companies} collection because they are
 * shared placement data managed by coordinators, not user owned documents. This
 * is the only repository that is not scoped to a student uid. It contains no UI
 * code.
 */
public class CompanyRepository {

    public static final String COLLECTION_COMPANIES = "companies";

    private static final String FIELD_COMPANY_ID = "companyId";
    private static final String FIELD_COMPANY_NAME = "companyName";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_MINIMUM_CGPA = "minimumCGPA";
    private static final String FIELD_REQUIRED_SKILLS = "requiredSkills";
    private static final String FIELD_MINIMUM_PROJECTS = "minimumProjects";
    private static final String FIELD_LOCATION = "location";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final CollectionReference companiesCollection;

    public CompanyRepository() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        companiesCollection = firestore.collection(COLLECTION_COMPANIES);
    }

    /**
     * Loads every company, newest created first. {@code onSuccess} always
     * receives a list (possibly empty).
     */
    public void getCompanies(OnSuccessListener<List<Company>> onSuccess,
                             OnFailureListener onFailure) {

        companiesCollection
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Company> companies = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Company company = document.toObject(Company.class);
                        if (company != null) {
                            companies.add(company);
                        }
                    }

                    onSuccess.onSuccess(companies);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Loads a single company. {@code onSuccess} receives {@code null} when no
     * document exists for {@code companyId}.
     */
    public void getCompany(String companyId,
                           OnSuccessListener<Company> onSuccess,
                           OnFailureListener onFailure) {

        companiesCollection.document(companyId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        onSuccess.onSuccess(snapshot.toObject(Company.class));
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Creates a new company document. A document reference is generated up front
     * so its id can be stored as {@code companyId} inside the document, and the
     * server sets both timestamps.
     */
    public void addCompany(Company company, OnCompleteListener<Void> listener) {

        DocumentReference document = companiesCollection.document();
        String companyId = document.getId();

        company.setCompanyId(companyId);

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_COMPANY_ID, companyId);
        data.put(FIELD_COMPANY_NAME, company.getCompanyName());
        data.put(FIELD_ROLE, company.getRole());
        data.put(FIELD_MINIMUM_CGPA, company.getMinimumCGPA());
        data.put(FIELD_REQUIRED_SKILLS, company.getRequiredSkills());
        data.put(FIELD_MINIMUM_PROJECTS, company.getMinimumProjects());
        data.put(FIELD_LOCATION, company.getLocation());
        data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        document.set(data).addOnCompleteListener(listener);
    }

    /**
     * Updates the editable fields of an existing company. {@code companyId} and
     * {@code createdAt} are preserved and the server refreshes
     * {@code updatedAt}.
     */
    public void updateCompany(Company company, OnCompleteListener<Void> listener) {

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_COMPANY_ID, company.getCompanyId());
        data.put(FIELD_COMPANY_NAME, company.getCompanyName());
        data.put(FIELD_ROLE, company.getRole());
        data.put(FIELD_MINIMUM_CGPA, company.getMinimumCGPA());
        data.put(FIELD_REQUIRED_SKILLS, company.getRequiredSkills());
        data.put(FIELD_MINIMUM_PROJECTS, company.getMinimumProjects());
        data.put(FIELD_LOCATION, company.getLocation());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        companiesCollection.document(company.getCompanyId())
                .update(data)
                .addOnCompleteListener(listener);
    }

    /**
     * Deletes the company document identified by {@code companyId}.
     */
    public void deleteCompany(String companyId, OnCompleteListener<Void> listener) {

        companiesCollection.document(companyId)
                .delete()
                .addOnCompleteListener(listener);
    }
}
