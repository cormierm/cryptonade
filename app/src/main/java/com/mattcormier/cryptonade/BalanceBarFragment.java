package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mattcormier.cryptonade.clients.APIClient;

/**
 * Created by matt on 10/24/2017.
 */

public class BalanceBarFragment extends Fragment {
    private static final String TAG = "BalanceBarFragment";
    TextView tvBalances;
    View balanceView;
    APIClient client;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        balanceView = inflater.inflate(R.layout.balance_bar_layout, container, false);
        context = getActivity();
        tvBalances = balanceView.findViewById(R.id.tvBalanceBar);
        Log.d(TAG, "onCreateView: client: " + client);

        UpdateBalanceBar();

        return balanceView;
    }

    @Override
    public void onResume() {
        UpdateBalanceBar();
        super.onResume();
    }

    public void UpdateBalanceBar() {
        if (client != null) {
            Log.d(TAG, "UpdateBalanceBar: client: " + client);
            client.UpdateBalanceBar(context);
        } else {
            Log.d(TAG, "UpdateBalanceBar: client is null");
            tvBalances.setText("No client selected");
        }
    }

    public void setClient(APIClient client) {
        this.client = client;
    }
}
