package com.project.ads.activities.client;

import com.project.ads.helpers.LoadingDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.project.ads.R;
import com.project.ads.helpers.SearchTimer;
import com.project.ads.models.RequestTaskItem;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.QuoteReference;
import com.project.ads.models.api.RequestActionData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.providers.RequestProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Solicitud, paso 2 de 2 · mockups 11 y 47 (SolicitudResumen / SolicitudSinRango).
 * El cliente escribe la dirección, ve el precio estimado por tarea que calcula el
 * backend (POST /api/requests/estimate/) y envía la solicitud (POST /api/requests/):
 * el backend valida al trabajador y las tareas, congela el precio de referencia,
 * crea requests/<id> y notifica al trabajador (B18).
 */
public class VerifyRequestActivity extends AppCompatActivity {

    private static final String TAG = "VerifyRequestActivity";

    private Button mConfirmButton;
    private LoadingDialog mProgressDialog;
    private RequestProvider mRequestProvider;
    private TextInputEditText mAddressInput;
    private TextInputLayout mAddressLayout;
    private TextInputEditText mDescriptionInput;

    private String mWorkerId;
    private String mWorkerName;
    private String mCategorySlug;
    private double mLat;
    private double mLng;
    private final List<RequestTaskItem> mItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_request);

        mRequestProvider = new RequestProvider(this);
        mConfirmButton = findViewById(R.id.btn_confirm);
        mAddressInput = findViewById(R.id.et_address);
        mAddressLayout = findViewById(R.id.til_address);
        mDescriptionInput = findViewById(R.id.et_description);

        // Paso 2 de 2: los dos tramos de la barra en naranja
        findViewById(R.id.view_step_2).setBackgroundResource(R.drawable.bg_ads_progress_on);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_use_location).setOnClickListener(v -> fillAddressFromLocation());

        loadReceivedData();
        renderPriceRows(null);
        loadEstimate();

        mConfirmButton.setOnClickListener(v -> {
            if (validateData()) submitRequest();
        });
    }

    private void loadReceivedData() {
        Intent intent = getIntent();
        mWorkerId = intent.getStringExtra("worker_id");
        mWorkerName = intent.getStringExtra("worker_name");
        mCategorySlug = intent.getStringExtra("service_category");
        mLat = intent.getDoubleExtra(ServiceRequestActivity.EXTRA_LAT, Double.NaN);
        mLng = intent.getDoubleExtra(ServiceRequestActivity.EXTRA_LNG, Double.NaN);
        String itemsJson = intent.getStringExtra("task_items");
        if (itemsJson != null) {
            RequestTaskItem[] items = new Gson().fromJson(itemsJson, RequestTaskItem[].class);
            if (items != null) mItems.addAll(Arrays.asList(items));
        }

        String firstName = !TextUtils.isEmpty(mWorkerName) ? mWorkerName.trim().split(" ")[0]
                : getString(R.string.request_the_worker);
        ((TextView) findViewById(R.id.tv_send_note)).setText(getString(R.string.request_send_note, firstName));
    }

    // ---------- precio estimado (mockups 11 y 47) ----------

    private void loadEstimate() {
        if (TextUtils.isEmpty(mCategorySlug) || mItems.isEmpty()) {
            showEstimate(null);
            return;
        }
        List<ApiBodies.TaskItemBody> bodies = new ArrayList<>();
        for (RequestTaskItem item : mItems) bodies.add(item.toBody());
        new CatalogProvider().estimate(mCategorySlug, bodies, new ApiCallback<QuoteReference>() {
            @Override
            public void onSuccess(QuoteReference estimate) {
                if (isFinishing()) return;
                showEstimate(estimate);
            }

            @Override
            public void onError(String message, int httpStatus) {
                if (isFinishing()) return;
                Log.e(TAG, "Estimado: " + message);
                showEstimate(null);
            }
        });
    }

    private void showEstimate(QuoteReference estimate) {
        findViewById(R.id.progress_estimate).setVisibility(View.GONE);
        renderPriceRows(estimate);

        TextView totalLabel = findViewById(R.id.tv_total_label);
        TextView total = findViewById(R.id.tv_amount_content);
        TextView priceTitle = findViewById(R.id.tv_price_title);
        TextView notice = findViewById(R.id.tv_price_notice);
        String range = ServiceRequestActivity.estimateRange(estimate);
        if (range != null) {
            priceTitle.setText(R.string.request_price_estimated);
            totalLabel.setText(R.string.request_total_estimated);
            total.setText(range);
            total.setTextSize(18);
            notice.setText(estimate.hasUnpricedItems()
                    ? R.string.request_price_notice_partial : R.string.request_price_notice);
        } else {
            // Ninguna tarea tiene rango (mockup 47) o el estimado no respondió
            priceTitle.setText(R.string.request_price);
            totalLabel.setText(R.string.request_total);
            total.setText(estimate != null ? R.string.request_total_unpriced : R.string.request_estimate_unavailable);
            total.setTextSize(14);
            notice.setText(estimate != null ? R.string.request_price_notice_unpriced : R.string.request_price_notice);
        }

        // La tarifa mínima viene del backend (D-decisiones: $25.500, piso del precio)
        TextView minFee = findViewById(R.id.tv_min_fee_notice);
        if (estimate != null && estimate.getMinVisitFeeCop() > 0) {
            minFee.setText(getString(R.string.request_min_fee_notice,
                    ServiceRequest.formatCop(estimate.getMinVisitFeeCop())));
            minFee.setVisibility(View.VISIBLE);
        } else {
            minFee.setVisibility(View.GONE);
        }
    }

    /** Una fila por tarea: nombre, cantidad y rango (o «El técnico la cotizará»). */
    private void renderPriceRows(QuoteReference estimate) {
        LinearLayout container = findViewById(R.id.layout_price_items);
        container.removeAllViews();
        List<QuoteReference.Item> estimated = estimate != null ? estimate.getItems() : null;
        boolean aligned = estimated != null && estimated.size() == mItems.size();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < mItems.size(); i++) {
            RequestTaskItem item = mItems.get(i);
            View row = inflater.inflate(R.layout.item_request_price, container, false);
            ((TextView) row.findViewById(R.id.tv_price_item_name)).setText(item.isCustom()
                    ? getString(R.string.request_custom_row, item.getName()) : item.getName());
            ((TextView) row.findViewById(R.id.tv_price_item_quantity)).setText(item.getQuantityLabel());

            TextView range = row.findViewById(R.id.tv_price_item_range);
            TextView unpriced = row.findViewById(R.id.tv_price_item_unpriced);
            QuoteReference.Item estimatedItem = aligned ? estimated.get(i) : null;
            if (estimatedItem != null && estimatedItem.getMinCop() != null && estimatedItem.getMaxCop() != null) {
                range.setText(ServiceRequest.formatCop(estimatedItem.getMinCop()) + " – "
                        + ServiceRequest.formatCop(estimatedItem.getMaxCop()));
                range.setVisibility(View.VISIBLE);
                unpriced.setVisibility(View.GONE);
            } else if (estimate != null) {
                range.setVisibility(View.GONE);
                unpriced.setVisibility(View.VISIBLE);
            } else {
                range.setVisibility(View.GONE);
                unpriced.setVisibility(View.GONE);
            }
            container.addView(row);
        }
    }

    // ---------- dirección ----------

    /** «Usar mi ubicación actual»: dirección aproximada a partir de la coordenada del cliente. */
    private void fillAddressFromLocation() {
        if (Double.isNaN(mLat) || Double.isNaN(mLng) || !Geocoder.isPresent()) {
            Toast.makeText(this, R.string.request_location_address_error, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            String line = null;
            try {
                @SuppressWarnings("deprecation")
                List<Address> addresses = new Geocoder(this, new Locale("es", "CO")).getFromLocation(mLat, mLng, 1);
                if (addresses != null && !addresses.isEmpty()) line = addresses.get(0).getAddressLine(0);
            } catch (Exception e) {
                Log.e(TAG, "Geocoder", e);
            }
            final String address = line;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (address == null) {
                    Toast.makeText(this, R.string.request_location_address_error, Toast.LENGTH_SHORT).show();
                } else {
                    mAddressInput.setText(address);
                    mAddressLayout.setError(null);
                }
            });
        }).start();
    }

    // ---------- envío ----------

    private String address() {
        return mAddressInput.getText() != null ? mAddressInput.getText().toString().trim() : "";
    }

    private boolean validateData() {
        if (address().isEmpty()) {
            mAddressLayout.setError(getString(R.string.request_address_required));
            mAddressInput.requestFocus();
            return false;
        }
        mAddressLayout.setError(null);
        if (TextUtils.isEmpty(mWorkerId) || TextUtils.isEmpty(mCategorySlug) || mItems.isEmpty()
                || Double.isNaN(mLat) || Double.isNaN(mLng)) {
            Toast.makeText(this, "Datos de solicitud incompletos", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    private void submitRequest() {
        mConfirmButton.setEnabled(false);
        showProgressDialog(getString(R.string.request_sending));

        List<ApiBodies.TaskItemBody> items = new ArrayList<>();
        for (RequestTaskItem item : mItems) items.add(item.toBody());
        String description = mDescriptionInput.getText() != null ? mDescriptionInput.getText().toString().trim() : "";
        // Tiempo de búsqueda (D22): sin una medición válida se envían vacías
        long[] timing = SearchTimer.values(this);
        ApiBodies.CreateRequestBody body = new ApiBodies.CreateRequestBody(
                mWorkerId, mCategorySlug, address(), description, mLat, mLng, items,
                timing != null ? timing[0] : null, timing != null ? timing[1] : null);

        mRequestProvider.createRequest(body, new ApiCallback<RequestActionData>() {
            @Override
            public void onSuccess(RequestActionData data) {
                hideProgressDialog();
                Log.d(TAG, "Solicitud creada con ID: " + data.getRequestId());
                SearchTimer.clear(VerifyRequestActivity.this);
                navigateToRequestSent(data.getRequestId());
            }

            @Override
            public void onError(String message, int httpStatus) {
                hideProgressDialog();
                mConfirmButton.setEnabled(true);
                // 409: el trabajador no está verificado o no ejerce el oficio; el mensaje viene del backend
                Toast.makeText(VerifyRequestActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Confirmación «Solicitud enviada» (mockup 12). */
    private void navigateToRequestSent(String requestId) {
        Intent intent = new Intent(this, RequestSentActivity.class);
        intent.putExtra(RequestSentActivity.EXTRA_REQUEST_ID, requestId);
        intent.putExtra(RequestSentActivity.EXTRA_WORKER_NAME, mWorkerName);
        startActivity(intent);
        finish();
    }

    private void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new LoadingDialog(this);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
