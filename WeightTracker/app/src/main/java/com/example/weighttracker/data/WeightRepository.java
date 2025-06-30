package com.example.weighttracker.data;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.weighttracker.model.WeightEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WeightRepository {

    private final DatabaseHelper dbHelper;
    private final MutableLiveData<List<WeightEntry>> weightEntriesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Double>> movingAverageLiveData = new MutableLiveData<>();

    public WeightRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
        weightEntriesLiveData.setValue(new ArrayList<>());
        movingAverageLiveData.setValue(new ArrayList<>());
    }

    // Expose weight entries for a specific user as LiveData
    public LiveData<List<WeightEntry>> getWeightsForUser(long userId) {
        loadWeightsFromDb(userId);
        return weightEntriesLiveData;
    }

    // Expose moving average LiveData
    public LiveData<List<Double>> getMovingAverage() {
        return movingAverageLiveData;
    }

    // Load weights from DB and update LiveData objects
    private void loadWeightsFromDb(long userId) {
        List<WeightEntry> weights = new ArrayList<>();
        Cursor cursor = dbHelper.getWeightsByUser(userId);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT_ID));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE));
                    double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT));

                    int goalIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_GOAL);
                    Double goal = null;
                    if (goalIndex != -1 && !cursor.isNull(goalIndex)) {
                        goal = cursor.getDouble(goalIndex);
                    }

                    WeightEntry entry = new WeightEntry(userId, date, weight, goal);
                    weights.add(entry);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        // Sort by date ascending (ISO date string comparison)
        Collections.sort(weights, Comparator.comparing(WeightEntry::getDate));

        weightEntriesLiveData.postValue(weights);

        // Calculate and post moving average with window of 7 days
        List<Double> averages = calculateMovingAverage(weights, 7);
        movingAverageLiveData.postValue(averages);
    }

    // Insert a new weight entry and reload data
    public void insertWeight(WeightEntry entry) {
        dbHelper.addWeight(entry.getUserId(), entry.getDate(), entry.getWeight(), entry.getGoal());
        loadWeightsFromDb(entry.getUserId());
    }

    // Remove a weight entry and reload data
    public void removeWeight(long weightId, long userId) {
        dbHelper.deleteWeight(weightId);
        loadWeightsFromDb(userId);
    }

    // Binary search for weight entry by date in sorted list
    public WeightEntry findWeightByDate(List<WeightEntry> entries, String targetDate) {
        int left = 0;
        int right = entries.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            String midDate = entries.get(mid).getDate();
            int cmp = midDate.compareTo(targetDate);

            if (cmp == 0) {
                return entries.get(mid);
            } else if (cmp < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return null; // Not found
    }

    // Calculate moving average over a sliding window of 'days' on weight entries
    public List<Double> calculateMovingAverage(List<WeightEntry> entries, int days) {
        List<Double> averages = new ArrayList<>();

        if (entries.size() < days) {
            // Not enough entries to calculate moving average
            return averages;
        }

        for (int i = days - 1; i < entries.size(); i++) {
            double sum = 0;
            for (int j = 0; j < days; j++) {
                sum += entries.get(i - j).getWeight();
            }
            averages.add(sum / days);
        }

        return averages;
    }

    // Delete a user and all their weights
    public boolean deleteUserAndWeights(long userId) {
        return dbHelper.deleteUserAndWeights(userId);
    }

    // Close database helper resources
    public void close() {
        dbHelper.close();
    }
}
