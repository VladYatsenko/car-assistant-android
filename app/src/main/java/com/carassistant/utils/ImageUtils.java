package com.carassistant.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import static com.carassistant.tflite.classification.SpeedLimitClassifier.INPUT_IMG_SIZE_HEIGHT;
import static com.carassistant.tflite.classification.SpeedLimitClassifier.INPUT_IMG_SIZE_WIDTH;

public class ImageUtils {

    public static Bitmap prepareImageForClassification(Bitmap bitmap) {
        Paint paint = new Paint();
        Bitmap finalBitmap = Bitmap.createScaledBitmap(
                bitmap,
                INPUT_IMG_SIZE_WIDTH,
                INPUT_IMG_SIZE_HEIGHT,
                false);
        Canvas canvas = new Canvas(finalBitmap);
        canvas.drawBitmap(finalBitmap, 0, 0, paint);
        return finalBitmap;
    }

}
