package com.mattcormier.cryptonade;

import android.app.Application;
import android.util.Log;

/**
 * Created by matt on 12/29/2017.
 */

public class CryptonadeApp extends Application {
    private static final String TAG = "CryptonadeApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: App started");
    }
}
