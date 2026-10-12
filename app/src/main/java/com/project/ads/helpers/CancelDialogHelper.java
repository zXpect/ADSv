package com.project.ads.helpers;

import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

        int padding = (int) (24 * mActivity.getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(mActivity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, padding / 2, padding, 0);

        RadioGroup group = new RadioGroup(mActivity);
        for (int i = 0; i < codes.size(); i++) {
            RadioButton option = new RadioButton(mActivity);
            option.setId(View.generateViewId());
            option.setTag(codes.get(i));
            option.setText(reasons.get(codes.get(i)));
            group.addView(option);
        }
        container.addView(group);

        TextInputLayout textLayout = new TextInputLayout(mActivity);
        textLayout.setHint(mActivity.getString(R.string.cancel_reason_text_hint));
        textLayout.setCounterEnabled(true);
        textLayout.setCounterMaxLength(ReasonHelper.MAX_TEXT);
        textLayout.setVisibility(View.GONE);
        TextInputEditText textInput = new TextInputEditText(textLayout.getContext());
        textInput.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(ReasonHelper.MAX_TEXT)});
        textInput.setMaxLines(3);
        textLayout.addView(textInput);
        container.addView(textLayout);

        group.setOnCheckedChangeListener((g, checkedId) -> {
            View checked = g.findViewById(checkedId);
            boolean other = checked != null && ReasonHelper.OTHER.equals(checked.getTag());
            textLayout.setVisibility(other ? View.VISIBLE : View.GONE);
        });

        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.cancel_title)
                .setMessage(R.string.cancel_message)
                .setView(container)
                .setPositiveButton(R.string.cancel_confirm, null)
                .setNegativeButton(R.string.cancel_back, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            View checked = group.findViewById(group.getCheckedRadioButtonId());
            if (checked == null) {
                Toast.makeText(mActivity, R.string.cancel_reason_required, Toast.LENGTH_SHORT).show();
                return;
            }
            String code = (String) checked.getTag();
            String text = textInput.getText() != null ? textInput.getText().toString().trim() : "";
            if (ReasonHelper.OTHER.equals(code) && TextUtils.isEmpty(text)) {
                textLayout.setError(mActivity.getString(R.string.reason_text_required));
                return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
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
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            // 409: el servicio ya inició; el backend indica tramitarlo como PQR
                            Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
                            if (httpStatus == 409) dialog.dismiss();
                        }
                    });
        }));
        dialog.show();
    }
}
