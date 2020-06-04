package com.example.honeyimhome;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class LocationInfo implements Serializable {
    public double latitude;
    public double longitude;
    public float accuracy;

    public LocationInfo(double latitude, double longitude, float accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    @NonNull
    @Override
    public String toString() {
        return "location`s latitude: " + latitude + "\n" +
                "location`s longitude: " + longitude + "\n" +
                "location`s accuracy: " + accuracy + " meters";
    }
}
