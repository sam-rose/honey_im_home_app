package com.example.honeyimhome;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LocationTracker extends LocationCallback {
    private Context context;
    private String LogTag = "LocationTracker";
    public boolean isTracking = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;

    public LocationTracker(Context context) {
        this.context = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void startTracking() {
        boolean permission_granted = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!permission_granted) {
            throw new AssertionError("Trying to start tracking without location permission");
        }
        isTracking = true;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3 * 1000);
        mFusedLocationClient.requestLocationUpdates(locationRequest, this, Looper.getMainLooper());
        Log.d(LogTag, "starting to track location");


    }

    public void stopTracking() {
        isTracking = false;
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(this);
        }
        Log.d(LogTag, "stopping tracking location");
    }

    @Override
    public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        if (locationResult == null) {
            return;
        }
        for (Location location : locationResult.getLocations()) {
            if (location != null) {
                sendLocationBroadcast(context, new LocationInfo(location.getLatitude(), location.getLongitude(), location.getAccuracy()));
            }
        }
    }

    private void sendLocationBroadcast(Context context, LocationInfo locationInfo) {
        Intent intent = new Intent();
        intent.setAction("location_update");
        intent.putExtra("location", locationInfo);
        context.sendBroadcast(intent);
    }
}
