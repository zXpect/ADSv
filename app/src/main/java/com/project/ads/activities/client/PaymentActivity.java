package com.project.ads.activities.client;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.ads.BuildConfig;
import com.project.ads.R;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.api.PaymentData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.PaymentProvider;
import com.project.ads.helpers.AdsSnackbar;
import com.project.ads.helpers.Motion;

import java.util.Map;

/**
 * Pago de un servicio completado · mockups 25 (pagar con Wompi) y 26-28 (aprobado,
 * rechazado, pendiente). La pantalla tiene un modo «pagar» y un modo «resultado».
 *
 * 1. Pide al backend el pago (POST /api/payments/); el monto lo define el servidor.
 * 2. Abre el checkout de la pasarela en un Custom Tab (no en un WebView: la app
 *    no puede leer ni alterar lo que el usuario escribe en la pasarela).
 * 3. La pasarela vuelve por adsapp://payment-result?request_id=... (deep link).
 * 4. El resultado se toma SIEMPRE de requests/<id>/payment_status en Firebase,
 *    que escribe el backend al validar el evento firmado de la pasarela. El
 *    deep link solo trae a la app de vuelta; no se confía en sus parámetros.
 */
public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "request_id";
    private static final String TAG = "PaymentActivity";

    private TextView mTvService;
    private TextView mTvAmount;
    private TextView mTvWorker;
    private TextView mTvMessage;
    private ProgressBar mProgress;
    private Button mBtnAction;
    // Ícono del resultado que se está mostrando (0: ninguno)
    private int mShownResultIcon = 0;

    private PaymentProvider mPaymentProvider;
    private DatabaseReference mRequestRef;
    private ValueEventListener mRequestListener;

    private String mRequestId;
    private ServiceRequest mServiceRequest;
    private boolean mCheckoutOpened;
    private boolean mCreatingPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mPaymentProvider = new PaymentProvider();
        initViews();

        mRequestId = extractRequestId(getIntent());
        if (mRequestId == null) {
            Toast.makeText(this, R.string.payment_error_no_request, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (savedInstanceState != null) {
            mCheckoutOpened = savedInstanceState.getBoolean("checkout_opened", false);
        }
        listenToRequest();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Regreso desde el checkout por deep link
        String requestId = extractRequestId(intent);
        if (requestId != null && !requestId.equals(mRequestId)) {
            removeListener();
            mRequestId = requestId;
            listenToRequest();
        }
        if (isDeepLink(intent)) {
            mCheckoutOpened = true;
            if (mServiceRequest != null) render(mServiceRequest);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("checkout_opened", mCheckoutOpened);
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        mTvService = findViewById(R.id.tv_payment_service);
        mTvAmount = findViewById(R.id.tv_payment_amount);
        mTvWorker = findViewById(R.id.tv_payment_worker);
        mTvMessage = findViewById(R.id.tv_payment_message);
        mProgress = findViewById(R.id.progress_payment);
        mBtnAction = findViewById(R.id.btn_payment_action);

        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btn_result_home).setOnClickListener(v -> goHome());
    }

    // ---------- Datos ----------

    private static boolean isDeepLink(Intent intent) {
        Uri data = intent != null ? intent.getData() : null;
        return data != null
                && PaymentProvider.REDIRECT_SCHEME.equals(data.getScheme())
                && PaymentProvider.REDIRECT_HOST.equals(data.getHost());
    }

    private static String extractRequestId(Intent intent) {
        if (intent == null) return null;
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        if (requestId == null && isDeepLink(intent)) {
            requestId = intent.getData().getQueryParameter(PaymentProvider.REDIRECT_PARAM_REQUEST_ID);
        }
        // Claves de Firebase: sin '/', '.', '#', '$', '[' ni ']'
        if (requestId == null || requestId.isEmpty() || requestId.matches(".*[/.#$\\[\\]].*")) {
            return null;
        }
        return requestId;
    }

    private void listenToRequest() {
        showLoading(getString(R.string.payment_loading));
        mRequestRef = FirebaseDatabase.getInstance().getReference().child("requests").child(mRequestId);
        mRequestListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !(snapshot.getValue() instanceof Map)) {
                    showFatal(getString(R.string.payment_error_no_request));
                    return;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                mServiceRequest = ServiceRequest.fromMap(data);
                mServiceRequest.setRequest_id(mRequestId);
                render(mServiceRequest);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Las reglas solo dejan leer la solicitud a sus participantes
                Log.e(TAG, "No se pudo leer la solicitud: " + error.getMessage());
                showFatal(getString(R.string.payment_error_no_request));
            }
        };
        mRequestRef.addValueEventListener(mRequestListener);
    }

    // ---------- UI ----------

    private void render(ServiceRequest request) {
        String worker = request.getWorker_name() != null
                ? request.getWorker_name() : getString(R.string.request_the_worker);
        mTvService.setText(request.getService_type() != null ? request.getService_type() : "");
        mTvAmount.setText(request.hasQuote() ? request.getFormattedQuotedPrice() : "");
        mTvWorker.setText(getString(R.string.payment_checkout_subtitle, worker));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.getUid().equals(request.getClient_id())) {
            showFatal(getString(R.string.payment_error_not_owner));
            return;
        }

        if (request.isPaid()) {
            // Pago aprobado (mockup 26)
            String amount = request.hasQuote() ? request.getFormattedQuotedPrice() : null;
            if (request.isRated()) {
                showResult(R.drawable.ic_ads_check, getString(R.string.payment_result_approved), amount,
                        getString(R.string.payment_result_approved_rated), 0, null, false);
            } else {
                showResult(R.drawable.ic_ads_check, getString(R.string.payment_result_approved), amount,
                        getString(R.string.payment_result_approved_message, worker),
                        R.string.payment_rate_button, v -> openRating(), false);
            }
            return;
        }
        if (!request.isCompleted() || !request.isQuoteAccepted()) {
            showFatal(getString(R.string.payment_error_not_payable));
            return;
        }
        if (mCreatingPayment) {
            showCheckout(true);
            return;
        }

        String paymentStatus = request.getPayment_status();
        if (PaymentProvider.STATUS_PENDING.equals(paymentStatus) && mCheckoutOpened) {
            // Volvió del checkout: se espera el evento de la pasarela vía backend (mockup 28)
            showResult(R.drawable.ic_clock, getString(R.string.payment_result_pending), null,
                    getString(R.string.payment_result_pending_message),
                    R.string.payment_reopen_button, v -> startPayment(), true);
        } else if (PaymentProvider.STATUS_DECLINED.equals(paymentStatus)) {
            // Pago rechazado (mockup 27)
            showResult(R.drawable.ic_close, getString(R.string.payment_result_declined), null,
                    getString(R.string.payment_result_declined_message),
                    R.string.payment_retry_button, v -> startPayment(), false);
        } else if (PaymentProvider.STATUS_ERROR.equals(paymentStatus)
                || PaymentProvider.STATUS_VOIDED.equals(paymentStatus)) {
            showResult(R.drawable.ic_close, getString(R.string.payment_result_error), null,
                    getString(R.string.payment_error), R.string.payment_retry_button, v -> startPayment(), false);
        } else {
            // Listo para pagar (mockup 25)
            showCheckout(false);
        }
    }

    /** Modo «pagar» (mockup 25); con loading mientras se crea el pago. */
    private void showCheckout(boolean loading) {
        findViewById(R.id.layout_payment_checkout).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_payment_result).setVisibility(View.GONE);
        mProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        mTvMessage.setText(loading ? R.string.payment_creating : R.string.payment_secure_screen_note);
        mBtnAction.setText(R.string.payment_pay_wompi);
        mBtnAction.setEnabled(!loading);
        mBtnAction.setOnClickListener(v -> startPayment());
    }

    /** Modo «resultado» (mockups 26, 27, 28 y errores). primaryText 0 = sin botón principal. */
    private void showResult(int icon, String title, String amount, String message,
                            int primaryText, View.OnClickListener primaryAction, boolean waiting) {
        findViewById(R.id.layout_payment_checkout).setVisibility(View.GONE);
        findViewById(R.id.layout_payment_result).setVisibility(View.VISIBLE);
        ImageView iconView = findViewById(R.id.iv_result_icon);
        iconView.setVisibility(View.VISIBLE);
        // El listener de payment_status repite el mismo resultado: animar solo si cambia
        if (icon != mShownResultIcon) {
            mShownResultIcon = icon;
            Motion.showResultIcon(iconView, icon);
        }
        ((TextView) findViewById(R.id.tv_result_title)).setText(title);
        TextView amountView = findViewById(R.id.tv_result_amount);
        amountView.setText(amount);
        amountView.setVisibility(amount != null ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.tv_result_message)).setText(message);
        findViewById(R.id.progress_result).setVisibility(waiting ? View.VISIBLE : View.GONE);
        Button primary = findViewById(R.id.btn_result_primary);
        primary.setVisibility(primaryText != 0 ? View.VISIBLE : View.GONE);
        if (primaryText != 0) {
            primary.setText(primaryText);
            primary.setEnabled(true);
            primary.setOnClickListener(primaryAction);
        }
    }

    private void showLoading(String message) {
        findViewById(R.id.layout_payment_checkout).setVisibility(View.GONE);
        findViewById(R.id.layout_payment_result).setVisibility(View.VISIBLE);
        findViewById(R.id.iv_result_icon).setVisibility(View.GONE);
        mShownResultIcon = 0;
        ((TextView) findViewById(R.id.tv_result_title)).setText("");
        findViewById(R.id.tv_result_amount).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.tv_result_message)).setText(message);
        findViewById(R.id.progress_result).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_result_primary).setVisibility(View.GONE);
    }

    private void showFatal(String message) {
        showResult(R.drawable.ic_info, getString(R.string.payment_result_unavailable), null, message,
                R.string.payment_back, v -> finish(), false);
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // ---------- Acciones ----------

    private void startPayment() {
        mCreatingPayment = true;
        showCheckout(true);

        mPaymentProvider.createPayment(mRequestId, new ApiCallback<PaymentData>() {
            @Override
            public void onSuccess(PaymentData payment) {
                mCreatingPayment = false;
                openCheckout(payment.getCheckoutUrl());
            }

            @Override
            public void onError(String message, int httpStatus) {
                mCreatingPayment = false;
                AdsSnackbar.showLong(PaymentActivity.this, message);
                if (mServiceRequest != null) render(mServiceRequest);
            }
        });
    }

    private void openCheckout(String checkoutUrl) {
        Uri uri = checkoutUrl != null ? Uri.parse(checkoutUrl) : null;
        // En release solo HTTPS; en debug se permite HTTP para el backend local (mock)
        boolean allowedScheme = uri != null && ("https".equals(uri.getScheme())
                || (BuildConfig.DEBUG && "http".equals(uri.getScheme())));
        if (!allowedScheme) {
            Log.e(TAG, "URL de checkout no permitida");
            AdsSnackbar.showLong(this, R.string.payment_error);
            if (mServiceRequest != null) render(mServiceRequest);
            return;
        }

        mCheckoutOpened = true;
        try {
            new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(this, uri);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No hay navegador para abrir el checkout", e);
            AdsSnackbar.showLong(this, R.string.payment_error_no_browser);
        }
        if (mServiceRequest != null) render(mServiceRequest);
    }

    private void openRating() {
        if (mServiceRequest == null) return;
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("request_id", mRequestId);
        intent.putExtra("worker_id", mServiceRequest.getWorker_id());
        intent.putExtra("worker_name", mServiceRequest.getWorker_name());
        intent.putExtra("service_type", mServiceRequest.getService_type());
        startActivity(intent);
        finish();
    }

    private void removeListener() {
        if (mRequestRef != null && mRequestListener != null) {
            mRequestRef.removeEventListener(mRequestListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListener();
    }
}
