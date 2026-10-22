package com.project.ads.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.project.ads.R;
import com.project.ads.helpers.NotificationRouter;
import com.project.ads.models.Pqr;
import com.project.ads.models.ServiceRequest;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.AuthProvider;
import com.project.ads.providers.PqrProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Crear PQR · mockup 43 (docs/mockups/PQRNueva.dc.html), para cliente y trabajador.
 * El backend exige la solicitud relacionada: de ella sale el rol del autor. Se
 * puede abrir con una solicitud ya elegida (seguimiento en camino o en progreso).
 */
public class PqrCreateActivity extends AppCompatActivity {

    private static final String TAG = "PqrCreateActivity";
    private static final String EXTRA_REQUEST_ID = "request_id";
    private static final String STATE_REQUEST_ID = "selected_request_id";
    private static final Locale LOCALE = new Locale("es", "CO");

    private final List<ServiceRequest> mRequests = new ArrayList<>();
    private String mSelectedRequestId;

    private ChipGroup mTypeGroup;
    private MaterialButton mBtnRequest, mBtnSend;
    private TextView mTypeError, mRequestError;
    private TextInputLayout mDescriptionLayout;
    private TextInputEditText mDescription;

    /** Crear PQR con la solicitud ya elegida (mockups 45 y 46). */
    public static Intent forRequest(Context context, String requestId) {
        return new Intent(context, PqrCreateActivity.class).putExtra(EXTRA_REQUEST_ID, requestId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pqr_create);

        String uid = new AuthProvider().getId();
        if (uid == null) {
            finish();
            return;
        }
        mSelectedRequestId = savedInstanceState != null
                ? savedInstanceState.getString(STATE_REQUEST_ID)
                : getIntent().getStringExtra(EXTRA_REQUEST_ID);

        mTypeGroup = findViewById(R.id.chip_group_pqr_type);
        mBtnRequest = findViewById(R.id.btn_pqr_request);
        mBtnSend = findViewById(R.id.btn_pqr_send);
        mTypeError = findViewById(R.id.tv_pqr_type_error);
        mRequestError = findViewById(R.id.tv_pqr_request_error);
        mDescriptionLayout = findViewById(R.id.til_pqr_description);
        mDescription = findViewById(R.id.et_pqr_description);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        mTypeGroup.setOnCheckedStateChangeListener((group, ids) -> mTypeError.setVisibility(View.GONE));
        mBtnRequest.setOnClickListener(v -> chooseRequest());
        mBtnSend.setOnClickListener(v -> send());
        mBtnRequest.setEnabled(false);

        loadRequests(uid);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_REQUEST_ID, mSelectedRequestId);
    }

    /** Las solicitudes en que participa el usuario; las reglas solo dejan leerlas filtradas por su uid. */
    @SuppressWarnings("unchecked")
    private void loadRequests(String uid) {
        String field = "trabajador".equals(NotificationRouter.role(this)) ? "worker_id" : "client_id";
        FirebaseDatabase.getInstance().getReference().child("requests")
                .orderByChild(field).equalTo(uid).get()
                .addOnSuccessListener(snapshot -> {
                    mRequests.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Object value = child.getValue();
                        if (!(value instanceof Map)) continue;
                        ServiceRequest request = ServiceRequest.fromMap((Map<String, Object>) value);
                        request.setRequest_id(child.getKey());
                        mRequests.add(request);
                    }
                    Collections.sort(mRequests, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    bindRequestButton();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "No se pudieron leer las solicitudes: " + e.getMessage());
                    bindRequestButton();
                });
    }

    private void bindRequestButton() {
        if (isFinishing()) return;
        ServiceRequest selected = findRequest(mSelectedRequestId);
        if (selected == null) mSelectedRequestId = null;
        mBtnRequest.setEnabled(!mRequests.isEmpty());
        if (selected != null) {
            mBtnRequest.setText(label(selected));
        } else {
            mBtnRequest.setText(mRequests.isEmpty() ? R.string.pqr_request_none : R.string.pqr_request_placeholder);
        }
    }

    private ServiceRequest findRequest(String id) {
        if (id == null) return null;
        for (ServiceRequest request : mRequests) {
            if (id.equals(request.getRequest_id())) return request;
        }
        return null;
    }

    /** «Electricidad · 17 oct», como el mockup 43. */
    private String label(ServiceRequest request) {
        String service = !TextUtils.isEmpty(request.getService_type())
                ? request.getService_type() : getString(R.string.history_service_default);
        if (request.getTimestamp() <= 0) return service;
        String date = new SimpleDateFormat("d MMM", LOCALE).format(new Date(request.getTimestamp())).replace(".", "");
        return getString(R.string.pqr_request_option, service, date);
    }

    private void chooseRequest() {
        if (mRequests.isEmpty()) return;
        String[] options = new String[mRequests.size()];
        int checked = -1;
        for (int i = 0; i < mRequests.size(); i++) {
            options[i] = label(mRequests.get(i));
            if (mRequests.get(i).getRequest_id().equals(mSelectedRequestId)) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.pqr_request_label)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    mSelectedRequestId = mRequests.get(which).getRequest_id();
                    mRequestError.setVisibility(View.GONE);
                    bindRequestButton();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String selectedType() {
        int id = mTypeGroup.getCheckedChipId();
        if (id == R.id.chip_pqr_petition) return Pqr.TYPE_PETITION;
        if (id == R.id.chip_pqr_complaint) return Pqr.TYPE_COMPLAINT;
        if (id == R.id.chip_pqr_claim) return Pqr.TYPE_CLAIM;
        return null;
    }

    private void send() {
        String type = selectedType();
        String description = mDescription.getText() != null ? mDescription.getText().toString().trim() : "";

        mTypeError.setVisibility(type == null ? View.VISIBLE : View.GONE);
        boolean noRequest = mSelectedRequestId == null;
        mRequestError.setText(mRequests.isEmpty() ? R.string.pqr_error_no_requests : R.string.pqr_error_request);
        mRequestError.setVisibility(noRequest ? View.VISIBLE : View.GONE);
        mDescriptionLayout.setError(description.isEmpty() ? getString(R.string.pqr_error_description) : null);
        if (type == null || noRequest || description.isEmpty()) return;

        mBtnSend.setEnabled(false);
        new PqrProvider().create(mSelectedRequestId, type, description, new ApiCallback<Pqr>() {
            @Override
            public void onSuccess(Pqr data) {
                Toast.makeText(PqrCreateActivity.this, R.string.pqr_sent, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message, int httpStatus) {
                mBtnSend.setEnabled(true);
                Toast.makeText(PqrCreateActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
