package com.project.ads.activities.client;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.project.ads.R;
import com.project.ads.models.RequestTaskItem;
import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.RequestActionData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.RequestProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resumen de la solicitud antes de enviarla. El envío es POST /api/requests/:
 * el backend valida al trabajador y las tareas, congela el precio de
 * referencia, crea requests/<id> y notifica al trabajador (B18).
 */
public class VerifyRequestActivity extends AppCompatActivity {

    private static final String TAG = "VerifyRequestActivity";

    private Button mConfirmButton;
    private ProgressDialog mProgressDialog;
    private RequestProvider mRequestProvider;

    private String mWorkerId;
    private String mAddress;
    private String mDescription;
    private String mCategorySlug;
    private double mLat;
    private double mLng;
    private final List<RequestTaskItem> mItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_request);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }

        mRequestProvider = new RequestProvider(this);
        mConfirmButton = findViewById(R.id.btn_confirm);
        loadReceivedData();
        mConfirmButton.setOnClickListener(v -> {
            if (validateData()) {
                submitRequest();
            } else {
                Toast.makeText(this, "Datos de solicitud incompletos", LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadReceivedData() {
        Intent intent = getIntent();
        mWorkerId = intent.getStringExtra("worker_id");
        mAddress = intent.getStringExtra("address");
        mDescription = intent.getStringExtra("description");
        mCategorySlug = intent.getStringExtra("service_category");
        mLat = intent.getDoubleExtra(ServiceRequestActivity.EXTRA_LAT, Double.NaN);
        mLng = intent.getDoubleExtra(ServiceRequestActivity.EXTRA_LNG, Double.NaN);
        String itemsJson = intent.getStringExtra("task_items");
        if (itemsJson != null) {
            RequestTaskItem[] items = new Gson().fromJson(itemsJson, RequestTaskItem[].class);
            if (items != null) mItems.addAll(Arrays.asList(items));
        }

        String workerName = intent.getStringExtra("worker_name");
        String serviceType = intent.getStringExtra("service_type");
        String estimate = intent.getStringExtra("estimate");

        ((ImageView) findViewById(R.id.worker_photo))
                .setImageResource(intent.getIntExtra("worker_photo", R.drawable.workerlogo));
        ((TextView) findViewById(R.id.worker_name))
                .setText(!TextUtils.isEmpty(workerName) ? workerName : "Trabajador");
        ((TextView) findViewById(R.id.tv_address_content))
                .setText(!TextUtils.isEmpty(mAddress) ? mAddress : "No especificada");

        // Oficio y tareas elegidas
        StringBuilder service = new StringBuilder(!TextUtils.isEmpty(serviceType) ? serviceType : "No especificado");
        for (RequestTaskItem item : mItems) {
            service.append("\n• ").append(item.getDisplayText());
        }
        ((TextView) findViewById(R.id.tv_service_type_content)).setText(service.toString());

        // Estimado de referencia (el precio final lo cotiza el trabajador)
        ((TextView) findViewById(R.id.tv_amount_content)).setText(
                !TextUtils.isEmpty(estimate) ? estimate : getString(R.string.request_estimate_unavailable));
    }

    private boolean validateData() {
        return !TextUtils.isEmpty(mWorkerId) && !TextUtils.isEmpty(mAddress)
                && !TextUtils.isEmpty(mCategorySlug) && !mItems.isEmpty()
                && !Double.isNaN(mLat) && !Double.isNaN(mLng);
    }

    private void submitRequest() {
        mConfirmButton.setEnabled(false);
        showProgressDialog(getString(R.string.request_sending));

        List<ApiBodies.TaskItemBody> items = new ArrayList<>();
        for (RequestTaskItem item : mItems) items.add(item.toBody());
        ApiBodies.CreateRequestBody body = new ApiBodies.CreateRequestBody(
                mWorkerId, mCategorySlug, mAddress, mDescription, mLat, mLng, items, null, null);

        mRequestProvider.createRequest(body, new ApiCallback<RequestActionData>() {
            @Override
            public void onSuccess(RequestActionData data) {
                hideProgressDialog();
                Log.d(TAG, "Solicitud creada con ID: " + data.getRequestId());
                Toast.makeText(VerifyRequestActivity.this, R.string.request_sent, LENGTH_SHORT).show();
                navigateToRequestDetail(data.getRequestId());
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

    /** Navega a los detalles de la solicitud recién creada. */
    private void navigateToRequestDetail(String requestId) {
        Intent intent = new Intent(this, ServiceRequestDetailActivity.class);
        intent.putExtra("request_id", requestId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
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
