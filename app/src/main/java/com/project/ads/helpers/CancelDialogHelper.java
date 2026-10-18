package com.project.ads.helpers;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.project.ads.R;
import com.project.ads.models.api.RequestActionData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.RequestProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cancelación con motivo (D8) para el cliente o el trabajador, hasta antes de
 * «Iniciar viaje». La hace el backend (POST /api/requests/<id>/cancel/), que
 * avisa a la otra parte; si el servicio ya inició responde 409 (PQR).
 * Se muestra a pantalla completa con el diseño de los mockups 21 (cliente) y
 * 50 (trabajador); los motivos son los códigos que acepta el backend.
 */
public class CancelDialogHelper {

    public interface OnCancelledListener {
        void onCancelled(RequestActionData data);
    }

    private final AppCompatActivity mActivity;

    public CancelDialogHelper(AppCompatActivity activity) {
        mActivity = activity;
    }

    /** @param role ReasonHelper.ROLE_CLIENT o ReasonHelper.ROLE_WORKER */
    public void show(String requestId, String role, OnCancelledListener listener) {
        Map<String, String> reasons = ReasonHelper.cancellationReasons(role);
        List<String> codes = new ArrayList<>(reasons.keySet());

        AppCompatDialog dialog = new AppCompatDialog(mActivity, R.style.Theme_ADS_FullScreenDialog);
        View root = LayoutInflater.from(mActivity).inflate(R.layout.dialog_cancel_service, null);
        dialog.setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ((TextView) root.findViewById(R.id.tv_cancel_message)).setText(ReasonHelper.ROLE_WORKER.equals(role)
                ? R.string.cancel_screen_message_worker : R.string.cancel_screen_message_client);

        TextInputLayout textLayout = root.findViewById(R.id.til_cancel_reason_text);
        TextInputEditText textInput = root.findViewById(R.id.et_cancel_reason_text);
        textLayout.setCounterMaxLength(ReasonHelper.MAX_TEXT);
        textInput.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(ReasonHelper.MAX_TEXT)});

        // Una tarjeta por motivo; elegir una desmarca las demás
        LinearLayout list = root.findViewById(R.id.layout_cancel_reasons);
        List<MaterialCardView> cards = new ArrayList<>();
        final String[] selected = {null};
        for (String code : codes) {
            MaterialCardView card = (MaterialCardView) LayoutInflater.from(mActivity)
                    .inflate(R.layout.item_cancel_reason, list, false);
            ((TextView) card.findViewById(R.id.tv_cancel_reason)).setText(reasons.get(code));
            card.setTag(code);
            card.setOnClickListener(v -> {
                selected[0] = code;
                for (MaterialCardView other : cards) setChecked(other, other == card);
                boolean isOther = ReasonHelper.OTHER.equals(code);
                textLayout.setVisibility(isOther ? View.VISIBLE : View.GONE);
                if (isOther) textInput.requestFocus();
            });
            setChecked(card, false);
            cards.add(card);
            list.addView(card);
        }

        root.findViewById(R.id.btn_cancel_back).setOnClickListener(v -> dialog.dismiss());
        root.findViewById(R.id.btn_cancel_keep).setOnClickListener(v -> dialog.dismiss());

        MaterialButton confirm = root.findViewById(R.id.btn_cancel_confirm);
        confirm.setOnClickListener(v -> {
            String code = selected[0];
            if (code == null) {
                Toast.makeText(mActivity, R.string.cancel_reason_required, Toast.LENGTH_SHORT).show();
                return;
            }
            String text = textInput.getText() != null ? textInput.getText().toString().trim() : "";
            if (ReasonHelper.OTHER.equals(code) && TextUtils.isEmpty(text)) {
                textLayout.setError(mActivity.getString(R.string.reason_text_required));
                return;
            }
            textLayout.setError(null);

            confirm.setEnabled(false);
            new RequestProvider(mActivity).cancelRequest(requestId, code,
                    ReasonHelper.OTHER.equals(code) ? text : "", new ApiCallback<RequestActionData>() {
                        @Override
                        public void onSuccess(RequestActionData data) {
                            dialog.dismiss();
                            Toast.makeText(mActivity, R.string.cancel_done, Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onCancelled(data);
                        }

                        @Override
                        public void onError(String message, int httpStatus) {
                            confirm.setEnabled(true);
                            // 409: el servicio ya inició; el backend indica tramitarlo como PQR
                            Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
                            if (httpStatus == 409) dialog.dismiss();
                        }
                    });
        });
        dialog.show();
    }

    /** Borde naranja y radio marcado en el motivo elegido. */
    private void setChecked(MaterialCardView card, boolean checked) {
        card.setStrokeColor(ContextCompat.getColor(mActivity, checked ? R.color.ads_brand_text : R.color.ads_line));
        card.setStrokeWidth(mActivity.getResources().getDimensionPixelSize(
                checked ? R.dimen.stroke_focus : R.dimen.stroke_control));
        card.findViewById(R.id.view_cancel_radio).setBackgroundResource(
                checked ? R.drawable.bg_ads_timeline_current : R.drawable.bg_ads_check_off);
        card.setChecked(checked);
    }
}
