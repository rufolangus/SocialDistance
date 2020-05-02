package com.blurryrobot.socialdistance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    TextInputLayout emailText;
    TextInputLayout passwordText;

    Button loginButton;
    Button registerButton;
    Button passwordResetButton;

    FirebaseAuth mAuth;
    FirebaseFirestore firestore;
    CollectionReference users;
    CollectionReference beacons;
    ListenerRegistration uuidListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        beacons = firestore.collection("beacons");
        users = firestore.collection("users");

        emailText = findViewById(R.id.email_field);
        emailText.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!validEmail(s.toString())){
                    emailText.setError(getString(R.string.email_error));
                }else{
                    emailText.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        passwordText = findViewById(R.id.password_field);
        passwordText.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!validPassword(s.toString())){
                    passwordText.setError(getString(R.string.password_error));
                }else{
                    passwordText.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);
        passwordResetButton = findViewById(R.id.password_reset_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailText.getEditText().getText().toString();
                String password = passwordText.getEditText().getText().toString();
                if(!validEmail(email)){
                    Toast.makeText(getApplicationContext(), R.string.invalid_email, Toast.LENGTH_LONG).show();
                    return;
                }
                if(!validPassword(password)){
                    Toast.makeText(getApplicationContext(), R.string.invalid_password, Toast.LENGTH_LONG).show();
                    return;
                }
                mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "Sign in Successful.");
                            verifyBeaconId(task.getResult().getUser());
                        }else{
                            Log.d(TAG, "Sign in Failed.");
                            Toast.makeText(getApplicationContext(),R.string.sign_in_failed, Toast.LENGTH_LONG).show();

                        }
                    }
                });
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        passwordResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, PasswordResetActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean validEmail(String target){
     return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    private boolean validPassword(String target){
        return (!TextUtils.isEmpty(target) && target.length() >= 6);
    }

    private void loadMainActivity(){
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    void verifyBeaconId(final FirebaseUser user){
        Log.d(TAG, "verifyBeaconId");
        Log.d(TAG, user.getUid());
       uuidListenerRegistration =  users.document(user.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                uuidListenerRegistration.remove();
                String beaconId = documentSnapshot.getString("beaconId");
                if(beaconId == null || TextUtils.isEmpty(beaconId)) {
                    createBeaconId(user);
                }
                else {
                    UserState.setBeaconId(UUID.fromString(beaconId));
                    Toast.makeText(getApplicationContext(),R.string.sign_in_success,Toast.LENGTH_LONG).show();
                    loadMainActivity();
                }
            }
        });
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

    private void updateBeaconCollection(String userId,final  String beaconId){
        Map<String,Object> beaconData = new HashMap<>();
        beaconData.put("uid",userId);
        beacons.document(beaconId).set(beaconData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    UserState.setBeaconId(UUID.fromString(beaconId));
                    Toast.makeText(getApplicationContext(),R.string.sign_in_success,Toast.LENGTH_LONG).show();
                    loadMainActivity();
                }else{
                    Toast.makeText(getApplicationContext(),R.string.sign_in_failed,Toast.LENGTH_LONG).show();
                    Log.e(TAG,"error creating beacon Id", task.getException());
                }
            }
        });

    }

}
