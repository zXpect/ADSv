package com.ads.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ads.activities.worker.DocumentItem;
import com.ads.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DocumentsAdapter extends RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder> {

    private List<DocumentItem> documents;
    private OnDocumentClickListener listener;

    public interface OnDocumentClickListener {
        void onDocumentClick(DocumentItem document);
    }

    public DocumentsAdapter(List<DocumentItem> documents, OnDocumentClickListener listener) {
        this.documents = documents;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        DocumentItem document = documents.get(position);
        holder.bind(document);
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivDocumentIcon;
        ImageView ivStatusIcon;
        TextView tvDocumentType;
        TextView tvDocumentStatus;
        TextView tvUploadDate;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardDocument);
            ivDocumentIcon = itemView.findViewById(R.id.ivDocumentIcon);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            tvDocumentType = itemView.findViewById(R.id.tvDocumentType);
            tvDocumentStatus = itemView.findViewById(R.id.tvStatus);
            tvUploadDate = itemView.findViewById(R.id.tvUploadDate);
        }

        public void bind(DocumentItem document) {
            tvDocumentType.setText(document.getType());

            // Configurar estado
            switch (document.getStatus().toLowerCase()) {
                case "approved":
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(itemView.getContext().getColor(R.color.success_color));
                    tvDocumentStatus.setText("Aprobado");
                    tvDocumentStatus.setTextColor(itemView.getContext().getColor(R.color.success_color));
                    break;
                case "pending":
                    ivStatusIcon.setImageResource(R.drawable.ic_pending);
                    ivStatusIcon.setColorFilter(itemView.getContext().getColor(R.color.warning_color));
                    tvDocumentStatus.setText("En revisión");
                    tvDocumentStatus.setTextColor(itemView.getContext().getColor(R.color.warning_color));
                    break;
                case "rejected":
                    ivStatusIcon.setImageResource(R.drawable.ic_error);
                    ivStatusIcon.setColorFilter(itemView.getContext().getColor(R.color.error_color));
                    tvDocumentStatus.setText("Rechazado");
                    tvDocumentStatus.setTextColor(itemView.getContext().getColor(R.color.error_color));
                    break;
            }

            // Fecha de subida
            if (document.getUploadedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                String date = sdf.format(new Date(document.getUploadedAt()));
                tvUploadDate.setText("Subido: " + date);
            } else {
                tvUploadDate.setText("");
            }

            // Click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDocumentClick(document);
                }
            });
        }
    }
}