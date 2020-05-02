package com.blurryrobot.socialdistance.utility;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothStatusIntentReceiver extends BroadcastReceiver {

    private static final String TAG = "BluetoothStatusReceiver";
    private final BluetoothStatusListener listener;

    public BluetoothStatusIntentReceiver (BluetoothStatusListener listener){
        this.listener = listener;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);
        if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
            listener.onStatusChanged();
        }
    }
    public interface  BluetoothStatusListener{
        void onStatusChanged();
    }
}
