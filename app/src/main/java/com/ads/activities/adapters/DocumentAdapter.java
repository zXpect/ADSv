package com.ads.activities.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.ads.R;
import com.ads.models.WorkerDocument;

import java.util.ArrayList;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private Context context;
    private List<WorkerDocument> documents;
    private OnDocumentActionListener listener;

    public interface OnDocumentActionListener {
        void onViewDocument(WorkerDocument document);
        void onUpdateDocument(WorkerDocument document);
    }

    public DocumentAdapter(Context context, OnDocumentActionListener listener) {
        this.context = context;
        this.documents = new ArrayList<>();
        this.listener = listener;
    }

    public void setDocuments(List<WorkerDocument> documents) {
        this.documents = documents != null ? documents : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addDocument(WorkerDocument document) {
        if (document != null) {
            this.documents.add(document);
            notifyItemInserted(documents.size() - 1);
        }
    }

    public void updateDocument(int position, WorkerDocument document) {
        if (position >= 0 && position < documents.size() && document != null) {
            this.documents.set(position, document);
            notifyItemChanged(position);
        }
    }

    public void removeDocument(int position) {
        if (position >= 0 && position < documents.size()) {
            this.documents.remove(position);
            notifyItemRemoved(position);
        }
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
        WorkerDocument document = documents.get(position);

        // Configurar información básica del documento usando métodos del modelo
        holder.tvDocumentType.setText(getDocumentDisplayName(document));

        // Mostrar nombre de archivo solo si está disponible
        if (document.getFileName() != null && !document.getFileName().isEmpty()) {
            holder.tvFileName.setVisibility(View.VISIBLE);
            holder.tvFileName.setText(document.getFileName());
        } else {
            holder.tvFileName.setVisibility(View.GONE);
        }

        // Configurar fecha de subida usando el método del modelo
        holder.tvUploadDate.setText("Subido: " + document.getFormattedUploadDate());

        // Configurar estado y apariencia según el status usando métodos del modelo
        if (document.isApproved()) {
            setupApprovedStatus(holder, document);
        } else if (document.isRejected()) {
            setupRejectedStatus(holder, document);
        } else {
            setupPendingStatus(holder, document);
        }

        // Configurar icono de tipo de documento usando métodos del modelo
        setupDocumentIcon(holder, document);

        // Click listeners para los botones
        holder.btnViewDocument.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDocument(document);
            }
        });

        holder.btnUpdateDocument.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpdateDocument(document);
            }
        });

        // Click en la card completa también abre el documento
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDocument(document);
            }
        });
    }

    private void setupApprovedStatus(DocumentViewHolder holder, WorkerDocument document) {
        holder.tvStatus.setText("✓ Aprobado");
        holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);

        holder.ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
        holder.ivStatusIcon.setColorFilter(Color.parseColor("#4CAF50"));

        // Mostrar fecha de revisión si está disponible
        if (document.isReviewed()) {
            holder.tvReviewDate.setVisibility(View.VISIBLE);
            holder.tvReviewDate.setText("Revisado: " + document.getFormattedReviewDate());
        } else {
            holder.tvReviewDate.setVisibility(View.GONE);
        }

        holder.layoutRejectionReason.setVisibility(View.GONE);
        holder.btnUpdateDocument.setVisibility(View.GONE);
    }

    private void setupRejectedStatus(DocumentViewHolder holder, WorkerDocument document) {
        holder.tvStatus.setText("✗ Rechazado");
        holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);

        holder.ivStatusIcon.setImageResource(R.drawable.ic_error);
        holder.ivStatusIcon.setColorFilter(Color.parseColor("#F44336"));

        // Mostrar fecha de revisión
        if (document.isReviewed()) {
            holder.tvReviewDate.setVisibility(View.VISIBLE);
            holder.tvReviewDate.setText("Revisado: " + document.getFormattedReviewDate());
        } else {
            holder.tvReviewDate.setVisibility(View.GONE);
        }

        // Mostrar razón de rechazo
        if (document.getRejectionReason() != null && !document.getRejectionReason().isEmpty()) {
            holder.layoutRejectionReason.setVisibility(View.VISIBLE);
            holder.tvRejectionReason.setText(document.getRejectionReason());
        } else {
            holder.layoutRejectionReason.setVisibility(View.VISIBLE);
            holder.tvRejectionReason.setText("El documento no cumple con los requisitos establecidos.");
        }

        // Mostrar botón de actualizar
        holder.btnUpdateDocument.setVisibility(View.VISIBLE);
    }

    private void setupPendingStatus(DocumentViewHolder holder, WorkerDocument document) {
        holder.tvStatus.setText("⏳ En revisión");
        holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge);

        holder.ivStatusIcon.setImageResource(R.drawable.ic_pending);
        holder.ivStatusIcon.setColorFilter(Color.parseColor("#FF9800"));

        holder.tvReviewDate.setVisibility(View.GONE);
        holder.layoutRejectionReason.setVisibility(View.GONE);
        holder.btnUpdateDocument.setVisibility(View.GONE);
    }

    private void setupDocumentIcon(DocumentViewHolder holder, WorkerDocument document) {
        // Usar métodos del modelo para determinar el icono
        if (document.isPDF()) {
            holder.ivDocumentIcon.setImageResource(R.drawable.ic_pdf);
        } else if (document.isImage()) {
            holder.ivDocumentIcon.setImageResource(R.drawable.ic_image);
        } else {
            holder.ivDocumentIcon.setImageResource(R.drawable.ic_document);
        }
    }

    private String getDocumentDisplayName(WorkerDocument document) {
        // Si tiene descripción personalizada, usarla
        if (document.getDescription() != null && !document.getDescription().isEmpty()) {
            return document.getDescription();
        }

        // Usar el método del modelo para obtener el tipo formateado
        if (document.isHojaVida()) {
            return "Hoja de Vida (CV)";
        } else if (document.isAntecedentes()) {
            return "Antecedentes Judiciales";
        } else if (document.isTitulo()) {
            int orden = document.getOrden();
            if (orden > 0) {
                return "Título/Certificado " + orden;
            }
            return "Título o Certificado";
        } else if (document.isCartaRecomendacion()) {
            int orden = document.getOrden();
            if (orden > 0) {
                return "Carta de Recomendación " + orden;
            }
            return "Carta de Recomendación";
        }

        return document.getFormattedDocumentType();
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivDocumentIcon;
        ImageView ivStatusIcon;
        TextView tvDocumentType;
        TextView tvFileName;
        TextView tvUploadDate;
        TextView tvReviewDate;
        TextView tvStatus;
        LinearLayout layoutRejectionReason;
        TextView tvRejectionReason;
        LinearLayout layoutActions;
        Button btnViewDocument;
        Button btnUpdateDocument;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardDocument);
            ivDocumentIcon = itemView.findViewById(R.id.ivDocumentIcon);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            tvDocumentType = itemView.findViewById(R.id.tvDocumentType);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvUploadDate = itemView.findViewById(R.id.tvUploadDate);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            layoutRejectionReason = itemView.findViewById(R.id.layoutRejectionReason);
            tvRejectionReason = itemView.findViewById(R.id.tvRejectionReason);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnViewDocument = itemView.findViewById(R.id.btnViewDocument);
            btnUpdateDocument = itemView.findViewById(R.id.btnUpdateDocument);
        }
    }
}