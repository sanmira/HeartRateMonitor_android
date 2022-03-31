package com.example.testapplication.ui.beat_counter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.Camera;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.core.content.ContextCompat;

import com.example.testapplication.R;

import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;

import android.content.pm.PackageManager;
import android.content.Context;

import android.widget.ImageView;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.camera2.interop.Camera2Interop;
import android.hardware.camera2.CaptureRequest;
import androidx.lifecycle.LifecycleOwner;
import androidx.appcompat.app.AppCompatActivity;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;

import android.Manifest;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton;
import android.media.Image;
import android.util.Size;
import android.util.Range;



public class CameraActivity extends AppCompatActivity {
    private String TAG = "Camera";
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageView previewView;
    private ImageAnalysis imageAnalysis;
    private Camera camera;
    private YUVtoRGB imgTranslator;

    private Vibrator vibrator;

    private BeatPlot beatPlot;
    private BeatProgress beatProgress;

    private enum AppState {
        IDLE,
        THRESHOLD_CHECK,
        COLLECT_DATA
    }
    private AppState state = AppState.IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Log.d(TAG, "Camera activity was created");

        final ToggleButton button = findViewById(R.id.torch_button);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    camera.getCameraControl().enableTorch(true);
                } else {
                    camera.getCameraControl().enableTorch(false);
                }
            }
        });

        beatPlot = new BeatPlot(this);
        beatProgress = new BeatProgress(this);

        imgTranslator = new YUVtoRGB();

        checkCameraPermissions(this);

        previewView = findViewById(R.id.camera_view);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Image analysis use case
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(240, 320))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        @SuppressLint("UnsafeOptInUsageError") Camera2Interop.Extender ext = new Camera2Interop.Extender<>(builder);
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(30, 30));
        //ext.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_TRANSFORM, cst );
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_FAST);
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME,Long.valueOf("30000000"));
//        ext.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, 3255);
        imageAnalysis = builder.build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
            new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(@NonNull ImageProxy image) {
                    @SuppressLint("UnsafeOptInUsageError") Image img = image.getImage();
                    // Log.d(TAG, "Img rotation degrees: " + image.getImageInfo().getRotationDegrees());
                    previewView.setRotation(image.getImageInfo().getRotationDegrees());
                    Bitmap iBitmap = imgTranslator.translateYUV(img, CameraActivity.this);

                    int imgSize = iBitmap.getWidth() * iBitmap.getHeight();
                    int[] pixels = new int[imgSize];
                    iBitmap.getPixels(pixels, 0, iBitmap.getWidth(),0, 0,
                            iBitmap.getWidth(), iBitmap.getHeight());

                    int redSum = 0;
                    int greenSum = 0;
                    int blueSum = 0;

                    for (int i = 0; i < imgSize; i++) {
                        int color = pixels[i];
                        int r = color >> 16 & 0xff;
                        int g = color >> 8 & 0xff;
                        int b = color & 0xff;

                        redSum += r;
                        greenSum += g;
                        blueSum += b;
                    }

                    float redMean = redSum / (320 * 240);
                    float greenMean = greenSum / (320 * 240);
                    float blueMean = blueSum / (320 * 240);

                    if ((redMean > 130) && (greenMean < 70) && (blueMean < 70)) {
                        processData(redMean, 1);
                    } else {
                        processData(redMean, -1);
                    }

//                    Log.e(TAG, "Red mean brightness: " + redMean);
//                    Log.e(TAG, "Green mean brightness: " + greenMean);
//                    Log.e(TAG, "Blue mean brightness: " + blueMean);

//                    iBitmap.setPixels(pixels, 0, iBitmap.getWidth(),0, 0,
//                            iBitmap.getWidth(), iBitmap.getHeight());
                    previewView.setImageBitmap(iBitmap);

                    image.close();
                }
            });

        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis);

        @SuppressLint("RestrictedApi") CameraCharacteristics camChars = Camera2CameraInfo
                .extractCameraCharacteristics(camera.getCameraInfo());
        Range discoveredMinFocusDistance = camChars
                .get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);

        Log.e(TAG, "Focus distance: " + discoveredMinFocusDistance);
    }

    private enum TimerState {
        TIMER_IDLE,
        TIMER_RUNNING,
        TIMER_FINISHED
    }

    // idle timer

    private CountDownTimer idleTimer = null;
    private TimerState idleTimerState = TimerState.TIMER_IDLE;
    private int idleTimerProgress = 0;
    private int idleTimerTimeout = 5000;
    private int idleTimerCountInterval = 100;

    private void startIdleTimer() {
        idleTimer = new CountDownTimer(idleTimerTimeout, idleTimerCountInterval) {
            public void onTick(long millisUntilFinished) {
                idleTimerProgress = (idleTimerTimeout - (int) millisUntilFinished);
//                Log.e(TAG, "seconds remaining: " + millisUntilFinished / 1000);
            }
            public void onFinish() {
//                Log.e(TAG, "Count finished");
                idleTimerProgress = 0;
                idleTimerState = TimerState.TIMER_FINISHED;
            }
        };
        idleTimerState = TimerState.TIMER_RUNNING;
        idleTimer.start();
    }

    void stopIdleTimer() {
        idleTimer.cancel();
    }

    // measurement timer

    private CountDownTimer measurementTimer = null;
    private TimerState measurementTimerState = TimerState.TIMER_IDLE;
    private int measurementTimerProgress = 0;
    private int measurementTimerTimeout = 30000;
    private int measurementTimerCountInterval = 100;

    private void startMeasurementTimer() {
        measurementTimer = new CountDownTimer(measurementTimerTimeout, measurementTimerCountInterval) {
            public void onTick(long millisUntilFinished) {
                measurementTimerProgress = (measurementTimerTimeout - (int) millisUntilFinished);
//                Log.e(TAG, "seconds remaining: " + millisUntilFinished / 1000);
            }
            public void onFinish() {
//                Log.e(TAG, "Count finished");
                measurementTimerProgress = 0;
                measurementTimerState = TimerState.TIMER_FINISHED;
            }
        };
        measurementTimerState = TimerState.TIMER_RUNNING;
        measurementTimer.start();
    }

    void stopMeasurementTimer() {
        measurementTimer.cancel();
    }

    private ArrayList<Float> pixelData = new ArrayList<Float>();

    private int analyzeData() {
        ArrayList<Float> pixelSmoothData = new ArrayList<Float>();

        Log.e(TAG, "Values: " + pixelData.toString());

        int window_size = 7;
        float sum = 0;
        for (int i = 0; i < (pixelData.size() - window_size + 1); i++) {
            sum = 0;
            for (int j = 0; j < window_size; j++) {
                sum = (float) (sum + pixelData.get(i + j));
            }
            sum /= window_size;
            pixelSmoothData.add(sum);
        }
        Log.e(TAG, "Smooth values: " + pixelSmoothData.toString());

        int minimums = 0;
        int i = 0;
        while (i < (pixelSmoothData.size() - 5)) {
            if ((pixelSmoothData.get(i) > pixelSmoothData.get(i + 1)) && (pixelSmoothData.get(i + 2) < pixelSmoothData.get(i + 4))) {
                minimums = minimums + 1;
                i = i + 5;
            } else {
                i = i + 1;
            }
        }
        Log.e(TAG, "Minimums: " + minimums);

        float value = ((float) minimums / (measurementTimerTimeout / 1000)) * 60;
        Log.e(TAG, "Value: " + (int) value);
//        Log.e(TAG, "Orig number: " + pixelData.size());
//        Log.e(TAG, "Smooth number: " + pixelSmoothData.size());

        return (int) value;
    }

    private void processData(float redMean, int result) {
        switch (state) {
            case IDLE:
//                Log.e(TAG, "IDLE");
                if (result == 1) {
                    startIdleTimer();
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                    state = AppState.THRESHOLD_CHECK;
                }
                break;
            case THRESHOLD_CHECK:
//                Log.e(TAG, "THRESHOLD_CHECK");
                if (result != 1) {
                    stopIdleTimer();
                    beatProgress.setProgress(0);
                    state = AppState.IDLE;
                    break;
                }
                if (idleTimerState == TimerState.TIMER_FINISHED) {
                    state = AppState.COLLECT_DATA;
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                    beatProgress.setProgress(0);
                    beatProgress.setMax(measurementTimerTimeout);
                    startMeasurementTimer();
                } else {
                    beatProgress.setProgress(idleTimerProgress);
                }
                break;
            case COLLECT_DATA:
//                Log.e(TAG, "COLLECT_DATA");
                if (result != 1) {
                    stopMeasurementTimer();
                    beatPlot.clearGraph();
                    beatProgress.setProgress(0);
                    pixelData.clear();
                    beatProgress.setMax(idleTimerTimeout);
                    state = AppState.IDLE;
                    break;
                }
                if (measurementTimerState == TimerState.TIMER_FINISHED) {
                    int value = analyzeData();
                    pixelData.clear();
                    beatProgress.setProgress(0);
                    measurementTimerState = TimerState.TIMER_IDLE;
                    Intent data = new Intent();
//---set the data to pass back---
                    data.putExtra("measurement_result", value);
                    setResult(RESULT_OK, data);
//---close the activity---
                    finish();
                } else {
                    pixelData.add(redMean);
                    beatPlot.addEntry(redMean);
                    beatProgress.setProgress(measurementTimerProgress);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    private void checkCameraPermissions(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.d("checkCameraPermissions", "No Camera Permissions");
            this.requestPermissions(
                    new String[] { Manifest.permission.CAMERA },
                    100);
        }
    }
}

