package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;

/**
 * Filename: TickerFragment.java
 * Description: Fragment that displays ticker information for market summary.
 * Created by Matt Cormier on 10/24/2017.
 */

public class TickerFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "TickerFragment";
    ListView lvTickers;
    CryptoDB db;
    APIClient client;
    View tickerView;
    Context context;
    long currentClientId;
    Spinner spnClients;
    MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: start");
        mainActivity = (MainActivity) getActivity();
        spnClients = mainActivity.findViewById(R.id.spnClients);
        spnClients.setOnItemSelectedListener(this);
        tickerView = inflater.inflate(R.layout.ticker_layout, container, false);
        context = getActivity();
        lvTickers = tickerView.findViewById(R.id.lvTickerList);

        db = new CryptoDB(context);

        client = (APIClient) spnClients.getSelectedItem();
        client.UpdateTickerActivity(context);

        return tickerView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        client = (APIClient) spnClients.getSelectedItem();
        if (client.getId() != currentClientId) {
            lvTickers.invalidateViews();
            client.UpdateTickerActivity(context);
            currentClientId = client.getId();
            ((APIClient) spnClients.getSelectedItem()).UpdateBalances(context);
        }
        mainActivity.UpdatePairsSpinner();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onResume() {
        super.onResume();

        mainActivity.getSupportActionBar().setTitle(getResources().getString(R.string.market_summary));
        Crypto.saveCurrentScreen(context, TAG);
    }
}