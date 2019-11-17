package com.carassistant.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.carassistant.R;
import com.carassistant.model.bus.MessageEventBus;
import com.carassistant.model.bus.model.EventGpsDisabled;
import com.carassistant.model.bus.model.EventUpdateLocation;
import com.carassistant.model.bus.model.EventUpdateStatus;
import com.carassistant.model.entity.Data;
import com.carassistant.model.entity.GpsStatusEntity;
import com.carassistant.ui.activities.DetectorActivity;

import static android.location.GpsStatus.GPS_EVENT_SATELLITE_STATUS;

public class GpsService extends Service implements LocationListener, GpsStatus.Listener {

    private LocationManager mLocationManager;

    Location lastlocation = new Location("last");
    Data data;

    double currentLon = 0;
    double currentLat = 0;
    double lastLon = 0;
    double lastLat = 0;

    PendingIntent contentIntent;

    private GpsServiceBinder callServiceBinder = new GpsServiceBinder();

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {

        Intent notificationIntent = new Intent(this, DetectorActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        contentIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 0);

        updateNotification(false);

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.addGpsStatusListener(this);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDisabledDialog();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        updateNotification(true);

        data = new Data();
        data.setLocation(location);
        currentLat = location.getLatitude();
        currentLon = location.getLongitude();

        double distance = 0;

        if (lastLat != 0 && lastLon != 0){
            lastlocation.setLatitude(lastLat);
            lastlocation.setLongitude(lastLon);
            distance = lastlocation.distanceTo(location);
        }

        if (location.getAccuracy() < distance || distance == 0) {
            data.setDistance(distance);
            lastLat = currentLat;
            lastLon = currentLon;
        }

        if (location.hasSpeed()) {
            data.setCurSpeed(location.getSpeed() * 3.6);
            if (location.getSpeed() == 0) {
                new isStillStopped().execute();
            }
        }

        MessageEventBus.INSTANCE.send(new EventUpdateLocation(data));

    }

    public void showGpsDisabledDialog() {
        MessageEventBus.INSTANCE.send(new EventGpsDisabled());
    }


    public void updateNotification(boolean asData) {

        String channelId = "Car assistant";
        String channelName = "Location";

        NotificationManager notificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                    new NotificationChannel(
                            channelId,
                            channelName,
                            NotificationManager.IMPORTANCE_LOW
                    )
            );
            notificationManager.getNotificationChannel(channelId).setSound(null, null);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext(), channelId)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(String.format("Location", '-', '-'))
                .setSmallIcon(R.drawable.ic_directions_car)
                .setColor(ContextCompat.getColor(this, R.color.orange))
                .setPriority(Notification.PRIORITY_LOW)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(false)
                .setAutoCancel(false)
                .setSound(null)
                .setVibrate(new long[]{0})
                .setContentIntent(contentIntent);

        Notification notification = builder.build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return callServiceBinder;
    }

    public class GpsServiceBinder extends Binder {
        public GpsService getService() {
            return GpsService.this;
        }
    }


    /* Remove the locationlistener updates when Services is stopped */
    @Override
    public void onDestroy() {
        mLocationManager.removeUpdates(this);
        mLocationManager.removeGpsStatusListener(this);
        stopForeground(true);
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

                String satellite = satsUsed + "/" + satsInView;
                String accuracy = null;
                String status = null;
                if (satsUsed == 0) {
                    stopService(new Intent(getBaseContext(), GpsService.class));
                    accuracy = "";
                    status = getResources().getString(R.string.waiting_for_fix);
                }

                MessageEventBus.INSTANCE.send(new EventUpdateStatus(new GpsStatusEntity(satellite, status, accuracy)));

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

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    class isStillStopped extends AsyncTask<Void, Integer, String> {
        int timer = 0;

        @Override
        protected String doInBackground(Void... unused) {
            try {
                while (data.getCurSpeed() == 0) {
                    Thread.sleep(1000);
                    timer++;
                }
            } catch (InterruptedException t) {
                return ("The sleep operation failed");
            }
            return ("return object when task is finished");
        }

        @Override
        protected void onPostExecute(String message) {
            data.setTimeStopped(timer);
        }
    }
}
