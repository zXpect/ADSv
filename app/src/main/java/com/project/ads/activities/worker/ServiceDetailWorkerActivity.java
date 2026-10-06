package com.project.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.project.ads.helpers.QuoteDialogHelper;
import com.project.ads.models.ServiceRequest;
import com.project.ads.providers.NotificationProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ServiceDetailWorkerActivity extends AppCompatActivity {

    private static final String TAG = "ServiceDetailWorker";

    // UI Components
    private ProgressBar mProgressBar;
    private LinearLayout mLayoutContent;
    private TextView mTvError;
    private ImageButton mBtnBack;

    // Service Info
    private TextView mTvRequestId;
    private TextView mTvStatus;
    private TextView mTvServiceType;
    private TextView mTvClientName;
    private TextView mTvClientPhone;
    private TextView mTvAddress;
    private TextView mTvDescription;
    private TextView mTvTimestamp;
    private ImageView mIvStatusIcon;

    // Action Buttons
    private CardView mCardActions;
    private Button mBtnStartTrip;
    private Button mBtnStartWork;
    private Button mBtnFinishWork;
    private Button mBtnCallClient;
    private Button mBtnViewMap;

    // Status Indicator
    private LinearLayout mLayoutStatusFlow;
    private View mStatusAccepted;
    private View mStatusOnTheWay;
    private View mStatusInProgress;
    private View mStatusCompleted;
    private View mLineAcceptedToWay;
    private View mLineWayToProgress;
    private View mLineProgressToCompleted;

    // Quote and Payment Card
    private CardView mCardQuote;
    private TextView mTvQuoteAmount;
    private TextView mTvQuoteStatus;
    private TextView mTvPaymentStatus;
    private Button mBtnSendQuote;

    // Firebase
    private DatabaseReference mDatabaseReference;
    private ValueEventListener mRequestListener;

    // Providers
    private NotificationProvider mNotificationProvider;

    // Data
    private String mRequestId;
    private ServiceRequest mServiceRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail_worker);

        // Get request ID
        mRequestId = getIntent().getStringExtra("request_id");

        if (mRequestId == null || mRequestId.isEmpty()) {
            Toast.makeText(this, "Error: ID de solicitud no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initProviders();
        initViews();
        setupStatusBar();
        loadRequestData();
    }

    private void initProviders() {
        mNotificationProvider = new NotificationProvider();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
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
        mTvClientName = findViewById(R.id.tv_client_name);
        mTvClientPhone = findViewById(R.id.tv_client_phone);
        mTvAddress = findViewById(R.id.tv_address);
        mTvDescription = findViewById(R.id.tv_description);
        mTvTimestamp = findViewById(R.id.tv_timestamp);
        mIvStatusIcon = findViewById(R.id.iv_status_icon);

        // Action Buttons
        mCardActions = findViewById(R.id.card_actions);
        mBtnStartTrip = findViewById(R.id.btn_start_trip);
        mBtnStartWork = findViewById(R.id.btn_start_work);
        mBtnFinishWork = findViewById(R.id.btn_finish_work);
        mBtnCallClient = findViewById(R.id.btn_call_client);
        mBtnViewMap = findViewById(R.id.btn_view_map);

        // Status Flow
        mLayoutStatusFlow = findViewById(R.id.layout_status_flow);
        mStatusAccepted = findViewById(R.id.status_accepted);
        mStatusOnTheWay = findViewById(R.id.status_on_the_way);
        mStatusInProgress = findViewById(R.id.status_in_progress);
        mStatusCompleted = findViewById(R.id.status_completed);
        mLineAcceptedToWay = findViewById(R.id.line_accepted_to_way);
        mLineWayToProgress = findViewById(R.id.line_way_to_progress);
        mLineProgressToCompleted = findViewById(R.id.line_progress_to_completed);

        // Quote and Payment Card
        mCardQuote = findViewById(R.id.card_quote);
        mTvQuoteAmount = findViewById(R.id.tv_quote_amount);
        mTvQuoteStatus = findViewById(R.id.tv_quote_status);
        mTvPaymentStatus = findViewById(R.id.tv_payment_status);
        mBtnSendQuote = findViewById(R.id.btn_quote_primary);

        // Setup click listeners
        mBtnBack.setOnClickListener(v -> finish());
        mBtnSendQuote.setOnClickListener(v -> showQuoteDialog());
        mBtnStartTrip.setOnClickListener(v -> confirmStatusChange("on_the_way"));
        mBtnStartWork.setOnClickListener(v -> confirmStatusChange("in_progress"));
        mBtnFinishWork.setOnClickListener(v -> confirmStatusChange("completed"));
        mBtnCallClient.setOnClickListener(v -> callClient());
        mBtnViewMap.setOnClickListener(v -> openMaps());
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
        mTvClientName.setText(request.getClient_name() != null ? request.getClient_name() : "Cliente");
        mTvClientPhone.setText(request.getClient_phone() != null ? request.getClient_phone() : "N/A");
        mTvAddress.setText(request.getAddress() != null ? request.getAddress() : "Sin dirección");
        mTvDescription.setText(request.getDescription() != null ? request.getDescription() : "Sin descripción");
        mTvTimestamp.setText(request.getTimeAgo());

        // Update status color
        mTvStatus.setTextColor(
                android.graphics.Color.parseColor(request.getStatusColor())
        );

        // Update status flow
        updateStatusFlow(request.getStatus());

        // Update quote/payment card and action buttons
        updateQuoteCard(request);
        updateActionButtons(request);

        // Update call button state
        mBtnCallClient.setEnabled(request.getClient_phone() != null && !request.getClient_phone().isEmpty());
    }

    private void updateStatusFlow(String status) {
        if (status == null) return;

        // Reset all
        resetStatusFlow();

        switch (status.toLowerCase()) {
            case "accepted":
                activateStatus(mStatusAccepted);
                break;
            case "on_the_way":
                activateStatus(mStatusAccepted);
                activateLine(mLineAcceptedToWay);
                activateStatus(mStatusOnTheWay);
                break;
            case "in_progress":
                activateStatus(mStatusAccepted);
                activateLine(mLineAcceptedToWay);
                activateStatus(mStatusOnTheWay);
                activateLine(mLineWayToProgress);
                activateStatus(mStatusInProgress);
                break;
            case "completed":
                activateStatus(mStatusAccepted);
                activateLine(mLineAcceptedToWay);
                activateStatus(mStatusOnTheWay);
                activateLine(mLineWayToProgress);
                activateStatus(mStatusInProgress);
                activateLine(mLineProgressToCompleted);
                activateStatus(mStatusCompleted);
                break;
        }
    }

    private void resetStatusFlow() {
        // Reset status circles
        deactivateStatus(mStatusAccepted);
        deactivateStatus(mStatusOnTheWay);
        deactivateStatus(mStatusInProgress);
        deactivateStatus(mStatusCompleted);

        // Reset lines
        deactivateLine(mLineAcceptedToWay);
        deactivateLine(mLineWayToProgress);
        deactivateLine(mLineProgressToCompleted);
    }

    private void activateStatus(View statusView) {
        statusView.setBackgroundResource(R.drawable.bg_status_active);
    }

    private void deactivateStatus(View statusView) {
        statusView.setBackgroundResource(R.drawable.bg_status_inactive);
    }

    private void activateLine(View lineView) {
        lineView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    }

    private void deactivateLine(View lineView) {
        lineView.setBackgroundColor(getResources().getColor(R.color.gray_light));
    }

    private void updateActionButtons(ServiceRequest request) {
        // Hide all buttons first
        mBtnStartTrip.setVisibility(View.GONE);
        mBtnStartWork.setVisibility(View.GONE);
        mBtnFinishWork.setVisibility(View.GONE);

        String status = request.getStatus();
        if (status == null) return;

        boolean hasAction = true;
        switch (status.toLowerCase()) {
            case "accepted":
                // Sin precio acordado no se puede iniciar (también lo exigen las reglas de Firebase)
                if (request.isQuoteAccepted()) {
                    mBtnStartTrip.setVisibility(View.VISIBLE);
                } else {
                    hasAction = false;
                }
                break;
            case "on_the_way":
                mBtnStartWork.setVisibility(View.VISIBLE);
                break;
            case "in_progress":
                mBtnFinishWork.setVisibility(View.VISIBLE);
                break;
            default:
                hasAction = false;
                break;
        }
        mCardActions.setVisibility(hasAction ? View.VISIBLE : View.GONE);
    }

    private void updateQuoteCard(ServiceRequest request) {
        String status = request.getStatus();
        boolean relevant = "accepted".equals(status) || "on_the_way".equals(status)
                || "in_progress".equals(status) || "completed".equals(status);
        mCardQuote.setVisibility(relevant ? View.VISIBLE : View.GONE);
        if (!relevant) return;

        mBtnSendQuote.setVisibility(View.GONE);
        mTvPaymentStatus.setVisibility(View.GONE);

        if (request.hasQuote()) {
            mTvQuoteAmount.setVisibility(View.VISIBLE);
            mTvQuoteAmount.setText(request.getFormattedQuotedPrice());
        } else {
            mTvQuoteAmount.setVisibility(View.GONE);
        }

        if (request.isQuoteAccepted()) {
            mTvQuoteStatus.setText(R.string.quote_status_agreed);
        } else if (request.isQuotePendingAnswer()) {
            mTvQuoteStatus.setText(R.string.quote_status_waiting_client);
        } else if (request.isQuoteRejected()) {
            String reason = request.getQuote_rejection_reason();
            mTvQuoteStatus.setText(reason != null && !reason.isEmpty()
                    ? getString(R.string.quote_status_rejected_reason, reason)
                    : getString(R.string.quote_status_rejected_worker));
            showSendQuoteButton(R.string.quote_send_new);
        } else {
            mTvQuoteStatus.setText(R.string.quote_status_missing_worker);
            showSendQuoteButton(R.string.quote_send);
        }

        if ("completed".equals(status)) {
            mTvPaymentStatus.setVisibility(View.VISIBLE);
            if (request.isPaid()) {
                mTvPaymentStatus.setText(R.string.payment_status_paid);
                mTvPaymentStatus.setTextColor(getResources().getColor(R.color.success_color));
            } else {
                mTvPaymentStatus.setText(R.string.payment_status_waiting_client);
                mTvPaymentStatus.setTextColor(getResources().getColor(R.color.warning_color));
            }
        }
    }

    private void showSendQuoteButton(int textRes) {
        // Solo mientras la solicitud está aceptada y sin precio acordado
        if (mServiceRequest != null && mServiceRequest.isAccepted()) {
            mBtnSendQuote.setText(textRes);
            mBtnSendQuote.setVisibility(View.VISIBLE);
        }
    }

    private void showQuoteDialog() {
        if (mServiceRequest == null) return;
        // El listener de Firebase actualiza la pantalla cuando el backend guarda la cotización
        new QuoteDialogHelper(this).show(mRequestId, mServiceRequest.getService_type(), null);
    }

    private void confirmStatusChange(String newStatus) {
        String title = "";
        String message = "";

        switch (newStatus) {
            case "on_the_way":
                title = "Iniciar Viaje";
                message = "¿Estás en camino hacia el cliente?";
                break;
            case "in_progress":
                title = "Iniciar Trabajo";
                message = "¿Has llegado y vas a comenzar el trabajo?";
                break;
            case "completed":
                title = "Finalizar Trabajo";
                message = "¿Has completado el servicio? El cliente podrá pagar y calificarte.";
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Sí, continuar", (dialog, which) -> updateStatus(newStatus))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateStatus(String newStatus) {
        if (mServiceRequest == null) return;

        mProgressBar.setVisibility(View.VISIBLE);
        disableActionButtons();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("last_updated", System.currentTimeMillis());

        // Add specific timestamps
        switch (newStatus) {
            case "on_the_way":
                updates.put("started_trip_at", System.currentTimeMillis());
                break;
            case "in_progress":
                updates.put("started_work_at", System.currentTimeMillis());
                break;
            case "completed":
                updates.put("completed_at", System.currentTimeMillis());
                break;
        }

        mDatabaseReference.child("requests").child(mRequestId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Estado actualizado a: " + newStatus);
                    sendStatusNotificationToClient(newStatus);
                    showSuccess(getSuccessMessage(newStatus));
                    mProgressBar.setVisibility(View.GONE);
                    enableActionButtons();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error actualizando estado", e);
                    Toast.makeText(this, "Error al actualizar el estado", Toast.LENGTH_SHORT).show();
                    mProgressBar.setVisibility(View.GONE);
                    enableActionButtons();
                });
    }

    private void sendStatusNotificationToClient(String status) {
        // El backend arma el mensaje según el estado actual de la solicitud
        mNotificationProvider.notify(mRequestId, NotificationProvider.TYPE_STATUS_CHANGE);
    }

    private String getSuccessMessage(String status) {
        switch (status) {
            case "on_the_way":
                return "Has marcado que vas en camino";
            case "in_progress":
                return "Has iniciado el trabajo";
            case "completed":
                return "¡Servicio completado! El cliente ya puede pagar";
            default:
                return "Estado actualizado";
        }
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void callClient() {
        if (mServiceRequest != null && mServiceRequest.getClient_phone() != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + mServiceRequest.getClient_phone()));
            startActivity(intent);
        }
    }

    private void openMaps() {
        if (mServiceRequest != null && mServiceRequest.getAddress() != null) {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(mServiceRequest.getAddress()));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Google Maps no está instalado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void disableActionButtons() {
        mBtnStartTrip.setEnabled(false);
        mBtnStartWork.setEnabled(false);
        mBtnFinishWork.setEnabled(false);
    }

    private void enableActionButtons() {
        mBtnStartTrip.setEnabled(true);
        mBtnStartWork.setEnabled(true);
        mBtnFinishWork.setEnabled(true);
    }

    private void showLoading(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mLayoutContent.setVisibility(show ? View.GONE : View.VISIBLE);
        mTvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        mProgressBar.setVisibility(View.GONE);
        mLayoutContent.setVisibility(View.GONE);
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