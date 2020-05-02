package com.blurryrobot.socialdistance;


import com.google.firebase.Timestamp;

public class ContactModel {
    private String id;
    private String beaconId;
    private double distance;
    private Timestamp timestamp;

    public ContactModel(){

    }

    public ContactModel(String beaconId, Timestamp timestamp) {
        this.beaconId = beaconId;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ContactModel{" +
                "id='" + id + '\'' +
                ", beaconId='" + beaconId + '\'' +
                ", distance=" + distance +
                ", timestamp=" + timestamp +
                '}';
    }

    public ContactModel(String id, String beaconId, double distance, Timestamp timestamp) {
        this.id = id;
        this.beaconId = beaconId;
        this.distance = distance;
        this.timestamp = timestamp;
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBeaconId() {
        return beaconId;
    }

    public void setBeaconId(String beaconId) {
        this.beaconId = beaconId;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
