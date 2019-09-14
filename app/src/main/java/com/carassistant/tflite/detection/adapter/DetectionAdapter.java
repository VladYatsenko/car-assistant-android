package com.carassistant.tflite.detection.adapter;

import android.graphics.Bitmap;
import android.util.Size;

import java.util.BitSet;

interface DetectionAdapter {

    void setupDetectionAdapter(final Size size, final int rotation, int orientation);

    void processImage(Bitmap bitmap);

    void setNumThreads(final int numThreads);

    void setUseNNAPI(final boolean isChecked);
}