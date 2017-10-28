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

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.clients.PoloniexClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.clients.QuadrigacxClient;
import com.mattcormier.cryptonade.models.Pair;

import java.util.List;

public class PairsFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "PairsFragment";
    Button btnRefresh;
    Button btnUpdate;
    ListView lvPairsList;
    CryptoDB db;
    View pairsView;
    MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        pairsView = inflater.inflate(R.layout.pairs_layout, container, false);
        btnRefresh = (Button) pairsView.findViewById(R.id.btnTradingPairsRefresh);
        btnRefresh.setOnClickListener(this);

        btnUpdate = (Button) pairsView.findViewById(R.id.btnTradingPairsRestore);
        btnUpdate.setOnClickListener(this);

        lvPairsList = (ListView) pairsView.findViewById(R.id.lvTradingPairsList);

        db = new CryptoDB(getActivity());

        updatePairsListView();
        return pairsView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnRefresh.getId()) {
            updatePairsListView();
        }
        else if (v.getId() == btnUpdate.getId()) {
            ((APIClient) mainActivity.spnClients.getSelectedItem()).RestorePairsInDB(getActivity());
        }
    }

    private void updatePairsListView() {
        APIClient client = (APIClient) mainActivity.spnClients.getSelectedItem();
        Log.d(TAG, "updatePairsListView: " + client.getId());
        List<Pair> pairsList = db.getPairs((int)client.getId());

        try {
            ArrayAdapter<Pair> dataAdapter = new ArrayAdapter<>(getActivity(),
                    R.layout.activity_listview, pairsList);
            lvPairsList.setAdapter(dataAdapter);
        } catch (Exception ex) {
            Log.d("Trading Pair Activity", "Error in onCreate: " + ex.toString());
        }
    }
}
