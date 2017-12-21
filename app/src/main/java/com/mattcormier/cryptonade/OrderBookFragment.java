package com.mattcormier.cryptonade;

import android.support.v4.app.Fragment;
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

/**
 * Filename: OrderBookFragment.java
 * Description: Fragment that displays order book information.
 * Created by Matt Cormier on 10/24/2017.
 */

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
        Log.d(TAG, "onCreateView: start");
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
        if (spnClients != null && spnPairs != null) {
            APIClient client = (APIClient)spnClients.getSelectedItem();
            Pair pair = (Pair)spnPairs.getSelectedItem();

            if (client != null && pair != null) {
                client.RefreshOrderBooks(mContext, pair.getExchangePair());
            }
        }
    }

    public void updateAsksList(List<HashMap<String, String>> asksList) {
        mAsksRecyclerViewAdapter.loadNewData(asksList);
    }

    public void updateBidsList(List<HashMap<String, String>> bidsList) {
        mBidsRecyclerViewAdapter.loadNewData(bidsList);
    }
}
