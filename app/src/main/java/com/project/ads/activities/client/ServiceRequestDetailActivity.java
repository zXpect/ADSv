package com.project.ads.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.project.ads.R;
import com.project.ads.models.ServiceRequest;
import com.project.ads.providers.RequestProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class ServiceRequestDetailActivity extends AppCompatActivity {

    private static final String TAG = "ServiceRequestDetail";

    // UI Components
    private ProgressBar mProgressBar;
    private LinearLayout mLayoutContent;
    private TextView mTvError;
    private ImageButton mBtnBack;
    private RequestProvider mRequestProvider;

    // Service Info
    private TextView mTvRequestId;
    private TextView mTvStatus;
    private TextView mTvServiceType;
    private TextView mTvWorkerName;
    private TextView mTvWorkerPhone;
    private TextView mTvAddress;
    private TextView mTvDescription;
    private TextView mTvTimestamp;
    private ImageView mIvStatusIcon;

    // Status Flow
    private LinearLayout mLayoutStatusFlow;
    private View mStatusPending;
    private View mStatusAccepted;
    private View mStatusOnTheWay;
    private View mStatusInProgress;
    private View mStatusCompleted;
    private View mLinePendingToAccepted;
    private View mLineAcceptedToWay;
    private View mLineWayToProgress;
    private View mLineProgressToCompleted;
    private TextView mTvStatusPending;
    private TextView mTvStatusAccepted;
    private TextView mTvStatusOnTheWay;
    private TextView mTvStatusInProgress;
    private TextView mTvStatusCompleted;

    // Action Buttons
    private CardView mCardActions;
    private Button mBtnCallWorker;
    private Button mBtnCancelRequest;
    private Button mBtnRateService;

    // Firebase
    private DatabaseReference mDatabaseReference;
    private ValueEventListener mRequestListener;

    // Data
    private String mRequestId;
    private ServiceRequest mServiceRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_request_detail);

        // Get request ID
        mRequestId = getIntent().getStringExtra("request_id");

        if (mRequestId == null || mRequestId.isEmpty()) {
            Toast.makeText(this, "Error: ID de solicitud no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupStatusBar();
        mRequestProvider = new RequestProvider(this);
        loadRequestData();

    }
    


    private void initViews() {
        // Loading and Error
        mProgressBar = findViewById(R.id.progress_bar);
        mLayoutContent = findViewById(R.id.layout_content);
        mTvError = findViewById(R.id.tv_error);
        mBtnBack = findViewById(R.id.btn_back);

        // Service Info
        mTvRequestId = findViewById(R.id.tv_request_id);
        mTvStatus = findViewById(R.id.tv_status);
        mTvServiceType = findViewById(R.id.tv_service_type);
        mTvWorkerName = findViewById(R.id.tv_worker_name);
        mTvWorkerPhone = findViewById(R.id.tv_worker_phone);
        mTvAddress = findViewById(R.id.tv_address);
        mTvDescription = findViewById(R.id.tv_description);
        mTvTimestamp = findViewById(R.id.tv_timestamp);
        mIvStatusIcon = findViewById(R.id.iv_status_icon);

        // Status Flow
        mLayoutStatusFlow = findViewById(R.id.layout_status_flow);
        mStatusPending = findViewById(R.id.status_pending);
        mStatusAccepted = findViewById(R.id.status_accepted);
        mStatusOnTheWay = findViewById(R.id.status_on_the_way);
        mStatusInProgress = findViewById(R.id.status_in_progress);
        mStatusCompleted = findViewById(R.id.status_completed);
        mLinePendingToAccepted = findViewById(R.id.line_pending_to_accepted);
        mLineAcceptedToWay = findViewById(R.id.line_accepted_to_way);
        mLineWayToProgress = findViewById(R.id.line_way_to_progress);
        mLineProgressToCompleted = findViewById(R.id.line_progress_to_completed);
        mTvStatusPending = findViewById(R.id.tv_status_pending);
        mTvStatusAccepted = findViewById(R.id.tv_status_accepted);
        mTvStatusOnTheWay = findViewById(R.id.tv_status_on_the_way);
        mTvStatusInProgress = findViewById(R.id.tv_status_in_progress);
        mTvStatusCompleted = findViewById(R.id.tv_status_completed);

        // Action Buttons
        mCardActions = findViewById(R.id.card_actions);
        mBtnCallWorker = findViewById(R.id.btn_call_worker);
        mBtnCancelRequest = findViewById(R.id.btn_cancel_request);
        mBtnRateService = findViewById(R.id.btn_rate_service);

        // Setup click listeners
        mBtnBack.setOnClickListener(v -> finish());
        mBtnCallWorker.setOnClickListener(v -> callWorker());
        mBtnCancelRequest.setOnClickListener(v -> cancelRequest());
        mBtnRateService.setOnClickListener(v -> openRatingActivity());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    private void loadRequestData() {
        showLoading(true);
        Log.d(TAG, "Cargando datos de solicitud: " + mRequestId);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // Remove previous listener if exists
        if (mRequestListener != null) {
            mDatabaseReference.child("requests").child(mRequestId).removeEventListener(mRequestListener);
        }

        mRequestListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    try {
                        Map<String, Object> requestData = (Map<String, Object>) dataSnapshot.getValue();
                        if (requestData != null) {
                            mServiceRequest = ServiceRequest.fromMap(requestData);
                            mServiceRequest.setRequest_id(mRequestId);
                            populateUI(mServiceRequest);
                            showLoading(false);
                        } else {
                            showError("Error al cargar los datos");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parseando solicitud", e);
                        showError("Error al procesar los datos");
                    }
                } else {
                    showError("Solicitud no encontrada");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error de base de datos: " + databaseError.getMessage());
                showError("Error de conexión: " + databaseError.getMessage());
            }
        };

        mDatabaseReference.child("requests").child(mRequestId)
                .addValueEventListener(mRequestListener);
    }

    private void populateUI(ServiceRequest request) {
        // Basic Info
        mTvRequestId.setText("Solicitud #" + mRequestId.substring(0, Math.min(8, mRequestId.length())));
        mTvStatus.setText(request.getStatusDisplayText());
        mTvServiceType.setText(request.getService_type() != null ? request.getService_type() : "N/A");
        mTvWorkerName.setText(request.getWorker_name() != null ? request.getWorker_name() : "Trabajador");
        mTvWorkerPhone.setText(request.getWorker_phone() != null ? request.getWorker_phone() : "N/A");
        mTvAddress.setText(request.getAddress() != null ? request.getAddress() : "Sin dirección");
        mTvDescription.setText(request.getDescription() != null ? request.getDescription() : "Sin descripción");
        mTvTimestamp.setText(request.getTimeAgo());

        // Update status color
        mTvStatus.setTextColor(
                android.graphics.Color.parseColor(request.getStatusColor())
        );

        // Update status flow
        updateStatusFlow(request.getStatus());

        // Update action buttons visibility
        updateActionButtons(request.getStatus(), request.getRating() > 0); // ← CAMBIO AQUÍ

        // Update call button state
        mBtnCallWorker.setEnabled(request.getWorker_phone() != null && !request.getWorker_phone().isEmpty());
    }
    private void updateStatusFlow(String status) {
        if (status == null) return;

        // Reset all
        resetStatusFlow();

        switch (status.toLowerCase()) {
            case "pending":
                activateStatus(mStatusPending, mTvStatusPending);
                break;
            case "accepted":
                activateStatus(mStatusPending, mTvStatusPending);
                activateLine(mLinePendingToAccepted);
                activateStatus(mStatusAccepted, mTvStatusAccepted);
                break;
            case "on_the_way":
                activateStatus(mStatusPending, mTvStatusPending);
                activateLine(mLinePendingToAccepted);
                activateStatus(mStatusAccepted, mTvStatusAccepted);
                activateLine(mLineAcceptedToWay);
                activateStatus(mStatusOnTheWay, mTvStatusOnTheWay);
                break;
            case "in_progress":
                activateStatus(mStatusPending, mTvStatusPending);
                activateLine(mLinePendingToAccepted);
                activateStatus(mStatusAccepted, mTvStatusAccepted);
                activateLine(mLineAcceptedToWay);
                activateStatus(mStatusOnTheWay, mTvStatusOnTheWay);
                activateLine(mLineWayToProgress);
                activateStatus(mStatusInProgress, mTvStatusInProgress);
                break;
            case "completed":
                activateStatus(mStatusPending, mTvStatusPending);
                activateLine(mLinePendingToAccepted);
                activateStatus(mStatusAccepted, mTvStatusAccepted);
                activateLine(mLineAcceptedToWay);
                activateStatus(mStatusOnTheWay, mTvStatusOnTheWay);
                activateLine(mLineWayToProgress);
                activateStatus(mStatusInProgress, mTvStatusInProgress);
                activateLine(mLineProgressToCompleted);
                activateStatus(mStatusCompleted, mTvStatusCompleted);
                break;
        }
    }

    private void resetStatusFlow() {
        // Reset status circles
        deactivateStatus(mStatusPending, mTvStatusPending);
        deactivateStatus(mStatusAccepted, mTvStatusAccepted);
        deactivateStatus(mStatusOnTheWay, mTvStatusOnTheWay);
        deactivateStatus(mStatusInProgress, mTvStatusInProgress);
        deactivateStatus(mStatusCompleted, mTvStatusCompleted);

        // Reset lines
        deactivateLine(mLinePendingToAccepted);
        deactivateLine(mLineAcceptedToWay);
        deactivateLine(mLineWayToProgress);
        deactivateLine(mLineProgressToCompleted);
    }

    private void activateStatus(View statusView, TextView textView) {
        statusView.setBackgroundResource(R.drawable.bg_status_active);
        textView.setTextColor(getResources().getColor(R.color.colorPrimary));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void deactivateStatus(View statusView, TextView textView) {
        statusView.setBackgroundResource(R.drawable.bg_status_inactive);
        textView.setTextColor(getResources().getColor(R.color.text_secondary));
        textView.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    private void activateLine(View lineView) {
        lineView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    }

    private void deactivateLine(View lineView) {
        lineView.setBackgroundColor(getResources().getColor(R.color.gray_light));
    }

    private void updateActionButtons(String status, boolean isRated) {
        // Hide all buttons first
        mBtnCallWorker.setVisibility(View.VISIBLE);
        mBtnCancelRequest.setVisibility(View.GONE);
        mBtnRateService.setVisibility(View.GONE);

        if (status == null) return;

        switch (status.toLowerCase()) {
            case "pending":
                mBtnCancelRequest.setVisibility(View.VISIBLE);
                mBtnCallWorker.setVisibility(View.GONE);
                break;
            case "accepted":
            case "on_the_way":
            case "in_progress":
                mBtnCallWorker.setVisibility(View.VISIBLE);
                break;
            case "completed":
                mBtnCallWorker.setVisibility(View.GONE);
                // Siempre mostrar el botón de calificar en "completed"
                mBtnRateService.setVisibility(View.VISIBLE);
                mBtnRateService.setEnabled(true);

                // Si ya está calificado, cambiar el texto del botón
                if (isRated) {
                    mBtnRateService.setText("Ver Calificación");
                } else {
                    mBtnRateService.setText("Calificar Servicio");
                }
                break;
            case "cancelled":
            case "rejected":
                mCardActions.setVisibility(View.GONE);
                break;
        }
    }
    private void callWorker() {
        if (mServiceRequest != null && mServiceRequest.getWorker_phone() != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + mServiceRequest.getWorker_phone()));
            startActivity(intent);
        }
    }

    private void cancelRequest() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancelar Solicitud")
                .setMessage("¿Estás seguro de que deseas cancelar esta solicitud?")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> {
                    mProgressBar.setVisibility(View.VISIBLE);

                    Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("status", "cancelled");
                    updates.put("cancelled_at", System.currentTimeMillis());

                    mDatabaseReference.child("requests").child(mRequestId)
                            .updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                mProgressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Solicitud cancelada", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                mProgressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Error al cancelar", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void openRatingActivity() {
        if (mServiceRequest == null) return;
        checkIfAlreadyRated();
    }

    private void checkIfAlreadyRated() {
        // Mostrar loading mientras verificamos
        mProgressBar.setVisibility(View.VISIBLE);

        mRequestProvider.getRequest(mRequestId).addOnSuccessListener(dataSnapshot -> {
            mProgressBar.setVisibility(View.GONE);

            if (dataSnapshot.exists()) {
                Boolean isRated = dataSnapshot.child("is_rated").getValue(Boolean.class);
                Double rating = dataSnapshot.child("client_rating").getValue(Double.class);

                if (isRated != null && isRated && rating != null && rating > 0) {
                    // Ya fue calificado, mostrar mensaje
                    String comment = dataSnapshot.child("client_comment").getValue(String.class);
                    showAlreadyRatedDialog(rating.floatValue(), comment);
                } else {
                    // No ha sido calificado, abrir pantalla de calificación
                    openRatingScreen();
                }
            } else {
                Toast.makeText(this, "Error al verificar estado", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            mProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error verificando calificación", e);
        });
    }

    private void openRatingScreen() {
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("request_id", mRequestId);
        intent.putExtra("worker_id", mServiceRequest.getWorker_id());
        intent.putExtra("worker_name", mServiceRequest.getWorker_name());
        intent.putExtra("service_type", mServiceRequest.getService_type());
        startActivity(intent);
    }

    private void showAlreadyRatedDialog(float rating, String comment) {
        String ratingText = String.format("%.1f", rating);
        String message = "Ya calificaste este servicio con " + ratingText + " estrellas";

        if (comment != null && !comment.isEmpty()) {
            message += "\n\nTu comentario:\n\"" + comment + "\"";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Ya Calificado")
                .setMessage(message)
                .setPositiveButton("Aceptar", null)
                .setIcon(R.drawable.ic_star)
                .show();
    }
    private void showLoading(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mLayoutContent.setVisibility(show ? View.GONE : View.VISIBLE);

        // Ocultar el contenedor de error
        LinearLayout errorContainer = findViewById(R.id.error_container);
        if (errorContainer != null) {
            errorContainer.setVisibility(View.GONE);
        }
        mTvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        mProgressBar.setVisibility(View.GONE);
        mLayoutContent.setVisibility(View.GONE);

        // Mostrar el contenedor de error completo
        LinearLayout errorContainer = findViewById(R.id.error_container);
        if (errorContainer != null) {
            errorContainer.setVisibility(View.VISIBLE);
        }

        // Actualizar el mensaje de error
        mTvError.setVisibility(View.VISIBLE);
        mTvError.setText(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRequestListener != null && mDatabaseReference != null) {
            mDatabaseReference.child("requests").child(mRequestId).removeEventListener(mRequestListener);
        }
    }
}