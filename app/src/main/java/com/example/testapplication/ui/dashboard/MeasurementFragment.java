package com.example.testapplication.ui.dashboard;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.testapplication.databinding.FragmentMeasurementBinding;
import com.example.testapplication.ui.beat_counter.CameraActivity;

public class MeasurementFragment extends Fragment {

    private String TAG = "MeasurementFragment";
    private FragmentMeasurementBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MeasurementViewModel measurementViewModel =
                new ViewModelProvider(this).get(MeasurementViewModel.class);

        binding = FragmentMeasurementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        measurementViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        Activity activity = getActivity();

        Button cameraButton = binding.cameraButton;
        cameraButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Camera button clicked");
                Intent intent = new Intent(activity, CameraActivity.class);
                startActivity(intent);
            }
        });

        Log.d("Activity", "Dashboard fragment was created");

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}