package com.ads.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ads.models.ServiceRequest;
import com.project.ads.R;

import java.util.List;

public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {

    private List<ServiceRequest> requests;
    private OnRequestClickListener listener;

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
            if (listener != null) {
                listener.onRequestClick(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvRequestId;
        private TextView tvClientName;
        private TextView tvServiceType;
        private TextView tvAddress;
        private TextView tvTimestamp;
        private TextView tvUrgencyLevel;
        private TextView tvEstimatedCost;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvRequestId = itemView.findViewById(R.id.tvRequestId);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUrgencyLevel = itemView.findViewById(R.id.tvUrgencyLevel);
            tvEstimatedCost = itemView.findViewById(R.id.tvEstimatedCost);
        }

        public void bind(ServiceRequest request) {
            // Set request ID with better formatting
            String requestId = request.getRequest_id() != null ? request.getRequest_id() : "N/A";
            tvRequestId.setText("Solicitud #" + requestId);
            
            // Set client name
            tvClientName.setText(request.getClient_name() != null ? request.getClient_name() : "Cliente no especificado");
            
            // Set service type
            tvServiceType.setText(request.getService_type() != null ? request.getService_type() : "Servicio no especificado");
            
            // Set address
            tvAddress.setText(request.getAddress() != null ? request.getAddress() : "Dirección no especificada");
            
            // Set timestamp
            tvTimestamp.setText(request.getFormattedTimestamp());

            // Set urgency level with appropriate badge background
            String urgency = request.getUrgency_level() != null ? request.getUrgency_level() : "Normal";
            tvUrgencyLevel.setText(urgency.toUpperCase());

            // Set urgency badge background based on level
            int urgencyBackground;
            switch (urgency.toLowerCase()) {
                case "alta":
                case "high":
                    urgencyBackground = itemView.getContext().getResources().getIdentifier(
                            "urgency_badge_high", "drawable", itemView.getContext().getPackageName());
                    break;
                case "media":
                case "medium":
                    urgencyBackground = itemView.getContext().getResources().getIdentifier(
                            "urgency_badge_medium", "drawable", itemView.getContext().getPackageName());
                    break;
                default:
                    urgencyBackground = itemView.getContext().getResources().getIdentifier(
                            "urgency_badge_low", "drawable", itemView.getContext().getPackageName());
                    break;
            }
            tvUrgencyLevel.setBackgroundResource(urgencyBackground);

            // Set estimated cost with better formatting
            if (request.getEstimated_cost() > 0) {
                tvEstimatedCost.setText(String.format("$%,.0f", request.getEstimated_cost()));
                tvEstimatedCost.setTextColor(itemView.getContext().getResources().getColor(R.color.success_color));
            } else {
                tvEstimatedCost.setText("Por determinar");
                tvEstimatedCost.setTextColor(itemView.getContext().getResources().getColor(R.color.secondary_text));
            }
        }
    }
}