package com.carassistant.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.carassistant.R;
import com.carassistant.tflite.detection.Classifier;
import com.carassistant.tflite.detection.adapter.DetectionHolder;
import com.carassistant.tflite.detection.adapter.DetectionListener;
import com.carassistant.tflite.tracking.MultiBoxTracker;
import com.carassistant.utils.customview.OverlayView;
import com.carassistant.utils.env.BorderedText;
import com.carassistant.utils.env.Logger;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.List;


public class DetectorActivity extends AppCompatActivity {
    private static final Logger LOGGER = new Logger();

    ImageView img;
    Button open, detect;

    private DetectionHolder detectionHolder;
    private MultiBoxTracker tracker;
    private BorderedText borderedText;

    OverlayView trackingOverlay;

    Bitmap b;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        img = findViewById(R.id.image);
        open = findViewById(R.id.openBtn);
        open.setOnClickListener(v -> {
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
        });

        detect = findViewById(R.id.detect);
        detect.setOnClickListener(v -> {
            if (b != null)
                detection(b);
        });
        detectionHolder = new DetectionHolder(this);
        detectionHolder.setDetectionListener(new DetectionListener() {
            @Override
            public void trackResults(List<Classifier.Recognition> mappedRecognitions, long currTimestamp) {
                tracker.trackResults(mappedRecognitions, currTimestamp);
                trackingOverlay.postInvalidate();
            }

            @Override
            public void showInfo(String frameInfo, String cropInfo, String processingTime) {
                runOnUiThread(() -> {
//                    showFrameInfo(frameInfo);
//                    showCropInfo(cropInfo);
//                    showInference(processingTime);
                });
            }

            @Override
            public void setTrackerFrameConfiguration(int previewWidth, int previewHeight, int sensorOrientation) {
//                DetectorActivity.this.previewWidth = previewWidth;
//                DetectorActivity.this.previewHeight = previewHeight;
//                DetectorActivity.this.sensorOrientation = sensorOrientation;

                trackingOverlay = findViewById(R.id.tracking_overlay);
                trackingOverlay.addCallback(
                        canvas -> {
                            tracker.draw(canvas);
                        });

                tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
            }
        });
    }

    private void detection(Bitmap bitmap) {

        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        detectionHolder.setupDetectionAdapter(new Size(bitmap.getWidth(), bitmap.getHeight()), 0, 0);

        detectionHolder.processImage(bitmap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        detectionHolder.setHandler();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detectionHolder.destroyHandler();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                img.setImageURI(resultUri);
                try {
                    b = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
