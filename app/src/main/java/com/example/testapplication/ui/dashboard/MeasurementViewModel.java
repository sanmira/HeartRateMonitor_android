package com.example.testapplication.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MeasurementViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public MeasurementViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Measurement page");
    }

    public LiveData<String> getText() {
        return mText;
    }
}