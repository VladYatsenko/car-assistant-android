package com.carassistant.model.entity;

public class GpsStatusEntity {

    private String satellite;
    private String status;
    private String accuracy;


    public GpsStatusEntity(String satellite, String status, String accuracy) {
        this.satellite = satellite;
        this.status = status;
        this.accuracy = accuracy;
    }

    public String getSatellite() {
        return satellite;
    }

    public void setSatellite(String satellite) {
        this.satellite = satellite;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }
}
