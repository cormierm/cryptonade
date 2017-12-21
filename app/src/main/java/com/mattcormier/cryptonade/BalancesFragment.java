package com.mattcormier.cryptonade;

import android.support.v4.app.Fragment;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mattcormier.cryptonade.adapters.BalancesAdapter;
import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.lib.Crypto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Filename: BalancesFragment.java
 * Description: Fragment for displaying balances on exchanges
 * Created by Matt Cormier on 10/30/2017.
 */

public class BalancesFragment extends Fragment {
    private static final String TAG = "BalancesFragment";
    View view;
    ListView lvBalances;
    Spinner spnClients;
    TextView tvTotals;
    Context context;
    MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: start.");

        spnClients = ((MainActivity) getActivity()).findViewById(R.id.spnClients);
        view = inflater.inflate(R.layout.balances_layout, container, false);
        lvBalances = view.findViewById(R.id.lvBalancesList);
        tvTotals = view.findViewById(R.id.tvBalancesTotals);

        context = getActivity();
        mainActivity = (MainActivity) getActivity();

        refreshAllClientBalances();

        updateBalancesList();
        updateTotals();
        setHasOptionsMenu(true);

        Log.d(TAG, "onCreateView: done.");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.getSupportActionBar().setTitle(getResources().getString(R.string.balances));
        Crypto.saveCurrentScreen(context, TAG);
        refreshBalances();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuRefresh) {
            refreshAllClientBalances();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshBalances() {
        updateBalancesList();
        updateTotals();
    }

    public void updateBalancesList() {
        Log.d(TAG, "updateBalancesList: start");
        ArrayList<APIClient> clientList = mainActivity.apiClientArrayList;
        if (clientList != null) {
            BalancesAdapter balancesAdapter = new BalancesAdapter(context, R.layout.listitem_balances, clientList);
            updateBalancesListView(balancesAdapter);
        }
    }

    public void updateBalancesListView(final BalancesAdapter balancesAdapter) {
        Log.d(TAG, "updateBalancesListView: ");
        lvBalances.post(new Runnable() {
            @Override
            public void run() {
                lvBalances.setAdapter(balancesAdapter);
            }
        });
    }

    public void updateTotalsTextView(final String text) {
        Log.d(TAG, "updateBalancesList: ");
        lvBalances.post(new Runnable() {
            @Override
            public void run() {
                tvTotals.setText(text);
            }
        });
    }

    public void updateTotals() {
        HashMap<String, Double> totals = new HashMap<>();
        for(APIClient client: mainActivity.apiClientArrayList) {
            HashMap<String, Double> balances = client.getBalances();
            if (balances != null) {
                for(Map.Entry<String, Double> bal: balances.entrySet()) {
                    String currency = bal.getKey();
                    Double balance = bal.getValue();
                    Double totalBalance = totals.get(currency);
                    if (totalBalance == null) {
                        totalBalance = balance;
                    }
                    else {
                        totalBalance = totalBalance + balance;
                    }
                    totals.put(currency, totalBalance);
                }
            }
        }
        String totalOutput = "";
        if (totals != null) {
            for(Map.Entry<String, Double> total: totals.entrySet()) {
                if (totalOutput != "") {
                    totalOutput += "\n";
                }
                totalOutput += total.getKey() + ": " + String.format("%.8f", total.getValue());
            }
        }
        if (totalOutput.equals("")) {
            totalOutput = "No Balance Information.";
        }
        updateTotalsTextView(totalOutput);
    }

    public void refreshAllClientBalances() {
        for(APIClient client: mainActivity.apiClientArrayList) {
            client.UpdateBalances(context);
        }
    }
}
