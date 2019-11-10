package com.carassistant.model.bus.model;

import com.carassistant.model.bus.EventModel;
import com.carassistant.model.entity.Data;

public class EventUpdateLocation implements EventModel {

    private Data data;

    public EventUpdateLocation(Data data) {
        this.data = data;
    }

    public Data getData() {
        return data;
    }

}
