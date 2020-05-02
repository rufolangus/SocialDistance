package com.blurryrobot.socialdistance;

import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    TextInputLayout emailText;
    TextInputLayout passwordText;
    TextInputLayout passwordVerification;

    Button registerButton;

    FirebaseAuth mAuth;

    FirebaseFirestore firestore;
    CollectionReference users;
    CollectionReference beacons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        users = firestore.collection("users");
        beacons = firestore.collection("beacons");
        emailText = findViewById(R.id.email_field);
        passwordText = findViewById(R.id.password_field);
        passwordVerification = findViewById(R.id.password_verification_field);
        registerButton = findViewById(R.id.registration_button);
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
        passwordVerification.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!validPassword(s.toString())){
                    passwordVerification.setError(getString(R.string.password_verification_error));
                }else{
                    passwordVerification.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailText.getEditText().getText().toString();
                String password = passwordText.getEditText().getText().toString();
                String verifyPass = passwordVerification.getEditText().getText().toString();
                if(!validEmail(email)){
                    Toast.makeText(getApplicationContext(), R.string.invalid_email, Toast.LENGTH_LONG).show();
                    return;
                }
                if(!validPassword(password)){
                    Toast.makeText(getApplicationContext(), R.string.invalid_password, Toast.LENGTH_LONG).show();
                    return;
                }
                if (!validVerification(verifyPass)) {
                    Toast.makeText(getApplicationContext(), R.string.invalid_password, Toast.LENGTH_LONG).show();
                    return;
                }
                mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            task.getResult().getUser();
                            createBeaconId(task.getResult().getUser());
                        }else{
                            Toast.makeText(getApplicationContext(),R.string.account_creation_failed,Toast.LENGTH_LONG).show();
                        }
                    }
                });
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

    private void updateBeaconCollection(String userId, final String beaconId){
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

    private boolean validEmail(String target){
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    private boolean validPassword(String target){
        return (!TextUtils.isEmpty(target) && target.length() >= 6);
    }


    private boolean validVerification(String target){
        String password = passwordText.getEditText().getText().toString();
        return (!TextUtils.isEmpty(target) && !TextUtils.isEmpty(password) && password.equals(target));
    }

    private void loadMainActivity(){
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
