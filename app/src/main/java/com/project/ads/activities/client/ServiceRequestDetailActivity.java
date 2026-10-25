package com.project.ads.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.project.ads.R;
import com.project.ads.helpers.Motion;
import com.project.ads.helpers.NotificationRouter;
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
import com.project.ads.helpers.DisplayNameHelper;
import com.project.ads.helpers.AdsSnackbar;
import com.project.ads.helpers.PaymentStatusChip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Detalle y seguimiento de la solicitud del cliente · mockups 17 (revisar
 * cotización), 20 (aceptada), 45 (en camino), 46 (en progreso) y 55 (completada).
 * Escucha requests/<id> en Firebase; aceptar o rechazar la cotización, cancelar,
 * pagar y calificar siguen pasando por el backend como antes.
 */
public class ServiceRequestDetailActivity extends AppCompatActivity {

    private static final String TAG = "ServiceRequestDetail";
    private static final Locale LOCALE = new Locale("es", "CO");

    // UI Components
    private ProgressBar mProgressBar;
    private LinearLayout mLayoutContent;
    private TextView mTvError;
    private RequestProvider mRequestProvider;
    private QuoteProvider mQuoteProvider;

    // Encabezado y técnico
    private TextView mTvStatus;
    private TextView mTvEyebrow;
    private TextView mTvTitle;
    private TextView mTvServiceType;
    private TextView mTvWorkerInitials;
    private TextView mTvWorkerName;
    private TextView mTvWorkerPhone;
    private TextView mTvAddress;
    private TextView mTvDescription;
    private ImageButton mBtnCallWorker;
    private View mStatusFlow;
    private LinearLayout mTimeline;
    // Paso de la línea de tiempo ya dibujado (NO_TIMELINE: todavía nada)
    private static final int NO_TIMELINE = -2;
    private int mShownTimelineStep = NO_TIMELINE;

    // Barra de acciones
    private View mCardActions;
    private TextView mTvActionsNoteTop;
    private TextView mTvActionsNote;
    private Button mBtnCancelRequest;
    private View mBtnCreatePqr;
    private Button mBtnRateService;
    private Button mBtnPayService;
    private Button mBtnAcceptQuote;
    private View mCardPaymentMethod;
    private TextView mTvPaymentMethod;
    private View mBtnChangePaymentMethod;
    private Button mBtnRejectQuote;

    // Quote and Payment Card
    private CardView mCardQuote;
    private TextView mTvQuoteAmount;
    private TextView mTvQuoteStatus;
    private TextView mTvPaymentStatus;
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
        NotificationRouter.markOpened(this, mRequestId);

        if (mRequestId == null || mRequestId.isEmpty()) {
            Toast.makeText(this, "Error: ID de solicitud no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        mRequestProvider = new RequestProvider(this);
        mQuoteProvider = new QuoteProvider();
        loadRequestData();
        loadCatalog();
    }

    private void initViews() {
        mProgressBar = findViewById(R.id.progress_bar);
        mLayoutContent = findViewById(R.id.layout_content);
        mTvError = findViewById(R.id.tv_error);

        mTvStatus = findViewById(R.id.tv_status);
        mTvEyebrow = findViewById(R.id.tv_detail_eyebrow);
        mTvTitle = findViewById(R.id.tv_detail_title);
        mTvServiceType = findViewById(R.id.tv_service_type);
        mTvWorkerInitials = findViewById(R.id.tv_worker_initials);
        mTvWorkerName = findViewById(R.id.tv_worker_name);
        mTvWorkerPhone = findViewById(R.id.tv_worker_phone);
        mTvAddress = findViewById(R.id.tv_address);
        mTvDescription = findViewById(R.id.tv_description);
        mBtnCallWorker = findViewById(R.id.btn_call_worker);
        mStatusFlow = findViewById(R.id.layout_status_flow);
        mTimeline = findViewById(R.id.layout_timeline);

        mCardActions = findViewById(R.id.card_actions);
        mTvActionsNoteTop = findViewById(R.id.tv_actions_note_top);
        mTvActionsNote = findViewById(R.id.tv_actions_note);
        mBtnCancelRequest = findViewById(R.id.btn_cancel_request);
        mBtnRateService = findViewById(R.id.btn_rate_service);
        mBtnPayService = findViewById(R.id.btn_pay_service);
        // Aceptar y rechazar van en la barra fija (mockup 17), no dentro de la tarjeta
        mBtnAcceptQuote = findViewById(R.id.btn_detail_accept_quote);
        mBtnRejectQuote = findViewById(R.id.btn_detail_reject_quote);

        mCardQuote = findViewById(R.id.card_quote);
        mTvQuoteAmount = findViewById(R.id.tv_quote_amount);
        mTvQuoteStatus = findViewById(R.id.tv_quote_status);
        mTvPaymentStatus = findViewById(R.id.tv_payment_status);
        mTvQuoteDetails = findViewById(R.id.tv_quote_details);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        mBtnCallWorker.setOnClickListener(v -> callWorker());
        mBtnCancelRequest.setOnClickListener(v -> cancelRequest());
        findViewById(R.id.btn_detail_review_requote).setOnClickListener(v -> {
            if (mServiceRequest != null && mServiceRequest.hasPendingRequote()) {
                startActivity(RequoteAnswerActivity.intent(this, mServiceRequest));
            }
        });
        mBtnCreatePqr = findViewById(R.id.btn_detail_create_pqr);
        mBtnCreatePqr.setOnClickListener(v ->
                startActivity(com.project.ads.activities.PqrCreateActivity.forRequest(this, mRequestId)));
        mBtnRateService.setOnClickListener(v -> openRatingActivity());
        mBtnPayService.setOnClickListener(v -> openPayment());
        mBtnAcceptQuote.setOnClickListener(v -> openAcceptQuote());
        mCardPaymentMethod = findViewById(R.id.card_payment_method);
        mTvPaymentMethod = findViewById(R.id.tv_payment_method);
        mBtnChangePaymentMethod = findViewById(R.id.btn_change_payment_method);
        mBtnChangePaymentMethod.setOnClickListener(v -> openChangePaymentMethod());
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
        String workerName = !TextUtils.isEmpty(request.getWorker_name())
                ? DisplayNameHelper.format(request.getWorker_name()) : getString(R.string.request_the_worker);
        mTvWorkerName.setText(workerName);
        mTvWorkerInitials.setText(initialsOf(workerName));
        boolean hasPhone = !TextUtils.isEmpty(request.getWorker_phone());
        mTvWorkerPhone.setText(hasPhone ? request.getWorker_phone() : "");
        mTvWorkerPhone.setVisibility(hasPhone ? View.VISIBLE : View.GONE);

        mTvAddress.setText(!TextUtils.isEmpty(request.getAddress()) ? request.getAddress() : "Sin dirección");
        boolean hasDescription = !TextUtils.isEmpty(request.getDescription());
        mTvDescription.setText(hasDescription ? request.getDescription() : "");
        mTvDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_description_label).setVisibility(hasDescription ? View.VISIBLE : View.GONE);

        bindHeader(request, workerName);
        bindTimeline(request);
        updateQuoteCard(request);
        updatePaymentMethodCard(request);
        updateActionButtons(request);
        if (!isFinishing() && CashPaidActivity.shouldShow(this, request)) {
            startActivity(CashPaidActivity.intent(this, request));
        }

        boolean activeWithWorker = "accepted".equals(request.getStatus())
                || "on_the_way".equals(request.getStatus()) || "in_progress".equals(request.getStatus());
        mBtnCallWorker.setVisibility(hasPhone && activeWithWorker ? View.VISIBLE : View.GONE);
    }

    // ---------- encabezado ----------

    /** Chip de estado, título y línea de contexto según el estado (mockups 17, 20, 45, 46, 55). */
    private void bindHeader(ServiceRequest request, String workerName) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        String service = !TextUtils.isEmpty(request.getService_type())
                ? request.getService_type() : getString(R.string.history_service_default);
        String firstName = workerName.trim().split(" ")[0];
        String schedule = request.hasSchedule() ? formatSchedule(request.getScheduledAt()) : null;

        mTvEyebrow.setVisibility(View.GONE);
        int chipStyle = 0;   // 0 naranja, 1 navy, 2 gris
        String chip;
        String title;
        String subtitle;
        switch (status) {
            case "on_the_way":
                chip = getString(R.string.request_status_on_the_way);
                chipStyle = 1;
                title = getString(R.string.detail_title_on_the_way, firstName);
                subtitle = schedule != null ? getString(R.string.detail_subtitle_arrival, service, schedule) : service;
                break;
            case "in_progress":
                chip = getString(R.string.request_status_in_progress);
                chipStyle = 1;
                title = getString(R.string.detail_title_in_progress, firstName);
                subtitle = request.getEstimatedDurationMin() > 0
                        ? getString(R.string.detail_subtitle_duration, service,
                        ServiceRequest.formatDuration(request.getEstimatedDurationMin()))
                        : service;
                break;
            case "completed":
                chip = getString(R.string.request_status_completed);
                chipStyle = 1;
                title = getString(R.string.detail_title_completed);
                subtitle = request.hasQuote()
                        ? service + " · " + workerName + " · " + request.getFormattedQuotedPrice()
                        : service + " · " + workerName;
                break;
            case "cancelled":
            case "rejected":
                chip = getString("rejected".equals(status)
                        ? R.string.request_status_rejected : R.string.request_status_cancelled);
                chipStyle = 2;
                title = getString(R.string.detail_title_cancelled);
                String reason = request.getCancellationReasonCode() != null
                        ? ReasonHelper.cancellationLabel(request.getCancellationReasonCode(),
                        request.getCancellationReasonText()) : null;
                subtitle = reason != null ? getString(R.string.history_cancel_reason, reason) : service;
                break;
            case "accepted":
                if (request.isQuotePendingAnswer()) {
                    // Revisar cotización (mockup 17)
                    chip = getString(R.string.request_status_quoted);
                    mTvEyebrow.setText(R.string.detail_eyebrow_quote);
                    mTvEyebrow.setVisibility(View.VISIBLE);
                    title = workerName;
                    subtitle = service;
                } else if (request.isQuoteAccepted()) {
                    chip = getString(R.string.request_status_accepted);
                    title = getString(R.string.detail_title_service);
                    subtitle = schedule != null ? service + " · " + schedule : service;
                } else {
                    chip = getString(R.string.request_status_pending);
                    title = getString(request.isQuoteRejected()
                            ? R.string.detail_title_new_quote : R.string.detail_title_waiting_quote);
                    subtitle = service + " · " + workerName;
                }
                break;
            default:   // pending
                chip = getString(R.string.request_status_pending);
                title = getString(R.string.detail_title_pending);
                subtitle = service + " · " + workerName;
        }

        mTvStatus.setText(chip);
        int background = chipStyle == 1 ? R.color.ads_chip_strong_bg : chipStyle == 2 ? R.color.ads_chip_neutral_bg : R.color.ads_chip_bg;
        int text = chipStyle == 1 ? R.color.ads_chip_strong_text : chipStyle == 2 ? R.color.ads_charcoal : R.color.ads_chip_text;
        ViewCompat.setBackgroundTintList(mTvStatus, ColorStateList.valueOf(ContextCompat.getColor(this, background)));
        mTvStatus.setTextColor(ContextCompat.getColor(this, text));
        mTvTitle.setText(title);
        mTvServiceType.setText(subtitle);
    }

    /** "Lun 19 oct, 3:00 p. m." */
    private static String formatSchedule(long millis) {
        String text = new SimpleDateFormat("EEE d MMM, h:mm a", LOCALE).format(new Date(millis));
        return text.substring(0, 1).toUpperCase(LOCALE) + text.substring(1);
    }

    // ---------- línea de tiempo ----------

    /**
     * Aceptada → En camino → En progreso → Completada. Se muestra desde que el
     * cliente acepta la cotización; antes la pantalla es la cotización (mockup 17).
     */
    private void bindTimeline(ServiceRequest request) {
        String status = request.getStatus() != null ? request.getStatus() : "";
        int current;
        switch (status) {
            case "on_the_way": current = 1; break;
            case "in_progress": current = 2; break;
            case "completed": current = 4; break;   // todos hechos
            case "accepted": current = request.isQuoteAccepted() ? 0 : -1; break;
            default: current = -1;
        }
        mStatusFlow.setVisibility(current >= 0 ? View.VISIBLE : View.GONE);
        // Solo se anima el avance mientras se mira la pantalla, no al abrirla
        boolean advanced = mShownTimelineStep != NO_TIMELINE && current > mShownTimelineStep;
        mShownTimelineStep = current;
        if (current < 0) return;
        int reached = current == 4 ? 3 : current;

        int[] titles = {R.string.request_status_accepted, R.string.request_status_on_the_way,
                R.string.request_status_in_progress, R.string.request_status_completed};
        int[] descriptions = {R.string.detail_timeline_accepted, R.string.detail_timeline_on_the_way,
                R.string.detail_timeline_in_progress, R.string.detail_timeline_completed};
        mTimeline.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < titles.length; i++) {
            View row = inflater.inflate(R.layout.item_timeline_step, mTimeline, false);
            boolean done = i < current || (current == 0 && i == 0);
            boolean isCurrent = i == current && current > 0;
            ImageView dot = row.findViewById(R.id.iv_timeline_dot);
            if (done) {
                dot.setBackgroundResource(R.drawable.bg_ads_check_on);
                dot.setImageResource(R.drawable.ic_ads_check);
            } else {
                dot.setBackgroundResource(isCurrent ? R.drawable.bg_ads_timeline_current : R.drawable.bg_ads_check_off);
                dot.setImageDrawable(null);
            }
            View line = row.findViewById(R.id.view_timeline_line);
            line.setVisibility(i == titles.length - 1 ? View.GONE : View.VISIBLE);
            if (i == titles.length - 1) ((android.view.ViewGroup) row).getChildAt(1).setPadding(0, 0, 0, 0);
            line.setBackgroundColor(ContextCompat.getColor(this, i < current ? R.color.ads_brand_text : R.color.ads_line));

            TextView title = row.findViewById(R.id.tv_timeline_title);
            title.setText(titles[i]);
            title.setTextColor(ContextCompat.getColor(this, done || isCurrent ? R.color.ads_text : R.color.ads_muted));
            // La descripción acompaña al último paso alcanzado
            boolean latest = (current == 0 && i == 0) || isCurrent || (current == 4 && i == 3);
            TextView description = row.findViewById(R.id.tv_timeline_description);
            description.setText(descriptions[i]);
            description.setVisibility(latest ? View.VISIBLE : View.GONE);
            mTimeline.addView(row);
            if (advanced && i == reached) {
                Motion.popIn(dot);
            } else if (advanced && i == reached - 1) {
                Motion.fill(line, false);
            }
        }
    }

    // ---------- barra de acciones ----------

    private void updateActionButtons(ServiceRequest request) {
        mBtnAcceptQuote.setVisibility(View.GONE);
        mBtnRejectQuote.setVisibility(View.GONE);
        mBtnPayService.setVisibility(View.GONE);
        mBtnRateService.setVisibility(View.GONE);
        mBtnCancelRequest.setVisibility(View.GONE);
        mBtnCreatePqr.setVisibility(View.GONE);
        View reviewRequote = findViewById(R.id.btn_detail_review_requote);
        reviewRequote.setVisibility(View.GONE);
        mTvActionsNoteTop.setVisibility(View.GONE);
        mTvActionsNote.setVisibility(View.GONE);

        String status = request.getStatus() != null ? request.getStatus().toLowerCase(LOCALE) : "";
        boolean any = false;

        // Cotización por responder (mockup 17)
        if ("accepted".equals(status) && request.isQuotePendingAnswer()) {
            mBtnAcceptQuote.setVisibility(View.VISIBLE);
            mBtnRejectQuote.setVisibility(View.VISIBLE);
            any = true;
        }

        // Completada: pagar y luego calificar (mockup 55)
        if ("completed".equals(status)) {
            if (request.isPaid()) {
                mBtnRateService.setVisibility(View.VISIBLE);
                mBtnRateService.setText(request.isRated() ? R.string.detail_view_rating : R.string.detail_rate);
                any = true;
            } else if (request.isPaymentPending()) {
                // Un intento sin terminar (checkout cerrado a medias) no debe dejar al
                // cliente sin forma de pagar: el backend reutiliza el intento o, si ya
                // venció, consulta la pasarela y abre otro sin cobrar dos veces
                showNote(mTvActionsNoteTop, R.string.payment_status_in_process);
                mBtnPayService.setVisibility(View.VISIBLE);
                mBtnPayService.setText(R.string.detail_continue_payment);
                showNote(mTvActionsNote, R.string.detail_continue_payment_note);
                any = true;
            } else if (request.isQuoteAccepted() && request.isCashPayment()) {
                // En efectivo no hay pago en línea: el técnico registra el pago (D10)
                showNote(mTvActionsNoteTop, R.string.detail_cash_note);
                any = true;
            } else if (request.isQuoteAccepted()) {
                mBtnPayService.setVisibility(View.VISIBLE);
                mBtnPayService.setText(getString(R.string.detail_pay, request.getFormattedQuotedPrice()));
                showNote(mTvActionsNote, R.string.detail_pay_note);
                any = true;
            }
        }

        // Cancelar con motivo hasta que el técnico inicie el viaje (D8, mockup 20)
        if (request.canBeCancelled()) {
            mBtnCancelRequest.setVisibility(View.VISIBLE);
            ((android.widget.LinearLayout.LayoutParams) mBtnCancelRequest.getLayoutParams()).topMargin =
                    any ? getResources().getDimensionPixelSize(R.dimen.space_3) : 0;
            if (!request.isQuotePendingAnswer()) showNote(mTvActionsNote, R.string.detail_cancel_note);
            any = true;
        } else if ("on_the_way".equals(status) || "in_progress".equals(status)) {
            // Después de «Iniciar viaje» ya no se cancela aquí (mockups 45 y 46)
            if (request.hasPendingRequote()) {
                showNote(mTvActionsNoteTop, R.string.requote_client_pending);
                reviewRequote.setVisibility(View.VISIBLE);
            } else {
                showNote(mTvActionsNoteTop, R.string.detail_cannot_cancel_note);
            }
            mBtnCreatePqr.setVisibility(View.VISIBLE);
            any = true;
        }

        mCardActions.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    private void showNote(TextView note, int text) {
        note.setText(text);
        note.setVisibility(View.VISIBLE);
    }

    // ---------- cotización (mockup 17) ----------

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
        mTvPaymentStatus.setVisibility(View.GONE);
        if (!withQuote) {
            mTvQuoteAmount.setVisibility(View.GONE);
            mTvQuoteStatus.setText("pending".equals(status) ? getString(R.string.quote_status_missing_client) : "");
            mTvQuoteStatus.setVisibility(mTvQuoteStatus.getText().length() > 0 ? View.VISIBLE : View.GONE);
            return;
        }
        mTvQuoteStatus.setVisibility(View.VISIBLE);

        if (request.hasQuote()) {
            mTvQuoteAmount.setVisibility(View.VISIBLE);
            mTvQuoteAmount.setText(request.getFormattedQuotedPrice());
        } else {
            mTvQuoteAmount.setVisibility(View.GONE);
        }

        if (request.isQuoteAccepted()) {
            // Con el valor: sola, «Precio acordado» quedaba sin precio debajo de las tareas (DEF-25)
            mTvQuoteStatus.setText(getString(R.string.quote_status_agreed_value, request.getFormattedQuotedPrice()));
        } else if (request.isQuotePendingAnswer()) {
            mTvQuoteStatus.setText(R.string.quote_status_answer_client);
        } else if (request.isQuoteRejected()) {
            mTvQuoteStatus.setText(R.string.quote_status_rejected_client);
        } else if ("accepted".equals(status)) {
            mTvQuoteStatus.setText(R.string.quote_status_missing_client);
        } else {
            // En curso o terminada sin cotización (solicitudes de antes de la cotización):
            // no hay cotización que esperar
            mTvQuoteStatus.setVisibility(View.GONE);
        }

        boolean hasPayment = request.hasQuote() || request.isPaid() || request.isPaymentPending();
        if (!request.hasQuote() && !hasPayment && details == null
                && mTvQuoteStatus.getVisibility() == View.GONE) {
            mCardQuote.setVisibility(View.GONE);
            return;
        }

        if ("completed".equals(status) && hasPayment) {
            mTvPaymentStatus.setVisibility(View.VISIBLE);
            if (request.isPaid()) {
                PaymentStatusChip.showPaid(mTvPaymentStatus, R.string.payment_status_paid);
            } else if (request.isPaymentPending()) {
                PaymentStatusChip.showText(mTvPaymentStatus, R.string.payment_status_in_process, R.color.ads_brand_text);
            } else if (request.isCashPayment()) {
                PaymentStatusChip.showText(mTvPaymentStatus, R.string.payment_status_cash_client, R.color.ads_brand_text);
            } else {
                PaymentStatusChip.showText(mTvPaymentStatus, R.string.payment_status_pending_client, R.color.ads_brand_text);
            }
        }
    }

    /** Bloque navy del precio con la referencia ADS, motivo del ajuste y agenda (mockup 17). */
    private void bindQuoteBreakdown(ServiceRequest request, boolean showPrice) {
        findViewById(R.id.layout_quote_price).setVisibility(showPrice ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.tv_quote_title)).setText(request.isQuotePendingAnswer()
                ? R.string.detail_quote_proposed : R.string.quote_card_title);

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
            ((TextView) findViewById(R.id.tv_quote_reason)).setText(ReasonHelper.labelWithComment(ReasonHelper.ADJUSTMENT,
                    request.getQuoteAdjustmentReasonCode(), request.getQuoteAdjustmentReasonText()));
        }

        boolean showSchedule = showPrice && request.hasSchedule() && !request.isCancelled();
        findViewById(R.id.layout_quote_schedule).setVisibility(showSchedule ? View.VISIBLE : View.GONE);
        if (showSchedule) {
            String arrival = new SimpleDateFormat("EEE d MMM · h:mm a", LOCALE)
                    .format(new Date(request.getScheduledAt()));
            ((TextView) findViewById(R.id.tv_quote_arrival))
                    .setText(arrival.substring(0, 1).toUpperCase(LOCALE) + arrival.substring(1));
            ((TextView) findViewById(R.id.tv_quote_arrival_label)).setText(request.getRescheduleCount() > 0
                    ? R.string.quote_arrival_rescheduled_label : R.string.quote_arrival_label);
            int duration = request.getEstimatedDurationMin();
            findViewById(R.id.layout_quote_duration).setVisibility(duration > 0 ? View.VISIBLE : View.GONE);
            ((TextView) findViewById(R.id.tv_quote_duration)).setText(ServiceRequest.formatDuration(duration));
        }
    }

    /** Aceptar lleva a elegir el método de pago (mockup 17 → 18, D10); allí se confirma. */
    private void openAcceptQuote() {
        if (mServiceRequest == null) return;
        startActivity(PaymentMethodActivity.acceptIntent(this, mRequestId,
                mServiceRequest.getFormattedQuotedPrice()));
    }

    /** Fila del método de pago (mockup 20): se puede cambiar hasta que el servicio termine. */
    private void updatePaymentMethodCard(ServiceRequest request) {
        boolean show = request.isQuoteAccepted() && !request.isCancelled();
        mCardPaymentMethod.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        mTvPaymentMethod.setText(request.isCashPayment()
                ? R.string.payment_method_cash : R.string.payment_method_digital);
        mBtnChangePaymentMethod.setVisibility(request.canChangePaymentMethod() && !request.isPaid()
                ? View.VISIBLE : View.GONE);
    }

    private void openChangePaymentMethod() {
        if (mServiceRequest == null) return;
        startActivity(PaymentMethodActivity.changeIntent(this, mRequestId,
                mServiceRequest.getFormattedQuotedPrice(), mServiceRequest.getPayment_method()));
    }

    /** Confirmar rechazo (mockup 19); el motivo sigue siendo opcional. */
    private void showRejectQuoteDialog() {
        if (mServiceRequest == null) return;
        EditText reasonInput = new EditText(this);
        reasonInput.setHint(R.string.quote_reject_reason_hint);
        reasonInput.setMaxLines(3);
        reasonInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});
        int padding = getResources().getDimensionPixelSize(R.dimen.screen_margin);

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(reasonInput);

        String worker = !TextUtils.isEmpty(mServiceRequest.getWorker_name())
                ? mServiceRequest.getWorker_name() : getString(R.string.request_the_worker);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.detail_reject_title)
                .setMessage(getString(R.string.detail_reject_message, worker, mServiceRequest.getFormattedQuotedPrice()))
                .setView(container)
                .setPositiveButton(R.string.detail_reject_confirm, (dialog, which) -> {
                    setQuoteButtonsEnabled(false);
                    mQuoteProvider.rejectQuote(mRequestId, reasonInput.getText().toString().trim(),
                            quoteCallback(R.string.quote_rejected_toast));
                })
                .setNegativeButton(R.string.detail_reject_back, null)
                .show();
    }

    private ApiCallback<QuoteData> quoteCallback(int successMessage) {
        return new ApiCallback<QuoteData>() {
            @Override
            public void onSuccess(QuoteData data) {
                // El listener de Firebase actualiza la pantalla con el nuevo estado
                setQuoteButtonsEnabled(true);
                AdsSnackbar.show(ServiceRequestDetailActivity.this, successMessage);
            }

            @Override
            public void onError(String message, int httpStatus) {
                setQuoteButtonsEnabled(true);
                AdsSnackbar.showLong(ServiceRequestDetailActivity.this, message);
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
            AdsSnackbar.show(this, R.string.rating_requires_payment);
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
                AdsSnackbar.show(this, R.string.error_check_status);
            }
        }).addOnFailureListener(e -> {
            mProgressBar.setVisibility(View.GONE);
            AdsSnackbar.show(this, R.string.error_connection);
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
        String ratingText = String.format(LOCALE, "%.1f", rating);
        String message = "Ya calificaste este servicio con " + ratingText + " estrellas";

        if (comment != null && !comment.isEmpty()) {
            message += "\n\nTu comentario:\n\"" + comment + "\"";
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Ya calificado")
                .setMessage(message)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private static String initialsOf(String name) {
        StringBuilder initials = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && initials.length() < 2) initials.append(Character.toUpperCase(part.charAt(0)));
        }
        return initials.toString();
    }

    private void showLoading(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mLayoutContent.setVisibility(show ? View.GONE : View.VISIBLE);
        findViewById(R.id.error_container).setVisibility(View.GONE);
    }

    private void showError(String message) {
        mProgressBar.setVisibility(View.GONE);
        mLayoutContent.setVisibility(View.GONE);
        findViewById(R.id.error_container).setVisibility(View.VISIBLE);
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
