package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.exchanges.PoloniexClient;
import com.mattcormier.cryptonade.exchanges.Exchange;

/**
 * Created by matt on 10/24/2017.
 */

public class BalanceBarFragment extends Fragment {
    TextView tvBalances;
    View balanceView;
    CryptoDB db;
    PoloniexClient exchange;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        balanceView = inflater.inflate(R.layout.balance_bar_layout, container, false);
        context = getActivity();
        tvBalances = balanceView.findViewById(R.id.tvBalanceBar);
        db = new CryptoDB(context);
        Exchange ex = db.getExchange(1);
        exchange = new PoloniexClient((int)ex.getId(), ex.getName(), ex.getAPIKey(), ex.getAPISecret(), ex.getAPIOther());
        UpdateBalanceBar();

        return balanceView;
    }

    @Override
    public void onResume() {
        UpdateBalanceBar();
        super.onResume();
    }

    public void UpdateBalanceBar() {
        exchange.UpdateBalanceBar(context);
    }
}
