package com.project.ads.activities.worker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.project.ads.R;
import com.project.ads.helpers.ReasonHelper;
import com.project.ads.models.ServiceRequest;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.QuoteProvider;

import java.util.Map;

/**
 * Re-cotizar · mockup 24 (docs/mockups/ReCotizar.dc.html). El trabajador propone
 * un nuevo precio con motivo, una sola vez, en camino o en progreso (D7). El
 * backend valida la tarifa mínima y el máximo; el cliente acepta o rechaza.
 */
public class RequoteActivity extends AppCompatActivity {

    private static final String EXTRA_REQUEST_ID = "request_id";
    private static final String EXTRA_PRICE = "quoted_price";

    private String mRequestId;
    private long mCurrentPrice;
    private ChipGroup mReasons;
    private TextInputLayout mPriceLayout, mReasonTextLayout;
    private TextInputEditText mPrice, mReasonText;

    public static Intent intent(Context context, String requestId, long quotedPrice) {
        return new Intent(context, RequoteActivity.class)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_PRICE, quotedPrice);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requote);

        mRequestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        if (TextUtils.isEmpty(mRequestId)) {
            finish();
            return;
        }
        mCurrentPrice = getIntent().getLongExtra(EXTRA_PRICE, 0);
        ((TextView) findViewById(R.id.tv_requote_current)).setText(
                getString(R.string.requote_current, ServiceRequest.formatCop(mCurrentPrice)));

        mPriceLayout = findViewById(R.id.til_requote_price);
        mPrice = findViewById(R.id.et_requote_price);
        mReasons = findViewById(R.id.chip_group_requote_reason);
        mReasonTextLayout = findViewById(R.id.til_requote_reason_text);
        mReasonText = findViewById(R.id.et_requote_reason_text);

        // Motivos del ajuste (los mismos del backend: ADJUSTMENT_REASONS); «Otro» pide escribirlo
        for (Map.Entry<String, String> entry : ReasonHelper.ADJUSTMENT.entrySet()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.view_reason_chip, mReasons, false);
            chip.setId(View.generateViewId());
            chip.setTag(entry.getKey());
            chip.setText(entry.getValue());
            mReasons.addView(chip);
        }
        mReasons.setOnCheckedStateChangeListener((group, ids) ->
                mReasonTextLayout.setVisibility(ReasonHelper.OTHER.equals(selectedReason()) ? View.VISIBLE : View.GONE));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_requote_send).setOnClickListener(v -> send());
    }

    private String selectedReason() {
        Chip chip = mReasons.findViewById(mReasons.getCheckedChipId());
        return chip != null ? (String) chip.getTag() : null;
    }

    private void send() {
        String raw = mPrice.getText() != null ? mPrice.getText().toString().replaceAll("[^0-9]", "") : "";
        long price = raw.isEmpty() ? 0 : Long.parseLong(raw);
        String reason = selectedReason();
        String reasonText = mReasonText.getText() != null ? mReasonText.getText().toString().trim() : "";

        mPriceLayout.setError(price <= 0 ? getString(R.string.requote_error_price)
                : price == mCurrentPrice ? getString(R.string.requote_error_same) : null);
        if (mPriceLayout.getError() != null) return;
        if (reason == null) {
            Toast.makeText(this, R.string.requote_error_reason, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean other = ReasonHelper.OTHER.equals(reason);
        mReasonTextLayout.setError(other && reasonText.isEmpty() ? getString(R.string.requote_error_reason_text) : null);
        if (mReasonTextLayout.getError() != null) return;

        findViewById(R.id.btn_requote_send).setEnabled(false);
        new QuoteProvider().requote(mRequestId, price, reason, other ? reasonText : "", new ApiCallback<Object>() {
            @Override
            public void onSuccess(Object data) {
                Toast.makeText(RequoteActivity.this, R.string.requote_sent, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message, int httpStatus) {
                // 400: fuera de la tarifa mínima o del máximo; 409: ya hubo una o el estado no lo permite
                findViewById(R.id.btn_requote_send).setEnabled(true);
                Toast.makeText(RequoteActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
