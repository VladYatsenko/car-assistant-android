package com.carassistant.utils.player;

import android.net.Uri;

import androidx.annotation.IdRes;

public interface PlayerAdapter {

    void loadMedia(@IdRes int resId);

    void reset();

}
