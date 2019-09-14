package com.carassistant.tflite.detection.adapter;

import com.carassistant.tflite.detection.Classifier;

import java.util.List;

public interface DetectionListener {

    void trackResults(List<Classifier.Recognition> mappedRecognitions, long currTimestamp);

    void showInfo(String frameInfo, String cropInfo, String processingTime);

    void setTrackerFrameConfiguration(int previewWidth, int previewHeight, int sensorOrientation);
}