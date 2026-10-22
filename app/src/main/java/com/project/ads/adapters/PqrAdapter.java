package com.project.ads.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.project.ads.R;
import com.project.ads.models.Pqr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lista de PQR (mockups 42 y 54). El estado va en texto (Abierta, En revisión,
 * Respondida), no solo en color. Tocar una tarjeta muestra el texto completo.
 */
public class PqrAdapter extends RecyclerView.Adapter<PqrAdapter.Holder> {

    private final List<Pqr> mItems = new ArrayList<>();
    private final Set<Long> mExpanded = new HashSet<>();

    public void setItems(List<Pqr> items) {
        mItems.clear();
        if (items != null) mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pqr, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(mItems.get(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        final TextView type, status, description, dates, response;
        final View responseBlock;

        Holder(View view) {
            super(view);
            type = view.findViewById(R.id.tv_pqr_type);
            status = view.findViewById(R.id.tv_pqr_status);
            description = view.findViewById(R.id.tv_pqr_description);
            dates = view.findViewById(R.id.tv_pqr_dates);
            response = view.findViewById(R.id.tv_pqr_response);
            responseBlock = view.findViewById(R.id.layout_pqr_response);
        }

        void bind(Pqr pqr) {
            Context context = itemView.getContext();
            boolean expanded = mExpanded.contains(pqr.getId());

            type.setText(Pqr.typeLabel(pqr.getType()));
            status.setText(Pqr.statusLabel(pqr.getStatus()));
            status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                    pqr.isClosed() ? R.color.ads_surface_alt : R.color.ads_chip_bg)));
            status.setTextColor(ContextCompat.getColor(context,
                    pqr.isClosed() ? R.color.ads_charcoal : R.color.ads_chip_text));

            description.setText(pqr.getDescription());
            description.setMaxLines(expanded ? Integer.MAX_VALUE : 2);

            String sent = context.getString(R.string.pqr_sent_on, Pqr.shortDate(pqr.getCreatedAt()));
            dates.setText(pqr.isClosed() ? sent
                    : context.getString(R.string.pqr_dates_open, sent, Pqr.shortDate(pqr.getDueAt())));

            responseBlock.setVisibility(pqr.hasResponse() ? View.VISIBLE : View.GONE);
            response.setText(pqr.getResponse());
            response.setMaxLines(expanded ? Integer.MAX_VALUE : 3);

            itemView.setOnClickListener(v -> {
                if (!mExpanded.remove(pqr.getId())) mExpanded.add(pqr.getId());
                notifyItemChanged(getBindingAdapterPosition());
            });
        }
    }
}
