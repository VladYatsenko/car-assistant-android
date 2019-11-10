package com.carassistant.managers;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

public class SharedPreferencesManager {

    private static final String PREFERENCES_FILE_NAME = "carassistant.shared_preferences";
    private static final String DISTANCE = "distance"; //m

    private SharedPreferences sharedPreferences;

    @Inject
    public SharedPreferencesManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    public void setDistance(float distance) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(DISTANCE, distance);
        editor.apply();
    }

    public float getDistance() {
        return sharedPreferences.getFloat(DISTANCE, 0f);
    }

}
