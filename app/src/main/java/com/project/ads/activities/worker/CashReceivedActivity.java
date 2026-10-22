package com.project.ads.activities.worker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.project.ads.R;
import com.project.ads.models.api.PaymentMethodData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.PaymentProvider;

/**
 * Recibí el pago · mockup 31 (docs/mockups/RegistrarEfectivo.dc.html). El trabajador
 * confirma que recibió el efectivo de un servicio completado (D10). El backend
 * registra el pago (payment_status = approved, cash_received_at) y avisa al cliente
 * con cash_received; el detalle del servicio se actualiza solo desde Firebase.
 */
public class CashReceivedActivity extends AppCompatActivity {

    private static final String EXTRA_REQUEST_ID = "request_id";
    private static final String EXTRA_PRICE = "formatted_price";
    private static final String EXTRA_CLIENT = "client_name";

    private String mRequestId;

    public static Intent intent(Context context, String requestId, String formattedPrice, String clientName) {
        return new Intent(context, CashReceivedActivity.class)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_PRICE, formattedPrice)
                .putExtra(EXTRA_CLIENT, clientName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash_received);

        mRequestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        if (TextUtils.isEmpty(mRequestId)) {
            finish();
            return;
        }
        String client = getIntent().getStringExtra(EXTRA_CLIENT);
        ((TextView) findViewById(R.id.tv_cash_received_subtitle)).setText(TextUtils.isEmpty(client)
                ? getString(R.string.cash_received_subtitle_generic)
                : getString(R.string.cash_received_subtitle, client));
        ((TextView) findViewById(R.id.tv_cash_received_amount)).setText(getIntent().getStringExtra(EXTRA_PRICE));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_cash_received_confirm).setOnClickListener(v -> confirm());
    }

    private void confirm() {
        findViewById(R.id.btn_cash_received_confirm).setEnabled(false);
        new PaymentProvider().registerCash(mRequestId, new ApiCallback<PaymentMethodData>() {
            @Override
            public void onSuccess(PaymentMethodData data) {
                Toast.makeText(CashReceivedActivity.this, R.string.cash_received_done, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message, int httpStatus) {
                // 409: no está completado, no es en efectivo o ya se registró; el mensaje viene del backend
                findViewById(R.id.btn_cash_received_confirm).setEnabled(true);
                Toast.makeText(CashReceivedActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
