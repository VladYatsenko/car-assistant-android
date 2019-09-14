package com.carassistant.tflite.detection.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Size;

import com.carassistant.tflite.detection.Classifier;
import com.carassistant.tflite.detection.TFLiteObjectDetectionAPIModel;
import com.carassistant.utils.env.ImageUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;


public class DetectionHolder {

    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;

    private DetectionListener detectionListener;

    private Classifier detector;

    private long timestamp = 0;
    private long lastProcessingTimeMs;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private int previewWidth = 0;
    private int previewHeight = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private Handler handler;
    private HandlerThread handlerThread;

    private Context context;

    @Inject
    public DetectionHolder(Context context) {
        this.context = context;
    }

    public void setDetectionListener(DetectionListener detectionListener) {
        this.detectionListener = detectionListener;
    }

    public void setHandler(){
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public void destroyHandler(){
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException ignored) { }
    }


    public void setupDetectionAdapter(final Size size, final int rotation, int orientation) {

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        int sensorOrientation = rotation - orientation;

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        detectionListener.setTrackerFrameConfiguration(previewWidth, previewHeight, sensorOrientation);

    }

    public void processImage(Bitmap bitmap) {
        ++timestamp;
        final long currTimestamp = timestamp;



        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(bitmap, frameToCropTransform, null);
        // For examining the actual TF input.
//        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap, context);
//        }

        runInBackground(
                () -> {
                    final long startTime = SystemClock.uptimeMillis();
                    final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                    final Canvas canvas1 = new Canvas(cropCopyBitmap);
                    final Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2.0f);


                    final List<Classifier.Recognition> mappedRecognitions =
                            new LinkedList<Classifier.Recognition>();

                    for (final Classifier.Recognition result : results) {
                        final RectF location = result.getLocation();
                        if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                            canvas1.drawRect(location, paint);

                            cropToFrameTransform.mapRect(location);

                            result.setLocation(location);
                            mappedRecognitions.add(result);
                        }
                    }


                    detectionListener.trackResults(mappedRecognitions, currTimestamp);

//                        tracker.trackResults(mappedRecognitions, currTimestamp);
//                        trackingOverlay.postInvalidate();


                    //TODO runOnUiThread
                    detectionListener.showInfo(previewWidth + "x" + previewHeight,
                            cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight(),
                            String.valueOf(lastProcessingTimeMs));

//                        runOnUiThread(
//                                new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        showFrameInfo(previewWidth + "x" + previewHeight);
//                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
//                                        showInference(lastProcessingTimeMs + "ms");
//                                    }
//                                });
                });

    }


    public void setNumThreads(int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }

    public void setUseNNAPI(boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    private synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }


}