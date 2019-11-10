package com.carassistant.model.bus;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by AlexLampa on 28.06.2019.
 */
public enum MessageEventBus {

    INSTANCE;

    private PublishSubject<EventModel> bus = PublishSubject.create();

    public void send(EventModel event) {
        bus.onNext(event);
    }

    public Observable<EventModel> toObservable() {
        return bus;
    }

}
