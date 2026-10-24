package com.project.ads.activities.worker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.project.ads.activities.adapters.DocumentAdapter;
import com.project.ads.helpers.DocumentOrderHelper;
import com.project.ads.helpers.FileHashHelper;
import com.project.ads.models.WorkerDocument;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.WorkerDocumentProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DocumentStatusActivity extends AppCompatActivity {

    private static final String TAG = "DocumentStatusActivity";

    // UI Components
    private RecyclerView recyclerDocuments;
    private ProgressBar progressBar;
    private TextView tvVerificationStatus;
    private TextView tvVerificationMessage;
    private ImageView ivStatusIcon;
    private CardView cardVerificationStatus;
    private LinearLayout layoutEmptyState;
    private View fabAddDocument;
    private ProgressBar uploadProgressBar;
    private TextView tvUploadProgress;
    private CardView cardUploadProgress;
    private TextView tvDocumentsTitle;

    // Providers
    private AuthProvider mAuthProvider;
    private WorkerDocumentProvider mDocumentProvider;

    // Data
    private String workerId;
    private DocumentAdapter documentsAdapter;

    // Upload state
    private boolean isUploading = false;
    private int uploadedCount = 0;
    private int totalDocuments = 0;
    private String selectedDocumentType;
    private String selectedCategory;
    private String selectedSubcategory;
    private String currentDocumentId;

    // File URIs
    private Uri selectedFileUri;
    private String selectedFileName;
    private String selectedFileHash;
    private String selectedOriginalName;

    // Documentos cargados, para numerar y no repetir archivos (DEF-23)
    private final List<WorkerDocument> currentDocuments = new ArrayList<>();

    // File picker launcher
    private ActivityResultLauncher<String> documentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_status);

        initProviders();
        getIntentData();
        initViews();
        setupRecyclerView();
        setupClickListeners();
        initializeFilePicker();
        loadDocuments();
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mDocumentProvider = new WorkerDocumentProvider();
    }

    private void getIntentData() {
        if (getIntent() != null) {
            workerId = getIntent().getStringExtra("worker_id");
        }

        if (workerId == null && mAuthProvider.getId() != null) {
            workerId = mAuthProvider.getId();
        }
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        recyclerDocuments = findViewById(R.id.recyclerDocuments);
        progressBar = findViewById(R.id.progressBar);
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvVerificationMessage = findViewById(R.id.tvVerificationMessage);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        cardVerificationStatus = findViewById(R.id.cardVerificationStatus);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        fabAddDocument = findViewById(R.id.fabAddDocument);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        tvUploadProgress = findViewById(R.id.tvUploadProgress);
        cardUploadProgress = findViewById(R.id.cardUploadProgress);
        tvDocumentsTitle = findViewById(R.id.tvDocumentsTitle);
    }

    private void setupRecyclerView() {
        documentsAdapter = new DocumentAdapter(this, new DocumentAdapter.OnDocumentActionListener() {
            @Override
            public void onViewDocument(WorkerDocument document) {
                openDocument(document);
            }

            @Override
            public void onUpdateDocument(WorkerDocument document) {
                showUpdateDocumentDialog(document);
            }
        });
        recyclerDocuments.setLayoutManager(new LinearLayoutManager(this));
        recyclerDocuments.setAdapter(documentsAdapter);
    }

    private void setupClickListeners() {
        if (fabAddDocument != null) {
            fabAddDocument.setOnClickListener(v -> showDocumentTypeDialog());
        }
    }

    private void initializeFilePicker() {
        documentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedFileHash = FileHashHelper.sha256(getContentResolver(), uri);
                        selectedOriginalName = FileHashHelper.displayName(getContentResolver(), uri);
                        if (DocumentOrderHelper.findSameFile(currentDocuments, selectedFileHash) != null) {
                            showError(getString(R.string.mydocs_same_file));
                            return;
                        }
                        if (DocumentOrderHelper.findSameName(currentDocuments, selectedOriginalName) != null) {
                            showError(getString(R.string.mydocs_same_name, selectedOriginalName));
                            return;
                        }
                        selectedFileUri = uri;
                        selectedFileName = generateFileName();
                        showUploadConfirmationDialog();
                    }
                }
        );
    }

    private void loadDocuments() {
        if (workerId == null) {
            showError("Error: ID de trabajador no disponible");
            return;
        }

        showLoading(true);

        mDocumentProvider.getWorkerDocumentsReference(workerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<WorkerDocument> documentsList = new ArrayList<>();

                        if (snapshot.exists()) {
                            for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                                String category = categorySnapshot.getKey();

                                // Documentos directos (hojaDeVida, antecedentesJudiciales)
                                if ("hojaDeVida".equals(category) || "antecedentesJudiciales".equals(category)) {
                                    WorkerDocument doc = categorySnapshot.getValue(WorkerDocument.class);
                                    if (doc != null) {
                                        doc.setCategory(category);
                                        documentsList.add(doc);
                                    }
                                }
                                // Certificaciones con subcategorías
                                else if ("certificaciones".equals(category)) {
                                    for (DataSnapshot subcategorySnapshot : categorySnapshot.getChildren()) {
                                        String subcategory = subcategorySnapshot.getKey();

                                        for (DataSnapshot docSnapshot : subcategorySnapshot.getChildren()) {
                                            WorkerDocument doc = docSnapshot.getValue(WorkerDocument.class);
                                            if (doc != null) {
                                                doc.setCategory(category);
                                                doc.setSubcategory(subcategory);
                                                documentsList.add(doc);
                                            }
                                        }
                                    }
                                }
                            }

                            DocumentOrderHelper.renumber(documentsList);

                            // Ordenar documentos por fecha de subida (más recientes primero)
                            Collections.sort(documentsList, (d1, d2) -> {
                                Long t1 = d1.getUploadedAt() != 0 ? d1.getUploadedAt() : 0L;
                                Long t2 = d2.getUploadedAt() != 0 ? d2.getUploadedAt() : 0L;
                                return t2.compareTo(t1);
                            });
                        }

                        currentDocuments.clear();
                        currentDocuments.addAll(documentsList);
                        updateVerificationStatusCard(documentsList);
                        updateUI(documentsList);
                        showLoading(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al cargar documentos", error.toException());
                        showError("Error al cargar documentos");
                        showLoading(false);
                    }
                });
    }

    private void updateVerificationStatusCard(List<WorkerDocument> documentsList) {
        int approved = 0;
        int pending = 0;
        int rejected = 0;

        for (WorkerDocument doc : documentsList) {
            if (doc.isApproved()) {
                approved++;
            } else if (doc.isPending()) {
                pending++;
            } else if (doc.isRejected()) {
                rejected++;
            }
        }

        if (documentsList.isEmpty()) {
            cardVerificationStatus.setVisibility(View.GONE);
            return;
        }

        cardVerificationStatus.setVisibility(View.VISIBLE);

        if (rejected > 0) {
            ivStatusIcon.setImageResource(R.drawable.ic_ads_verify_rejected);
            tvVerificationStatus.setText(R.string.mydocs_status_rejected);
            tvVerificationMessage.setText(getString(R.string.mydocs_status_rejected_message, rejected));
        } else if (pending > 0) {
            ivStatusIcon.setImageResource(R.drawable.ic_ads_verify_pending);
            tvVerificationStatus.setText(R.string.mydocs_status_pending);
            tvVerificationMessage.setText(getString(R.string.mydocs_status_pending_message, pending));
        } else if (approved == documentsList.size()) {
            ivStatusIcon.setImageResource(R.drawable.ic_ads_verify_approved);
            tvVerificationStatus.setText(R.string.mydocs_status_approved);
            tvVerificationMessage.setText(R.string.mydocs_status_approved_message);
        }
    }

    private void updateUI(List<WorkerDocument> documentsList) {
        if (documentsList.isEmpty()) {
            recyclerDocuments.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvDocumentsTitle.setVisibility(View.GONE);
        } else {
            recyclerDocuments.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            tvDocumentsTitle.setVisibility(View.VISIBLE);
            documentsAdapter.setDocuments(documentsList);
        }
    }

    private void openDocument(WorkerDocument document) {
        if (document.getFileUrl() != null && !document.getFileUrl().isEmpty()) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(document.getFileUrl()));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo abrir el documento", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error al abrir documento", e);
            }
        } else {
            Toast.makeText(this, "URL del documento no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUpdateDocumentDialog(WorkerDocument document) {
        String documentName = document.getDescription() != null && !document.getDescription().isEmpty() ?
                document.getDescription() :
                getDocumentTypeName(document.getDocumentType());

        String message = "¿Deseas actualizar el documento: " + documentName + "?";
        if (document.getRejectionReason() != null && !document.getRejectionReason().isEmpty()) {
            message += "\n\nMotivo del rechazo:\n" + document.getRejectionReason();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Actualizar Documento")
                .setMessage(message)
                .setPositiveButton("Actualizar", (dialog, which) -> {
                    // Configurar el tipo de documento para la actualización
                    selectedDocumentType = document.getDocumentType();
                    selectedCategory = document.getCategory();
                    selectedSubcategory = document.getSubcategory();
                    currentDocumentId = document.getId();

                    // Abrir selector de archivos
                    documentLauncher.launch("application/pdf");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showDocumentTypeDialog() {
        if (isUploading) {
            Toast.makeText(this, "Hay una subida en progreso", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear el diálogo personalizado
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_document_type, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Referencias a las CardViews
        CardView cardHojaVida = dialogView.findViewById(R.id.cardHojaVida);
        CardView cardAntecedentes = dialogView.findViewById(R.id.cardAntecedentes);
        CardView cardCarta = dialogView.findViewById(R.id.cardCarta);
        CardView cardTitulo = dialogView.findViewById(R.id.cardTitulo);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        // Click listeners
        cardHojaVida.setOnClickListener(v -> {
            currentDocumentId = null;
            selectedDocumentType = WorkerDocument.TYPE_HOJA_VIDA;
            selectedCategory = WorkerDocument.CATEGORY_HOJA_VIDA;
            selectedSubcategory = null;
            dialog.dismiss();
            documentLauncher.launch("application/pdf");
        });

        cardAntecedentes.setOnClickListener(v -> {
            currentDocumentId = null;
            selectedDocumentType = WorkerDocument.TYPE_ANTECEDENTES;
            selectedCategory = WorkerDocument.CATEGORY_ANTECEDENTES;
            selectedSubcategory = null;
            dialog.dismiss();
            documentLauncher.launch("application/pdf");
        });

        cardCarta.setOnClickListener(v -> {
            currentDocumentId = null;
            selectedDocumentType = WorkerDocument.TYPE_CARTA_RECOMENDACION;
            selectedCategory = WorkerDocument.CATEGORY_CERTIFICACIONES;
            selectedSubcategory = WorkerDocument.SUBCATEGORY_CARTAS;
            dialog.dismiss();
            documentLauncher.launch("application/pdf");
        });

        cardTitulo.setOnClickListener(v -> {
            currentDocumentId = null;
            selectedDocumentType = WorkerDocument.TYPE_TITULO;
            selectedCategory = WorkerDocument.CATEGORY_CERTIFICACIONES;
            selectedSubcategory = WorkerDocument.SUBCATEGORY_TITULOS;
            dialog.dismiss();
            documentLauncher.launch("application/pdf");
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String generateFileName() {
        long timestamp = System.currentTimeMillis();
        return selectedDocumentType + "_" + timestamp + ".pdf";
    }

    private void showUploadConfirmationDialog() {
        String documentName = getDocumentTypeName(selectedDocumentType);
        String action = currentDocumentId != null ? "actualizar" : "subir";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar " + action)
                .setMessage("¿Deseas " + action + " el documento: " + documentName + "?")
                .setPositiveButton(action.substring(0, 1).toUpperCase() + action.substring(1),
                        (dialog, which) -> startUploadProcess())
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    selectedFileUri = null;
                    selectedFileName = null;
        selectedFileHash = null;
        selectedOriginalName = null;
                })
                .show();
    }

    private void startUploadProcess() {
        if (isUploading || selectedFileUri == null) {
            return;
        }

        isUploading = true;
        uploadedCount = 0;
        totalDocuments = 1;

        setUIUploading(true);
        updateUploadProgress(0);

        // Si estamos actualizando, eliminar el documento anterior
        if (currentDocumentId != null) {
            updateExistingDocument();
        } else {
            uploadDocument();
        }
    }

    private void updateExistingDocument() {
        Toast.makeText(this, "Actualizando documento...", Toast.LENGTH_SHORT).show();

        // Primero necesitamos obtener el documento actual para tener el fileName
        mDocumentProvider.getDocument(workerId, selectedCategory, selectedSubcategory, currentDocumentId)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        WorkerDocument oldDocument = snapshot.getValue(WorkerDocument.class);
                        if (oldDocument != null && oldDocument.getFileName() != null) {
                            // Ahora sí eliminar el documento antiguo con todos los parámetros
                            mDocumentProvider.deleteDocument(
                                            workerId,
                                            selectedCategory,
                                            selectedSubcategory,
                                            currentDocumentId,
                                            oldDocument.getFileName()
                                    )
                                    .addOnSuccessListener(aVoid -> {
                                        // Luego subir el nuevo
                                        uploadDocument();
                                    })
                                    .addOnFailureListener(e -> {
                                        setUIUploading(false);
                                        isUploading = false;
                                        Log.e(TAG, "Error al eliminar documento antiguo", e);
                                        showError("Error al actualizar documento: " + e.getMessage());
                                        cleanupUploadState();
                                    });
                        } else {
                            // Si no se encuentra el fileName, subir el nuevo documento directamente
                            Log.w(TAG, "No se encontró fileName del documento antiguo, subiendo nuevo documento");
                            uploadDocument();
                        }
                    } else {
                        // El documento no existe, subir el nuevo
                        Log.w(TAG, "Documento antiguo no existe, subiendo nuevo documento");
                        uploadDocument();
                    }
                })
                .addOnFailureListener(e -> {
                    setUIUploading(false);
                    isUploading = false;
                    Log.e(TAG, "Error al obtener documento antiguo", e);
                    showError("Error al actualizar documento: " + e.getMessage());
                    cleanupUploadState();
                });
    }

    private void uploadDocument() {
        WorkerDocument document = new WorkerDocument();
        document.setId(mDocumentProvider.generateDocumentId());
        document.setWorkerId(workerId);
        document.setDocumentType(selectedDocumentType);
        document.setCategory(selectedCategory);

        if (selectedSubcategory != null) {
            document.setSubcategory(selectedSubcategory);
        }

        document.setFileName(selectedFileName);
        document.setFileType("application/pdf");
        document.setFileHash(selectedFileHash);
        document.setOriginalFileName(selectedOriginalName);
        document.setDescription(getDocumentTypeName(selectedDocumentType));

        // Determinar orden para cartas y títulos
        if (selectedSubcategory != null) {
            int orden = calculateDocumentOrder();
            document.setOrden(orden);
        }

        String action = currentDocumentId != null ? "Actualizando" : "Subiendo";
        Toast.makeText(this, action + " documento...", Toast.LENGTH_SHORT).show();

        mDocumentProvider.uploadDocument(document, selectedFileUri)
                .addOnSuccessListener(aVoid -> {
                    uploadedCount++;
                    updateUploadProgress(uploadedCount);
                    onUploadComplete();
                })
                .addOnFailureListener(e -> {
                    setUIUploading(false);
                    isUploading = false;
                    Log.e(TAG, "Error al subir documento", e);
                    showError("Error al subir documento: " + e.getMessage());
                    cleanupUploadState();
                });
    }

    /** Al reemplazar se conserva el número; uno nuevo va después del último (DEF-23). */
    private int calculateDocumentOrder() {
        if (currentDocumentId != null) {
            for (WorkerDocument doc : currentDocuments) {
                if (currentDocumentId.equals(doc.getId()) && doc.getOrden() > 0) {
                    return doc.getOrden();
                }
            }
        }
        return DocumentOrderHelper.nextOrder(currentDocuments, selectedSubcategory);
    }

    private void onUploadComplete() {
        isUploading = false;
        setUIUploading(false);

        String action = currentDocumentId != null ? "actualizado" : "subido";

        // Actualizar estado de verificación
        mDocumentProvider.updateWorkerVerificationStatus(workerId, "documents_submitted")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Documento " + action + " exitosamente", Toast.LENGTH_SHORT).show();
                    cleanupUploadState();
                    loadDocuments(); // Recargar la lista
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Documento " + action + " pero hubo un error actualizando el estado",
                            Toast.LENGTH_SHORT).show();
                    cleanupUploadState();
                    loadDocuments();
                });
    }

    private void cleanupUploadState() {
        selectedFileUri = null;
        selectedFileName = null;
        selectedFileHash = null;
        selectedOriginalName = null;
        selectedDocumentType = null;
        selectedCategory = null;
        selectedSubcategory = null;
        currentDocumentId = null;
    }

    private void updateUploadProgress(int uploaded) {
        int percentage = (int) ((uploaded / (float) totalDocuments) * 100);
        uploadProgressBar.setProgress(percentage);
        tvUploadProgress.setText(getString(R.string.mydocs_upload_progress, uploaded, totalDocuments));
    }

    private void setUIUploading(boolean uploading) {
        if (fabAddDocument != null) fabAddDocument.setEnabled(!uploading);

        cardUploadProgress.setVisibility(uploading ? View.VISIBLE : View.GONE);

        if (uploading) {
            uploadProgressBar.setProgress(0);
        }
    }

    private String getDocumentTypeName(String documentType) {
        if (documentType == null) return "Documento";

        switch (documentType) {
            case WorkerDocument.TYPE_HOJA_VIDA:
                return "Hoja de Vida (CV)";
            case WorkerDocument.TYPE_ANTECEDENTES:
                return "Antecedentes Judiciales";
            case WorkerDocument.TYPE_CARTA_RECOMENDACION:
                return "Carta de Recomendación";
            case WorkerDocument.TYPE_TITULO:
                return "Título o Certificado";
            default:
                return documentType;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isUploading) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Subida en progreso")
                    .setMessage("¿Deseas cancelar la subida?")
                    .setPositiveButton("Sí, cancelar", (dialog, which) -> {
                        super.onBackPressed();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            super.onBackPressed();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}