package com.example.testapplication.ui.beat_counter;

import android.app.Activity;
import android.widget.ProgressBar;

import com.example.testapplication.R;

public class BeatProgress {
    Activity m_activity;

    private ProgressBar beatProgressBar;

    public BeatProgress(Activity activity) {
        m_activity = activity;

        beatProgressBar = m_activity.findViewById(R.id.beat_progress);
        beatProgressBar.setMin(0);
        beatProgressBar.setMax(5000);
    }

    public void setIndeterminateMode(boolean indeterminate) {
        beatProgressBar.setIndeterminate(indeterminate);
    }

    public void setProgress(int value) {
        beatProgressBar.setProgress(value);
    }
}
