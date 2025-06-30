package com.example.weighttracker.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weighttracker.R;
import com.example.weighttracker.model.WeightEntry;

import java.util.ArrayList;
import java.util.List;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.ViewHolder> {
    private final Context context;
    private List<WeightEntry> weightList = new ArrayList<>();

    public WeightAdapter(Context context, List<WeightEntry> weightList) {
        this.context = context;
        this.weightList = weightList;
    }

    public void setWeightList(List<WeightEntry> weightList) {
        this.weightList = weightList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_weight, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeightEntry entry = weightList.get(position);

        holder.tvDate.setText(entry.getDate());
        holder.tvWeight.setText(String.format("%.1f lbs", entry.getWeight()));

        if (entry.getGoal() != null) {
            double currentWeight = entry.getWeight();
            double goal = entry.getGoal();

            holder.tvGoal.setText(String.format("%.1f lbs", goal));
            holder.tvGoalLabel.setVisibility(View.VISIBLE);
            holder.tvGoal.setVisibility(View.VISIBLE);
            holder.tvProgress.setVisibility(View.VISIBLE);

            if (currentWeight <= goal) {
                holder.tvProgress.setText("✓ Achieved");
                holder.tvProgress.setTextColor(ContextCompat.getColor(context, R.color.green_500));
            } else {
                holder.tvProgress.setText(String.format("%.1f lbs to go", currentWeight - goal));
                holder.tvProgress.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
            }
        } else {
            holder.tvGoalLabel.setVisibility(View.GONE);
            holder.tvGoal.setVisibility(View.GONE);
            holder.tvProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return weightList != null ? weightList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvWeight, tvGoalLabel, tvGoal, tvProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvWeight = itemView.findViewById(R.id.tv_weight);
            tvGoalLabel = itemView.findViewById(R.id.tv_goal_label);
            tvGoal = itemView.findViewById(R.id.tv_goal);
            tvProgress = itemView.findViewById(R.id.tv_progress);
        }
    }
}
