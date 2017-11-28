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

public class BalanceBarFragment extends Fragment {
    private static final String TAG = "BalanceBarFragment";
    TextView tvBalanceBar;
    HashMap<String, Double> currentBalances;
    View balanceView;
    Spinner spnClients;
    APIClient client;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        spnClients = getActivity().findViewById(R.id.spnClients);
        balanceView = inflater.inflate(R.layout.balance_bar_layout, container, false);
        context = getActivity();
        tvBalanceBar = balanceView.findViewById(R.id.tvBalanceBar);

        return balanceView;
    }

    @Override
    public void onResume() {
       startBalanceBarTimer();
        super.onResume();
    }

    public void startBalanceBarTimer() {
        Log.d(TAG, "startBalanceBarTimer: start");
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                client = (APIClient)spnClients.getSelectedItem();
                if (client != null) {
                    HashMap<String, Double> availableBalances = client.getAvailableBalances();
                    if (availableBalances == null) {
                        setBalanceBarText("No Balance Information.");
                    }
                    else if (currentBalances != availableBalances) {
                        String output = "";
                        for (Map.Entry<String, Double> b: availableBalances.entrySet()) {
                            output += b.getKey() + ": " + String.format("%.8f", b.getValue()) + "        ";
                        }
                        output = output.trim();
                        if (output.isEmpty()) {
                            output = "No Balance Information.";
                        }
                        setBalanceBarText(output);
                    }
                }
            }
        };
        Timer timer = new Timer(true);
        timer.schedule(task, 0, 1000);
    }

    public void setBalanceBarText(final String text) {
        tvBalanceBar.post(new Runnable() {
            @Override
            public void run() {
                tvBalanceBar.setText(text);
            }
        });
    }
}
