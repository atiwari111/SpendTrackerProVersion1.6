package com.spendtracker.pro;

import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_splash);
        NotificationHelper.createChannels(this);
        new Handler().postDelayed(this::checkBiometric, 1200);
    }

    private void checkBiometric() {
        SharedPreferences prefs = getSharedPreferences("stp_prefs", MODE_PRIVATE);
        boolean bioEnabled = prefs.getBoolean("bio_enabled", false);
        if (bioEnabled) {
            showBiometric();
        } else {
            goToMain();
        }
    }

    private void showBiometric() {
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("SpendTracker Pro")
                .setSubtitle("Verify your identity")
                .setNegativeButtonText("Cancel")
                .build();

        new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) { goToMain(); }
                    @Override public void onAuthenticationError(int code, CharSequence msg) { finish(); }
                    @Override public void onAuthenticationFailed() {}
                }).authenticate(info);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
