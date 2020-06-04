package com.example.honeyimhome;

import android.app.Application;


public class HoneyImHomeApp extends Application {

    public LocationTracker tracker;

    @Override
    public void onCreate() {
        super.onCreate();
        tracker = new LocationTracker(this);
    }
}
