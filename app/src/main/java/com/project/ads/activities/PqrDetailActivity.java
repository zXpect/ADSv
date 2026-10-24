package com.project.ads.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.project.ads.R;
import com.project.ads.activities.client.ServiceRequestDetailActivity;
import com.project.ads.activities.worker.ServiceDetailWorkerActivity;
import com.project.ads.helpers.NotificationRouter;
import com.project.ads.models.Pqr;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.PqrProvider;

/**
 * Detalle de PQR (HU-09): tipo, estado, fechas, el mensaje completo, la respuesta
 * de ADS y la solicitud relacionada. Se abre desde la lista y desde el push
 * pqr_answered (extra pqr_id). GET /api/pqr/{id}/ solo devuelve las propias.
 */
public class PqrDetailActivity extends AppCompatActivity {

    private String mPqrId;
    private Pqr mPqr;

    public static Intent intent(Context context, long pqrId) {
        return new Intent(context, PqrDetailActivity.class)
                .putExtra(NotificationRouter.EXTRA_PQR_ID, String.valueOf(pqrId));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pqr_detail);

        mPqrId = getIntent().getStringExtra(NotificationRouter.EXTRA_PQR_ID);
        if (TextUtils.isEmpty(mPqrId)) {
            finish();
            return;
        }
        // Si se abrió desde el push, la notificación queda leída
        NotificationRouter.markOpened(this, null);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_pqr_detail_retry).setOnClickListener(v -> load());
        findViewById(R.id.btn_pqr_detail_request).setOnClickListener(v -> openRequest());
        load();
    }

    private void load() {
        findViewById(R.id.progress_pqr_detail).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_pqr_detail_error).setVisibility(View.GONE);
        new PqrProvider().get(mPqrId, new ApiCallback<Pqr>() {
            @Override
            public void onSuccess(Pqr data) {
                if (isFinishing()) return;
                findViewById(R.id.progress_pqr_detail).setVisibility(View.GONE);
                if (data == null) {
                    showError(null);
                    return;
                }
                mPqr = data;
                bind(data);
            }

            @Override
            public void onError(String message, int httpStatus) {
                if (isFinishing()) return;
                findViewById(R.id.progress_pqr_detail).setVisibility(View.GONE);
                // 404: no existe o no es tuya (el backend no lo distingue)
                showError(httpStatus == 404 ? getString(R.string.pqr_detail_not_found) : message);
            }
        });
    }

    private void showError(String message) {
        findViewById(R.id.layout_pqr_detail_error).setVisibility(View.VISIBLE);
        if (message != null) ((TextView) findViewById(R.id.tv_pqr_detail_error)).setText(message);
    }

    private void bind(Pqr pqr) {
        findViewById(R.id.scroll_pqr_detail).setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.tv_pqr_detail_type)).setText(Pqr.typeLabel(pqr.getType()));
        TextView status = findViewById(R.id.tv_pqr_detail_status);
        status.setText(Pqr.statusLabel(pqr.getStatus()));
        status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                pqr.isClosed() ? R.color.ads_chip_neutral_bg : R.color.ads_chip_bg)));
        status.setTextColor(ContextCompat.getColor(this, pqr.isClosed() ? R.color.ads_charcoal : R.color.ads_chip_text));

        ((TextView) findViewById(R.id.tv_pqr_detail_title)).setText(getString(R.string.pqr_detail_title, pqr.getId()));

        String sent = getString(R.string.pqr_sent_on, Pqr.shortDate(pqr.getCreatedAt()));
        String dates;
        if (pqr.isClosed()) {
            dates = getString(R.string.pqr_dates_closed, sent, Pqr.shortDate(pqr.getClosedAt()));
        } else {
            dates = getString(R.string.pqr_dates_open, sent, Pqr.shortDate(pqr.getDueAt()));
        }
        ((TextView) findViewById(R.id.tv_pqr_detail_dates)).setText(dates);

        ((TextView) findViewById(R.id.tv_pqr_detail_description)).setText(pqr.getDescription());

        TextView response = findViewById(R.id.tv_pqr_detail_response);
        TextView responseLabel = findViewById(R.id.tv_pqr_detail_response_label);
        if (pqr.hasResponse()) {
            responseLabel.setText(R.string.pqr_response_label);
            response.setText(pqr.getResponse());
        } else {
            responseLabel.setText(R.string.pqr_detail_no_response_title);
            response.setText(R.string.pqr_detail_no_response);
        }

        findViewById(R.id.btn_pqr_detail_request).setVisibility(
                TextUtils.isEmpty(pqr.getServiceRequestId()) ? View.GONE : View.VISIBLE);
    }

    /** La solicitud en la pantalla de cada rol, como el seguimiento y Mis trabajos. */
    private void openRequest() {
        if (mPqr == null || TextUtils.isEmpty(mPqr.getServiceRequestId())) return;
        boolean worker = "trabajador".equals(NotificationRouter.role(this));
        Intent intent = new Intent(this, worker ? ServiceDetailWorkerActivity.class : ServiceRequestDetailActivity.class);
        intent.putExtra(NotificationRouter.EXTRA_REQUEST_ID, mPqr.getServiceRequestId());
        startActivity(intent);
    }
}
