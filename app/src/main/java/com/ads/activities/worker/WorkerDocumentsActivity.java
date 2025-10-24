package com.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ads.providers.AuthProvider;
import com.ads.providers.WorkerDocumentProvider;
import com.ads.providers.WorkerProvider;
import com.ads.models.Worker;
import com.ads.models.WorkerDocument;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.ads.R;

public class WorkerDocumentsActivity extends AppCompatActivity {

    private static final String TAG = "WorkerDocumentsActivity";
    private static final int PICK_PDF_HOJA_VIDA = 1001;
    private static final int PICK_PDF_ANTECEDENTES = 1002;
    private static final int PICK_PDF_CARTA_1 = 1003;
    private static final int PICK_PDF_CARTA_2 = 1004;
    private static final int PICK_PDF_CARTA_3 = 1005;

    // Providers
    private AuthProvider mAuthProvider;
    private WorkerDocumentProvider mDocumentProvider;
    private WorkerProvider mWorkerProvider;

    // UI Components
    private Toolbar mToolbar;
    private ProgressBar mProgressBar;

    // Verification Status Card
    private CardView mCardVerificationStatus;
    private ImageView mIconVerificationStatus;
    private TextView mTextVerificationStatus;
    private TextView mTextVerificationMessage;
    private TextView mTextVerificationDate;

    // Hoja de Vida Card
    private CardView mCardHojaVida;
    private ImageView mIconHojaVida;
    private TextView mTextHojaVidaStatus;
    private TextView mTextHojaVidaDate;
    private MaterialButton mButtonUploadHojaVida;
    private MaterialButton mButtonViewHojaVida;
    private TextView mTextHojaVidaRejectionReason;
    private LinearLayout mLayoutHojaVidaRejection;

    // Antecedentes Card
    private CardView mCardAntecedentes;
    private ImageView mIconAntecedentes;
    private TextView mTextAntecedentesStatus;
    private TextView mTextAntecedentesDate;
    private MaterialButton mButtonUploadAntecedentes;
    private MaterialButton mButtonViewAntecedentes;
    private TextView mTextAntecedentesRejectionReason;
    private LinearLayout mLayoutAntecedentesRejection;

    // Cartas de Recomendación Cards
    private CardView mCardCarta1, mCardCarta2, mCardCarta3;
    private ImageView mIconCarta1, mIconCarta2, mIconCarta3;
    private TextView mTextCarta1Status, mTextCarta2Status, mTextCarta3Status;
    private TextView mTextCarta1Date, mTextCarta2Date, mTextCarta3Date;
    private MaterialButton mButtonUploadCarta1, mButtonUploadCarta2, mButtonUploadCarta3;
    private MaterialButton mButtonViewCarta1, mButtonViewCarta2, mButtonViewCarta3;
    private TextView mTextCarta1RejectionReason, mTextCarta2RejectionReason, mTextCarta3RejectionReason;
    private LinearLayout mLayoutCarta1Rejection, mLayoutCarta2Rejection, mLayoutCarta3Rejection;

    // Submit Button
    private MaterialButton mButtonSubmitDocuments;

    // Data
    private String mWorkerId;
    private String mVerificationStatus;
    private WorkerDocument mHojaVida;
    private WorkerDocument mAntecedentes;
    private WorkerDocument mCarta1, mCarta2, mCarta3;

    private ValueEventListener mDocumentsListener;
    private boolean hasAllDocuments = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_documents);

        initProviders();
        initViews();
        setupToolbar();
        setupStatusBar();
        setupClickListeners();

        mWorkerId = mAuthProvider.getId();
        if (mWorkerId != null) {
            loadDocumentsStatus();
            listenToDocumentChanges();
        } else {
            showError("Error: Usuario no autenticado");
            finish();
        }
    }

    private void initProviders() {
        mAuthProvider = new AuthProvider();
        mDocumentProvider = new WorkerDocumentProvider();
        mWorkerProvider = new WorkerProvider();
    }

    private void initViews() {
        mToolbar = findViewById(R.id.toolbar);
        mProgressBar = findViewById(R.id.progressBar);

        // Verification Status
        mCardVerificationStatus = findViewById(R.id.cardVerificationStatus);
        mIconVerificationStatus = findViewById(R.id.iconVerificationStatus);
        mTextVerificationStatus = findViewById(R.id.textVerificationStatus);
        mTextVerificationMessage = findViewById(R.id.textVerificationMessage);
        mTextVerificationDate = findViewById(R.id.textVerificationDate);

        // Hoja de Vida
        mCardHojaVida = findViewById(R.id.cardHojaVida);
        mIconHojaVida = findViewById(R.id.iconHojaVida);
        mTextHojaVidaStatus = findViewById(R.id.textHojaVidaStatus);
        mTextHojaVidaDate = findViewById(R.id.textHojaVidaDate);
        mButtonUploadHojaVida = findViewById(R.id.buttonUploadHojaVida);
        mButtonViewHojaVida = findViewById(R.id.buttonViewHojaVida);
        mLayoutHojaVidaRejection = findViewById(R.id.layoutHojaVidaRejection);
        mTextHojaVidaRejectionReason = findViewById(R.id.textHojaVidaRejectionReason);

        // Antecedentes
        mCardAntecedentes = findViewById(R.id.cardAntecedentes);
        mIconAntecedentes = findViewById(R.id.iconAntecedentes);
        mTextAntecedentesStatus = findViewById(R.id.textAntecedentesStatus);
        mTextAntecedentesDate = findViewById(R.id.textAntecedentesDate);
        mButtonUploadAntecedentes = findViewById(R.id.buttonUploadAntecedentes);
        mButtonViewAntecedentes = findViewById(R.id.buttonViewAntecedentes);
        mLayoutAntecedentesRejection = findViewById(R.id.layoutAntecedentesRejection);
        mTextAntecedentesRejectionReason = findViewById(R.id.textAntecedentesRejectionReason);

        // Carta 1
        mCardCarta1 = findViewById(R.id.cardCarta1);
        mIconCarta1 = findViewById(R.id.iconCarta1);
        mTextCarta1Status = findViewById(R.id.textCarta1Status);
        mTextCarta1Date = findViewById(R.id.textCarta1Date);
        mButtonUploadCarta1 = findViewById(R.id.buttonUploadCarta1);
        mButtonViewCarta1 = findViewById(R.id.buttonViewCarta1);
        mLayoutCarta1Rejection = findViewById(R.id.layoutCarta1Rejection);
        mTextCarta1RejectionReason = findViewById(R.id.textCarta1RejectionReason);

        // Carta 2
        mCardCarta2 = findViewById(R.id.cardCarta2);
        mIconCarta2 = findViewById(R.id.iconCarta2);
        mTextCarta2Status = findViewById(R.id.textCarta2Status);
        mTextCarta2Date = findViewById(R.id.textCarta2Date);
        mButtonUploadCarta2 = findViewById(R.id.buttonUploadCarta2);
        mButtonViewCarta2 = findViewById(R.id.buttonViewCarta2);
        mLayoutCarta2Rejection = findViewById(R.id.layoutCarta2Rejection);
        mTextCarta2RejectionReason = findViewById(R.id.textCarta2RejectionReason);

        // Carta 3
        mCardCarta3 = findViewById(R.id.cardCarta3);
        mIconCarta3 = findViewById(R.id.iconCarta3);
        mTextCarta3Status = findViewById(R.id.textCarta3Status);
        mTextCarta3Date = findViewById(R.id.textCarta3Date);
        mButtonUploadCarta3 = findViewById(R.id.buttonUploadCarta3);
        mButtonViewCarta3 = findViewById(R.id.buttonViewCarta3);
        mLayoutCarta3Rejection = findViewById(R.id.layoutCarta3Rejection);
        mTextCarta3RejectionReason = findViewById(R.id.textCarta3RejectionReason);

        // Submit Button
        mButtonSubmitDocuments = findViewById(R.id.buttonSubmitDocuments);
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mis Documentos");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }
    }

    private void setupClickListeners() {
        mButtonUploadHojaVida.setOnClickListener(v -> pickDocument(PICK_PDF_HOJA_VIDA));
        mButtonUploadAntecedentes.setOnClickListener(v -> pickDocument(PICK_PDF_ANTECEDENTES));
        mButtonUploadCarta1.setOnClickListener(v -> pickDocument(PICK_PDF_CARTA_1));
        mButtonUploadCarta2.setOnClickListener(v -> pickDocument(PICK_PDF_CARTA_2));
        mButtonUploadCarta3.setOnClickListener(v -> pickDocument(PICK_PDF_CARTA_3));

        mButtonViewHojaVida.setOnClickListener(v -> viewDocument(mHojaVida));
        mButtonViewAntecedentes.setOnClickListener(v -> viewDocument(mAntecedentes));
        mButtonViewCarta1.setOnClickListener(v -> viewDocument(mCarta1));
        mButtonViewCarta2.setOnClickListener(v -> viewDocument(mCarta2));
        mButtonViewCarta3.setOnClickListener(v -> viewDocument(mCarta3));

        mButtonSubmitDocuments.setOnClickListener(v -> submitDocumentsForReview());
    }

    private void pickDocument(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Seleccionar PDF"), requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            showError("No hay ningún gestor de archivos instalado");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();

            switch (requestCode) {
                case PICK_PDF_HOJA_VIDA:
                    uploadDocument(fileUri, WorkerDocument.CATEGORY_HOJA_VIDA,
                            WorkerDocument.TYPE_HOJA_VIDA, null, 0);
                    break;

                case PICK_PDF_ANTECEDENTES:
                    uploadDocument(fileUri, WorkerDocument.CATEGORY_ANTECEDENTES,
                            WorkerDocument.TYPE_ANTECEDENTES, null, 0);
                    break;

                case PICK_PDF_CARTA_1:
                    uploadDocument(fileUri, WorkerDocument.CATEGORY_CERTIFICACIONES,
                            WorkerDocument.TYPE_CARTA_RECOMENDACION,
                            WorkerDocument.SUBCATEGORY_CARTAS, 1);
                    break;

                case PICK_PDF_CARTA_2:
                    uploadDocument(fileUri, WorkerDocument.CATEGORY_CERTIFICACIONES,
                            WorkerDocument.TYPE_CARTA_RECOMENDACION,
                            WorkerDocument.SUBCATEGORY_CARTAS, 2);
                    break;

                case PICK_PDF_CARTA_3:
                    uploadDocument(fileUri, WorkerDocument.CATEGORY_CERTIFICACIONES,
                            WorkerDocument.TYPE_CARTA_RECOMENDACION,
                            WorkerDocument.SUBCATEGORY_CARTAS, 3);
                    break;
            }
        }
    }

    private void uploadDocument(Uri fileUri, String category, String documentType,
                                String subcategory, int orden) {
        showProgress(true);

        String documentId = mDocumentProvider.generateDocumentId();
        String fileName = mDocumentProvider.generateFileName(mWorkerId, documentType, "pdf");

        WorkerDocument document = new WorkerDocument(documentId, mWorkerId, documentType, category);
        document.setSubcategory(subcategory);
        document.setFileName(fileName);
        document.setFileType("application/pdf");
        document.setOrden(orden);
        document.setStatus(WorkerDocument.STATUS_PENDING);

        mDocumentProvider.uploadDocument(document, fileUri)
                .addOnSuccessListener(aVoid -> {
                    showProgress(false);
                    showSuccess("Documento subido exitosamente");
                    loadDocumentsStatus();
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    showError("Error al subir documento: " + e.getMessage());
                    Log.e(TAG, "Error uploading document", e);
                });
    }

    private void loadDocumentsStatus() {
        showProgress(true);

        // Primero cargar el estado de verificación del worker
        mWorkerProvider.getWorker(mWorkerId)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        DataSnapshot verificationSnapshot = snapshot.child("verificationStatus");
                        if (verificationSnapshot.exists()) {
                            mVerificationStatus = verificationSnapshot.child("status")
                                    .getValue(String.class);
                            Long submittedAt = verificationSnapshot.child("submittedAt")
                                    .getValue(Long.class);
                            updateVerificationStatusUI(mVerificationStatus, submittedAt);
                        }
                    }
                    // Luego cargar los documentos
                    loadAllDocuments();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading verification status", e);
                    loadAllDocuments();
                });
    }

    private void loadAllDocuments() {
        mDocumentProvider.getWorkerDocuments(mWorkerId)
                .addOnSuccessListener(snapshot -> {
                    showProgress(false);

                    if (snapshot.exists()) {
                        // Hoja de Vida
                        if (snapshot.child(WorkerDocument.CATEGORY_HOJA_VIDA).exists()) {
                            mHojaVida = snapshot.child(WorkerDocument.CATEGORY_HOJA_VIDA)
                                    .getValue(WorkerDocument.class);
                            updateDocumentUI(mHojaVida, mIconHojaVida, mTextHojaVidaStatus,
                                    mTextHojaVidaDate, mButtonUploadHojaVida,
                                    mButtonViewHojaVida, mLayoutHojaVidaRejection,
                                    mTextHojaVidaRejectionReason);
                        }

                        // Antecedentes
                        if (snapshot.child(WorkerDocument.CATEGORY_ANTECEDENTES).exists()) {
                            mAntecedentes = snapshot.child(WorkerDocument.CATEGORY_ANTECEDENTES)
                                    .getValue(WorkerDocument.class);
                            updateDocumentUI(mAntecedentes, mIconAntecedentes, mTextAntecedentesStatus,
                                    mTextAntecedentesDate, mButtonUploadAntecedentes,
                                    mButtonViewAntecedentes, mLayoutAntecedentesRejection,
                                    mTextAntecedentesRejectionReason);
                        }

                        // Cartas de Recomendación
                        DataSnapshot cartasSnapshot = snapshot
                                .child(WorkerDocument.CATEGORY_CERTIFICACIONES)
                                .child(WorkerDocument.SUBCATEGORY_CARTAS);

                        for (DataSnapshot cartaSnapshot : cartasSnapshot.getChildren()) {
                            WorkerDocument carta = cartaSnapshot.getValue(WorkerDocument.class);
                            if (carta != null) {
                                switch (carta.getOrden()) {
                                    case 1:
                                        mCarta1 = carta;
                                        updateDocumentUI(carta, mIconCarta1, mTextCarta1Status,
                                                mTextCarta1Date, mButtonUploadCarta1,
                                                mButtonViewCarta1, mLayoutCarta1Rejection,
                                                mTextCarta1RejectionReason);
                                        break;
                                    case 2:
                                        mCarta2 = carta;
                                        updateDocumentUI(carta, mIconCarta2, mTextCarta2Status,
                                                mTextCarta2Date, mButtonUploadCarta2,
                                                mButtonViewCarta2, mLayoutCarta2Rejection,
                                                mTextCarta2RejectionReason);
                                        break;
                                    case 3:
                                        mCarta3 = carta;
                                        updateDocumentUI(carta, mIconCarta3, mTextCarta3Status,
                                                mTextCarta3Date, mButtonUploadCarta3,
                                                mButtonViewCarta3, mLayoutCarta3Rejection,
                                                mTextCarta3RejectionReason);
                                        break;
                                }
                            }
                        }
                    }

                    updateSubmitButtonState();
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    showError("Error al cargar documentos");
                    Log.e(TAG, "Error loading documents", e);
                });
    }

    private void updateVerificationStatusUI(String status, Long submittedAt) {
        if (status == null) {
            status = "incomplete";
        }

        switch (status.toLowerCase()) {
            case "approved":
                mIconVerificationStatus.setImageResource(R.drawable.ic_check_circle);
                mIconVerificationStatus.setColorFilter(getResources().getColor(R.color.success_color, getTheme()));
                mTextVerificationStatus.setText("✅ Documentos Aprobados");
                mTextVerificationStatus.setTextColor(getResources().getColor(R.color.success_color, getTheme()));
                mTextVerificationMessage.setText("Tus documentos han sido verificados y aprobados. Ya puedes conectarte y ofrecer tus servicios.");
                break;

            case "documents_submitted":
            case "pending":
                mIconVerificationStatus.setImageResource(R.drawable.ic_clock);
                mIconVerificationStatus.setColorFilter(getResources().getColor(R.color.warning_color, getTheme()));
                mTextVerificationStatus.setText("⏳ En Revisión");
                mTextVerificationStatus.setTextColor(getResources().getColor(R.color.warning_color, getTheme()));
                mTextVerificationMessage.setText("Tus documentos están siendo revisados por nuestro equipo. Te notificaremos cuando sean aprobados.");
                break;

            case "rejected":
                mIconVerificationStatus.setImageResource(R.drawable.ic_error);
                mIconVerificationStatus.setColorFilter(getResources().getColor(R.color.error_color, getTheme()));
                mTextVerificationStatus.setText("❌ Documentos Rechazados");
                mTextVerificationStatus.setTextColor(getResources().getColor(R.color.error_color, getTheme()));
                mTextVerificationMessage.setText("Algunos documentos fueron rechazados. Por favor, revisa los motivos y vuelve a subirlos.");
                break;

            default:
                mIconVerificationStatus.setImageResource(R.drawable.ic_upload);
                mIconVerificationStatus.setColorFilter(getResources().getColor(R.color.text_secondary, getTheme()));
                mTextVerificationStatus.setText("📄 Documentos Incompletos");
                mTextVerificationStatus.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                mTextVerificationMessage.setText("Sube todos los documentos requeridos y envíalos para revisión.");
                break;
        }

        if (submittedAt != null && submittedAt > 0) {
            mTextVerificationDate.setText("Enviado: " + formatDate(submittedAt));
            mTextVerificationDate.setVisibility(View.VISIBLE);
        } else {
            mTextVerificationDate.setVisibility(View.GONE);
        }
    }

    private void updateDocumentUI(WorkerDocument document, ImageView icon, TextView statusText,
                                  TextView dateText, MaterialButton uploadButton,
                                  MaterialButton viewButton, LinearLayout rejectionLayout,
                                  TextView rejectionReasonText) {
        if (document == null) {
            icon.setImageResource(R.drawable.ic_upload);
            icon.setColorFilter(getResources().getColor(R.color.text_secondary, getTheme()));
            statusText.setText("Sin subir");
            statusText.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            dateText.setVisibility(View.GONE);
            uploadButton.setText("Subir");
            uploadButton.setEnabled(true);
            viewButton.setVisibility(View.GONE);
            rejectionLayout.setVisibility(View.GONE);
            return;
        }

        dateText.setText(document.getFormattedUploadDate());
        dateText.setVisibility(View.VISIBLE);
        viewButton.setVisibility(View.VISIBLE);

        switch (document.getStatus()) {
            case WorkerDocument.STATUS_APPROVED:
                icon.setImageResource(R.drawable.ic_check_circle);
                icon.setColorFilter(getResources().getColor(R.color.success_color, getTheme()));
                statusText.setText("✅ Aprobado");
                statusText.setTextColor(getResources().getColor(R.color.success_color, getTheme()));
                uploadButton.setVisibility(View.GONE);
                rejectionLayout.setVisibility(View.GONE);
                break;

            case WorkerDocument.STATUS_PENDING:
                icon.setImageResource(R.drawable.ic_clock);
                icon.setColorFilter(getResources().getColor(R.color.warning_color, getTheme()));
                statusText.setText("⏳ Pendiente");
                statusText.setTextColor(getResources().getColor(R.color.warning_color, getTheme()));
                uploadButton.setVisibility(View.GONE);
                rejectionLayout.setVisibility(View.GONE);
                break;

            case WorkerDocument.STATUS_REJECTED:
                icon.setImageResource(R.drawable.ic_error);
                icon.setColorFilter(getResources().getColor(R.color.error_color, getTheme()));
                statusText.setText("❌ Rechazado");
                statusText.setTextColor(getResources().getColor(R.color.error_color, getTheme()));
                uploadButton.setText("Volver a subir");
                uploadButton.setVisibility(View.VISIBLE);
                uploadButton.setEnabled(true);

                if (document.getRejectionReason() != null && !document.getRejectionReason().isEmpty()) {
                    rejectionLayout.setVisibility(View.VISIBLE);
                    rejectionReasonText.setText(document.getRejectionReason());
                } else {
                    rejectionLayout.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void updateSubmitButtonState() {
        hasAllDocuments = mHojaVida != null && mAntecedentes != null &&
                mCarta1 != null && mCarta2 != null && mCarta3 != null;

        boolean canSubmit = hasAllDocuments &&
                (mVerificationStatus == null ||
                        "incomplete".equals(mVerificationStatus) ||
                        "rejected".equals(mVerificationStatus));

        mButtonSubmitDocuments.setEnabled(canSubmit);
        mButtonSubmitDocuments.setAlpha(canSubmit ? 1.0f : 0.5f);

        if (!hasAllDocuments) {
            mButtonSubmitDocuments.setText("Completa todos los documentos");
        } else if ("documents_submitted".equals(mVerificationStatus) ||
                "pending".equals(mVerificationStatus)) {
            mButtonSubmitDocuments.setText("Documentos enviados");
        } else if ("approved".equals(mVerificationStatus)) {
            mButtonSubmitDocuments.setText("Documentos aprobados");
        } else {
            mButtonSubmitDocuments.setText("Enviar para revisión");
        }
    }

    private void submitDocumentsForReview() {
        if (!hasAllDocuments) {
            showError("Debes subir todos los documentos requeridos");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Enviar documentos")
                .setMessage("¿Estás seguro de que deseas enviar tus documentos para revisión?")
                .setPositiveButton("Enviar", (dialog, which) -> {
                    showProgress(true);

                    mDocumentProvider.updateWorkerVerificationStatus(mWorkerId, "documents_submitted")
                            .addOnSuccessListener(aVoid -> {
                                showProgress(false);
                                showSuccess("Documentos enviados exitosamente");
                                mVerificationStatus = "documents_submitted";
                                updateVerificationStatusUI(mVerificationStatus, System.currentTimeMillis());
                                updateSubmitButtonState();
                            })
                            .addOnFailureListener(e -> {
                                showProgress(false);
                                showError("Error al enviar documentos");
                                Log.e(TAG, "Error submitting documents", e);
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void viewDocument(WorkerDocument document) {
        if (document == null || document.getFileUrl() == null) {
            showError("Documento no disponible");
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(document.getFileUrl()), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening document", e);
            showError("No se puede abrir el documento");
        }
    }

    private void listenToDocumentChanges() {
        mDocumentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadDocumentsStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error listening to documents", error.toException());
            }
        };

        mDocumentProvider.listenToWorkerDocuments(mWorkerId, mDocumentsListener);
    }

    private String formatDate(long timestamp) {
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(timestamp));
    }

    private void showProgress(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showSuccess(String message) {
        Toast.makeText(this, "✅ " + message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDocumentsListener != null && mWorkerId != null) {
            mDocumentProvider.removeListener(mWorkerId, mDocumentsListener);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}