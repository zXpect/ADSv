package com.project.ads.activities.worker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import android.content.Intent;
import android.net.Uri;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.project.ads.R;
import com.project.ads.helpers.NotificationRouter;
import com.project.ads.helpers.CancelDialogHelper;
import com.project.ads.helpers.QuoteDialogHelper;
import com.project.ads.helpers.ReasonHelper;
import com.project.ads.helpers.RequestDetailsHelper;
import com.project.ads.models.api.CatalogData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.models.ServiceRequest;
import com.project.ads.providers.NotificationProvider;
import com.project.ads.helpers.DisplayNameHelper;
import com.project.ads.helpers.AdsSnackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Servicio del trabajador · mockups 23 (aceptada), 48 (en camino) y 49 (en progreso).
 * Los cambios de estado, la cotización, reprogramar y cancelar no cambian.
 */
public class ServiceDetailWorkerActivity extends AppCompatActivity {

    private static final String TAG = "ServiceDetailWorker";

    // UI Components
    private ProgressBar mProgressBar;
    private LinearLayout mLayoutContent;
    private TextView mTvError;
    private ImageButton mBtnBack;

    // Service Info
    private TextView mTvStatus;
    private TextView mTvClientName;
    private TextView mTvAddress;
    private TextView mTvDescription;
    private TextView mTvTimestamp;
    private TextView mTvStepNote;

    // Action Buttons
    private View mCardActions;
    private Button mBtnStartTrip;
    private Button mBtnStartWork;
    private Button mBtnFinishWork;
    private Button mBtnCallClient;
    private Button mBtnViewMap;

    // Pasos: Viaje · Trabajo · Finalizar
    private View[] mStepBars;
    private TextView[] mStepLabels;

    // Quote and Payment Card
    private View mCardQuote;
    private TextView mTvQuoteAmount;
    private TextView mTvQuoteStatus;
    private TextView mTvPaymentStatus;
    private Button mBtnSendQuote;
    private TextView mTvQuoteDetails;
    private Button mBtnReschedule;
    private Button mBtnCancelService;
    private CatalogData mCatalog;   // nombres de las tareas; puede no estar cargado

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
        NotificationRouter.markOpened(this, mRequestId);

        if (mRequestId == null || mRequestId.isEmpty()) {
            Toast.makeText(this, "Error: ID de solicitud no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initProviders();
        initViews();
        loadRequestData();
        loadCatalog();
    }

    private void loadCatalog() {
        new CatalogProvider().getCatalog(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData catalog) {
                mCatalog = catalog;
                if (mServiceRequest != null && !isFinishing()) {
                    updateQuoteCard(mServiceRequest);
                    updateActionButtons(mServiceRequest);
                }
            }

            @Override
            public void onError(String message, int httpStatus) {
                // Sin catálogo las tareas se muestran por su identificador
            }
        });
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
        mTvStatus = findViewById(R.id.tv_status);
        mTvClientName = findViewById(R.id.tv_client_name);
        mTvAddress = findViewById(R.id.tv_address);
        mTvDescription = findViewById(R.id.tv_description);
        mTvTimestamp = findViewById(R.id.tv_timestamp);
        mTvStepNote = findViewById(R.id.tv_step_note);

        // Action Buttons
        mCardActions = findViewById(R.id.card_actions);
        mBtnStartTrip = findViewById(R.id.btn_start_trip);
        mBtnStartWork = findViewById(R.id.btn_start_work);
        mBtnFinishWork = findViewById(R.id.btn_finish_work);
        mBtnCallClient = findViewById(R.id.btn_call_client);
        mBtnViewMap = findViewById(R.id.btn_view_map);

        mStepBars = new View[]{findViewById(R.id.status_on_the_way),
                findViewById(R.id.status_in_progress), findViewById(R.id.status_completed)};
        mStepLabels = new TextView[]{findViewById(R.id.tv_step_trip),
                findViewById(R.id.tv_step_work), findViewById(R.id.tv_step_finish)};

        // Quote and Payment Card
        mCardQuote = findViewById(R.id.card_quote);
        mTvQuoteAmount = findViewById(R.id.tv_quote_amount);
        mTvQuoteStatus = findViewById(R.id.tv_quote_status);
        mTvPaymentStatus = findViewById(R.id.tv_payment_status);
        mBtnSendQuote = findViewById(R.id.btn_quote_primary);
        mTvQuoteDetails = findViewById(R.id.tv_quote_details);
        mBtnReschedule = findViewById(R.id.btn_quote_reschedule);
        mBtnCancelService = findViewById(R.id.btn_quote_cancel);
        mBtnReschedule.setOnClickListener(v -> showRescheduleDialog());
        mBtnCancelService.setOnClickListener(v -> showCancelDialog());

        // Setup click listeners
        mBtnBack.setOnClickListener(v -> finish());
        mBtnSendQuote.setOnClickListener(v -> showQuoteDialog());
        mBtnStartTrip.setOnClickListener(v -> confirmStatusChange("on_the_way"));
        mBtnStartWork.setOnClickListener(v -> confirmStatusChange("in_progress"));
        mBtnFinishWork.setOnClickListener(v -> confirmStatusChange("completed"));
        findViewById(R.id.btn_requote).setOnClickListener(v -> {
            if (mServiceRequest == null) return;
            startActivity(RequoteActivity.intent(this, mRequestId, mServiceRequest.getQuoted_price()));
        });
        findViewById(R.id.btn_register_cash).setOnClickListener(v -> {
            if (mServiceRequest == null) return;
            startActivity(CashReceivedActivity.intent(this, mRequestId,
                    mServiceRequest.getFormattedQuotedPrice(), mServiceRequest.getClient_name()));
        });
        mBtnCallClient.setOnClickListener(v -> callClient());
        mBtnViewMap.setOnClickListener(v -> openMaps());
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
        String status = request.getStatus() != null ? request.getStatus().toLowerCase() : "";
        mTvClientName.setText(request.getClient_name() != null
                ? DisplayNameHelper.format(request.getClient_name()) : getString(R.string.inbox_client_default));
        mTvAddress.setText(request.getAddress() != null
                ? request.getAddress() : getString(R.string.service_worker_no_address));
        String description = request.getDescription();
        mTvDescription.setVisibility(description != null && !description.trim().isEmpty() ? View.VISIBLE : View.GONE);
        mTvDescription.setText(description);
        mTvTimestamp.setText(timeLine(request, status));

        // Chip: naranja mientras está aceptada; navy en camino, en progreso o completada
        int label;
        switch (status) {
            case "accepted": label = R.string.request_status_accepted; break;
            case "on_the_way": label = R.string.request_status_on_the_way; break;
            case "in_progress": label = R.string.request_status_in_progress; break;
            case "completed": label = R.string.request_status_completed; break;
            case "cancelled": label = R.string.request_status_cancelled; break;
            default: label = 0;
        }
        if (label != 0) mTvStatus.setText(label); else mTvStatus.setText(request.getStatusDisplayText());
        boolean orange = "accepted".equals(status);
        ViewCompat.setBackgroundTintList(mTvStatus, ColorStateList.valueOf(ContextCompat.getColor(this,
                orange ? R.color.ads_chip_bg : R.color.ads_chip_strong_bg)));
        mTvStatus.setTextColor(ContextCompat.getColor(this, orange ? R.color.ads_chip_text : R.color.ads_chip_strong_text));

        updateStatusFlow(status);
        updateStepNote(request, status);

        // Update quote/payment card and action buttons
        updateQuoteCard(request);
        updateActionButtons(request);

        // Update call button state
        mBtnCallClient.setEnabled(request.getClient_phone() != null && !request.getClient_phone().isEmpty());
        mBtnViewMap.setEnabled(request.getAddress() != null);
    }

    /** "Iniciado 3:05 p. m. · 2 h estimadas" en progreso; si no, la agenda o hace cuánto llegó. */
    private String timeLine(ServiceRequest request, String status) {
        if ("in_progress".equals(status) && request.getStarted_work_at() > 0) {
            String time = new SimpleDateFormat("h:mm a", new Locale("es", "CO")).format(new Date(request.getStarted_work_at()));
            return request.getEstimatedDurationMin() > 0
                    ? getString(R.string.service_worker_started_estimate, time,
                    ServiceRequest.formatDuration(request.getEstimatedDurationMin()))
                    : getString(R.string.service_worker_started, time);
        }
        if (request.hasSchedule()) return request.getScheduleDisplay();
        return request.getTimeAgo();
    }

    /** Paso actual en naranja; hechos en naranja oscuro; pendientes en gris. */
    private void updateStatusFlow(String status) {
        int current;
        switch (status) {
            case "accepted": current = 0; break;
            case "on_the_way": current = 1; break;
            case "in_progress": current = 2; break;
            case "completed": current = 3; break;
            default: current = -1;
        }
        findViewById(R.id.layout_status_flow).setVisibility(current >= 0 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < mStepBars.length; i++) {
            boolean done = i < current;
            boolean now = i == current;
            mStepBars[i].setBackgroundResource(done || now ? R.drawable.bg_ads_progress_on : R.drawable.bg_ads_progress_off);
            ViewCompat.setBackgroundTintList(mStepBars[i], done
                    ? ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ads_brand_text)) : null);
            androidx.core.widget.TextViewCompat.setTextAppearance(mStepLabels[i], now
                    ? R.style.TextAppearance_ADS_Label : R.style.TextAppearance_ADS_Caption);
            // Actual en fuerte, hechos en normal, pendientes apagados
            mStepLabels[i].setTextColor(ContextCompat.getColor(this,
                    now ? R.color.ads_text : done ? R.color.ads_charcoal : R.color.ads_muted));
        }
    }

    private void updateStepNote(ServiceRequest request, String status) {
        int note = 0;
        if ("accepted".equals(status)) {
            note = request.isQuoteAccepted() ? R.string.service_worker_note_accepted : R.string.service_worker_note_waiting;
        } else if ("on_the_way".equals(status) || "in_progress".equals(status)) {
            if (request.hasPendingRequote()) {
                note = R.string.requote_worker_waiting;
            } else if (request.canRequote()) {
                note = R.string.requote_worker_hint;
            } else if (request.getRequote() != null && request.getRequote().isRejected()) {
                note = R.string.requote_worker_rejected;
            } else {
                note = R.string.service_worker_note_no_cancel;
            }
        }
        mTvStepNote.setVisibility(note != 0 ? View.VISIBLE : View.GONE);
        if (note != 0) mTvStepNote.setText(note);
    }

    private void updateActionButtons(ServiceRequest request) {
        // Hide all buttons first
        mBtnStartTrip.setVisibility(View.GONE);
        mBtnStartWork.setVisibility(View.GONE);
        mBtnFinishWork.setVisibility(View.GONE);
        View registerCash = findViewById(R.id.btn_register_cash);
        registerCash.setVisibility(View.GONE);
        View requote = findViewById(R.id.btn_requote);
        requote.setVisibility(request.canRequote() ? View.VISIBLE : View.GONE);

        String status = request.getStatus();
        if (status != null) {
            switch (status.toLowerCase()) {
                case "accepted":
                    // Sin precio acordado no se puede iniciar (también lo exigen las reglas de Firebase)
                    if (request.isQuoteAccepted()) mBtnStartTrip.setVisibility(View.VISIBLE);
                    break;
                case "on_the_way":
                    mBtnStartWork.setVisibility(View.VISIBLE);
                    break;
                case "in_progress":
                    // Con una re-cotización pendiente las reglas no dejan finalizar
                    if (!request.hasPendingRequote()) mBtnFinishWork.setVisibility(View.VISIBLE);
                    break;
                case "completed":
                    if (request.isCashPayment() && !request.isPaid()) registerCash.setVisibility(View.VISIBLE);
                    break;
            }
        }
        // Cancelar vive en la barra de acción (visible antes de «Iniciar viaje»)
        boolean hasAction = mBtnStartTrip.getVisibility() == View.VISIBLE
                || mBtnStartWork.getVisibility() == View.VISIBLE
                || mBtnFinishWork.getVisibility() == View.VISIBLE
                || registerCash.getVisibility() == View.VISIBLE
                || requote.getVisibility() == View.VISIBLE
                || mBtnCancelService.getVisibility() == View.VISIBLE;
        mCardActions.setVisibility(hasAction ? View.VISIBLE : View.GONE);
        // Sin botón principal, Cancelar no necesita separación arriba
        ((android.view.ViewGroup.MarginLayoutParams) mBtnCancelService.getLayoutParams()).topMargin =
                hasAction && mBtnCancelService.getVisibility() == View.VISIBLE
                        && mBtnStartTrip.getVisibility() == View.GONE ? 0
                        : Math.round(10 * getResources().getDisplayMetrics().density);
    }

    private void updateQuoteCard(ServiceRequest request) {
        String status = request.getStatus();
        boolean relevant = "accepted".equals(status) || "on_the_way".equals(status)
                || "in_progress".equals(status) || "completed".equals(status);
        mCardQuote.setVisibility(relevant ? View.VISIBLE : View.GONE);
        if (!relevant) return;

        mBtnSendQuote.setVisibility(View.GONE);
        mTvPaymentStatus.setVisibility(View.GONE);

        TextView paymentMethod = findViewById(R.id.tv_payment_method);
        paymentMethod.setVisibility(request.isQuoteAccepted() ? View.VISIBLE : View.GONE);
        paymentMethod.setText(request.isCashPayment()
                ? R.string.payment_method_worker_cash : R.string.payment_method_worker_digital);

        String details = RequestDetailsHelper.details(this, request, mCatalog, false);
        mTvQuoteDetails.setVisibility(details != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_quote_details_label).setVisibility(details != null ? View.VISIBLE : View.GONE);
        mTvQuoteDetails.setText(details);
        // Antes de «Iniciar viaje» el trabajador puede reprogramar o cancelar con motivo (D8, D9)
        mBtnReschedule.setVisibility(request.canBeRescheduled() ? View.VISIBLE : View.GONE);
        mBtnCancelService.setVisibility(request.canBeCancelled() ? View.VISIBLE : View.GONE);

        if (request.hasQuote()) {
            mTvQuoteAmount.setVisibility(View.VISIBLE);
            mTvQuoteAmount.setText(request.getFormattedQuotedPrice());
        } else {
            mTvQuoteAmount.setVisibility(View.GONE);
        }
        // El bloque navy del precio solo se muestra cuando hay precio
        findViewById(R.id.layout_quote_price).setVisibility(request.hasQuote() ? View.VISIBLE : View.GONE);

        // Con precio acordado el bloque navy ya lo dice
        mTvQuoteStatus.setVisibility(request.isQuoteAccepted() ? View.GONE : View.VISIBLE);
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
                mTvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.ads_text));
            } else if (request.isCashPayment()) {
                mTvPaymentStatus.setText(R.string.payment_status_cash_worker);
                mTvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.ads_brand_text));
            } else {
                mTvPaymentStatus.setText(R.string.payment_status_waiting_client);
                mTvPaymentStatus.setTextColor(ContextCompat.getColor(this, R.color.ads_brand_text));
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
        new QuoteDialogHelper(this).show(mServiceRequest, null);
    }

    private void showRescheduleDialog() {
        if (mServiceRequest == null) return;
        // El listener de Firebase muestra la nueva agenda; el backend avisa al cliente
        new QuoteDialogHelper(this).showReschedule(mServiceRequest, null);
    }

    private void showCancelDialog() {
        if (mServiceRequest == null) return;
        new CancelDialogHelper(this).show(mRequestId, ReasonHelper.ROLE_WORKER, data -> finish());
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
                    AdsSnackbar.show(this, R.string.error_update_status);
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
        AdsSnackbar.showLong(this, message);
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
                AdsSnackbar.show(this, R.string.error_no_maps);
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