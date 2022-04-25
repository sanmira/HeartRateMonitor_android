package com.example.testapplication.ui.dashboard;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.testapplication.R;
import com.example.testapplication.databinding.FragmentMeasurementBinding;
import com.example.testapplication.ui.beat_counter.CameraActivity;
import com.example.testapplication.ui.beat_counter.StatClient;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class MeasurementFragment extends Fragment {

    private String TAG = "MeasurementFragment";
    private FragmentMeasurementBinding binding;

    private TextView heartBeatIndicator;

    private StatClient statClient;

    void resetBeatIndicator() {
        heartBeatIndicator.setText("...");
    }

    void updateBeatIndicator(int value) {
        String stringToDisplay = Integer.toString(value).concat(" bpm");
        heartBeatIndicator.setText(stringToDisplay);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MeasurementViewModel measurementViewModel =
                new ViewModelProvider(this).get(MeasurementViewModel.class);

        binding = FragmentMeasurementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        measurementViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        Activity activity = getActivity();

        heartBeatIndicator = root.findViewById(R.id.bps_text);

        // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                            int measurementValue = data.getIntExtra("measurement_result", 0);

                            updateBeatIndicator(measurementValue);
                        }
                    }
                });


        Button cameraButton = binding.cameraButton;
        cameraButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Camera button clicked");
                resetBeatIndicator();
                Intent intent = new Intent(activity, CameraActivity.class);
                someActivityResultLauncher.launch(intent);
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