package com.example.weighttracker.ui.main;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weighttracker.R;
import com.example.weighttracker.adapter.WeightAdapter;
import com.example.weighttracker.model.WeightEntry;
import com.example.weighttracker.ui.main.MainViewModel;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MainViewModel weightViewModel;
    private WeightAdapter weightAdapter;
    private long currentUserId;
    private TextView tvMovingAverage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentUserId = getIntent().getLongExtra("USER_ID", -1);
        if (currentUserId == -1) {
            Toast.makeText(this, "Error: User ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EditText etDate = findViewById(R.id.et_date);
        EditText etWeight = findViewById(R.id.et_weight);
        EditText etGoal = findViewById(R.id.et_goal);
        Button btnAdd = findViewById(R.id.btn_add);
        tvMovingAverage = findViewById(R.id.tv_moving_average);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        weightAdapter = new WeightAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(weightAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        weightViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Observe weight entries
        weightViewModel.getWeightEntries(currentUserId).observe(this, entries -> {
            weightAdapter.setWeightList(entries);
        });

        // Observe 7-day moving average values
        weightViewModel.getMovingAverage().observe(this, averages -> {
            if (averages == null || averages.isEmpty()) {
                tvMovingAverage.setText("7-Day Moving Average: N/A");
            } else {
                double latestAverage = averages.get(averages.size() - 1);
                tvMovingAverage.setText(String.format("7-Day Moving Average: %.2f", latestAverage));
            }
        });

        btnAdd.setOnClickListener(v -> {
            String date = etDate.getText().toString().trim();
            String weightStr = etWeight.getText().toString().trim();
            String goalStr = etGoal.getText().toString().trim();

            if (date.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "Date and weight are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double weight = Double.parseDouble(weightStr);
                Double goal = goalStr.isEmpty() ? null : Double.parseDouble(goalStr);

                WeightEntry newEntry = new WeightEntry(currentUserId, date, weight, goal);
                weightViewModel.addWeight(
                        newEntry.getUserId(),
                        newEntry.getDate(),
                        newEntry.getWeight(),
                        newEntry.getGoal(),
                        null
                );

                etDate.setText("");
                etWeight.setText("");
                etGoal.setText("");

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
