package com.example.honeyimhome;

import android.app.Application;
import android.content.IntentFilter;


public class HoneyImHomeApp extends Application {

    public LocationTracker tracker;
    public LocalSendSmsBroadcastReceiver smsBR = new LocalSendSmsBroadcastReceiver();
    @Override
    public void onCreate() {
        super.onCreate();
        tracker = new LocationTracker(this);
        registerReceiver(smsBR, new IntentFilter(LocalSendSmsBroadcastReceiver.actionName));

    }
}
