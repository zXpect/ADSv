package com.project.ads.activities.worker;

import androidx.appcompat.app.*;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.R;
import com.project.ads.helpers.QuoteDialogHelper;
import com.project.ads.helpers.RequestDetailsHelper;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.models.ServiceRequest;
import com.project.ads.providers.RequestProvider;

import java.util.Map;

/**
 * Detalle de una solicitud nueva para el trabajador · mockup 15.
 * «Cotizar» abre la cotización (mockup 16); al enviarla el backend pasa la
 * solicitud a aceptada. «Rechazar solicitud» se conserva del flujo anterior.
 */
public class RequestDetailActivity extends AppCompatActivity {
    
    private static final String TAG = "RequestDetailActivity";
    
    // UI Elements
    private ProgressBar progressBar;
    private LinearLayout layoutContent;
    private TextView tvError;
    private TextView tvRequestId;
    private TextView tvStatus;
    private TextView tvTimestamp;
    private TextView tvClientName;
    private TextView tvClientPhone;
    private TextView tvClientEmail;
    private TextView tvServiceType;
    private TextView tvAddress;
    private TextView tvUrgencyLevel;
    private TextView tvEstimatedCost;
    private TextView tvDescription;
    private Button btnAccept;
    private Button btnReject;
    private ImageButton btnBack;
    
    // Firebase
    private DatabaseReference databaseReference;
    private RequestProvider requestProvider;
    
    // Data
    private String requestId;
    private ServiceRequest serviceRequest;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "RequestDetailActivity created");
        setContentView(R.layout.activity_request_detail);
        
        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        requestProvider = new RequestProvider(this);
        
        // Get request ID from intent
        requestId = getIntent().getStringExtra("request_id");
        
        if (requestId == null || requestId.isEmpty()) {
            Log.e(TAG, "No request ID provided");
            Toast.makeText(this, "Error: No se proporcionó ID de solicitud", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize UI
        initializeUI();
        
        // Load request data
        loadRequestData();
        // Nombres de las tareas para el detalle; al llegar se vuelve a pintar
        new CatalogProvider().getCatalog(new com.project.ads.providers.ApiCallback<com.project.ads.models.api.CatalogData>() {
            @Override
            public void onSuccess(com.project.ads.models.api.CatalogData catalog) {
                if (serviceRequest != null && !isFinishing()) populateUI(serviceRequest);
            }

            @Override
            public void onError(String message, int httpStatus) {
            }
        });
    }
    
    private void initializeUI() {
        progressBar = findViewById(R.id.progressBar);
        layoutContent = findViewById(R.id.layoutContent);
        tvError = findViewById(R.id.tvError);
        tvRequestId = findViewById(R.id.tvRequestId);
        tvStatus = findViewById(R.id.tvStatus);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvClientName = findViewById(R.id.tvClientName);
        tvClientPhone = findViewById(R.id.tvClientPhone);
        tvClientEmail = findViewById(R.id.tvClientEmail);
        tvServiceType = findViewById(R.id.tvServiceType);
        tvAddress = findViewById(R.id.tvAddress);
        tvUrgencyLevel = findViewById(R.id.tvUrgencyLevel);
        tvEstimatedCost = findViewById(R.id.tvEstimatedCost);
        tvDescription = findViewById(R.id.tvDescription);
        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);
        btnBack = findViewById(R.id.btnBack);
        
        // Set up click listeners
        btnBack.setOnClickListener(v -> finish());
        // Cotizar = enviar cotización: el backend marca la solicitud como aceptada
        btnAccept.setOnClickListener(v -> acceptRequest());
        btnReject.setOnClickListener(v -> rejectRequest());
        
        // Set up phone click listener
        tvClientPhone.setOnClickListener(v -> {
            if (serviceRequest != null && serviceRequest.getClient_phone() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + serviceRequest.getClient_phone()));
                startActivity(intent);
            }
        });
        
        // Set up email click listener
        tvClientEmail.setOnClickListener(v -> {
            if (serviceRequest != null && serviceRequest.getClient_email() != null) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + serviceRequest.getClient_email()));
                startActivity(intent);
            }
        });
    }

    private boolean isValidRequestId(String id) {
        return id != null && !id.isEmpty() && !id.equals("null");
    }

    private void loadRequestData() {
        if (!isValidRequestId(requestId)) {
            Log.e(TAG, "Invalid request ID: " + requestId);
            showError("ID de solicitud inválido");
            return;
        }

        showLoading(true);

        Log.d(TAG, "Loading request data for ID: " + requestId);

        databaseReference.child("requests").child(requestId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d(TAG, "DataSnapshot exists: " + dataSnapshot.exists());

                        if (dataSnapshot.exists()) {
                            try {
                                // Convert to ServiceRequest object
                                Map<String, Object> requestData = (Map<String, Object>) dataSnapshot.getValue();

                                if (requestData != null) {
                                    serviceRequest = ServiceRequest.fromMap(requestData);

                                    if (serviceRequest != null) {
                                        // Asegurar que el request_id esté establecido
                                        if (serviceRequest.getRequest_id() == null) {
                                            serviceRequest.setRequest_id(requestId);
                                        }

                                        populateUI(serviceRequest);
                                        showLoading(false);
                                    } else {
                                        Log.e(TAG, "Failed to parse ServiceRequest from data");
                                        showError("Error al procesar los datos de la solicitud");
                                    }
                                } else {
                                    Log.e(TAG, "Request data is null");
                                    showError("No se encontraron datos de la solicitud");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing request data", e);
                                showError("Error al cargar los datos: " + e.getMessage());
                            }
                        } else {
                            Log.w(TAG, "Request not found for ID: " + requestId);
                            showError("Solicitud no encontrada con ID: " + requestId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Database error: " + databaseError.getMessage() + " (Code: " + databaseError.getCode() + ")");
                        showError("Error de conexión: " + databaseError.getMessage());
                    }
                });
    }
    
    private void populateUI(ServiceRequest request) {
        tvStatus.setText(statusLabel(request.getStatus()));
        tvClientName.setText(request.getClient_name() != null ? request.getClient_name() : "Cliente");
        String service = request.getService_type() != null ? request.getService_type() : "";
        tvTimestamp.setText(service.isEmpty() ? request.getTimeAgo() : service + " · " + request.getTimeAgo());

        boolean hasPhone = request.getClient_phone() != null && !request.getClient_phone().isEmpty();
        tvClientPhone.setText(hasPhone ? request.getClient_phone() : "");
        tvClientPhone.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
        boolean hasEmail = request.getClient_email() != null && !request.getClient_email().isEmpty();
        tvClientEmail.setText(hasEmail ? request.getClient_email() : "");
        tvClientEmail.setVisibility(hasEmail ? View.VISIBLE : View.GONE);

        bindTasks(request);
        tvAddress.setText(request.getAddress() != null ? request.getAddress() : "Sin dirección");
        boolean hasDescription = request.getDescription() != null && !request.getDescription().isEmpty();
        tvDescription.setText(hasDescription ? request.getDescription() : "");
        tvDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);

        // Precio: el cotizado si ya hay; si no, el estimado de referencia congelado
        com.project.ads.models.api.QuoteReference reference = request.getQuoteReference();
        TextView priceNote = findViewById(R.id.tv_detail_price_note);
        if (request.hasQuote()) {
            tvEstimatedCost.setText(request.getFormattedQuotedPrice());
            priceNote.setText(R.string.worker_detail_price_quoted);
        } else if (reference != null && reference.hasRange()) {
            tvEstimatedCost.setText(ServiceRequest.formatCop(reference.getMinCop()) + " – "
                    + ServiceRequest.formatCop(reference.getMaxCop()));
            priceNote.setText(reference.getMinVisitFeeCop() > 0
                    ? getString(R.string.worker_detail_price_note_fee,
                    ServiceRequest.formatCop(reference.getMinVisitFeeCop()))
                    : getString(R.string.worker_detail_price_note));
        } else {
            tvEstimatedCost.setText(R.string.request_task_quoted_by_worker);
            priceNote.setText(R.string.worker_detail_price_unpriced);
        }

        updateButtonsVisibility(request.getStatus());
    }

    /** Tareas y cantidades (mockup 15), a partir del texto de tareas del catálogo. */
    private void bindTasks(ServiceRequest request) {
        android.widget.LinearLayout rows = findViewById(R.id.layout_detail_tasks);
        rows.removeAllViews();
        String tasks = request.getTaskItemsDisplay(CatalogProvider.cachedCatalog());
        boolean has = tasks != null && !tasks.isEmpty();
        findViewById(R.id.tv_detail_tasks_title).setVisibility(has ? View.VISIBLE : View.GONE);
        findViewById(R.id.card_detail_tasks).setVisibility(has ? View.VISIBLE : View.GONE);
        if (!has) return;
        for (String line : tasks.split("\n")) {
            String text = line.startsWith("• ") ? line.substring(2) : line;
            int split = text.lastIndexOf(" · ");
            View row = getLayoutInflater().inflate(R.layout.item_detail_task_row, rows, false);
            ((TextView) row.findViewById(R.id.tv_detail_task_name)).setText(split > 0 ? text.substring(0, split) : text);
            ((TextView) row.findViewById(R.id.tv_detail_task_quantity)).setText(split > 0 ? text.substring(split + 3) : "");
            rows.addView(row);
        }
    }

    private String statusLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "pending": return getString(R.string.request_status_pending);
            case "accepted": return getString(R.string.request_status_quoted);
            case "rejected": return getString(R.string.request_status_rejected);
            case "cancelled": return getString(R.string.request_status_cancelled);
            default: return status;
        }
    }

    private void updateButtonsVisibility(String status) {
        boolean pending = "pending".equals(status);
        findViewById(R.id.layout_detail_actions).setVisibility(pending ? View.VISIBLE : View.GONE);
        btnAccept.setVisibility(pending ? View.VISIBLE : View.GONE);
        btnReject.setVisibility(pending ? View.VISIBLE : View.GONE);
    }

    /**
     * Aceptar una solicitud exige cotizarla: el backend guarda el precio, pasa
     * la solicitud a 'accepted' y notifica al cliente.
     */
    private void acceptRequest() {
        if (serviceRequest == null) return;

        new QuoteDialogHelper(this).show(serviceRequest, quote -> {
            serviceRequest.setStatus(quote.getStatus());
            tvStatus.setText(statusLabel(quote.getStatus()));
            updateButtonsVisibility(quote.getStatus());
            if (quote.getQuotedPrice() != null) {
                tvEstimatedCost.setText(ServiceRequest.formatCop(quote.getQuotedPrice()));
            }
        });
    }
    
    private void rejectRequest() {
        if (serviceRequest == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnAccept.setEnabled(false);
        btnReject.setEnabled(false);

        requestProvider.rejectRequest(requestId)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show();
                    
                    // Update local object and UI
                    serviceRequest.setStatus("rejected");
                    tvStatus.setText(statusLabel("rejected"));
                    updateButtonsVisibility("rejected");
                    
                    btnAccept.setEnabled(true);
                    btnReject.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error updating request status", e);
                    Toast.makeText(this, "Error al actualizar el estado de la solicitud", Toast.LENGTH_SHORT).show();
                    
                    btnAccept.setEnabled(true);
                    btnReject.setEnabled(true);
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Verificar si hay un nuevo request_id
        String newRequestId = intent.getStringExtra("request_id");
        if (newRequestId != null && !newRequestId.equals(requestId)) {
            Log.d(TAG, "New request ID received: " + newRequestId);
            requestId = newRequestId;
            serviceRequest = null; // Reset current request
            loadRequestData(); // Cargar los nuevos datos
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Recargar datos si volvemos a la actividad y no hay datos cargados
        if (serviceRequest == null && requestId != null && !requestId.isEmpty()) {
            loadRequestData();
        }
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        layoutContent.setVisibility(show ? View.GONE : View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }
    
    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }
}


