package com.mattcormier.cryptonade;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.mattcormier.cryptonade.adapters.OrderTransactionsAdapter;
import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.OrderTransaction;
import com.mattcormier.cryptonade.models.Pair;

import java.util.ArrayList;

/**
 * Filename: OrdersFragement.java
 * Description: Fragment that displays open order and transaction information.
 * Created by Matt Cormier on 10/24/2017.
 */

public class TransactionsFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "TransactionsFragment";
    ListView lvOrderTransactions;
    Spinner spnPairs;
    Spinner spnClients;
    TextView tvTransactionsRightHeader;
    MainActivity mainActivity;
    View ordersView;
    Context context;
    SwipeRefreshLayout mSwipeRefreshLayout;
    RelativeLayout rlNoOrdersFound;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: start");

        mainActivity = (MainActivity) getActivity();
        ordersView = inflater.inflate(R.layout.transactions_layout, container, false);

        rlNoOrdersFound = ordersView.findViewById(R.id.rlTransactionsNoOrdersFound);

        mSwipeRefreshLayout = ordersView.findViewById(R.id.swipeRefreshTransactions);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateTransationsFrag();
            }
        });

        tvTransactionsRightHeader = ordersView.findViewById(R.id.tvTransactionsHeaderRight);
        spnPairs = mainActivity.findViewById(R.id.spnPairs);
        spnPairs.setOnItemSelectedListener(this);
        spnClients = mainActivity.findViewById(R.id.spnClients);
        spnClients.setOnItemSelectedListener(this);

        context = getActivity();
        lvOrderTransactions = ordersView.findViewById(R.id.lvTransactions);

        updateTransationsFrag();
        return ordersView;
    }

    public void updateTransationsFrag() {
        rlNoOrdersFound.setVisibility(View.GONE);
        Log.d(TAG, "updateTransationsFrag: ");
        if (spnPairs != null) {
            Pair pair = (Pair)(spnPairs).getSelectedItem();
            if (pair != null) {
                tvTransactionsRightHeader.setText(pair.toString());
                APIClient client = (APIClient) spnClients.getSelectedItem();
                if (client != null) {
                    client.UpdateOrderTransactions(context, pair.getExchangePair());
                }
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        lvOrderTransactions.invalidateViews();

        if (parent.getId() == spnClients.getId()) {
            mainActivity.UpdatePairsSpinner();
            ((APIClient)spnClients.getSelectedItem()).UpdateBalances(context);
        }
        updateTransationsFrag();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onResume() {
        super.onResume();

        mainActivity.getSupportActionBar().setTitle(getResources().getString(R.string.transactions));
        Crypto.saveCurrentScreen(context, TAG);
        updateTransationsFrag();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void updateTransactionsList(ArrayList<OrderTransaction> orderTransactionsList) {
        if (orderTransactionsList.isEmpty()) {
            rlNoOrdersFound.setVisibility(View.VISIBLE);
        } else {
            rlNoOrdersFound.setVisibility(View.GONE);
        }
        OrderTransactionsAdapter orderTransactionsAdapter = new OrderTransactionsAdapter(
                getContext(), R.layout.listitem_order_transaction, orderTransactionsList);
        lvOrderTransactions.setAdapter(orderTransactionsAdapter);

        mSwipeRefreshLayout.setRefreshing(false);
    }
}
