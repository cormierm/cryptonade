package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.clients.PoloniexClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.clients.QuadrigacxClient;
import com.mattcormier.cryptonade.models.Pair;

import org.w3c.dom.Text;

public class OrdersFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "OrdersFragment";
    ListView lvOpenOrders;
    ListView lvOrderTransactions;
    Spinner spnPairs;
    Spinner spnClients;
    TextView tvRightHeader;
    TextView tvOrderTransactionsRightHeader;

    MainActivity mainActivity;
    View ordersView;
    Context context;

    APIClient client;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");

        mainActivity = (MainActivity) getActivity();
        ordersView = inflater.inflate(R.layout.orders_layout, container, false);

        tvRightHeader = ordersView.findViewById(R.id.tvOrdersHeaderRight);
        tvOrderTransactionsRightHeader = ordersView.findViewById(R.id.tvOrdertransactionsHeaderRight);
        spnPairs = (Spinner) mainActivity.findViewById(R.id.spnPairs);
        spnPairs.setOnItemSelectedListener(this);
        spnClients = (Spinner) mainActivity.findViewById(R.id.spnClients);
        spnClients.setOnItemSelectedListener(this);

        context = getActivity();
        lvOpenOrders = (ListView) ordersView.findViewById(R.id.lvOpenOrders);
        lvOrderTransactions = (ListView) ordersView.findViewById(R.id.lvOrdertransactions);

        UpdateOrdersFrag();
        return ordersView;
    }

    public void UpdateOrdersFrag() {
        Log.d(TAG, "UpdateOrdersFrag: ");
        tvRightHeader.setText(((Spinner)spnPairs).getSelectedItem().toString());
        tvOrderTransactionsRightHeader.setText(((Spinner)spnPairs).getSelectedItem().toString());
        APIClient client = (APIClient) spnClients.getSelectedItem();
        client.UpdateOpenOrders(context);
        client.UpdateOrderTransactions(context);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        lvOpenOrders.invalidateViews();
        lvOrderTransactions.invalidateViews();

        if (parent.getId() == spnClients.getId()) {
            mainActivity.UpdatePairsSpinner();
            ((APIClient)spnClients.getSelectedItem()).UpdateBalances(context);
        }
        UpdateOrdersFrag();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
