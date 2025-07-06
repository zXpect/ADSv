package com.ads.activities.worker;

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
import com.ads.models.ServiceRequest;

import java.util.HashMap;
import java.util.Map;

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
    
    // Data
    private String requestId;
    private ServiceRequest serviceRequest;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);
        
        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
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
    
    private void loadRequestData() {
        showLoading(true);
        
        databaseReference.child("service_requests").child(requestId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            try {
                                // Convert to ServiceRequest object
                                Map<String, Object> requestData = (Map<String, Object>) dataSnapshot.getValue();
                                serviceRequest = ServiceRequest.fromMap(requestData);
                                
                                if (serviceRequest != null) {
                                    populateUI(serviceRequest);
                                    showLoading(false);
                                } else {
                                    showError("Error al procesar los datos de la solicitud");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing request data", e);
                                showError("Error al cargar los datos de la solicitud");
                            }
                        } else {
                            showError("Solicitud no encontrada");
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Database error: " + databaseError.getMessage());
                        showError("Error de conexión: " + databaseError.getMessage());
                    }
                });
    }
    
    private void populateUI(ServiceRequest request) {
        tvRequestId.setText("ID: #" + (request.getRequest_id() != null ? request.getRequest_id() : "N/A"));
        tvStatus.setText(request.getStatusDisplayText());
        tvTimestamp.setText(request.getFormattedTimestamp());
        
        tvClientName.setText(request.getClient_name() != null ? request.getClient_name() : "N/A");
        tvClientPhone.setText(request.getClient_phone() != null ? request.getClient_phone() : "N/A");
        tvClientEmail.setText(request.getClient_email() != null ? request.getClient_email() : "N/A");
        
        tvServiceType.setText(request.getService_type() != null ? request.getService_type() : "N/A");
        tvAddress.setText(request.getAddress() != null ? request.getAddress() : "N/A");
        tvUrgencyLevel.setText(request.getUrgency_level() != null ? request.getUrgency_level() : "Normal");
        
        if (request.getEstimated_cost() > 0) {
            tvEstimatedCost.setText(String.format("$%.2f", request.getEstimated_cost()));
        } else {
            tvEstimatedCost.setText("Por determinar");
        }
        
        tvDescription.setText(request.getDescription() != null ? request.getDescription() : "Sin descripción");
        
        // Update button visibility based on status
        updateButtonsVisibility(request.getStatus());
    }
    
    private void updateButtonsVisibility(String status) {
        if ("pending".equals(status)) {
            btnAccept.setVisibility(View.VISIBLE);
            btnReject.setVisibility(View.VISIBLE);
        } else {
            btnAccept.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
        }
    }
    
    private void acceptRequest() {
        if (serviceRequest == null) return;
        
        updateRequestStatus("accepted", "Solicitud aceptada exitosamente");
    }
    
    private void rejectRequest() {
        if (serviceRequest == null) return;
        
        updateRequestStatus("rejected", "Solicitud rechazada");
    }
    
    private void updateRequestStatus(String newStatus, String successMessage) {
        progressBar.setVisibility(View.VISIBLE);
        btnAccept.setEnabled(false);
        btnReject.setEnabled(false);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("last_updated", System.currentTimeMillis());
        
        databaseReference.child("service_requests").child(requestId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                    
                    // Update local object and UI
                    serviceRequest.setStatus(newStatus);
                    tvStatus.setText(serviceRequest.getStatusDisplayText());
                    updateButtonsVisibility(newStatus);
                    
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
