package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.mattcormier.cryptonade.clients.APIClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by matt on 10/24/2017.
 */

public class TickerBarFragment extends Fragment {
    private static final String TAG = "BalanceBarFragment";
    TextView tvTickerBar;
    HashMap<String, Double> currentBalances;
    View view;
    Spinner spnClients;
    APIClient client;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        spnClients = ((MainActivity)getActivity()).findViewById(R.id.spnClients);
        view = inflater.inflate(R.layout.balance_bar_layout, container, false);
        context = getActivity();
        tvTickerBar = view.findViewById(R.id.tvBalanceBar);

        return view;
    }

    @Override
    public void onResume() {
        startTickerBarTimer();
        super.onResume();
    }

    public void startTickerBarTimer() {
        Log.d(TAG, "startTickerBarTimer: start");
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                client = (APIClient)spnClients.getSelectedItem();
                if (client != null) {
                    HashMap<String, String> tickerInfo = client.getTickerInfo();
                    if (tickerInfo == null) {
                        setTickerBarText("");
                    }
                    else {
                        String output = "";
                        for (Map.Entry<String, String> t: tickerInfo.entrySet()) {
                            output += t.getKey() + ": " + t.getValue() + "    ";
                        }
                        output.trim();
                        if (output.isEmpty()) {
                            output = "";
                        }
                        setTickerBarText(output);
                    }
                }
            }
        };
        Timer timer = new Timer(true);
        timer.schedule(task, 0, 1000);
    }

    public void setTickerBarText(final String text) {
        tvTickerBar.post(new Runnable() {
            @Override
            public void run() {
                tvTickerBar.setText(text);
            }
        });
    }
}
