package com.carassistant.model.entity;

public class ClassificationEntity {

    private final String title;
    private final float confidence;

    public ClassificationEntity(String title, float confidence) {
        this.title = title;
        this.confidence = confidence;
    }

    public String getTitle() {
        return title;
    }

    public float getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return title + " " + String.format("(%.1f%%) ", confidence * 100.0f);
    }
}
