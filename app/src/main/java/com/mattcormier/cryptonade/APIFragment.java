package com.mattcormier.cryptonade;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
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
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.Exchange;

import java.util.ArrayList;

/**
 * Filename: APIFragment.java
 * Description: Fragment for API Keys
 * Created by Matt Cormier on 10/24/2017.
 */

public class APIFragment extends Fragment {
    private static final String TAG = "APIFragment";
    ListView lvAPIList;
    CryptoDB db;
    View view;
    Context context;
    MainActivity mainActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: start");
        mainActivity = (MainActivity)getActivity();
        context = getActivity();
        view = inflater.inflate(R.layout.api_layout, container, false);

        lvAPIList = view.findViewById(R.id.lvAPIList);

        FloatingActionButton fabAddAPIKey = view.findViewById(R.id.fabAPIAdd);
        fabAddAPIKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, APISettingsActivity.class);
                intent.putExtra("ExchangeId", 0);
                startActivity(intent);
            }
        });

        lvAPIList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Exchange ex = (Exchange) parent.getAdapter().getItem(position);
                Log.d(TAG, "onItemClick: " + ex.getId());

                Intent intent = new Intent(context, APISettingsActivity.class);
                intent.putExtra("ExchangeId", (int)ex.getId());
                startActivity(intent);
            }
        });

        db = new CryptoDB(context);


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mainActivity.getSupportActionBar().setTitle(getResources().getString(R.string.api_keys));
        Crypto.saveCurrentScreen(context, TAG);

        ArrayList<Exchange> exchangeList = db.getExchanges();
        APIAdapter apiAdapter = new APIAdapter(context, R.layout.listitem_exchange, exchangeList);
        lvAPIList.setAdapter(apiAdapter);
    }
}
