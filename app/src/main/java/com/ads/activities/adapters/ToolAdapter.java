package com.ads.activities.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ads.models.Tool;
import com.project.ads.R;

import java.util.List;

public class ToolAdapter extends RecyclerView.Adapter<ToolAdapter.ViewHolder> {

    private List<Tool> mTools;
    private Context context;

    public ToolAdapter(Context context, List<Tool> tools) {
        this.context = context;
        this.mTools = tools;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tool, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tool tool = mTools.get(position);
        holder.textViewName.setText(tool.getName());
        holder.textViewPrice.setText(String.format("$%.2f", tool.getPrice()));
        holder.imageViewTool.setImageResource(tool.getImageResourceId());
    }

    @Override
    public int getItemCount() {
        return mTools.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewPrice;
        ImageView imageViewTool;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewToolName);
            textViewPrice = itemView.findViewById(R.id.textViewToolPrice);
            imageViewTool = itemView.findViewById(R.id.imageViewTool);
        }
    }
}