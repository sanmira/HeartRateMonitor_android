package com.example.testapplication.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HistoryModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HistoryModel() {
        mText = new MutableLiveData<>();
        mText.setValue("History page");
    }

    public LiveData<String> getText() {
        return mText;
    }
}