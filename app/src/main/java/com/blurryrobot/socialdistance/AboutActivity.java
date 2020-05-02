package com.blurryrobot.socialdistance;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

public class AboutActivity extends AppCompatActivity {

    Button openSourceButton;
    Button emailButton;
    Button termsOfServiceButton;
    Button privacyPolicyButton;
    TextView versionNameText;
    TextView versionCodeText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        versionNameText = findViewById(R.id.versionNameText);
        versionCodeText = findViewById(R.id.versionCodeText);
        versionNameText.setText(BuildConfig.VERSION_NAME);
        versionCodeText.setText("" + BuildConfig.VERSION_CODE);
        openSourceButton = findViewById(R.id.open_source_button);
        emailButton = findViewById(R.id.email_button);
        termsOfServiceButton = findViewById(R.id.terms_of_service_button);
        privacyPolicyButton = findViewById(R.id.privacy_policy_button);

        openSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                OssLicensesMenuActivity.setActivityTitle(getString(R.string.custom_license_title));
                startActivity(new Intent(AboutActivity.this, OssLicensesMenuActivity.class));
            }
        });
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.rafael_blurryrobot_net)));
                startActivity(Intent.createChooser(emailIntent, "rafael@blurryrobot.net"));

            }
        });
        termsOfServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://firebasestorage.googleapis.com/v0/b/social-dstance.appspot.com/o/legal%2Fterms_and_conditions.html?alt=media&token=42e9f3a3-fe73-4e0a-8d5e-c084e74c7e76";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        privacyPolicyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://firebasestorage.googleapis.com/v0/b/social-dstance.appspot.com/o/legal%2Fprivacy_policy.html?alt=media&token=29b9fcc7-0a92-4a36-93f5-06a37b252034";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
    }
}
