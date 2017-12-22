package com.mattcormier.cryptonade;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import com.mattcormier.cryptonade.adapters.OpenOrdersAdapter;
import com.mattcormier.cryptonade.adapters.OrderTransactionsAdapter;
import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.OpenOrder;
import com.mattcormier.cryptonade.models.Pair;

import java.util.ArrayList;

/**
 * Filename: OrdersFragement.java
 * Description: Fragment that displays open order and transaction information.
 * Created by Matt Cormier on 10/24/2017.
 */

public class OpenOrdersFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "OpenOrdersFragment";
    ListView lvOpenOrders;
    Spinner spnPairs;
    Spinner spnClients;
    TextView tvRightHeader;
    MainActivity mainActivity;
    View view;
    Context context;
    SwipeRefreshLayout mSwipeRefreshLayout;
    RelativeLayout rlNoOrdersFound;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: start");

        mainActivity = (MainActivity) getActivity();
        view = inflater.inflate(R.layout.open_orders_layout, container, false);

        rlNoOrdersFound = view.findViewById(R.id.rlOpenOrdersNoOrdersFound);

        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshOpenOrders);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateOpenOrdersFrag();
            }
        });

        tvRightHeader = view.findViewById(R.id.tvOpenOrdersHeaderRight);
        spnPairs = mainActivity.findViewById(R.id.spnPairs);
        spnPairs.setOnItemSelectedListener(this);
        spnClients = mainActivity.findViewById(R.id.spnClients);
        spnClients.setOnItemSelectedListener(this);

        context = getActivity();
        lvOpenOrders = view.findViewById(R.id.lvOpenOrders);

        updateOpenOrdersFrag();
        return view;
    }

    public void updateOpenOrdersFrag() {
        Log.d(TAG, "updateOpenOrdersFrag: ");
        if (spnPairs != null) {
            Pair pair = (Pair)(spnPairs).getSelectedItem();
            if (pair != null) {
                tvRightHeader.setText(pair.toString());
                APIClient client = (APIClient) spnClients.getSelectedItem();
                if (client != null) {
                    client.UpdateOpenOrders(context);
                }
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        lvOpenOrders.invalidateViews();

        if (parent.getId() == spnClients.getId()) {
            mainActivity.UpdatePairsSpinner();
            ((APIClient)spnClients.getSelectedItem()).UpdateBalances(context);
        }
        updateOpenOrdersFrag();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onResume() {
        super.onResume();

        mainActivity.getSupportActionBar().setTitle(getResources().getString(R.string.open_orders));
        updateOpenOrdersFrag();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void updateOpenOrdersList(ArrayList<OpenOrder> openOrdersList){
        if (openOrdersList.isEmpty()) {
            rlNoOrdersFound.setVisibility(View.VISIBLE);
        } else {
            rlNoOrdersFound.setVisibility(View.GONE);
        }
        OpenOrdersAdapter openOrdersAdapter = new OpenOrdersAdapter(
                getContext(), R.layout.listitem_openorder, openOrdersList);
        lvOpenOrders.setAdapter(openOrdersAdapter);

        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }
}
