package com.example.testapplication.ui.beat_counter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.Camera;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.core.content.ContextCompat;

import com.example.testapplication.R;

import android.os.Handler;
import android.os.OperationCanceledException;
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

import androidx.core.content.res.TypedArrayUtils;
import androidx.lifecycle.LifecycleOwner;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.Timer;
import java.util.concurrent.Executors;
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

    private int beatPlotSkipValue = 0;
    private BeatPlot beatPlot;
    private BeatProgress beatProgress;

    private StatClient statClient;

    private ListenableFuture<Integer> exposureCompensationFuture;
    private boolean isExposureAdjusted = true;
    private int exposureIndex = 0;

    private Executor cameraExecutor = Executors.newSingleThreadExecutor();

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

        setTitle("Camera");

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

        statClient = new StatClient();

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

    @RequiresApi(api = Build.VERSION_CODES.R)
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
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(60, 60));
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_FAST);
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
//        ext.setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME,Long.valueOf("30000000"));
//        ext.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, 3255);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true);
//        ext.setCaptureRequestOption(CaptureRequest.BLACK_LEVEL_LOCK, true);
//        ext.setCaptureRequestOption(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_FAST);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_DISABLED);
//        ext.setCaptureRequestOption(CaptureRequest.STATISTICS_OIS_DATA_MODE, CaptureRequest.STATISTICS_OIS_DATA_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_OFF);
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, true);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_EXTENDED_SCENE_MODE, CaptureRequest.CONTROL_EXTENDED_SCENE_MODE_DISABLED);
//        ext.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, 0);
        ext.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST);
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_OFF);
        ext.setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
        ext.setCaptureRequestOption(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_OFF);
        ext.setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
        imageAnalysis = builder.build();

        imageAnalysis.setAnalyzer(cameraExecutor,
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
                    int redSumOrig = 0;
                    int greenSum = 0;
                    int blueSum = 0;

                    for (int i = 0; i < imgSize; i++) {
                        int color = pixels[i];
                        int r_o = color >> 16 & 0xff;
                        int r = (color >> 16 & 0xff) * 25;
                        int g = color >> 8 & 0xff;
                        int b = color & 0xff;

                        redSum += r;
                        redSumOrig += r_o;
                        greenSum += g;
                        blueSum += b;
                    }

                    double redMean = redSum / (320 * 240);
                    double redMeanOrig = redSumOrig / (320 * 240);
                    double greenMean = greenSum / (320 * 240);
                    double blueMean = blueSum / (320 * 240);

                    if ((redMean > 70) && (greenMean < 30) && (blueMean < 30)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processData(redMean, redMeanOrig, 1);
                            }
                        });
                    } else {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processData(redMean, redMeanOrig,-1);
                            }
                        });
                    }

//                    Log.e(TAG, "Red mean brightness: " + redMean);
//                    Log.e(TAG, "Green mean brightness: " + greenMean);
//                    Log.e(TAG, "Blue mean brightness: " + blueMean);

//                    iBitmap.setPixels(pixels, 0, iBitmap.getWidth(),0, 0,
//                            iBitmap.getWidth(), iBitmap.getHeight());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            previewView.setImageBitmap(iBitmap);
                        }
                    });

                    image.close();
                }
            });

        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis);

        @SuppressLint("RestrictedApi") CameraCharacteristics camChars = Camera2CameraInfo
                .extractCameraCharacteristics(camera.getCameraInfo());
        Range discoveredMinFocusDistance = camChars
                .get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);

        @SuppressLint("RestrictedApi") CameraCharacteristics camFrameRate = Camera2CameraInfo
                .extractCameraCharacteristics(camera.getCameraInfo());
        Range<Integer>[] discoveredMaxFrameRate = camFrameRate
                .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

        @SuppressLint("RestrictedApi") CameraCharacteristics densities = Camera2CameraInfo
                .extractCameraCharacteristics(camera.getCameraInfo());
        float[] discoveredDensities = densities
                .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES);

        Log.e(TAG, "Focus distance: " + discoveredMinFocusDistance);
        for (Range range: discoveredMaxFrameRate) {
            Log.e(TAG, "Available FPS range: " + range.toString());
        }

        for (float density: discoveredDensities) {
            Log.e(TAG, "Available density: " + density);
        }
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
                isExposureAdjusted = true;
            }
            public void onFinish() {
//                Log.e(TAG, "Count finished");
                idleTimerProgress = 0;
                idleTimerState = TimerState.TIMER_FINISHED;
            }
        };
        idleTimerState = TimerState.TIMER_RUNNING;
        isExposureAdjusted = true;
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

    private ArrayList<Double> pixelData = new ArrayList<Double>();

    private int analyzeData() {
        ArrayList<Double> pixelSmoothData = new ArrayList<Double>();

//        Log.e(TAG, "Values: " + pixelData.toString());

        double[] values = pixelData.stream().mapToDouble(Double::doubleValue).toArray();

        int sigma = 3;
        double sd = (double) sigma;
        double truncate=4.0;

        int radius = (int)(truncate * sd + 0.5);
        double[] kernel_3 = gauss1d_kernel(radius, sd);
        double[] smooth_values_3 = colvoltion1D(values, kernel_3);

        sigma = 5;
        sd = (double) sigma;

        radius = (int)(truncate * sd + 0.5);
        double[] kernel_5 = gauss1d_kernel(radius, sd);
        double[] smooth_values_5 = colvoltion1D(smooth_values_3, kernel_5);

        List<Integer> peaks_down = new ArrayList<Integer>();
        List<Integer> peaks_up = new ArrayList<Integer>();
        for (int i = 0; i < smooth_values_5.length - 2; i++) {
            if (smooth_values_5[i] > smooth_values_5[i + 1] && smooth_values_5[i + 2] > smooth_values_5[i + 1]) {
                peaks_down.add(i);
            } else if (smooth_values_5[i] < smooth_values_5[i + 1] && smooth_values_5[i + 2] < smooth_values_5[i + 1]) {
                peaks_up.add(i);
            }
        }

        Log.e(TAG, "Down peaks: " + peaks_down.size());
        Log.e(TAG, "Up peaks: " + peaks_up.size());

        double value = ((double) peaks_down.size() / (measurementTimerTimeout / 1000)) * 60;
        Log.e(TAG, "Value: " + (int) value);
//        Log.e(TAG, "Orig number: " + pixelData.size());
//        Log.e(TAG, "Smooth number: " + pixelSmoothData.size());

        statClient.execute("http://51.250.98.194:5000/data", pixelData.toString()
                .replace("[", "")
                .replace("]", "")
                .replace(" ", "")
                .trim(),
                String.valueOf(value));

        return (int) value;
    }

    private void processData(double redMean, double redMeanOrig, int result) {
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
                    exposureIndex = 0;
                    exposureCompensationFuture = camera.getCameraControl().setExposureCompensationIndex(exposureIndex);
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
                    if (isExposureAdjusted) {
                        if (redMeanOrig < 150) {
                            exposureCompensationFuture = camera.getCameraControl().setExposureCompensationIndex(++exposureIndex);
                            isExposureAdjusted = false;
//                            imageAnalysis.set
                            Log.e(TAG, "Exposure increase! Index: " + exposureIndex + " Red mean orig: " + redMeanOrig);
                        } else if (redMeanOrig > 190) {
                            exposureCompensationFuture = camera.getCameraControl().setExposureCompensationIndex(--exposureIndex);
                            isExposureAdjusted = false;
                            Log.e(TAG, "Exposure decrease! Index " + exposureIndex + " Red mean orig: " + redMeanOrig);
                        }
                    }
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
                    exposureIndex = 0;
                    exposureCompensationFuture = camera.getCameraControl().setExposureCompensationIndex(exposureIndex);
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
                    exposureIndex = 0;
                    exposureCompensationFuture = camera.getCameraControl().setExposureCompensationIndex(exposureIndex);
                    finish();
                } else {
                    pixelData.add(redMean);

                    int window_size = 5;
                    if (pixelData.size() > window_size) {
                        float sum = 0;
                        for (int i = 0; i < (pixelData.size() - window_size + 1); i++) {
                            sum = 0;
                            for (int j = 0; j < window_size; j++) {
                                sum = (float) (sum + pixelData.get(i + j));
                            }
                            sum /= window_size;
                        }
                        beatPlot.addEntry(sum);
                    }
//                    if (beatPlotSkipValue == 0) {
//                        beatPlot.addEntry(redMean);
//                        beatPlotSkipValue++;
//                    } else {
//                        if (beatPlotSkipValue == 1) {
//                            beatPlotSkipValue++;
//                        } else {
//                            beatPlotSkipValue = 0;
//                        }
//                    }
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

    private void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("log.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private double[] gauss1d_kernel(int radius, double sigma) {
        double sigma2 = sigma * sigma;
        double[] x = new double[(radius * 2) + 1];
        for (int i = -radius; i < radius + 1; i++) {
            x[i + radius] = i;
        }
        double[] phi_x = new double[(radius * 2) + 1];
        for (int i = 0; i < (radius * 2) + 1; i++) {
            phi_x[i] = Math.exp((-0.5 / sigma2) * x[i]*x[i]);
        }
        double phi_x_sum = 0;
        for (int i = 0; i < (radius * 2) + 1; i++) {
            phi_x_sum += phi_x[i];
        }
        for (int i = 0; i < (radius * 2) + 1; i++) {
            phi_x[i] = phi_x[i] / phi_x_sum;
        }
        return phi_x;
    }

    private double[] colvoltion1D(double[] in, double[] kernel) {
        double[] out = new double[in.length];

        int inputSize = in.length;
        int kernelSize = kernel.length;

        int padding = kernelSize - 1;
        int left_pad  = (int)(Math.ceil(padding / 2));
        int right_pad = padding - left_pad;

        // Make a new dataVector by appending zeros.
        double[] dataVec = new double [padding + inputSize];

//        Log.e(TAG, "Padding: " + padding);
//        Log.e(TAG, "Left_pad: " + left_pad);
//        Log.e(TAG, "Right_pad: " + right_pad);

        //left padding
        for (int i = 0; i < left_pad; i++) {
            dataVec[i] = in[left_pad - i - 1];
        }

        //right padding
        for (int i = 0; i < right_pad; i++) {
            dataVec[inputSize + left_pad + i] = in[inputSize - i - 1];
        }

        // add data in dataVec
        for(int i = 0; i < inputSize; i++) {
            //Add data in between the padded zeros...
            dataVec[i + left_pad] = in[i];
        }

        // convolution begins here...
        int end = 0;
        while (end < inputSize) {
            double sum = 0;
            for (int i = 0; i < kernelSize; i++) {
                sum += kernel[i] * dataVec[end + i];
            }
            out[end] = sum;
            end = end + 1;
        }
        return out;
    }
}

