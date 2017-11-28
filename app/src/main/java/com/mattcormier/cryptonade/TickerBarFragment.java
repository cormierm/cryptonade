package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.models.Pair;

import java.util.HashMap;
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
    Spinner spnPairs;
    APIClient client;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: start");
        spnClients = ((MainActivity)getActivity()).findViewById(R.id.spnClients);
        spnPairs = ((MainActivity)getActivity()).findViewById(R.id.spnPairs);
        view = inflater.inflate(R.layout.balance_bar_layout, container, false);
        context = getActivity();
        tvTickerBar = view.findViewById(R.id.tvBalanceBar);

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_ticker_info_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuRefreshTickerInfo) {
            refreshTickerBar();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                        String output = "LAST: " + tickerInfo.get("Last") + "    " +
                                "ASK: " + tickerInfo.get("Ask") + "    " +
                                "BID: " + tickerInfo.get("Bid");
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

    public void refreshTickerBar(){
        String pair = ((Pair)(spnPairs.getSelectedItem())).getExchangePair();
        ((APIClient)spnClients.getSelectedItem()).UpdateTickerInfo(context, pair);
    }
}
