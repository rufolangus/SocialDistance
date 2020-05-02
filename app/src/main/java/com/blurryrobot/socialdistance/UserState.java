package com.blurryrobot.socialdistance;

import java.util.UUID;

public class UserState {

    private static UUID beaconId;
    public static UUID getBeaconId(){
        return beaconId;
    }

    public static void setBeaconId(UUID beaconId){
        UserState.beaconId = beaconId;
    }
    private UserState(){
        //HideConstructor
    }
}
