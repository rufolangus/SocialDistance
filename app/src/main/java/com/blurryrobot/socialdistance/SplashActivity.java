package com.blurryrobot.socialdistance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SplashActivity extends AppCompatActivity {

    private static boolean adMobInitialized = false;
    private final String TAG = "SplashActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore fireStore;
    private CollectionReference users;
    private ListenerRegistration uuidListenerRegistration;
    private CollectionReference beacons;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_splash);
        mAuth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        users = fireStore.collection("users");
        beacons = fireStore.collection("beacons");
        if (!adMobInitialized)
            initializeAdmob();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            startLoginActivity();
        else
            verifyBeaconId(user);

    }

    void verifyBeaconId(final FirebaseUser user){
        Log.d(TAG, "verifyBeaconId");
        Log.d(TAG, user.getUid());
        uuidListenerRegistration = users.document(user.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                String beaconId = documentSnapshot.getString("beaconId");
                uuidListenerRegistration.remove();
                if(beaconId == null || TextUtils.isEmpty(beaconId)) {
                    createBeaconId(user);
                }
                else {
                    UserState.setBeaconId(UUID.fromString(beaconId));
                    Toast.makeText(getApplicationContext(),R.string.sign_in_success,Toast.LENGTH_LONG).show();
                    startMainActivity();
                }
            }
        });
    }

    void startMainActivity(){
        Log.d(TAG, "startMainActivity");
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    void startLoginActivity(){
        Log.d(TAG, "startLoginActivity");
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void createBeaconId(FirebaseUser user){
        Log.d(TAG, "createBeaconId");
        final String beaconId = UUID.randomUUID().toString();
        final String userId = user.getUid();
        Map<String,Object> data = new HashMap<>();
        data.put("beaconId",beaconId);
        users.document(userId).set(data, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    updateBeaconCollection(userId,beaconId);
                }else{
                    Toast.makeText(getApplicationContext(),R.string.sign_in_failed,Toast.LENGTH_LONG).show();
                    Log.e(TAG,"error creating beacon Id", task.getException());
                }
            }
        });
    }

    private void updateBeaconCollection(String userId, final String beaconId){
        Map<String,Object> beaconData = new HashMap<>();
        beaconData.put("uid",userId);
        beacons.document(beaconId).set(beaconData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(getApplicationContext(),R.string.sign_in_success,Toast.LENGTH_LONG).show();
                    UserState.setBeaconId(UUID.fromString(beaconId));
                    startMainActivity();
                }else{
                    Toast.makeText(getApplicationContext(),R.string.sign_in_failed,Toast.LENGTH_LONG).show();
                    Log.e(TAG,"error creating beacon Id", task.getException());
                }
            }
        });
    }

    void initializeAdmob(){
        Log.d(TAG, "initiaizeAdmob");
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                adMobInitialized = true;
                Log.d(TAG, "Admob Initialized");
            }
        });
    }
}
