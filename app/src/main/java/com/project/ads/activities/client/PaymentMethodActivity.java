package com.project.ads.activities.client;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.project.ads.R;
import com.project.ads.models.api.PaymentMethodData;
import com.project.ads.models.api.QuoteData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.PaymentProvider;
import com.project.ads.providers.QuoteProvider;

/**
 * Método de pago · mockup 18 (docs/mockups/Pago.dc.html). El cliente elige
 * pago en línea (Wompi) o efectivo al aceptar la cotización, y lo puede cambiar
 * desde el seguimiento hasta que el servicio termine (D10). El backend guarda
 * requests/<id>/payment_method; la pantalla del detalle lo lee de Firebase.
 */
public class PaymentMethodActivity extends AppCompatActivity {

    private static final String EXTRA_REQUEST_ID = "request_id";
    private static final String EXTRA_PRICE = "formatted_price";
    private static final String EXTRA_METHOD = "payment_method";
    private static final String EXTRA_ACCEPT = "accept_quote";

    private String mRequestId;
    private String mInitialMethod;
    private boolean mAcceptQuote;
    private String mSelected;

    private MaterialCardView mCardDigital, mCardCash;
    private MaterialRadioButton mRadioDigital, mRadioCash;
    private MaterialButton mBtnConfirm;

    /** Aceptar la cotización eligiendo el método (mockup 17 → 18). */
    public static Intent acceptIntent(Context context, String requestId, String formattedPrice) {
        return new Intent(context, PaymentMethodActivity.class)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_PRICE, formattedPrice)
                .putExtra(EXTRA_ACCEPT, true);
    }

    /** Cambiar el método de una cotización ya aceptada (mockup 20 → 18). */
    public static Intent changeIntent(Context context, String requestId, String formattedPrice, String currentMethod) {
        return new Intent(context, PaymentMethodActivity.class)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_PRICE, formattedPrice)
                .putExtra(EXTRA_METHOD, currentMethod);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_method);

        mRequestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        if (mRequestId == null) {
            finish();
            return;
        }
        mAcceptQuote = getIntent().getBooleanExtra(EXTRA_ACCEPT, false);
        // Sin método guardado la solicitud se paga en línea
        mInitialMethod = PaymentProvider.METHOD_CASH.equals(getIntent().getStringExtra(EXTRA_METHOD))
                ? PaymentProvider.METHOD_CASH : PaymentProvider.METHOD_DIGITAL;

        mCardDigital = findViewById(R.id.card_method_digital);
        mCardCash = findViewById(R.id.card_method_cash);
        mRadioDigital = findViewById(R.id.radio_method_digital);
        mRadioCash = findViewById(R.id.radio_method_cash);
        mBtnConfirm = findViewById(R.id.btn_payment_method_confirm);

        String price = getIntent().getStringExtra(EXTRA_PRICE);
        ((TextView) findViewById(R.id.tv_payment_total)).setText(price);
        findViewById(R.id.layout_payment_total).setVisibility(price != null ? android.view.View.VISIBLE : android.view.View.GONE);
        mBtnConfirm.setText(mAcceptQuote ? R.string.payment_method_confirm_accept : R.string.payment_method_confirm_change);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        mCardDigital.setOnClickListener(v -> select(PaymentProvider.METHOD_DIGITAL));
        mCardCash.setOnClickListener(v -> select(PaymentProvider.METHOD_CASH));
        mBtnConfirm.setOnClickListener(v -> confirm());

        select(savedInstanceState != null
                ? savedInstanceState.getString(EXTRA_METHOD, mInitialMethod) : mInitialMethod);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_METHOD, mSelected);
    }

    private void select(String method) {
        mSelected = method;
        boolean cash = PaymentProvider.METHOD_CASH.equals(method);
        style(mCardDigital, mRadioDigital, !cash, R.string.payment_method_digital);
        style(mCardCash, mRadioCash, cash, R.string.payment_method_cash);
    }

    /** Elegida: borde naranja de 2 dp y el indicador lleno; el lector de pantalla dice el estado. */
    private void style(MaterialCardView card, MaterialRadioButton radio, boolean checked, int title) {
        radio.setChecked(checked);
        card.setStrokeColor(getColor(checked ? R.color.ads_brand_text : R.color.ads_line));
        card.setStrokeWidth(getResources().getDimensionPixelSize(
                checked ? R.dimen.stroke_focus : R.dimen.stroke_control));
        card.setContentDescription(getString(checked
                ? R.string.payment_method_selected : R.string.payment_method_not_selected, getString(title)));
    }

    private void confirm() {
        if (!mAcceptQuote && mSelected.equals(mInitialMethod)) {
            finish();
            return;
        }
        mBtnConfirm.setEnabled(false);
        if (mAcceptQuote) {
            new QuoteProvider().acceptQuote(mRequestId, mSelected, new ApiCallback<QuoteData>() {
                @Override
                public void onSuccess(QuoteData data) {
                    done(R.string.quote_accepted_toast);
                }

                @Override
                public void onError(String message, int httpStatus) {
                    failed(message);
                }
            });
        } else {
            new PaymentProvider().setPaymentMethod(mRequestId, mSelected, new ApiCallback<PaymentMethodData>() {
                @Override
                public void onSuccess(PaymentMethodData data) {
                    done(R.string.payment_method_changed);
                }

                @Override
                public void onError(String message, int httpStatus) {
                    failed(message);
                }
            });
        }
    }

    // El detalle escucha la solicitud en Firebase y se actualiza solo
    private void done(int message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void failed(String message) {
        mBtnConfirm.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
