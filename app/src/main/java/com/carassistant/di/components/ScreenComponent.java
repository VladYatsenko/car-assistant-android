package com.carassistant.di.components;

import com.carassistant.di.scopes.ScreenScope;
import com.carassistant.ui.activities.DetectorActivity;

import dagger.Component;

@ScreenScope
@Component(dependencies = ApplicationComponent.class)
public interface ScreenComponent {


    void inject(DetectorActivity detectorActivity);
}
