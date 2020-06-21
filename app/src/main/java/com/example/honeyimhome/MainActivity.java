package com.example.honeyimhome;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static final int MY_PERMISSIONS_REQUEST_SMS =98;
    public static String spLocationInfo = "homeLocationInfo";
    public static String spLocationInfoKey = "homeLocation";
    public static String spPhoneNumberKey = "smsNumber";
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
                return;
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
            if (currentLocationText != null && !currentLocationText.isEmpty() && checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, MY_PERMISSIONS_REQUEST_LOCATION)) {
                    startTracking();
            }
        }

        sp = getSharedPreferences(spLocationInfo, MODE_PRIVATE);
        String homeLocationText = sp.getString(spLocationInfoKey, null);
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
                    String homeLocationText = lastLocation.latitude + "," + lastLocation.longitude ;
                    editor.putString("homeLocation", homeLocationText);
                    editor.apply();
                    setTextView("<" + homeLocationText + ">", R.id.home_location_textView);
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
                    if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, MY_PERMISSIONS_REQUEST_LOCATION)) {
                        startTracking();
                    }
                }
            }
    });


        Button phoneNumbutton = findViewById(R.id.phoneNumbutton);
            phoneNumbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checkPermission(Manifest.permission.SEND_SMS, MY_PERMISSIONS_REQUEST_SMS)) {
                        storePhoneNumber();
                    }
                }
        });
        String phoneNumber = sp.getString(spPhoneNumberKey, "");
        manageTestSmsButton(phoneNumber);

        Button testSmsButton = findViewById(R.id.test_sms_button);
        testSmsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = sp.getString(spPhoneNumberKey, "");
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    sendPhoneNumberBroadcast(phoneNumber, "Honey I'm Sending a Test Message!");
                }
                else {
                    Log.e(LogTag, "trying to send sms but no number is assigned");
                }
            }
        });

        PeriodicWorkRequest requestForAsyncWorker = new PeriodicWorkRequest.Builder(LocationSmsWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build();

        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueue(requestForAsyncWorker);
    }

    public void startTracking() {
        LocationTracker tracker = ((HoneyImHomeApp) getApplicationContext()).tracker;
        Button trackLocationButton = findViewById(R.id.track_button);
        trackLocationButton.setText("stop tracking");
        tracker.startTracking();
    }

//
    public boolean checkPermission(final String permissionType, final int requestCode) {
        if (ContextCompat.checkSelfPermission(this,
                permissionType)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permissionType)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Importance")
                        .setMessage("This permission is important for using this application")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{permissionType},
                                        requestCode);
                            }
                        })
                        .create()
                        .show();


            }
            else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{permissionType},
                        requestCode);
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
                break;
                }


            case MY_PERMISSIONS_REQUEST_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Request location updates:
                    storePhoneNumber();
                }
                break;
            }
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

    private void sendPhoneNumberBroadcast(String phoneNumber, String smsContent) {
        Intent intent = new Intent();
        intent.setAction(LocalSendSmsBroadcastReceiver.actionName);
        intent.putExtra(LocalSendSmsBroadcastReceiver.phoneKey, phoneNumber);
        intent.putExtra(LocalSendSmsBroadcastReceiver.smsContentKey, smsContent);
        sendBroadcast(intent);
    }

    private void  storePhoneNumber() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("set SMS phone number");

    // Set up the input
    final EditText input = new EditText(this);
    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_PHONE);
    builder.setView(input);

    // Set up the buttons
    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String phoneNumber = input.getText().toString();
            SharedPreferences.Editor editor = sp.edit();
            if (!phoneNumber.isEmpty()) {
                Log.d(LogTag, "setting phone number to " + phoneNumber);
                editor.putString(spPhoneNumberKey, phoneNumber);
            }
            else {
                editor.remove(spPhoneNumberKey);
            }
            manageTestSmsButton(phoneNumber);
            editor.apply();
        }
    });
    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    });

    builder.show();

    }

    private void manageTestSmsButton(String phoneNumber) {
        Button testSmsButton = findViewById(R.id.test_sms_button);
        if (phoneNumber == null  || phoneNumber.isEmpty()) {
            testSmsButton.setVisibility(View.GONE);
        }
        else {
            testSmsButton.setVisibility(View.VISIBLE);
        }
    }
}
