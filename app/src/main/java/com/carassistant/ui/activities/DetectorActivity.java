package com.carassistant.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.carassistant.R;
import com.carassistant.di.components.DaggerScreenComponent;
import com.carassistant.managers.SharedPreferencesManager;
import com.carassistant.model.entity.Data;
import com.carassistant.model.entity.SignEntity;
import com.carassistant.service.GpsServices;
import com.carassistant.tflite.detection.Classifier;
import com.carassistant.tflite.detection.TFLiteObjectDetectionAPIModel;
import com.carassistant.tflite.tracking.MultiBoxTracker;
import com.carassistant.ui.adapter.SignAdapter;
import com.carassistant.utils.customview.OverlayView;
import com.carassistant.utils.env.BorderedText;
import com.carassistant.utils.env.ImageUtils;
import com.carassistant.utils.env.Logger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import static android.location.GpsStatus.GPS_EVENT_SATELLITE_STATUS;


public class DetectorActivity extends CameraActivity
        implements OnImageAvailableListener, GpsStatus.Listener, LocationListener {
    private static final Logger LOGGER = new Logger();

    private final String TAG = DetectorActivity.class.getSimpleName();

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    //  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static float MINIMUM_CONFIDENCE_TF_OD_API = 0.7f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;

    //location
    private static Data data;
    private Data.OnGpsServiceUpdate onGpsServiceUpdate;
    private LocationManager mLocationManager;
    private boolean firstfix;

    private TextView currentSpeed, distance, satellite, status, accuracy, totalDistance;
    private RecyclerView signRecycler;
    private SignAdapter adapter;
    private ArrayList<SignEntity> signs;

    private Ringtone ringtone = null;
    SwitchCompat notification;
    private static double distanceValue = 0;

    @Inject
    SharedPreferencesManager sharedPreferencesManager;

    private final String SIGN_LIST = "sign_list";
    private final String DISTANCE = "distance";
    private final String DISTANCE_VAL = "distance_val";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inject();
        setupLocation();
        setupRecycler();
        setupViews();

        startService(new Intent(getBaseContext(), GpsServices.class));
    }

    private void inject() {
        DaggerScreenComponent.builder()
                .applicationComponent(getApplicationComponent())
                .build()
                .inject(this);
    }

    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SIGN_LIST, new Gson().toJson(adapter.getSigns()));
        outState.putString(DISTANCE, distance.getText().toString());
        outState.putDouble(DISTANCE_VAL, data.getDistance());
        Log.i("distance", "save Distance = " + distance.getText().toString());

    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String json = savedInstanceState.getString(SIGN_LIST);
        ArrayList<SignEntity> items = null;
        try {
            items = (new Gson()).fromJson(json, new TypeToken<ArrayList<SignEntity>>() {
            }.getType());
        } catch (Exception ignored) {
            items = new ArrayList<>();
        }
        adapter.setSigns(items);

        String savedDistance = savedInstanceState.getString(DISTANCE, "0 km");
        Log.i("distance", "savedDistance = " + savedDistance);
        distance.setText(savedDistance);
        data.addDistance(savedInstanceState.getDouble(DISTANCE_VAL, 0));
    }

    @SuppressLint("DefaultLocale")
    private void setupViews() {
        TextView confidence = findViewById(R.id.confidence_value);
        confidence.setText(String.format("%.2f", MINIMUM_CONFIDENCE_TF_OD_API));

        SwitchCompat camera = findViewById(R.id.camera_switch);
        camera.setOnCheckedChangeListener((buttonView, isChecked) ->
                findViewById(R.id.container).setAlpha(isChecked ? 1f : 0f)
        );

        notification = findViewById(R.id.notification_switch);

        SeekBar confidenceSeekBar = findViewById(R.id.confidence_seek);
        confidenceSeekBar.setMax(100);
        confidenceSeekBar.setProgress((int) (MINIMUM_CONFIDENCE_TF_OD_API * 100));

        confidenceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MINIMUM_CONFIDENCE_TF_OD_API = progress / 100.0F;
                confidence.setText(String.format("%.2f", MINIMUM_CONFIDENCE_TF_OD_API));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        totalDistance.setText(
                String.format("%.1f %s", sharedPreferencesManager.getDistance(), "km")
                        .replace(',', '.')
                        .replace(".0", ""));
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mLocationManager.getAllProviders().indexOf(LocationManager.GPS_PROVIDER) >= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermission();
                    return;
                }
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        } else {
            Log.w("MainActivity", "No GPS location provider found. GPS data display will not be available.");
        }

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDisabledDialog();
        }

        mLocationManager.addGpsStatusListener(this);

        ringtone = RingtoneManager.getRingtone(getApplicationContext(),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
        mLocationManager.removeGpsStatusListener(this);

        ringtone = null;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(canvas -> {
            tracker.draw(canvas);
            if (isDebug()) {
                tracker.drawDebug(canvas);
            }
        });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);


                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);

                                runOnUiThread(() -> updateSignList(result, croppedBitmap));
                            }
                        }
                        
                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(() -> {
                            showFrameInfo(previewWidth + "x" + previewHeight);
                            showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                            showInference(lastProcessingTimeMs + "ms");
                        });
                    }
                });
    }

    private void updateSignList(Classifier.Recognition result, Bitmap bitmap) {

        SignEntity sign = getSignImage(result, bitmap);

        ArrayList<SignEntity> list = new ArrayList<>(adapter.getSigns());

        if (list.isEmpty()){
            addSignToAdapter(sign);
            return;
        }
        if (list.contains(sign)) {
            if (isRemoveValid(sign, list.get(list.indexOf(sign)))) {
                adapter.getSigns().remove(sign);
                addSignToAdapter(sign);
            }
        } else {
            addSignToAdapter(sign);
        }

    }

    private void addSignToAdapter(SignEntity sign){
        adapter.setSign(sign);
        playNotification();
    }

    private boolean isRemoveValid(SignEntity sign1, SignEntity sign2) {
        return isTimeDifferenceValid(sign1.getDate(), sign2.getDate())
                || isLocationDifferenceValid(sign1.getLocation(), sign2.getLocation());
    }

    private boolean isTimeDifferenceValid(Date date1, Date date2) {
        long milliseconds = date1.getTime() - date2.getTime();
        Log.i("sign", "isTimeDifferenceValid "+ ((milliseconds / (1000)) > 30));
        return (int) (milliseconds / (1000)) > 30;
    }

    private boolean isLocationDifferenceValid(Location location1, Location location2) {
        if (location1 == null || location2 == null)
            return false;
        return location1.distanceTo(location2) > 5;
    }

    private void playNotification() {
        try {
            if (!ringtone.isPlaying() && notification.isChecked()) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("DefaultLocale")
    private void setupLocation() {

        data = new Data(onGpsServiceUpdate);

        satellite = findViewById(R.id.satellite_info);
        status = findViewById(R.id.gps_status_info);
        accuracy = findViewById(R.id.accuracy_info);
        distance = findViewById(R.id.distanceValueTxt);
        totalDistance = findViewById(R.id.totalDistanceValueTxt);
        currentSpeed = findViewById(R.id.currentSpeedTxt);
//
        onGpsServiceUpdate = () -> {
            double maxSpeedTemp = data.getMaxSpeed();
            double distanceTemp = data.getDistance();
            double averageTemp = data.getAverageSpeed();

            String speedUnits = "km/h";
            String distanceUnits;
            if (distanceTemp <= 1000.0) {
                distanceUnits = "m";
            } else {
                distanceTemp /= 1000.0;
                distanceUnits = "km";
            }

            distance.setText(String.format("%.1f %s", distanceTemp, distanceUnits).replace(',', '.'));
            Log.i("distance", "onGpsServiceUpdate Distance = " + distance.getText().toString());

            Log.i("distance", "distanceValue Distance = " + distanceValue);

            if (distanceValue != distanceTemp) {
                double distance = sharedPreferencesManager.getDistance();
                Log.i("distance", "sharedPreferencesManager Distance = " + distance);

                if (distanceUnits.equals("m"))
                    distance += (distanceTemp - distanceValue) / 1000.0;
                else distance += (distanceTemp - distanceValue);

                Log.i("distance", "setDistance Distance = " + distance);

                sharedPreferencesManager.setDistance((float) distance);

                distanceValue = distanceTemp;
                totalDistance.setText(String.format("%.1f %s", distance, "km").replace(',', '.').replace(".0", ""));
            }

        };

        data.setOnGpsServiceUpdate(onGpsServiceUpdate);
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        data.setRunning(true);
        data.setFirstTime(true);
    }

    private void setupRecycler() {
        adapter = new SignAdapter(this);
        signs = new ArrayList<>();

        signRecycler = findViewById(R.id.signRecycler);
        signRecycler.setAdapter(adapter);
        signRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    public static Data getData() {
        return data;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GPS_EVENT_SATELLITE_STATUS:
                @SuppressLint("MissingPermission") GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
                int satsInView = 0;
                int satsUsed = 0;
                Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
                for (GpsSatellite sat : sats) {
                    satsInView++;
                    if (sat.usedInFix()) {
                        satsUsed++;
                    }
                }
                satellite.setText(satsUsed + "/" + satsInView);
                if (satsUsed == 0) {
                    data.setRunning(false);
                    status.setText("");
                    stopService(new Intent(getBaseContext(), GpsServices.class));
                    accuracy.setText("");
                    status.setText(getResources().getString(R.string.waiting_for_fix));
                    firstfix = true;
                }
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    showGpsDisabledDialog();
                }
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onLocationChanged(Location location) {
        if (location.hasAccuracy()) {
            double acc = location.getAccuracy();
            String units = "m";

            SpannableString s = new SpannableString(String.format("%.0f %s", acc, units));
            s.setSpan(new RelativeSizeSpan(0.75f), s.length() - units.length() - 1, s.length(), 0);
            accuracy.setText(s);

            if (firstfix) {
                status.setText("");
                firstfix = false;
            }
        } else {
            firstfix = true;
        }

        if (location.hasSpeed()) {
            double speed = location.getSpeed() * 3.6;
//            String units = "km/h";

//            SpannableString s = new SpannableString(String.format(Locale.ENGLISH, "%.0f %s", speed, units));
//            s.setSpan(new RelativeSizeSpan(0.25f), s.length() - units.length() - 1, s.length(), 0);
            currentSpeed.setText(String.format("%.0f", speed));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private void showGpsDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.gps_disabled))
                .setMessage(getString(R.string.please_enable_gps))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setOnShowListener(arg ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(ContextCompat.getColor(this, R.color.cod_gray))
        );
        dialog.show();
    }

    private SignEntity getSignImage(Classifier.Recognition result, Bitmap bitmap) {
        SignEntity sign = null;
        if ("crosswalk".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.crosswalk);
        } else if ("stop".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.stop);
        } else if ("main road".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.main_road);
        } else if ("give road".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.give_road);
        } else if ("children".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.children);
        } else if ("dont stop".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.dont_stop);
        } else if ("no parking".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.no_parking);
        } else if ("dont move".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.dont_move);
        } else if ("dont enter".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.dont_enter);
        } else if ("no overtake".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.no_overtake);
        } else if ("speed limit 5".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_5);
        } else if ("speed limit 10".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_10);
        } else if ("speed limit 20".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_20);
        } else if ("speed limit 30".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_30);
        } else if ("speed limit 40".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_40);
        } else if ("speed limit 50".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_50);
        } else if ("speed limit 60".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_60);
        } else if ("speed limit 70".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_70);
        } else if ("speed limit 80".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_80);
        } else if ("speed limit 90".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_90);
        } else if ("speed limit 100".equals(result.getTitle())) {
            sign = new SignEntity(result.getTitle(), R.drawable.speed_limit_100);
        }

        if (sign != null) {
            sign.setScreenLocation(result.getLocation());
            sign.setLocation(data.getLocation());

            if (sign.getName().contains("speed") && sign.isValidSize(rgbFrameBitmap)) {
                try {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    Bitmap crop = Bitmap.createBitmap(rgbFrameBitmap,
                            (int) sign.getScreenLocation().left,
                            (int) sign.getScreenLocation().top,
                            (int) sign.getScreenLocation().width(),
                            (int) sign.getScreenLocation().height(),
                            matrix,
                            true);
                    Log.i(TAG, new Gson().toJson(sign));
//                ImageUtils.saveBitmap(crop, sign.getName());
                } catch (Exception e) {
                }
            }

//            Log.i(TAG, new Gson().toJson(sign));
        }

        return sign;
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        stopService(new Intent(getBaseContext(), GpsServices.class));
    }
}
