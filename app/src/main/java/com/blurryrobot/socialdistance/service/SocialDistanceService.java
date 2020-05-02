package com.blurryrobot.socialdistance.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.blurryrobot.socialdistance.MainActivity;
import com.blurryrobot.socialdistance.R;
import com.blurryrobot.socialdistance.UserState;
import com.blurryrobot.socialdistance.utility.UUIDHelper;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class SocialDistanceService extends Service implements BeaconConsumer {

    private static final String TAG = "SocialDistanceBackground";
    private static final String CHANNEL_ID = "SocialDistanceBackgroundChannel";
    private static final String REGION = "COVID";
    private Region region = new Region(REGION,null,null,null);

    private final  IBinder binder = new LocalBinder();

    private static final Identifier MY_MATCHING_IDENTIFIER = Identifier.fromInt(0x8b9c);

    private BeaconManager beaconManager;
    private UUID userId;
    private ServiceListener serviceListener;
    private BeaconTransmitter beaconTransmitter;

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "onStartAdvertiseSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            String reason = "";
            switch (errorCode){
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    reason = "Already started";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    reason = "Data too large";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    reason = "Feature unsupported";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    reason = "Internal error";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    reason = "Too many advertisers";
                    break;
            }
            Log.d(TAG, "AdvertiseCallback: onStartFailure - " + reason);
        }
    };

    private RangeNotifier rangeNotifier = new RangeNotifier() {
        @Override
        public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
            Log.d(TAG, "Beacons in Region: " + collection.size());
            collection.forEach(new Consumer<Beacon>() {
                @Override
                public void accept(Beacon beacon) {
                    if (beacon.getId1().equals(MY_MATCHING_IDENTIFIER)) {
                        byte[] bytes = beacon.getId2().toByteArray();
                        final UUID uuid = UUIDHelper.asUuid(bytes);
                        Log.d(TAG, "I just received: "+ uuid.toString() + " Distance: " + beacon.getDistance());
                        if (beacon.getDistance() <= 2.5){
                            BeaconResult beaconResult = new BeaconResult(uuid.toString(),beacon.getDistance());
                            serviceListener.onResult(beaconResult);
                        }

                    }
                }
            });
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
   }

    private void startScan(){
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-5,i:6-21,p:24-24"));
        beaconManager.addRangeNotifier(rangeNotifier);
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.e(TAG, "Error starting scan.", e);
        }
    }

    private void stopScan(){
        beaconManager.removeAllRangeNotifiers();
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.e(TAG, "Error stopping scan.", e);
        }
        beaconManager.getBeaconParsers().clear();
    }

    private void startAdvertising(){
        BeaconParser beaconParser =new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-5,i:6-21,p:24-24");
        byte[] stringToTransmitAsAsciiBytes = UUIDHelper.asBytes(UserState.getBeaconId());
        Log.d(TAG, "UUUID: " + UserState.getBeaconId().toString() + " bytes: " + stringToTransmitAsAsciiBytes.length);
        Beacon beacon = new Beacon.Builder().setId1(MY_MATCHING_IDENTIFIER.toString())
                .setId2(Identifier.fromBytes(stringToTransmitAsAsciiBytes, 0, 16, false).toString())
                .setTxPower(-59).build();

        beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitter.startAdvertising(beacon,advertiseCallback);
    }

    private void stopAdvertising(){
        if (beaconTransmitter != null) {
            beaconTransmitter.stopAdvertising();
            beaconTransmitter = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public boolean start(ServiceListener serviceListener, String Uuid){
        if(serviceListener == null|| Uuid == null)
            return false;
        Log.d(TAG, "userId:" + Uuid);
        userId = UUID.fromString(Uuid);
        this.serviceListener = serviceListener;

        beaconManager = BeaconManager.getInstanceForApplication(getApplicationContext());
        beaconManager.bind(this);
        return true;
    }

    @Override
    public void onBeaconServiceConnect() {
        startScan();
        startAdvertising();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.social_dstance))
                .setContentText(getString(R.string.scanning_and_advertising))
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        //do heavy work on a background thread
        //stopSelf();
        return START_NOT_STICKY;
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public class LocalBinder extends Binder{
        public SocialDistanceService getService() {
            return SocialDistanceService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAdvertising();
        stopScan();
        beaconManager.unbind(this);
    }

    public interface ServiceListener{
        void onResult(BeaconResult result);
    }

}
