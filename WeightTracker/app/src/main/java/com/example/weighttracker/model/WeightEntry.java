package com.example.weighttracker.model;

public class WeightEntry {
    private long userId;
    private String date;
    private double weight;
    private Double goal; // optional

    public WeightEntry(long userId, String date, double weight, Double goal) {
        this.userId = userId;
        this.date = date;
        this.weight = weight;
        this.goal = goal;
    }

    public long getUserId() {
        return userId;
    }

    public String getDate() {
        return date;
    }

    public double getWeight() {
        return weight;
    }

    public Double getGoal() {
        return goal;
    }
}
