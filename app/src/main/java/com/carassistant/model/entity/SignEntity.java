package com.carassistant.model.entity;

import android.graphics.RectF;
import android.location.Location;

import androidx.annotation.IdRes;

import java.util.Date;
import java.util.UUID;

public class SignEntity {

    private UUID uuid;
    private String name;
    @IdRes
    private int image;
    private Date date;
    private RectF screenLocation;
    private Location location;
//    private Location

    public SignEntity(String name, int image){
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
}
