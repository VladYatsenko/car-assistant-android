package com.carassistant.model.bus.model

import android.location.Location
import com.carassistant.model.bus.EventModel

class EventUpdateLocation constructor(var location: Location): EventModel {
}