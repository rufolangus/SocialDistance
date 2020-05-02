package com.blurryrobot.socialdistance.service;

public class BeaconResult {
    String beaconId;
    double distance;

    public BeaconResult(){

    }

    public BeaconResult(String beaconId, double distance) {
        this.beaconId = beaconId;
        this.distance = distance;
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

    @Override
    public String toString() {
        return "BeaconResult{" +
                "beaconId='" + beaconId + '\'' +
                ", distance=" + distance +
                '}';
    }
}
