package com.mattcormier.cryptonade;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;

/**
 * Filename: StartActivity.java
 * Description: Activity that loads on app start which checks for app start password
 *   and displays splash screen.
 * Created by Matt Cormier on 10/24/2017.
 */

public class StartActivity extends AppCompatActivity {
    private static final String TAG = "StartActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    @Override
    protected void onResume() {
        super.onResume();

        CryptoDB db = new CryptoDB(this);

        String passwordHash = db.getPasswordHash();
        if (!passwordHash.equals("")) {
            Crypto.loginPassword(this);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }
}
