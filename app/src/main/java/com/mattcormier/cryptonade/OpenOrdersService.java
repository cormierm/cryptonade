package com.mattcormier.cryptonade;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.AlertOrder;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by matt on 12/29/2017.
 */

public class OpenOrdersService extends Service {
    private static final String TAG = "OpenOrdersService";
    private CryptonadeApp app;
    private CryptoDB db;
    private Timer timer;


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: service created");
        app = (CryptonadeApp) getApplication();
        db = new CryptoDB(getApplicationContext());
        startTimer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: service bound");
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: service destroyed");
        stopTimer();
    }

    private void startTimer() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "run: timer started");

                ArrayList<AlertOrder> orders = db.getAlertOrders();

                if(orders.isEmpty()) {
                    stopTimer();
                    stopSelf();
                }

                for (AlertOrder order: orders) {
                    APIClient client = Crypto.getAPIClient(db.getExchange(order.getExchangeId()));
                    client.CheckOpenOrder(getApplicationContext(), order.getOrderId(), order.getSymbol());
                }
            }
        };

        timer = new Timer(true);
        timer.schedule(task, 10* 1000, 10 * 1000);
    }

    private void stopTimer() {
        if(timer != null) {
            timer.cancel();
        }
    }


}
