package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.clients.PoloniexClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.clients.QuadrigacxClient;

public class TickerFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "TickerFragment";
    ListView lvTickers;
    CryptoDB db;
    APIClient client;
    View tickerView;
    Context context;
    long cachedClientId;
    Spinner spnClients;
    MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity)getActivity();
        spnClients = (Spinner)mainActivity.findViewById(R.id.spnClients);
        spnClients.setOnItemSelectedListener(this);
        tickerView = inflater.inflate(R.layout.ticker_layout, container, false);
        context = getActivity();
        lvTickers = (ListView) tickerView.findViewById(R.id.lvTickerList);

        db = new CryptoDB(context);
        client = (APIClient) spnClients.getSelectedItem();

        client.UpdateTickerActivity(context);
        return tickerView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        client = (APIClient)spnClients.getSelectedItem();
        if (client.getId() != cachedClientId) {
            lvTickers.invalidateViews();
            client.UpdateTickerActivity(context);
            cachedClientId = client.getId();
            ((APIClient)spnClients.getSelectedItem()).UpdateBalances(context);
        }
        mainActivity.UpdatePairsSpinner();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menuRefresh:
//                client.UpdateTickerActivity(context);
//                return true;
//            case R.id.menuSettings:
//                getFragmentManager().beginTransaction().replace(
//                        R.id.content_frame, new APISettingsFragment()).commit();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
}
