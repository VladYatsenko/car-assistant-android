package com.carassistant.managers;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

public class SharedPreferencesManager {

    private static final String PREFERENCES_FILE_NAME = "carassistant.shared_preferences";
    private static final String CURRENT_LANG = "CURRENT_LANG";

    private SharedPreferences sharedPreferences;

    @Inject
    public SharedPreferencesManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

//    public void setLanguage(Enums.Langs lang) {
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString(CURRENT_LANG, (lang == Enums.Langs.UKR) ? "ua" : "ru");
//        editor.apply();
//    }
//
//    public Enums.Langs getLanguage() {
//        String langStr = sharedPreferences.getString(CURRENT_LANG, "ua");  // ua or ru ; ua by default
//        return (langStr.equals("ua") ? Enums.Langs.UKR : Enums.Langs.RUS);
//    }
}
