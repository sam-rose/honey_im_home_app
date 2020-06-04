package com.example.honeyimhome;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    SharedPreferences sp;
    LocationInfo lastLocation;
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("location_update")) {
                return;
            }
            final Button setHomeLocation = findViewById(R.id.set_home_button);
            LocationInfo currentLocation = (LocationInfo) intent.getSerializableExtra("location");
            if (currentLocation.accuracy >= 50) {
                setHomeLocation.setVisibility(View.GONE);
            }
            setHomeLocation.setVisibility(View.VISIBLE);
            lastLocation = currentLocation;
            setTextView(lastLocation.toString(), R.id.current_location_textView);
        }
    };
    private String LogTag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final LocationTracker tracker = ((HoneyImHomeApp) getApplicationContext()).tracker;
        if (savedInstanceState != null) {
            String currentLocationText = savedInstanceState.getString("lastUICurrentLocation");
            if (currentLocationText != null && !currentLocationText.isEmpty() && checkLocationPermission()) {
                    startTracking();
            }
        }

        sp = getSharedPreferences("homeLocationInfo", MODE_PRIVATE);
        String homeLocationText = sp.getString("homeLocation", null);
        final Button clearButton = findViewById(R.id.clear_home_location_button);
        if (homeLocationText != null) {
            setTextView(homeLocationText, R.id.home_location_textView);
            clearButton.setVisibility(View.VISIBLE);
        }
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTextView("", R.id.home_location_textView);
                SharedPreferences.Editor editor = sp.edit();
                editor.remove("homeLocation");
                editor.apply();
                clearButton.setVisibility(View.GONE);
            }
        });

        final Button setHomeLocationButton = findViewById(R.id.set_home_button);
        setHomeLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastLocation != null) {
                    SharedPreferences.Editor editor = sp.edit();
                    String homeLocationText = "<" + lastLocation.latitude + ", " + lastLocation.longitude + ">";
                    editor.putString("homeLocation", homeLocationText);
                    editor.apply();
                    setTextView(homeLocationText, R.id.home_location_textView);
                    clearButton.setVisibility(View.VISIBLE);
                }
            }
        });

        registerReceiver(br, new IntentFilter("location_update"));
        Button trackLocationButton = findViewById(R.id.track_button);
        trackLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LogTag, "tracker.isTracking = " + tracker.isTracking);
                if(tracker.isTracking) {
                    Button trackLocationButton = (Button)v;
                    trackLocationButton.setText("start tracking location");
                    tracker.stopTracking();
                    setTextView("", R.id.current_location_textView);
                    setHomeLocationButton.setVisibility(View.GONE);
                }
                else {
                    if (checkLocationPermission()) {
                        startTracking();
                    }
                }
            }
        });
    }

    public void startTracking() {
        LocationTracker tracker = ((HoneyImHomeApp) getApplicationContext()).tracker;
        Button trackLocationButton = findViewById(R.id.track_button);
        trackLocationButton.setText("stop tracking");
        tracker.startTracking();
    }


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Importance")
                        .setMessage("Location permission is important for using this application")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            }
            else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        //Request location updates:
                        startTracking();
                    }
                }
                return;
            }
    }

    protected void setTextView(String textToDisplay, int viewId) {
        TextView textView = findViewById(viewId);
        textView.setText(textToDisplay);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        super.onSaveInstanceState(outState);
        TextView textView = findViewById(R.id.current_location_textView);
        outState.putString("lastUICurrentLocation", textView.getText().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationTracker tracker = ((HoneyImHomeApp) getApplicationContext()).tracker;
        unregisterReceiver(br);
        tracker.stopTracking();
    }
}
