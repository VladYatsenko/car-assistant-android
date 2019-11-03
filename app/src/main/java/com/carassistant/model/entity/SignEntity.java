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
//    private Location

    public SignEntity(String name, int image) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.image = image;
        this.date = new Date();
//        this.screenLocation = screenLocation;
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
