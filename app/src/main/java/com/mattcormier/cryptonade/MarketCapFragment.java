package com.mattcormier.cryptonade;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mattcormier.cryptonade.adapters.MarketCapRecyclerViewAdapter;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.Coin;

import java.util.ArrayList;

/**
 * Filename: MarketCapFragment.java
 * Description: Fragment that displays ticker information for market summary.
 * Created by Matt Cormier on 12/22/2017.
 */

public class MarketCapFragment extends Fragment {
    private static final String TAG = "MarketCapFragment";
    MainActivity mainActivity;
    MarketCapRecyclerViewAdapter mMarketCapRecyclerViewAdapter;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: start");
        View view = inflater.inflate(R.layout.market_cap_layout, container, false);

        mainActivity = (MainActivity) getActivity();

        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshMarketCap);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateMarketCapData();
            }
        });

        RecyclerView rvCoinList = view.findViewById(R.id.rvMarketCapList);
        rvCoinList.setLayoutManager(new LinearLayoutManager(getContext()));
        mMarketCapRecyclerViewAdapter = new MarketCapRecyclerViewAdapter(getContext(), new ArrayList<Coin>());
        rvCoinList.setAdapter(mMarketCapRecyclerViewAdapter);

        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(
                rvCoinList.getContext(), DividerItemDecoration.VERTICAL);
        rvCoinList.addItemDecoration(mDividerItemDecoration);

        updateMarketCapData();

        return view;
    }

    public void updateMarketCapData() {
        GetMarketCapData getMarketCapData = new GetMarketCapData(this);
        getMarketCapData.execute();
    }

    public void onDownloadComplete(ArrayList<Coin> coinList) {
        mMarketCapRecyclerViewAdapter.loadNewData(coinList);
        mSwipeRefreshLayout.setRefreshing(false);
    }


    @Override
    public void onResume() {
        super.onResume();

        mainActivity.getSupportActionBar().setTitle(getResources().getString(R.string.coin_market_cap));
        Crypto.saveCurrentScreen(getContext(), TAG);
    }
}