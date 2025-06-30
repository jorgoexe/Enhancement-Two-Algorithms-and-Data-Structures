package com.example.weighttracker.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.weighttracker.data.WeightRepository;
import com.example.weighttracker.model.WeightEntry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {

    private final WeightRepository repository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new WeightRepository(application.getApplicationContext());
    }

    public LiveData<List<WeightEntry>> getWeightEntries(long userId) {
        return repository.getWeightsForUser(userId);
    }

    // ✅ Expose moving average LiveData
    public LiveData<List<Double>> getMovingAverage() {
        return repository.getMovingAverage();
    }

    public void addWeight(long userId, String date, double weight, Double goal, Runnable callback) {
        executorService.execute(() -> {
            repository.insertWeight(new WeightEntry(userId, date, weight, goal));
            if (callback != null) callback.run();
        });
    }

    public void deleteWeight(long weightId, long userId, Runnable callback) {
        executorService.execute(() -> {
            repository.removeWeight(weightId, userId);
            if (callback != null) callback.run();
        });
    }

    public void deleteUser(long userId, Runnable callback) {
        executorService.execute(() -> {
            repository.deleteUserAndWeights(userId);
            if (callback != null) callback.run();
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.close();
        executorService.shutdown();
    }
}
