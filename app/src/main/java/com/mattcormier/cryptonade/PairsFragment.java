package com.mattcormier.cryptonade;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.Pair;

import java.util.List;

/**
 * Filename: PairsFragment.java
 * Description: Fragment that displays trading pairs for exchange.
 * Created by Matt Cormier on 10/24/2017.
 */

public class PairsFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "PairsFragment";
    Button btnUpdate;
    ListView lvPairsList;
    CryptoDB db;
    View pairsView;
    MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: start");
        mainActivity = (MainActivity) getActivity();
        pairsView = inflater.inflate(R.layout.pairs_layout, container, false);

        btnUpdate = pairsView.findViewById(R.id.btnTradingPairsRestore);
        btnUpdate.setOnClickListener(this);

        lvPairsList = pairsView.findViewById(R.id.lvTradingPairsList);

        db = new CryptoDB(getActivity());

        updatePairsListView();
        return pairsView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnUpdate.getId()) {
            ((APIClient) mainActivity.spnClients.getSelectedItem()).RestorePairsInDB(getActivity());
        }
    }

    public void updatePairsListView() {
        APIClient client = (APIClient) mainActivity.spnClients.getSelectedItem();
        Log.d(TAG, "updatePairsListView: " + client.getId());
        List<Pair> pairsList = db.getPairs((int)client.getId());

        try {
            ArrayAdapter<Pair> dataAdapter = new ArrayAdapter<>(getActivity(),
                    R.layout.listview_pairs, pairsList);
            lvPairsList.setAdapter(dataAdapter);
        } catch (Exception ex) {
            Log.d("Trading Pair Activity", "Error in onCreate: " + ex.toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.getSupportActionBar().setTitle(getResources().getString(R.string.exchange_pairs));
        Crypto.saveCurrentScreen(getActivity(), TAG);
    }
}
