package com.project.ads.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.project.ads.R;
import com.project.ads.helpers.CategoryHelper;
import com.project.ads.helpers.NotificationRouter;
import com.project.ads.helpers.RequestDetailsHelper;
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
            mBtnRequest.setText(label(selected).replace("\n", " · "));
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

    /**
     * «Electricidad · 17 oct, 2:06 p. m.» y debajo «Completada · $50.000»: con varias
     * solicitudes del mismo oficio el mismo día, la hora, el estado y el precio las
     * distinguen (MEJ-02). En el botón va en una sola línea.
     */
    private String label(ServiceRequest request) {
        String service = serviceName(request);
        String date = dateLabel(request);
        String first = date != null ? getString(R.string.pqr_request_option, service, date) : service;
        StringBuilder second = new StringBuilder();
        int status = RequestDetailsHelper.statusLabel(request.getDisplayStatus());
        if (status != 0) second.append(getString(status));
        if (request.hasQuote()) {
            if (second.length() > 0) second.append(" · ");
            second.append(request.getFormattedQuotedPrice());
        }
        return second.length() > 0 ? first + "\n" + second : first;
    }

    private String serviceName(ServiceRequest request) {
        return !TextUtils.isEmpty(request.getService_type())
                ? request.getService_type() : getString(R.string.history_service_default);
    }

    /** «17 oct, 2:06 p. m.», o null si la solicitud no tiene fecha. */
    private String dateLabel(ServiceRequest request) {
        if (request.getTimestamp() <= 0) return null;
        Date created = new Date(request.getTimestamp());
        return new SimpleDateFormat("d MMM", LOCALE).format(created).replace(".", "")
                + ", " + new SimpleDateFormat("h:mm a", LOCALE).format(created);
    }

    private void chooseRequest() {
        if (mRequests.isEmpty()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pqr_request_label)
                .setAdapter(new RequestOptionsAdapter(), (dialog, which) -> {
                    mSelectedRequestId = mRequests.get(which).getRequest_id();
                    mRequestError.setVisibility(View.GONE);
                    bindRequestButton();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Filas del pop-up: ícono del oficio, estado, fecha y precio (item_pqr_request_option). */
    private class RequestOptionsAdapter extends ArrayAdapter<ServiceRequest> {

        RequestOptionsAdapter() {
            super(PqrCreateActivity.this, R.layout.item_pqr_request_option, mRequests);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View row = convertView != null ? convertView : LayoutInflater.from(getContext())
                    .inflate(R.layout.item_pqr_request_option, parent, false);
            ServiceRequest request = mRequests.get(position);

            ((ImageView) row.findViewById(R.id.iv_request_icon))
                    .setImageResource(CategoryHelper.iconFor(request));
            ((TextView) row.findViewById(R.id.tv_pqr_request_service)).setText(serviceName(request));
            bindStatus(row.findViewById(R.id.tv_pqr_request_status), request.getDisplayStatus());

            String date = dateLabel(request);
            String price = request.hasQuote() ? request.getFormattedQuotedPrice() : null;
            String detail = date != null && price != null
                    ? getString(R.string.pqr_request_option, date, price) : date != null ? date : price;
            TextView detailView = row.findViewById(R.id.tv_pqr_request_detail);
            detailView.setText(detail);
            detailView.setVisibility(detail != null ? View.VISIBLE : View.GONE);

            // La elegida: borde naranja y visto, para que no dependa solo del color
            boolean selected = request.getRequest_id().equals(mSelectedRequestId);
            MaterialCardView card = row.findViewById(R.id.card_pqr_request);
            card.setStrokeColor(ContextCompat.getColor(getContext(),
                    selected ? R.color.ads_brand_text : R.color.ads_card_edge));
            card.setStrokeWidth(getResources().getDimensionPixelSize(
                    selected ? R.dimen.stroke_focus : R.dimen.stroke_line));
            row.findViewById(R.id.iv_pqr_request_selected).setVisibility(selected ? View.VISIBLE : View.GONE);
            return row;
        }

        /** Chip como el del historial: naranja en curso, navy completada, gris cancelada. */
        private void bindStatus(TextView chip, String displayStatus) {
            int label = RequestDetailsHelper.statusLabel(displayStatus);
            chip.setVisibility(label != 0 ? View.VISIBLE : View.GONE);
            if (label == 0) return;
            chip.setText(label);
            int background = R.color.ads_chip_bg;
            int text = R.color.ads_chip_text;
            if ("completed".equals(displayStatus)) {
                background = R.color.ads_chip_strong_bg;
                text = R.color.ads_chip_strong_text;
            } else if ("cancelled".equals(displayStatus) || "rejected".equals(displayStatus)
                    || ServiceRequest.DISPLAY_QUOTE_REJECTED.equals(displayStatus)) {
                background = R.color.ads_chip_neutral_bg;
                text = R.color.ads_charcoal;
            }
            ViewCompat.setBackgroundTintList(chip, ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), background)));
            chip.setTextColor(ContextCompat.getColor(getContext(), text));
        }
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
