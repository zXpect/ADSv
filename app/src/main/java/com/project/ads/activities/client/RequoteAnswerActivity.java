package com.project.ads.activities.client;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.project.ads.R;
import com.project.ads.helpers.ReasonHelper;
import com.project.ads.models.Requote;
import com.project.ads.models.ServiceRequest;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.QuoteProvider;
import com.project.ads.helpers.AdsSnackbar;

/**
 * Re-cotización · mockup 22 (docs/mockups/ReCotizacionCliente.dc.html). El cliente
 * acepta el nuevo precio (quoted_price cambia) o lo rechaza y el servicio sigue
 * con el precio original (D7). Sin contraoferta.
 */
public class RequoteAnswerActivity extends AppCompatActivity {

    private static final String EXTRA_REQUEST_ID = "request_id";
    private static final String EXTRA_CURRENT = "current_price";
    private static final String EXTRA_NEW = "new_price";
    private static final String EXTRA_REASON = "reason";
    private static final String EXTRA_WORKER = "worker_name";
    private static final String EXTRA_STATUS = "status";

    private String mRequestId;

    public static Intent intent(Context context, ServiceRequest request) {
        Requote requote = request.getRequote();
        return new Intent(context, RequoteAnswerActivity.class)
                .putExtra(EXTRA_REQUEST_ID, request.getRequest_id())
                .putExtra(EXTRA_CURRENT, request.getQuoted_price())
                .putExtra(EXTRA_NEW, requote.getPrice())
                .putExtra(EXTRA_REASON, ReasonHelper.labelWithComment(ReasonHelper.ADJUSTMENT,
                        requote.getReasonCode(), requote.getReasonText()))
                .putExtra(EXTRA_WORKER, request.getWorker_name())
                .putExtra(EXTRA_STATUS, request.getStatus());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requote_answer);

        mRequestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        if (TextUtils.isEmpty(mRequestId)) {
            finish();
            return;
        }
        String current = ServiceRequest.formatCop(getIntent().getLongExtra(EXTRA_CURRENT, 0));
        String worker = getIntent().getStringExtra(EXTRA_WORKER);

        ((TextView) findViewById(R.id.tv_requote_answer_status)).setText(
                "on_the_way".equals(getIntent().getStringExtra(EXTRA_STATUS))
                        ? R.string.requote_answer_status_on_the_way : R.string.requote_answer_status_in_progress);
        ((TextView) findViewById(R.id.tv_requote_answer_title)).setText(TextUtils.isEmpty(worker)
                ? getString(R.string.requote_answer_title_generic) : getString(R.string.requote_answer_title, worker));
        ((TextView) findViewById(R.id.tv_requote_answer_current)).setText(current);
        ((TextView) findViewById(R.id.tv_requote_answer_new)).setText(
                ServiceRequest.formatCop(getIntent().getLongExtra(EXTRA_NEW, 0)));
        ((TextView) findViewById(R.id.tv_requote_answer_reason)).setText(getIntent().getStringExtra(EXTRA_REASON));
        ((TextView) findViewById(R.id.tv_requote_answer_reject_note)).setText(
                getString(R.string.requote_answer_reject_note, current));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_requote_accept).setOnClickListener(v -> answer(true));
        findViewById(R.id.btn_requote_reject).setOnClickListener(v -> answer(false));
    }

    private void answer(boolean accept) {
        findViewById(R.id.btn_requote_accept).setEnabled(false);
        findViewById(R.id.btn_requote_reject).setEnabled(false);
        new QuoteProvider().answerRequote(mRequestId, accept, new ApiCallback<Object>() {
            @Override
            public void onSuccess(Object data) {
                Toast.makeText(RequoteAnswerActivity.this,
                        accept ? R.string.requote_answer_accepted : R.string.requote_answer_rejected,
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message, int httpStatus) {
                findViewById(R.id.btn_requote_accept).setEnabled(true);
                findViewById(R.id.btn_requote_reject).setEnabled(true);
                AdsSnackbar.showLong(RequoteAnswerActivity.this, message);
            }
        });
    }
}
