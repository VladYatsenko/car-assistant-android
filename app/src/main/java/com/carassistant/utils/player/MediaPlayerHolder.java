package com.carassistant.utils.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.IdRes;

import com.carassistant.R;

import java.util.ArrayList;
import java.util.List;

public class MediaPlayerHolder implements PlayerAdapter {

    private MediaPlayer mMediaPlayer;
    private Context context;
    @IdRes
    private List<Integer> soundList = new ArrayList<>();

    public MediaPlayerHolder(Context context){
        this.context = context;
    }

    private void initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(mp -> {
                mMediaPlayer.start();
            });
            mMediaPlayer.setOnCompletionListener(mp -> {
                playNext();
            });
        }
    }

    @Override
    public void loadMedia(@IdRes int resId) {
        initializeMediaPlayer();

        if (isPlaying() && resId != R.raw.speed_limit_was_exceeded){
            soundList.add(resId);
            return;
        }
        Uri mediaPath = Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(context, mediaPath);
        } catch (Exception e) {}

        try {
            mMediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean isPlaying() {
        if (mMediaPlayer != null) {
             return mMediaPlayer.isPlaying();
        } else return false;
    }

    private void playNext() {
        if (!soundList.isEmpty()) {
            int media = soundList.get(0);
            loadMedia(media);
            soundList.remove(0);
        }
    }

    @Override
    public void reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        soundList.clear();
    }

}
