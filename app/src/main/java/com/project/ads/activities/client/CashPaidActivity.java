package com.project.ads.activities.client;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.project.ads.R;
import com.project.ads.helpers.Motion;
import com.project.ads.activities.PqrCreateActivity;
import com.project.ads.models.ServiceRequest;

/**
 * Pago en efectivo registrado · mockup 29 (docs/mockups/PagoEfectivo.dc.html). Se
 * muestra una vez al cliente cuando el técnico registra el efectivo (D10): puede
 * calificar o, si no está de acuerdo, abrir una PQR.
 */
public class CashPaidActivity extends AppCompatActivity {

    private static final String PREFS = "cash_paid_shown";
    private static final String EXTRA_REQUEST_ID = "request_id";
    private static final String EXTRA_PRICE = "formatted_price";
    private static final String EXTRA_WORKER_ID = "worker_id";
    private static final String EXTRA_WORKER_NAME = "worker_name";
    private static final String EXTRA_SERVICE = "service_type";

    /** Solo la primera vez que el detalle ve el pago registrado. */
    public static boolean shouldShow(Context context, ServiceRequest request) {
        return request.isCompleted() && request.isCashPayment() && request.isPaid() && !request.isRated()
                && !context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(request.getRequest_id(), false);
    }

    public static Intent intent(Context context, ServiceRequest request) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(request.getRequest_id(), true).apply();
        return new Intent(context, CashPaidActivity.class)
                .putExtra(EXTRA_REQUEST_ID, request.getRequest_id())
                .putExtra(EXTRA_PRICE, request.getFormattedQuotedPrice())
                .putExtra(EXTRA_WORKER_ID, request.getWorker_id())
                .putExtra(EXTRA_WORKER_NAME, request.getWorker_name())
                .putExtra(EXTRA_SERVICE, request.getService_type());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash_paid);

        String requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        if (TextUtils.isEmpty(requestId)) {
            finish();
            return;
        }
        Motion.popIn(findViewById(R.id.frame_cash_paid_icon));
        String worker = getIntent().getStringExtra(EXTRA_WORKER_NAME);
        ((TextView) findViewById(R.id.tv_cash_paid_amount)).setText(getIntent().getStringExtra(EXTRA_PRICE));
        ((TextView) findViewById(R.id.tv_cash_paid_message)).setText(TextUtils.isEmpty(worker)
                ? getString(R.string.cash_paid_message_generic)
                : getString(R.string.cash_paid_message, worker));

        findViewById(R.id.btn_cash_paid_pqr).setOnClickListener(v ->
                startActivity(PqrCreateActivity.forRequest(this, requestId)));
        findViewById(R.id.btn_cash_paid_rate).setOnClickListener(v -> {
            // Mismos extras que usa el detalle para calificar
            Intent intent = new Intent(this, RatingActivity.class);
            intent.putExtra("request_id", requestId);
            intent.putExtra("worker_id", getIntent().getStringExtra(EXTRA_WORKER_ID));
            intent.putExtra("worker_name", worker);
            intent.putExtra("service_type", getIntent().getStringExtra(EXTRA_SERVICE));
            startActivity(intent);
            finish();
        });
        findViewById(R.id.btn_cash_paid_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeUserActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
