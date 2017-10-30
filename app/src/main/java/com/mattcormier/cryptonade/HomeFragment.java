package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class HomeFragment extends Fragment implements OnClickListener {

    Button btnHomeTradingPairs;
    Button btnHomeApiSettings;
    Button btnHomeTrade;
    Button btnHomeOrders;
    Button btnHomeBalances;
    Button btnHomeTicker;
    View homeView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        homeView = inflater.inflate(R.layout.home_layout, container, false);

        btnHomeTrade = (Button) homeView.findViewById(R.id.btnHomeTrade);
        btnHomeTradingPairs = (Button) homeView.findViewById(R.id.btnHomeTradingPairs);
        btnHomeOrders = (Button) homeView.findViewById(R.id.btnHomeOrders);
        btnHomeBalances = (Button) homeView.findViewById(R.id.btnHomeBalances);
        btnHomeTicker = (Button) homeView.findViewById(R.id.btnHomeTicker);
        btnHomeApiSettings = (Button) homeView.findViewById(R.id.btnHomeApiSettings);
        btnHomeTrade.setOnClickListener(this);
        btnHomeTradingPairs.setOnClickListener(this);
        btnHomeApiSettings.setOnClickListener(this);
        btnHomeOrders.setOnClickListener(this);
        btnHomeBalances.setOnClickListener(this);
        btnHomeTicker.setOnClickListener(this);

        return homeView;
    }

    @Override
    public void onClick(View v) {
        FragmentManager fragmentManager = getFragmentManager();
        if (v.getId() == R.id.btnHomeTrade) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new TradeFragment(), "trade")
                    .addToBackStack("trade")
                    .commit();
        }
        else if (v.getId() == R.id.btnHomeTicker) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new TickerFragment(), "ticker")
                    .addToBackStack("ticker")
                    .commit();
        }
        else if (v.getId() == R.id.btnHomeBalances) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new BalancesFragment(), "balances")
                    .addToBackStack("balances")
                    .commit();
        }
        else if (v.getId() == R.id.btnHomeTradingPairs) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new PairsFragment(), "pairs")
                    .addToBackStack("pairs")
                    .commit();
        }
        else if (v.getId() == R.id.btnHomeOrders) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new OrdersFragment(), "orders")
                    .addToBackStack("orders")
                    .commit();
        }
        else if (v.getId() == R.id.btnHomeApiSettings) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new APIFragment(), "api")
                    .addToBackStack("trade")
                    .commit();
        }
    }

}
