package com.project.ads.helpers;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

        View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_send_quote, null);
        TextView tvReference = view.findViewById(R.id.tv_quote_reference);
        TextInputLayout tilPrice = view.findViewById(R.id.til_quote_price);
        TextInputEditText etPrice = view.findViewById(R.id.et_quote_price);
        TextView tvLimits = view.findViewById(R.id.tv_quote_limits);
        View reasonLayout = view.findViewById(R.id.layout_adjustment_reason);
        TextView reasonLabel = view.findViewById(R.id.tv_adjustment_reason_label);
        Spinner reasonSpinner = view.findViewById(R.id.sp_adjustment_reason);
        TextInputLayout tilReasonText = view.findViewById(R.id.til_adjustment_reason_text);
        TextInputEditText etReasonText = view.findViewById(R.id.et_adjustment_reason_text);
        ScheduleFields schedule = new ScheduleFields(mActivity, view.findViewById(R.id.schedule_fields));

        tvReference.setText(referenceText(request, reference, catalog));
        if (limits != null) {
            tvLimits.setVisibility(View.VISIBLE);
            tvLimits.setText(mActivity.getString(R.string.quote_dialog_min_fee,
                    ServiceRequest.formatCop(limits.getMinPrice())));
        }

        // Motivos: primera opción vacía para obligar a elegir
        List<String> codes = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        codes.add(null);
        labels.add(mActivity.getString(R.string.quote_reason_choose));
        for (Map.Entry<String, String> entry : ReasonHelper.ADJUSTMENT.entrySet()) {
            codes.add(entry.getKey());
            labels.add(entry.getValue());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reasonSpinner.setAdapter(adapter);
        reasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                tilReasonText.setVisibility(ReasonHelper.OTHER.equals(codes.get(position)) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // El motivo aparece cuando el precio necesita ajuste (mismo criterio que el backend)
        Runnable updateReasonVisibility = () -> {
            Long price = parsePrice(etPrice.getText() != null ? etPrice.getText().toString() : "");
            boolean adjusted = price == null || QuoteReference.isAdjusted(reference, price);
            reasonLayout.setVisibility(adjusted ? View.VISIBLE : View.GONE);
            reasonLabel.setText(reference == null || reference.hasUnpricedItems() || !reference.hasRange()
                    ? R.string.quote_reason_label_unpriced : R.string.quote_reason_label);
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

        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.quote_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.quote_dialog_send, null)
                .setNegativeButton(R.string.quote_dialog_cancel, null)
                .create();

        // Se reemplaza el click para validar sin cerrar el diálogo
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
                reasonCode = codes.get(reasonSpinner.getSelectedItemPosition());
                if (reasonCode == null) {
                    Toast.makeText(mActivity, R.string.quote_reason_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (ReasonHelper.OTHER.equals(reasonCode)) {
                    reasonText = etReasonText.getText() != null ? etReasonText.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(reasonText)) {
                        tilReasonText.setError(mActivity.getString(R.string.reason_text_required));
                        return;
                    }
                }
            }

            Long scheduledAt = schedule.getScheduledAt();
            if (scheduledAt == null) return;

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            ApiBodies.QuoteBody body = new ApiBodies.QuoteBody(
                    price, reasonCode, reasonText, scheduledAt, schedule.getDurationMin());
            mQuoteProvider.submitQuote(request.getRequest_id(), body, new ApiCallback<QuoteData>() {
                @Override
                public void onSuccess(QuoteData quote) {
                    dialog.dismiss();
                    Toast.makeText(mActivity, R.string.quote_sent, Toast.LENGTH_LONG).show();
                    if (listener != null) listener.onQuoteSent(quote);
                }

                @Override
                public void onError(String message, int httpStatus) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    tilPrice.setError(message);
                }
            });
        }));

        dialog.show();
    }

    /** Tareas pedidas y rango de referencia congelado, para orientar la cotización. */
    private String referenceText(ServiceRequest request, QuoteReference reference, CatalogData catalog) {
        StringBuilder text = new StringBuilder();
        String tasks = request.getTaskItemsDisplay(catalog);
        if (tasks != null) text.append(tasks).append("\n\n");
        if (reference == null) {
            text.append(mActivity.getString(R.string.quote_reference_missing));
        } else if (!reference.hasRange()) {
            text.append(mActivity.getString(R.string.quote_reference_unpriced));
        } else {
            text.append(mActivity.getString(R.string.quote_reference_range,
                    ServiceRequest.formatCop(reference.getMinCop()), ServiceRequest.formatCop(reference.getMaxCop())));
            if (reference.hasUnpricedItems()) {
                text.append('\n').append(mActivity.getString(R.string.quote_reference_partial));
            }
        }
        return text.toString();
    }

    // ---------- reprogramar ----------

    public void showReschedule(ServiceRequest request, OnRescheduledListener listener) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.view_schedule_fields, null);
        int padding = (int) (24 * mActivity.getResources().getDisplayMetrics().density);
        view.setPadding(padding, 0, padding, 0);
        ScheduleFields schedule = new ScheduleFields(mActivity, view);
        schedule.setInitial(request.getScheduledAt(), request.getEstimatedDurationMin());

        AlertDialog dialog = new AlertDialog.Builder(mActivity)
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
                            Toast.makeText(mActivity, R.string.reschedule_done, Toast.LENGTH_LONG).show();
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
