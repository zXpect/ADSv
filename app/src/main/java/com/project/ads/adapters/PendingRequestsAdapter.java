package com.project.ads.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.project.ads.R;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.api.QuoteReference;

import java.util.List;

/**
 * Tarjetas de la bandeja del trabajador (mockup 14): cliente y hace cuánto,
 * oficio, dirección y precio. Las solicitudes nuevas llevan borde naranja.
 */
public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {

    private final List<ServiceRequest> requests;
    private final OnRequestClickListener listener;

    public interface OnRequestClickListener {
        void onRequestClick(ServiceRequest request);
    }

    public PendingRequestsAdapter(List<ServiceRequest> requests, OnRequestClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceRequest request = requests.get(position);
        holder.bind(request);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRequestClick(request);
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final TextView tvClientName;
        private final TextView tvServiceType;
        private final TextView tvAddress;
        private final TextView tvTimestamp;
        private final TextView tvEstimatedCost;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_pending_request);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvEstimatedCost = itemView.findViewById(R.id.tvEstimatedCost);
        }

        public void bind(ServiceRequest request) {
            tvClientName.setText(!TextUtils.isEmpty(request.getClient_name())
                    ? request.getClient_name() : itemView.getContext().getString(R.string.inbox_client_default));
            tvTimestamp.setText(request.getTimeAgo());

            String service = request.getService_type() != null ? request.getService_type() : "";
            if (!TextUtils.isEmpty(request.getDescription())) service += " · " + request.getDescription();
            tvServiceType.setText(service);

            boolean hasAddress = !TextUtils.isEmpty(request.getAddress());
            tvAddress.setText(hasAddress ? request.getAddress() : "");
            tvAddress.setVisibility(hasAddress ? View.VISIBLE : View.INVISIBLE);

            // Precio cotizado si ya hay; si no, el rango de referencia congelado
            QuoteReference reference = request.getQuoteReference();
            if (request.hasQuote()) {
                tvEstimatedCost.setText(request.getFormattedQuotedPrice());
                tvEstimatedCost.setVisibility(View.VISIBLE);
            } else if (reference != null && reference.hasRange()) {
                tvEstimatedCost.setText(ServiceRequest.formatCop(reference.getMinCop()) + " – "
                        + ServiceRequest.formatCop(reference.getMaxCop()));
                tvEstimatedCost.setVisibility(View.VISIBLE);
            } else {
                tvEstimatedCost.setText(R.string.inbox_price_to_quote);
                tvEstimatedCost.setVisibility(View.VISIBLE);
            }

            // Nuevas (por responder): borde naranja
            boolean isNew = "pending".equals(request.getStatus());
            card.setStrokeColor(ContextCompat.getColor(itemView.getContext(),
                    isNew ? R.color.ads_brand_text : R.color.ads_line));
            card.setStrokeWidth(itemView.getResources().getDimensionPixelSize(
                    isNew ? R.dimen.stroke_focus : R.dimen.stroke_control));
        }
    }
}
