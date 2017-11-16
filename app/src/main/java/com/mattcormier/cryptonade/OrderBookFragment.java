package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.mattcormier.cryptonade.adapters.BookRecyclerViewAdapter;
import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.models.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class OrderBookFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = "OrderBookFragment";

    View view;
    ImageButton btnRefresh;
    BookRecyclerViewAdapter mAsksRecyclerViewAdapter;
    BookRecyclerViewAdapter mBidsRecyclerViewAdapter;
    Context mContext;
    MainActivity mainActivity;
    Spinner spnPairs;
    Spinner spnClients;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.order_book_layout, container, false);
        btnRefresh = view.findViewById(R.id.btnOrderBookRefresh);
        btnRefresh.setOnClickListener(this);

        mContext = getActivity();
        mainActivity = (MainActivity) getActivity();
        spnClients = mainActivity.findViewById(R.id.spnClients);
        spnPairs = mainActivity.findViewById(R.id.spnPairs);

        RecyclerView rvAsks = view.findViewById(R.id.rvOrderBooksAsks);
        rvAsks.setLayoutManager(new LinearLayoutManager(mContext));
        mAsksRecyclerViewAdapter = new BookRecyclerViewAdapter(mContext, new ArrayList<HashMap<String, String>>());
        rvAsks.setAdapter(mAsksRecyclerViewAdapter);

        RecyclerView rvBids = view.findViewById(R.id.rvOrderBooksBids);
        rvBids.setLayoutManager(new LinearLayoutManager(mContext));
        mBidsRecyclerViewAdapter = new BookRecyclerViewAdapter(mContext, new ArrayList<HashMap<String, String>>());
        rvBids.setAdapter(mBidsRecyclerViewAdapter);

        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.order_book_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuRefreshOrderBooks) {
            refreshBooks();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshBooks();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == btnRefresh.getId()) {
            refreshBooks();
        }
    }

    public void refreshBooks() {
        APIClient client = null;
        if (spnClients != null) {
            client = (APIClient) spnClients.getSelectedItem();
        }
        String pair = null;
        if (spnPairs != null) {
            pair = ((Pair)spnPairs.getSelectedItem()).getExchangePair();
        }


        if (client != null && pair != null) {
            client.RefreshOrderBooks(mContext, pair);
        }

    }

    public void updateAsksList(List<HashMap<String, String>> asksList) {
        mAsksRecyclerViewAdapter.loadNewData(asksList);
    }

    public void updateBidsList(List<HashMap<String, String>> bidsList) {
        mBidsRecyclerViewAdapter.loadNewData(bidsList);
    }
}
