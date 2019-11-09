package com.carassistant.model.bus

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

enum class MessageEventBus {

    INSTANCE;

    private val bus = PublishSubject.create<EventModel>()

    fun send(event: EventModel) {
        bus.onNext(event)
    }

    fun toObservable(): Observable<EventModel> {
        return bus
    }

}