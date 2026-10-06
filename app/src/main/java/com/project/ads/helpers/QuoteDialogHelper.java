package com.project.ads.helpers;

import android.app.ProgressDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.project.ads.R;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.api.QuoteData;
import com.project.ads.models.api.QuoteLimits;
import com.project.ads.providers.ApiCallback;
import com.project.ads.providers.QuoteProvider;

/**
 * Diálogo del trabajador para enviar una cotización. Consulta el rango
 * permitido al backend, valida en la app y envía el precio; el backend vuelve
 * a validar todo (la app nunca es la autoridad sobre el precio).
 */
public class QuoteDialogHelper {

    public interface OnQuoteSentListener {
        void onQuoteSent(QuoteData quote);
    }

    private final AppCompatActivity mActivity;
    private final QuoteProvider mQuoteProvider = new QuoteProvider();

    public QuoteDialogHelper(AppCompatActivity activity) {
        mActivity = activity;
    }

    public void show(String requestId, String serviceType, OnQuoteSentListener listener) {
        ProgressDialog progress = ProgressDialog.show(mActivity, null,
                mActivity.getString(R.string.quote_loading_limits), true, false);

        mQuoteProvider.getQuoteLimits(serviceType, new ApiCallback<QuoteLimits>() {
            @Override
            public void onSuccess(QuoteLimits limits) {
                progress.dismiss();
                showDialog(requestId, limits, listener);
            }

            @Override
            public void onError(String message, int httpStatus) {
                progress.dismiss();
                // Sin el rango igual se puede cotizar: el backend valida al enviar
                showDialog(requestId, null, listener);
            }
        });
    }

    private void showDialog(String requestId, QuoteLimits limits, OnQuoteSentListener listener) {
        if (mActivity.isFinishing()) return;

        View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_send_quote, null);
        TextInputLayout tilPrice = view.findViewById(R.id.til_quote_price);
        TextInputEditText etPrice = view.findViewById(R.id.et_quote_price);
        TextView tvLimits = view.findViewById(R.id.tv_quote_limits);

        if (limits != null) {
            tvLimits.setVisibility(View.VISIBLE);
            tvLimits.setText(mActivity.getString(R.string.quote_dialog_limits,
                    ServiceRequest.formatCop(limits.getMinPrice()),
                    ServiceRequest.formatCop(limits.getMaxPrice())));
        }

        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.quote_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.quote_dialog_send, null)
                .setNegativeButton(R.string.quote_dialog_cancel, null)
                .create();

        // Se reemplaza el click para validar sin cerrar el diálogo
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            tilPrice.setError(null);
            Long price = parsePrice(etPrice.getText() != null ? etPrice.getText().toString() : "");

            if (price == null || price <= 0) {
                tilPrice.setError(mActivity.getString(R.string.quote_error_invalid));
                return;
            }
            if (limits != null && (price < limits.getMinPrice() || price > limits.getMaxPrice())) {
                tilPrice.setError(mActivity.getString(R.string.quote_dialog_limits,
                        ServiceRequest.formatCop(limits.getMinPrice()),
                        ServiceRequest.formatCop(limits.getMaxPrice())));
                return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            mQuoteProvider.submitQuote(requestId, price, new ApiCallback<QuoteData>() {
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
