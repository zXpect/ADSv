package com.project.ads.helpers;

import com.google.android.material.chip.ChipGroup;

import com.google.android.material.chip.Chip;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatDialog;

import android.view.ViewGroup;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.project.ads.R;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.CatalogData;
import com.project.ads.models.api.QuoteData;
import com.project.ads.models.api.QuoteLimits;
import com.project.ads.models.api.QuoteReference;
import com.project.ads.models.api.RequestActionData;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.CatalogProvider;
import com.project.ads.providers.QuoteProvider;
import com.project.ads.providers.RequestProvider;
import com.project.ads.helpers.AdsSnackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Diálogos del trabajador sobre una solicitud:
 * - Cotizar: muestra el precio de referencia congelado y las tareas, pide el
 *   precio, el motivo cuando el precio se ajusta (D5) y la agenda (D9).
 * - Reprogramar: nueva fecha, hora y duración (D9); el backend avisa al cliente.
 * La app valida para orientar al trabajador; el backend vuelve a validar todo.
 */
public class QuoteDialogHelper {

    public interface OnQuoteSentListener {
        void onQuoteSent(QuoteData quote);
    }

    public interface OnRescheduledListener {
        void onRescheduled(RequestActionData data);
    }

    private final AppCompatActivity mActivity;
    private final QuoteProvider mQuoteProvider = new QuoteProvider();

    public QuoteDialogHelper(AppCompatActivity activity) {
        mActivity = activity;
    }

    // ---------- cotizar ----------

    public void show(ServiceRequest request, OnQuoteSentListener listener) {
        // La tarifa mínima y el tope vienen del backend; el catálogo da los nombres de las tareas
        mQuoteProvider.getQuoteLimits(null, new ApiCallback<QuoteLimits>() {
            @Override
            public void onSuccess(QuoteLimits limits) {
                loadCatalogAndShow(request, limits, listener);
            }

            @Override
            public void onError(String message, int httpStatus) {
                // Sin los límites igual se puede cotizar: el backend valida al enviar
                loadCatalogAndShow(request, null, listener);
            }
        });
    }

    private void loadCatalogAndShow(ServiceRequest request, QuoteLimits limits, OnQuoteSentListener listener) {
        new CatalogProvider().getCatalog(new ApiCallback<CatalogData>() {
            @Override
            public void onSuccess(CatalogData catalog) {
                showQuoteDialog(request, limits, catalog, listener);
            }

            @Override
            public void onError(String message, int httpStatus) {
                showQuoteDialog(request, limits, null, listener);
            }
        });
    }

    private void showQuoteDialog(ServiceRequest request, QuoteLimits limits, CatalogData catalog,
                                 OnQuoteSentListener listener) {
        if (mActivity.isFinishing()) return;
        QuoteReference reference = request.getQuoteReference();

        // Pantalla completa con el diseño del mockup 16
        AppCompatDialog dialog = new AppCompatDialog(mActivity, R.style.Theme_ADS_FullScreenDialog);
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_send_quote, null);
        dialog.setContentView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tvHint = view.findViewById(R.id.tv_quote_hint);
        TextView tvReference = view.findViewById(R.id.tv_quote_reference);
        TextInputLayout tilPrice = view.findViewById(R.id.til_quote_price);
        TextInputEditText etPrice = view.findViewById(R.id.et_quote_price);
        TextView tvLimits = view.findViewById(R.id.tv_quote_limits);
        View reasonLayout = view.findViewById(R.id.layout_adjustment_reason);
        TextView reasonLabel = view.findViewById(R.id.tv_adjustment_reason_label);
        ChipGroup reasonChips = view.findViewById(R.id.chip_group_adjustment_reason);
        TextInputLayout tilReasonText = view.findViewById(R.id.til_adjustment_reason_text);
        TextInputEditText etReasonText = view.findViewById(R.id.et_adjustment_reason_text);
        ScheduleFields schedule = new ScheduleFields(mActivity, view.findViewById(R.id.schedule_fields));
        MaterialButton send = view.findViewById(R.id.btn_quote_send);
        view.findViewById(R.id.btn_quote_back).setOnClickListener(v -> dialog.dismiss());

        // «Estimado de ADS: $X – $Y» y las tareas pedidas
        tvHint.setText(estimateText(reference));
        String tasks = request.getTaskItemsDisplay(catalog);
        tvReference.setText(tasks);
        tvReference.setVisibility(tasks != null ? View.VISIBLE : View.GONE);
        if (limits != null) {
            tvLimits.setVisibility(View.VISIBLE);
            tvLimits.setText(mActivity.getString(R.string.quote_dialog_min_fee,
                    ServiceRequest.formatCop(limits.getMinPrice())));
        }

        // Motivos como chips (mockup 16); «Otro» ya viene en ADJUSTMENT y pide escribirlo (DEF-13).
        // Con los demás el texto es un comentario opcional para el cliente (D5, MEJ-01)
        for (Map.Entry<String, String> entry : ReasonHelper.ADJUSTMENT.entrySet()) {
            Chip chip = new Chip(mActivity);
            chip.setId(View.generateViewId());
            chip.setTag(entry.getKey());
            chip.setText(entry.getValue());
            chip.setCheckable(true);
            reasonChips.addView(chip);
        }
        reasonChips.setOnCheckedStateChangeListener((group, ids) -> {
            String code = selectedReason(group);
            tilReasonText.setVisibility(code != null ? View.VISIBLE : View.GONE);
            tilReasonText.setError(null);
            etReasonText.setHint(ReasonHelper.OTHER.equals(code)
                    ? R.string.cancel_reason_text_hint : R.string.quote_reason_comment_hint);
        });

        // El motivo aparece cuando el precio necesita ajuste (mismo criterio que el backend)
        Runnable updateReasonVisibility = () -> {
            Long price = parsePrice(etPrice.getText() != null ? etPrice.getText().toString() : "");
            boolean adjusted = price != null && QuoteReference.isAdjusted(reference, price);
            reasonLayout.setVisibility(adjusted ? View.VISIBLE : View.GONE);
            reasonLabel.setText(reference == null || reference.hasUnpricedItems() || !reference.hasRange()
                    ? R.string.quote_reason_label_unpriced : R.string.quote_card_reason_label);
        };
        updateReasonVisibility.run();
        etPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateReasonVisibility.run();
            }
        });

        send.setOnClickListener(v -> {
            tilPrice.setError(null);
            tilReasonText.setError(null);
            Long price = parsePrice(etPrice.getText() != null ? etPrice.getText().toString() : "");

            if (price == null || price <= 0) {
                tilPrice.setError(mActivity.getString(R.string.quote_error_invalid));
                return;
            }
            if (limits != null && price < limits.getMinPrice()) {
                tilPrice.setError(mActivity.getString(R.string.quote_dialog_min_fee,
                        ServiceRequest.formatCop(limits.getMinPrice())));
                return;
            }
            if (limits != null && price > limits.getMaxPrice()) {
                tilPrice.setError(mActivity.getString(R.string.quote_error_too_high));
                return;
            }

            String reasonCode = null;
            String reasonText = null;
            if (QuoteReference.isAdjusted(reference, price)) {
                reasonCode = selectedReason(reasonChips);
                if (reasonCode == null) {
                    Toast.makeText(mActivity, R.string.quote_reason_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                reasonText = etReasonText.getText() != null ? etReasonText.getText().toString().trim() : "";
                if (ReasonHelper.OTHER.equals(reasonCode) && TextUtils.isEmpty(reasonText)) {
                    tilReasonText.setError(mActivity.getString(R.string.reason_text_required));
                    return;
                }
                if (TextUtils.isEmpty(reasonText)) reasonText = null;
            }

            Long scheduledAt = schedule.getScheduledAt();
            if (scheduledAt == null) return;

            send.setEnabled(false);
            ApiBodies.QuoteBody body = new ApiBodies.QuoteBody(
                    price, reasonCode, reasonText, scheduledAt, schedule.getDurationMin());
            mQuoteProvider.submitQuote(request.getRequest_id(), body, new ApiCallback<QuoteData>() {
                @Override
                public void onSuccess(QuoteData quote) {
                    dialog.dismiss();
                    AdsSnackbar.showLong(mActivity, R.string.quote_sent);
                    if (listener != null) listener.onQuoteSent(quote);
                }

                @Override
                public void onError(String message, int httpStatus) {
                    send.setEnabled(true);
                    tilPrice.setError(message);
                }
            });
        });

        dialog.show();
    }

    private static String selectedReason(ChipGroup group) {
        int id = group.getCheckedChipId();
        View chip = id != View.NO_ID ? group.findViewById(id) : null;
        return chip != null ? (String) chip.getTag() : null;
    }

    /** «Estimado de ADS: $X – $Y», o el aviso cuando no hay rango. */
    private String estimateText(QuoteReference reference) {
        if (reference == null) return mActivity.getString(R.string.quote_reference_missing);
        if (!reference.hasRange()) return mActivity.getString(R.string.quote_reference_unpriced);
        String text = mActivity.getString(R.string.quote_screen_estimate,
                ServiceRequest.formatCop(reference.getMinCop()), ServiceRequest.formatCop(reference.getMaxCop()));
        return reference.hasUnpricedItems()
                ? text + "\n" + mActivity.getString(R.string.quote_reference_partial) : text;
    }

    // ---------- reprogramar ----------

    public void showReschedule(ServiceRequest request, OnRescheduledListener listener) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.view_schedule_fields, null);
        int padding = (int) (24 * mActivity.getResources().getDisplayMetrics().density);
        view.setPadding(padding, 0, padding, 0);
        ScheduleFields schedule = new ScheduleFields(mActivity, view);
        schedule.setInitial(request.getScheduledAt(), request.getEstimatedDurationMin());

        AlertDialog dialog = new MaterialAlertDialogBuilder(mActivity)
                .setTitle(R.string.reschedule_title)
                .setMessage(R.string.reschedule_message)
                .setView(view)
                .setPositiveButton(R.string.reschedule_send, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Long scheduledAt = schedule.getScheduledAt();
            if (scheduledAt == null) return;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new RequestProvider(mActivity).reschedule(request.getRequest_id(), scheduledAt, schedule.getDurationMin(),
                    new ApiCallback<RequestActionData>() {
                        @Override
                        public void onSuccess(RequestActionData data) {
                            dialog.dismiss();
                            AdsSnackbar.showLong(mActivity, R.string.reschedule_done);
                            if (listener != null) listener.onRescheduled(data);
                        }

                        @Override
                        public void onError(String message, int httpStatus) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            schedule.showError(message);
                        }
                    });
        }));
        dialog.show();
    }

    private static Long parsePrice(String text) {
        String digits = text.replaceAll("[^0-9]", "");
        if (TextUtils.isEmpty(digits)) return null;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
