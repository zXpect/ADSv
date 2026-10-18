package com.project.ads.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.project.ads.R;
import com.project.ads.helpers.CancelDialogHelper;
import com.project.ads.helpers.ReasonHelper;
import com.project.ads.models.api.QuoteReference;
import com.project.ads.helpers.RequestDetailsHelper;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.api.CatalogData;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.models.api.QuoteData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.QuoteProvider;
import com.project.ads.providers.RequestProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ServiceRequestDetailActivity extends AppCompatActivity {

    private static final String TAG = "ServiceRequestDetail";

    // UI Components
    private ProgressBar mProgressBar;
    private LinearLayout mLayoutContent;
    private TextView mTvError;
    private ImageButton mBtnBack;
    private RequestProvider mRequestProvider;
    private QuoteProvider mQuoteProvider;

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
    private Button mBtnPayService;

    // Quote and Payment Card
    private CardView mCardQuote;
    private TextView mTvQuoteAmount;
    private TextView mTvQuoteStatus;
    private TextView mTvPaymentStatus;
    private Button mBtnAcceptQuote;
    private Button mBtnRejectQuote;
    private TextView mTvQuoteDetails;
    private CatalogData mCatalog;   // nombres de las tareas; puede no estar cargado

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
        mQuoteProvider = new QuoteProvider();
        loadRequestData();
        loadCatalog();

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
        mBtnPayService = findViewById(R.id.btn_pay_service);

        // Quote and Payment Card
        mCardQuote = findViewById(R.id.card_quote);
        mTvQuoteAmount = findViewById(R.id.tv_quote_amount);
        mTvQuoteStatus = findViewById(R.id.tv_quote_status);
        mTvPaymentStatus = findViewById(R.id.tv_payment_status);
        mBtnAcceptQuote = findViewById(R.id.btn_quote_primary);
        mBtnRejectQuote = findViewById(R.id.btn_quote_secondary);
        mTvQuoteDetails = findViewById(R.id.tv_quote_details);
        mBtnAcceptQuote.setText(R.string.quote_accept);
        mBtnRejectQuote.setText(R.string.quote_reject);

        // Setup click listeners
        mBtnBack.setOnClickListener(v -> finish());
        mBtnCallWorker.setOnClickListener(v -> callWorker());
        mBtnCancelRequest.setOnClickListener(v -> cancelRequest());
        mBtnRateService.setOnClickListener(v -> openRatingActivity());
        mBtnPayService.setOnClickListener(v -> openPayment());
        mBtnAcceptQuote.setOnClickListener(v -> confirmAcceptQuote());
        mBtnRejectQuote.setOnClickListener(v -> showRejectQuoteDialog());
    }

    private void loadCatalog() {
        new CatalogProvider().getCatalog(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData catalog) {
                mCatalog = catalog;
                if (mServiceRequest != null && !isFinishing()) updateQuoteCard(mServiceRequest);
            }

            @Override
            public void onError(String message, int httpStatus) {
                // Sin catálogo las tareas se muestran por su identificador
            }
        });
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

        // Update quote/payment card and action buttons
        updateQuoteCard(request);
        updateActionButtons(request);

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

    private void updateActionButtons(ServiceRequest request) {
        // Hide all buttons first
        mCardActions.setVisibility(View.VISIBLE);
        mBtnCallWorker.setVisibility(View.VISIBLE);
        mBtnCancelRequest.setVisibility(View.GONE);
        mBtnRateService.setVisibility(View.GONE);
        mBtnPayService.setVisibility(View.GONE);

        String status = request.getStatus();
        if (status == null) return;

        // Se puede cancelar, con motivo, hasta que el trabajador inicie el viaje (D8)
        if (request.canBeCancelled()) {
            mBtnCancelRequest.setVisibility(View.VISIBLE);
        }

        switch (status.toLowerCase()) {
            case "pending":
                mBtnCallWorker.setVisibility(View.GONE);
                break;
            case "accepted":
            case "on_the_way":
            case "in_progress":
                mBtnCallWorker.setVisibility(View.VISIBLE);
                break;
            case "completed":
                mBtnCallWorker.setVisibility(View.GONE);
                if (request.isPaid()) {
                    // La calificación se habilita después del pago
                    mBtnRateService.setVisibility(View.VISIBLE);
                    mBtnRateService.setText(request.isRated() ? "Ver Calificación" : "Calificar Servicio");
                } else if (request.isQuoteAccepted()) {
                    mBtnPayService.setVisibility(View.VISIBLE);
                }
                break;
            case "cancelled":
            case "rejected":
                mCardActions.setVisibility(View.GONE);
                break;
        }
    }

    private void updateQuoteCard(ServiceRequest request) {
        String status = request.getStatus();
        boolean withQuote = "accepted".equals(status) || "on_the_way".equals(status)
                || "in_progress".equals(status) || "completed".equals(status);
        // Con precio, la referencia, el motivo y la agenda van en sus propias filas (mockup 17)
        boolean showPrice = withQuote && request.hasQuote();
        String details = RequestDetailsHelper.details(this, request, mCatalog, !showPrice);
        bindQuoteBreakdown(request, showPrice);
        // En pending y cancelled la tarjeta muestra solo las tareas, la referencia y la cancelación
        boolean relevant = withQuote || (details != null && ("pending".equals(status) || "cancelled".equals(status)));
        mCardQuote.setVisibility(relevant ? View.VISIBLE : View.GONE);
        if (!relevant) return;

        mTvQuoteDetails.setVisibility(details != null ? View.VISIBLE : View.GONE);
        mTvQuoteDetails.setText(details);
        mBtnAcceptQuote.setVisibility(View.GONE);
        mBtnRejectQuote.setVisibility(View.GONE);
        mTvPaymentStatus.setVisibility(View.GONE);
        if (!withQuote) {
            mTvQuoteAmount.setVisibility(View.GONE);
            mTvQuoteStatus.setText("pending".equals(status) ? getString(R.string.quote_status_missing_client) : "");
            return;
        }

        if (request.hasQuote()) {
            mTvQuoteAmount.setVisibility(View.VISIBLE);
            mTvQuoteAmount.setText(request.getFormattedQuotedPrice());
        } else {
            mTvQuoteAmount.setVisibility(View.GONE);
        }

        if (request.isQuoteAccepted()) {
            mTvQuoteStatus.setText(R.string.quote_status_agreed);
        } else if (request.isQuotePendingAnswer()) {
            mTvQuoteStatus.setText(R.string.quote_status_answer_client);
            mBtnAcceptQuote.setVisibility(View.VISIBLE);
            mBtnRejectQuote.setVisibility(View.VISIBLE);
        } else if (request.isQuoteRejected()) {
            mTvQuoteStatus.setText(R.string.quote_status_rejected_client);
        } else {
            mTvQuoteStatus.setText(R.string.quote_status_missing_client);
        }

        if ("completed".equals(status)) {
            mTvPaymentStatus.setVisibility(View.VISIBLE);
            if (request.isPaid()) {
                mTvPaymentStatus.setText(R.string.payment_status_paid);
                mTvPaymentStatus.setTextColor(getResources().getColor(R.color.success_color));
            } else if (request.isPaymentPending()) {
                mTvPaymentStatus.setText(R.string.payment_status_in_process);
                mTvPaymentStatus.setTextColor(getResources().getColor(R.color.warning_color));
            } else {
                mTvPaymentStatus.setText(R.string.payment_status_pending_client);
                mTvPaymentStatus.setTextColor(getResources().getColor(R.color.warning_color));
            }
        }
    }

    /** Bloque navy del precio con la referencia ADS, motivo del ajuste y agenda (mockup 17). */
    private void bindQuoteBreakdown(ServiceRequest request, boolean showPrice) {
        findViewById(R.id.layout_quote_price).setVisibility(showPrice ? View.VISIBLE : View.GONE);

        QuoteReference reference = request.getQuoteReference();
        boolean showReference = showPrice && reference != null && reference.hasRange();
        findViewById(R.id.layout_quote_reference).setVisibility(showReference ? View.VISIBLE : View.GONE);
        if (showReference) {
            long min = reference.getMinCop();
            long max = reference.getMaxCop();
            ((TextView) findViewById(R.id.tv_quote_reference_min)).setText(ServiceRequest.formatCop(min));
            ((TextView) findViewById(R.id.tv_quote_reference_max)).setText(ServiceRequest.formatCop(max));
            float position = max > min ? (float) (request.getQuoted_price() - min) / (max - min) : 0.5f;
            View marker = findViewById(R.id.view_quote_marker);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) marker.getLayoutParams();
            params.horizontalBias = Math.max(0f, Math.min(1f, position));
            marker.setLayoutParams(params);
        }

        boolean showReason = showPrice && request.isQuoteAdjusted()
                && request.getQuoteAdjustmentReasonCode() != null;
        findViewById(R.id.layout_quote_reason).setVisibility(showReason ? View.VISIBLE : View.GONE);
        if (showReason) {
            ((TextView) findViewById(R.id.tv_quote_reason)).setText(ReasonHelper.label(ReasonHelper.ADJUSTMENT,
                    request.getQuoteAdjustmentReasonCode(), request.getQuoteAdjustmentReasonText()));
        }

        boolean showSchedule = showPrice && request.hasSchedule() && !request.isCancelled();
        findViewById(R.id.layout_quote_schedule).setVisibility(showSchedule ? View.VISIBLE : View.GONE);
        if (showSchedule) {
            Locale locale = new Locale("es", "CO");
            String arrival = new SimpleDateFormat("EEE d MMM · h:mm a", locale)
                    .format(new Date(request.getScheduledAt()));
            ((TextView) findViewById(R.id.tv_quote_arrival))
                    .setText(arrival.substring(0, 1).toUpperCase(locale) + arrival.substring(1));
            ((TextView) findViewById(R.id.tv_quote_arrival_label)).setText(request.getRescheduleCount() > 0
                    ? R.string.quote_arrival_rescheduled_label : R.string.quote_arrival_label);
            int duration = request.getEstimatedDurationMin();
            findViewById(R.id.layout_quote_duration).setVisibility(duration > 0 ? View.VISIBLE : View.GONE);
            ((TextView) findViewById(R.id.tv_quote_duration)).setText(ServiceRequest.formatDuration(duration));
        }
    }

    private void confirmAcceptQuote() {
        if (mServiceRequest == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.quote_accept_title)
                .setMessage(getString(R.string.quote_accept_message, mServiceRequest.getFormattedQuotedPrice()))
                .setPositiveButton(R.string.quote_accept, (dialog, which) -> {
                    setQuoteButtonsEnabled(false);
                    mQuoteProvider.acceptQuote(mRequestId, quoteCallback(R.string.quote_accepted_toast));
                })
                .setNegativeButton(R.string.quote_dialog_cancel, null)
                .show();
    }

    private void showRejectQuoteDialog() {
        EditText reasonInput = new EditText(this);
        reasonInput.setHint(R.string.quote_reject_reason_hint);
        reasonInput.setMaxLines(3);
        reasonInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});
        int padding = (int) (20 * getResources().getDisplayMetrics().density);

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(reasonInput);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.quote_reject_title)
                .setMessage(R.string.quote_reject_message)
                .setView(container)
                .setPositiveButton(R.string.quote_reject, (dialog, which) -> {
                    setQuoteButtonsEnabled(false);
                    mQuoteProvider.rejectQuote(mRequestId, reasonInput.getText().toString().trim(),
                            quoteCallback(R.string.quote_rejected_toast));
                })
                .setNegativeButton(R.string.quote_dialog_cancel, null)
                .show();
    }

    private ApiCallback<QuoteData> quoteCallback(int successMessage) {
        return new ApiCallback<QuoteData>() {
            @Override
            public void onSuccess(QuoteData data) {
                // El listener de Firebase actualiza la tarjeta con el nuevo estado
                setQuoteButtonsEnabled(true);
                Toast.makeText(ServiceRequestDetailActivity.this, successMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message, int httpStatus) {
                setQuoteButtonsEnabled(true);
                Toast.makeText(ServiceRequestDetailActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void setQuoteButtonsEnabled(boolean enabled) {
        mBtnAcceptQuote.setEnabled(enabled);
        mBtnRejectQuote.setEnabled(enabled);
    }

    private void openPayment() {
        if (mServiceRequest == null) return;
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_REQUEST_ID, mRequestId);
        startActivity(intent);
    }
    private void callWorker() {
        if (mServiceRequest != null && mServiceRequest.getWorker_phone() != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + mServiceRequest.getWorker_phone()));
            startActivity(intent);
        }
    }

    private void cancelRequest() {
        if (mServiceRequest == null) return;
        // El listener de Firebase refleja la cancelación; el backend avisa al trabajador
        new CancelDialogHelper(this).show(mRequestId, ReasonHelper.ROLE_CLIENT, data -> finish());
    }

    private void openRatingActivity() {
        if (mServiceRequest == null) return;
        if (!mServiceRequest.isPaid()) {
            Toast.makeText(this, R.string.rating_requires_payment, Toast.LENGTH_SHORT).show();
            return;
        }
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