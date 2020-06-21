package com.example.honeyimhome;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import static android.provider.Settings.System.getString;

public class LocalSendSmsBroadcastReceiver extends BroadcastReceiver {
    public static final String phoneKey = "PHONE";
    public static final String smsContentKey = "CONTENT";
    public static final String actionName = "POST_PC.ACTION_SEND_SMS";
    private static final String logLabel = "SendSmsReceiver";

    public void onReceive(Context context, Intent intent) {
        boolean permission_granted = ContextCompat.checkSelfPermission(context,
                Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        if (!permission_granted) {
            Log.e(logLabel, "sms permission not granted");
            return;
        }

        if (!intent.getAction().equals(actionName)) {
            return;
        }
        String phoneNumber = intent.getStringExtra(phoneKey);
        if (phoneNumber == null) {
            Log.e(logLabel, "no phone number given for sending sms");
            return;
        }

        String smsContent = intent.getStringExtra(smsContentKey);
        if (smsContent == null) {
            Log.e(logLabel, "no sms content given for sending sms");
            return;
        }
        createNotificationChannel(context);

        Notification ntfc = new NotificationCompat.Builder(context, "99")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("sending sms to  " + phoneNumber + ": " + smsContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        // notificationId is a unique int for each notification that you must define
        int notificationId = 5364;
        NotificationManagerCompat.from(context).notify(notificationId, ntfc);

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, smsContent, null, null);
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "sms_notification";
            String description = "show that sms is sent";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("99", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
