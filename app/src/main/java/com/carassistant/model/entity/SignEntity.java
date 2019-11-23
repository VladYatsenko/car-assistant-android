package com.carassistant.model.entity;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.location.Location;

import androidx.annotation.IdRes;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class SignEntity implements Serializable {

    private UUID uuid;
    private String name;
    @IdRes
    private int image;
    private Date date;
    private RectF screenLocation;
    private Location location;
    @IdRes
    private int soundNotification;
    private float confidenceDetection;
    private float confidenceClassification;

    public SignEntity(String name, int image, int soundNotification) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.image = image;
        this.date = new Date();
        this.soundNotification = soundNotification;
        confidenceDetection = 0f;
        confidenceClassification = 0f;

//        this.location = location;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getImage() {
        return image;
    }

    public Date getDate() {
        return date;
    }

    public void setScreenLocation(RectF screenLocation) {
        this.screenLocation = screenLocation;
    }

    public RectF getScreenLocation() {
        return screenLocation;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public int getSoundNotification() {
        return soundNotification;
    }

    public float getConfidenceDetection() {
        return confidenceDetection;
    }

    public void setConfidenceDetection(float confidenceDetection) {
        this.confidenceDetection = confidenceDetection;
    }

    public float getConfidenceClassification() {
        return confidenceClassification;
    }

    public void setConfidenceClassification(float confidenceClassification) {
        this.confidenceClassification = confidenceClassification;
    }

    public boolean isValidSize(Bitmap rgbFrameBitmap) {
        return screenLocation.height() > 0 &&
                screenLocation.width() > 0 &&
                screenLocation.left > 0 &&
                screenLocation.top > 0 &&
                screenLocation.bottom > 0 &&
                screenLocation.right > 0 ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignEntity that = (SignEntity) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
