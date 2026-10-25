package com.project.ads.activities.client;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.project.ads.R;
import com.project.ads.helpers.Motion;

/**
 * Solicitud enviada · mockup 12 (docs/mockups/SolicitudEnviada.dc.html).
 * Confirma el envío y muestra el estado (pendiente → cotizada → aceptada).
 * «Ver mi solicitud» abre el detalle; «Volver al inicio» (y atrás) vuelve al
 * inicio del cliente cerrando los pasos de la solicitud.
 */
public class RequestSentActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "request_id";
    public static final String EXTRA_WORKER_NAME = "worker_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_sent);
        Motion.showResultIcon(findViewById(R.id.iv_request_sent_icon), R.drawable.ic_ads_check);

        String requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        String workerName = getIntent().getStringExtra(EXTRA_WORKER_NAME);
        ((TextView) findViewById(R.id.tv_request_sent_message)).setText(getString(R.string.request_sent_message,
                !TextUtils.isEmpty(workerName) ? workerName : getString(R.string.request_the_worker)));

        findViewById(R.id.btn_view_request).setOnClickListener(v -> {
            Intent home = homeIntent();
            Intent detail = new Intent(this, ServiceRequestDetailActivity.class);
            detail.putExtra("request_id", requestId);
            // Inicio debajo del detalle, para que atrás vuelva al inicio
            startActivities(new Intent[]{home, detail});
            finish();
        });
        findViewById(R.id.btn_back_home).setOnClickListener(v -> goHome());
    }

    private Intent homeIntent() {
        Intent intent = new Intent(this, HomeUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    private void goHome() {
        startActivity(homeIntent());
        finish();
    }

    @Override
    public void onBackPressed() {
        goHome();
    }
}
