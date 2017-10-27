package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mattcormier.cryptonade.adapters.APIAdapter;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.clients.PoloniexClient;
import com.mattcormier.cryptonade.models.Exchange;

import java.util.ArrayList;

/**
 * Created by matt on 10/24/2017.
 */

public class APIFragment extends Fragment {
    private static final String TAG = "APIFragment";
    ListView lvAPIList;
    CryptoDB db;
    PoloniexClient exchange;
    View apiView;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        apiView = inflater.inflate(R.layout.api_layout, container, false);
        context = getActivity();
        lvAPIList = (ListView) apiView.findViewById(R.id.lvAPIList);

        FloatingActionButton fabAdd = (FloatingActionButton) apiView.findViewById(R.id.fabAPIAdd);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                APISettingsFragment frag = new APISettingsFragment();
                frag.setExchangeId(0);
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, frag)
                        .addToBackStack("api_settings")
                        .commit();
            }
        });
        lvAPIList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick: ");
                Exchange ex = (Exchange) parent.getAdapter().getItem(position);
                Log.d(TAG, "onItemClick: " + ex.getId());
                APISettingsFragment frag = new APISettingsFragment();
                frag.setExchangeId((int)ex.getId());
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, frag)
                        .addToBackStack("api_settings")
                        .commit();
            }
        });

        db = new CryptoDB(context);
        ArrayList<Exchange> exchangeList = db.getExchanges();

        APIAdapter tickerAdapter = new APIAdapter(context, R.layout.listitem_exchange, exchangeList);
        lvAPIList.setAdapter(tickerAdapter);
        return apiView;
    }
}
