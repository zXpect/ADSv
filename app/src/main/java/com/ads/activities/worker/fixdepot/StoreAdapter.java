package com.ads.activities.worker.fixdepot;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.ads.R;

import java.util.List;
import java.util.Map;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private List<StoreFinderActivity.Store> mStores;

    public StoreAdapter(List<StoreFinderActivity.Store> stores) {
        mStores = stores;
    }

    public void updateStores(List<StoreFinderActivity.Store> stores) {
        mStores = stores;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_store, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        StoreFinderActivity.Store store = mStores.get(position);
        holder.textStoreName.setText(store.getName());
        holder.ratingBar.setRating(store.getRating());

        // Build the available tools list
        StringBuilder toolsBuilder = new StringBuilder();
        for (Map.Entry<String, Float> entry : store.getToolPrices().entrySet()) {
            if (toolsBuilder.length() > 0) {
                toolsBuilder.append("\n");
            }
            toolsBuilder.append(entry.getKey())
                    .append(": $")
                    .append(String.format("%.2f", entry.getValue()));
        }
        holder.textAvailableTools.setText(toolsBuilder.toString());
    }

    @Override
    public int getItemCount() {
        return mStores.size();
    }

    static class StoreViewHolder extends RecyclerView.ViewHolder {
        TextView textStoreName;
        RatingBar ratingBar;
        TextView textAvailableTools;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            textStoreName = itemView.findViewById(R.id.textStoreName);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            textAvailableTools = itemView.findViewById(R.id.textAvailableTools);
        }
    }
}