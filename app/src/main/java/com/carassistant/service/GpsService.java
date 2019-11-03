package com.carassistant.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.carassistant.R;
import com.carassistant.model.entity.Data;
import com.carassistant.ui.activities.DetectorActivity;

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
    }

    @Override
    public void onLocationChanged(Location location) {
        data = DetectorActivity.getData();
        if (data.isRunning()){

            data.setLocation(location);
            currentLat = location.getLatitude();
            currentLon = location.getLongitude();

            if (data.isFirstTime()){
                lastLat = currentLat;
                lastLon = currentLon;
                data.setFirstTime(false);
            }

            lastlocation.setLatitude(lastLat);
            lastlocation.setLongitude(lastLon);
            double distance = lastlocation.distanceTo(location);

            if (location.getAccuracy() < distance){
                data.addDistance(distance);

                lastLat = currentLat;
                lastLon = currentLon;
            }

            if (location.hasSpeed()) {
                data.setCurSpeed(location.getSpeed() * 3.6);
                if(location.getSpeed() == 0){
                    new isStillStopped().execute();
                }
            }
            data.update();
            updateNotification(true);
        }
    }

    public void showGpsDisabledDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.gps_disabled))
                .setMessage(getString(R.string.please_enable_gps))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
                });
        builder.create().show();
    }


    public void updateNotification(boolean asData){

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
                .setSmallIcon(R.drawable.ic_directions_car)
                .setColor(ContextCompat.getColor(this, R.color.orange))
                .setPriority(Notification.PRIORITY_LOW)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(false)
                .setAutoCancel(false)
                .setSound(null)
                .setVibrate(new long[]{0})
                .setContentIntent(contentIntent);

        if(asData){
            builder.setContentText(String.format("Location", data.getMaxSpeed(), data.getDistance()));
        }else{
            builder.setContentText(String.format("Location", '-', '-'));
        }
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

    public class GpsServiceBinder extends Binder  {
        public GpsService getService(){
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
    public void onGpsStatusChanged(int event) {}

    @Override
    public void onProviderDisabled(String provider) {}
   
    @Override
    public void onProviderEnabled(String provider) {}
   
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

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
