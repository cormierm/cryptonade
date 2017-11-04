package com.mattcormier.cryptonade;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;

import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


public class StartActivity extends AppCompatActivity {
    private static final String TAG = "StartActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

//        char[] password = "hello".toCharArray();
//        byte[] salt = "as".getBytes();
//        SecretKey secret = Crypto.createSecret(password, salt);
//        HashMap<String, String> asdf = Crypto.encryptString("Hello there", secret);
//        Log.d(TAG, "onCreate: " + asdf.get("ciphertext"));
//
//        String blah = Crypto.decryptString(asdf.get("ciphertext"), secret, asdf.get("iv"));
//        Log.e(TAG, "onCreate: " + blah );


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
