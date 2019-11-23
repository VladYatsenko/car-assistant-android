package com.carassistant.model.bus.model;

import com.carassistant.model.bus.EventModel;
import com.carassistant.model.entity.GpsStatusEntity;

public class EventUpdateStatus implements EventModel {

    private GpsStatusEntity status;

    public EventUpdateStatus(GpsStatusEntity status) {
        this.status = status;
    }

    public GpsStatusEntity getStatus() {
        return status;
    }

}

