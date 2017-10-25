package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.exchanges.PoloniexClient;
import com.mattcormier.cryptonade.exchanges.Exchange;
import com.mattcormier.cryptonade.exchanges.QuadrigacxClient;
import com.mattcormier.cryptonade.models.Pair;

import java.util.List;

public class PairsFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "PairsFragment";
    Button btnRefresh;
    Button btnUpdate;
    ListView lvPairsList;
    CryptoDB db;
    Exchange exchange;
    View pairsView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        pairsView = inflater.inflate(R.layout.pairs_layout, container, false);
        btnRefresh = (Button) pairsView.findViewById(R.id.btnTradingPairsRefresh);
        btnRefresh.setOnClickListener(this);

        btnUpdate = (Button) pairsView.findViewById(R.id.btnTradingPairsRestore);
        btnUpdate.setOnClickListener(this);

        lvPairsList = (ListView) pairsView.findViewById(R.id.lvTradingPairsList);

        db = new CryptoDB(getActivity());
        Exchange ex = db.getExchange(5);
        //polo = new PoloniexClient((int)ex.getId(), ex.getName(), ex.getAPIKey(), ex.getAPISecret(), ex.getAPIOther());
        exchange = new QuadrigacxClient((int)ex.getId(), ex.getName(), ex.getAPIKey(), ex.getAPISecret(), ex.getAPIOther());

        updatePairsListView();
        return pairsView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnRefresh.getId()) {
            updatePairsListView();
        }
        else if (v.getId() == btnUpdate.getId()) {
            exchange.RestorePairsInDB(getActivity());
        }
    }

    private void updatePairsListView() {
        Log.d(TAG, "updatePairsListView: " + exchange.getId());
        List<Pair> pairsList = db.getPairs((int)exchange.getId());

        try {
            ArrayAdapter<Pair> dataAdapter = new ArrayAdapter<>(getActivity(),
                    R.layout.activity_listview, pairsList);
            lvPairsList.setAdapter(dataAdapter);
        } catch (Exception ex) {
            Log.d("Trading Pair Activity", "Error in onCreate: " + ex.toString());
        }
    }
}
