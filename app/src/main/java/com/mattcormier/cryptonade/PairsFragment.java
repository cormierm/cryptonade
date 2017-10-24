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
import android.widget.Spinner;

import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.PoloniexClient;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.Pair;

import java.util.List;

public class PairsFragment extends Fragment implements View.OnClickListener {
    Button btnRefresh;
    Button btnUpdate;
    ListView lvPairsList;
    CryptoDB db;
    PoloniexClient polo;
    View pairsView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        pairsView = inflater.inflate(R.layout.pairs_layout, container, false);
        btnRefresh = (Button) pairsView.findViewById(R.id.btnTradingPairsRefresh);
        btnRefresh.setOnClickListener(this);

        btnUpdate = (Button) pairsView.findViewById(R.id.btnTradingPairsUpdate);
        btnUpdate.setOnClickListener(this);

        lvPairsList = (ListView) pairsView.findViewById(R.id.lvTradingPairsList);

        db = new CryptoDB(getActivity());
        Exchange ex = db.getExchange(1);
        polo = new PoloniexClient((int)ex.getId(), ex.getName(), ex.getAPIKey(), ex.getAPISecret(), ex.getAPIOther());

        updatePairsListView();
        return pairsView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnRefresh.getId()) {
            updatePairsListView();
        }
        else if (v.getId() == btnUpdate.getId()) {
            polo.UpdateTradingPairs(getActivity(), null);
        }
    }

    private void updatePairsListView() {
        List<Pair> pairsList = db.getPairs(1);

        try {
            ArrayAdapter<Pair> dataAdapter = new ArrayAdapter<>(getActivity(),
                    R.layout.activity_listview, pairsList);
            lvPairsList.setAdapter(dataAdapter);
        } catch (Exception ex) {
            Log.d("Trading Pair Activity", "Error in onCreate: " + ex.toString());
        }
    }
}
