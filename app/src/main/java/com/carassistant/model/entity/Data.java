package com.carassistant.model.entity;


import android.location.Location;

public class Data {
    private boolean isRunning;
    private long time;
    private long timeStopped;
    private boolean isFirstTime;

    private double distanceM;
    private double curSpeed;
    private double maxSpeed;

    private double sessionDistanceM;

    private Location location;

    private OnGpsServiceUpdate onGpsServiceUpdate;

    public double getSessionDistanceM() {
        return sessionDistanceM;
    }

    public void setSessionDistanceM(double sessionDistanceM) {
        this.sessionDistanceM = sessionDistanceM;
    }

    public interface OnGpsServiceUpdate {
        void update();
    }

    public void setOnGpsServiceUpdate(OnGpsServiceUpdate onGpsServiceUpdate) {
        this.onGpsServiceUpdate = onGpsServiceUpdate;
    }

    public void update() {
        if (onGpsServiceUpdate != null)
            onGpsServiceUpdate.update();
    }

    public Data() {
        isRunning = false;
        distanceM = 0;
        curSpeed = 0;
        maxSpeed = 0;
        timeStopped = 0;
    }

    public Data(OnGpsServiceUpdate onGpsServiceUpdate) {
        this();
        setOnGpsServiceUpdate(onGpsServiceUpdate);
    }

    public void setDistance(double distance) {
        distanceM = distance;
    }

    public void addDistance(double distance) {
        distanceM = distanceM + distance;
    }

    public double getDistance() {
        return distanceM;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getAverageSpeed() {
        double average;
        String units;
        if (time <= 0) {
            average = 0.0;
        } else {
            average = (distanceM / (time / 1000.0)) * 3.6;
        }
        return average;
    }

    public double getAverageSpeedMotion() {
        long motionTime = time - timeStopped;
        double average;
        String units;
        if (motionTime <= 0) {
            average = 0.0;
        } else {
            average = (distanceM / (motionTime / 1000.0)) * 3.6;
        }
        return average;
    }

    public void setCurSpeed(double curSpeed) {
        this.curSpeed = curSpeed;
        if (curSpeed > maxSpeed) {
            maxSpeed = curSpeed;
        }
    }

    public boolean isFirstTime() {
        return isFirstTime;
    }

    public void setFirstTime(boolean isFirstTime) {
        this.isFirstTime = isFirstTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public void setTimeStopped(long timeStopped) {
        this.timeStopped += timeStopped;
    }

    public double getCurSpeed() {
        return curSpeed;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}

