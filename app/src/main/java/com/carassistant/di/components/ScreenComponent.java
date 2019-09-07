package com.carassistant.di.components;

import com.carassistant.di.scopes.ScreenScope;

import dagger.Component;

@ScreenScope
@Component(dependencies = ApplicationComponent.class)
public interface ScreenComponent {


}
