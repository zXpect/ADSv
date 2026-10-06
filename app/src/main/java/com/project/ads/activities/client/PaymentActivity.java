package com.project.ads.activities.client;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

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

import java.util.Map;

/**
 * Pago de un servicio completado.
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
        setupStatusBar();

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
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
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
        mTvService.setText(request.getService_type() != null ? request.getService_type() : "");
        mTvAmount.setText(request.hasQuote() ? request.getFormattedQuotedPrice() : "");
        mTvWorker.setText(request.getWorker_name() != null
                ? getString(R.string.payment_worker, request.getWorker_name()) : "");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.getUid().equals(request.getClient_id())) {
            showFatal(getString(R.string.payment_error_not_owner));
            return;
        }

        if (request.isPaid()) {
            if (request.isRated()) {
                showState(getString(R.string.payment_approved), R.string.payment_back,
                        R.color.colorPrimary, v -> finish());
            } else {
                showState(getString(R.string.payment_approved), R.string.payment_rate_button,
                        R.color.success_color, v -> openRating());
            }
            return;
        }
        if (!request.isCompleted() || !request.isQuoteAccepted()) {
            showFatal(getString(R.string.payment_error_not_payable));
            return;
        }
        if (mCreatingPayment) {
            showLoading(getString(R.string.payment_creating));
            return;
        }

        String paymentStatus = request.getPayment_status();
        if (PaymentProvider.STATUS_PENDING.equals(paymentStatus) && mCheckoutOpened) {
            // Volvió del checkout: se espera el evento de la pasarela vía backend
            mProgress.setVisibility(View.VISIBLE);
            mTvMessage.setText(R.string.payment_confirming);
            showButton(R.string.payment_reopen_button, R.color.colorPrimary, v -> startPayment());
        } else if (PaymentProvider.STATUS_DECLINED.equals(paymentStatus)) {
            showState(getString(R.string.payment_declined), R.string.payment_retry_button,
                    R.color.colorPrimary, v -> startPayment());
        } else if (PaymentProvider.STATUS_ERROR.equals(paymentStatus)
                || PaymentProvider.STATUS_VOIDED.equals(paymentStatus)) {
            showState(getString(R.string.payment_error), R.string.payment_retry_button,
                    R.color.colorPrimary, v -> startPayment());
        } else {
            showState(getString(R.string.payment_ready), R.string.payment_pay_now_button,
                    R.color.success_color, v -> startPayment());
        }
    }

    private void showLoading(String message) {
        mProgress.setVisibility(View.VISIBLE);
        mTvMessage.setText(message);
        mBtnAction.setVisibility(View.GONE);
    }

    private void showState(String message, int buttonText, int buttonColor, View.OnClickListener action) {
        mProgress.setVisibility(View.GONE);
        mTvMessage.setText(message);
        showButton(buttonText, buttonColor, action);
    }

    private void showButton(int buttonText, int buttonColor, View.OnClickListener action) {
        mBtnAction.setText(buttonText);
        mBtnAction.setBackgroundTintList(ContextCompat.getColorStateList(this, buttonColor));
        mBtnAction.setOnClickListener(action);
        mBtnAction.setEnabled(true);
        mBtnAction.setVisibility(View.VISIBLE);
    }

    private void showFatal(String message) {
        mProgress.setVisibility(View.GONE);
        mTvMessage.setText(message);
        showButton(R.string.payment_back, R.color.colorPrimary, v -> finish());
    }

    // ---------- Acciones ----------

    private void startPayment() {
        mCreatingPayment = true;
        showLoading(getString(R.string.payment_creating));

        mPaymentProvider.createPayment(mRequestId, new ApiCallback<PaymentData>() {
            @Override
            public void onSuccess(PaymentData payment) {
                mCreatingPayment = false;
                openCheckout(payment.getCheckoutUrl());
            }

            @Override
            public void onError(String message, int httpStatus) {
                mCreatingPayment = false;
                Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, R.string.payment_error, Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, R.string.payment_error_no_browser, Toast.LENGTH_LONG).show();
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
