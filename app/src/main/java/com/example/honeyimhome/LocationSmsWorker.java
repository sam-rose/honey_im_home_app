package com.example.honeyimhome;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

public class LocationSmsWorker extends ListenableWorker {
    private CallbackToFutureAdapter.Completer<Result> callback;
    private BroadcastReceiver receiver;
    private String logLabel = "LocationSmsWorker";
    public String spPrevLocationKey = "prevLocation";

    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public LocationSmsWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        Log.d(logLabel, "startWork start");
        ListenableFuture<Result> future = CallbackToFutureAdapter.getFuture(new CallbackToFutureAdapter.Resolver<Result>() {
            @Nullable
            @Override
            public Object attachCompleter(@NonNull CallbackToFutureAdapter.Completer<Result> completer) throws Exception {
                callback = completer;
                return null;
            }
        });
        if(!checkPrerequisites()) {
            exitSuccess();
            return future;
        }
        placeReceiver();
        Log.d(logLabel, "startWork done");
        return future;
    }

    private void placeReceiver() {
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(LocationTracker.locationAction)) {
                    return;
                }
                LocationInfo currentLocation = (LocationInfo) intent.getSerializableExtra("location");
                if (currentLocation.accuracy >= 50) {
                    return;
                }
                onReceivedBroadcast(currentLocation);
            }
        };
        SharedPreferences sp = this.getApplicationContext().getSharedPreferences(MainActivity.spLocationInfo, Context.MODE_PRIVATE);
        String homeLocationText = sp.getString(MainActivity.spLocationInfoKey, null);
        if (homeLocationText == null) {
            Log.e(logLabel, "placeReceiver do not have homeLocation");
            exitSuccess();
            return;
        }

        String phoneNumberText = sp.getString(MainActivity.spPhoneNumberKey, null);
        if (phoneNumberText == null) {
            Log.e(logLabel, "placeReceiver do not have phone Number");
            exitSuccess();
            return;
        }
        HoneyImHomeApp app = (HoneyImHomeApp)this.getApplicationContext();
        app.tracker.startTracking();
        app.registerReceiver(this.receiver, new IntentFilter(LocationTracker.locationAction));
        Log.d(logLabel, "startTracking and registerReceiver");

    }

    private void onReceivedBroadcast(LocationInfo currentLocation){
        Log.d(logLabel, "onReceivedBroadcast start");
        HoneyImHomeApp app = (HoneyImHomeApp)this.getApplicationContext();
        app.unregisterReceiver(this.receiver);
        app.tracker.stopTracking();
        SharedPreferences sp = this.getApplicationContext().getSharedPreferences(MainActivity.spLocationInfo, Context.MODE_PRIVATE);
        String prevLocation = sp.getString(spPrevLocationKey, null);
        sp.edit().putString(spPrevLocationKey, currentLocation.latitude + "," + currentLocation.longitude).apply();
        if (prevLocation == null) {
            Log.d(logLabel, "onReceivedBroadcast prevLocation is null");
            exitSuccess();
            return;
        }
        double distance = getDistance(currentLocation, prevLocation);
        if (distance < 50) {
            Log.d(logLabel, "onReceivedBroadcast currentLocation closer than 50 meters to prevLocation");
            exitSuccess();
            return;
        }

        String homeLocation = sp.getString(MainActivity.spLocationInfoKey, null);
        if (homeLocation == null) {
            Log.e(logLabel, "onReceivedBroadcast do not have homeLocation");
            exitSuccess();
            return;
        }
        distance = getDistance(currentLocation, homeLocation);
        if (distance >= 50) {
            Log.d(logLabel, "onReceivedBroadcast currentLocation closer than 50 meters to homeLocation");
            exitSuccess();
            return;
        }
        String phoneNumber = sp.getString(MainActivity.spPhoneNumberKey, null);
        if (phoneNumber == null) {
            Log.d(logLabel, "onReceivedBroadcast phoneNumber is null");
            exitSuccess();
            return;
        }
        // send sms im home
        Log.d(logLabel, "sendPhoneNumberBroadcast");

        sendPhoneNumberBroadcast(phoneNumber, "Honey I'm Sending a Test Message!");
        exitSuccess();
    }

    private boolean checkPrerequisites() {
        boolean locationPermission = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean smsPermission = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;

        return locationPermission && smsPermission;
    }

    private void exitSuccess() {
        if (callback != null) {
            callback.set(Result.success());
        }
    }

    private double getDistance(LocationInfo currentLocation, String otherLocation) {
        String[] otherLocationResult = otherLocation.split(",");
        double prevLatitude = Double.parseDouble(otherLocationResult[0]);
        double prevLongitude = Double.parseDouble(otherLocationResult[1]);
        return Math.sqrt(Math.pow(prevLatitude - currentLocation.latitude, 2) + Math.pow(prevLongitude - currentLocation.longitude, 2));
    }

    private void sendPhoneNumberBroadcast(String phoneNumber, String smsContent) {
        Intent intent = new Intent();
        intent.setAction(LocalSendSmsBroadcastReceiver.actionName);
        intent.putExtra(LocalSendSmsBroadcastReceiver.phoneKey, phoneNumber);
        intent.putExtra(LocalSendSmsBroadcastReceiver.smsContentKey, smsContent);
        this.getApplicationContext().sendBroadcast(intent);
    }
}
