package com.example.weighttracker.utils;

import com.example.weighttracker.model.WeightEntry;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WeightUtils {

    // Sort entries by date in ascending order (assumes ISO format: yyyy-MM-dd)
    public static void sortEntriesByDate(List<WeightEntry> entries) {
        Collections.sort(entries, new Comparator<WeightEntry>() {
            @Override
            public int compare(WeightEntry e1, WeightEntry e2) {
                return e1.getDate().compareTo(e2.getDate());
            }
        });
    }

    // Binary search for a weight entry by date
    public static WeightEntry findEntryByDate(List<WeightEntry> sortedEntries, String targetDate) {
        int left = 0;
        int right = sortedEntries.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            WeightEntry midEntry = sortedEntries.get(mid);
            int cmp = midEntry.getDate().compareTo(targetDate);

            if (cmp == 0) {
                return midEntry;
            } else if (cmp < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return null; // Not found
    }

    // Calculate 7-day moving average
    public static double[] calculate7DayMovingAverage(List<WeightEntry> sortedEntries) {
        int days = 7;
        int n = sortedEntries.size();
        if (n < days) return new double[0];

        double[] averages = new double[n - days + 1];

        for (int i = 0; i <= n - days; i++) {
            double sum = 0;
            for (int j = 0; j < days; j++) {
                sum += sortedEntries.get(i + j).getWeight();
            }
            averages[i] = sum / days;
        }

        return averages;
    }
}
