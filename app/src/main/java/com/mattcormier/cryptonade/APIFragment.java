package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.mattcormier.cryptonade.adapters.APIAdapter;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.PoloniexClient;
import com.mattcormier.cryptonade.models.Exchange;

import java.util.ArrayList;

/**
 * Created by matt on 10/24/2017.
 */

public class APIFragment extends Fragment {
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

        db = new CryptoDB(context);
        ArrayList<Exchange> exchangeList = db.getExchanges();

        APIAdapter tickerAdapter = new APIAdapter(context, R.layout.listitem_exchange, exchangeList);
        lvAPIList.setAdapter(tickerAdapter);
        return apiView;
    }
}
