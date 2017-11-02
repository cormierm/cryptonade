package com.mattcormier.cryptonade.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mattcormier.cryptonade.MainActivity;
import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.models.OpenOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by matt on 10/30/2017.
 */

public class BalancesAdapter extends ArrayAdapter {
    private static final String TAG = "BalancesAdapter";
    private final int layoutResource;
    private final LayoutInflater layoutInflater;
    private List<APIClient> clientsList;

    public BalancesAdapter(@NonNull Context context, @LayoutRes int resource, List<APIClient> clientsList) {
        super(context, resource);
        this.layoutResource = resource;
        this.layoutInflater = LayoutInflater.from(context);
        this.clientsList = clientsList;
    }

    @Override
    public int getCount() {
        return clientsList.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Log.d(TAG, "getView: ");
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(layoutResource, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final APIClient currentClient = clientsList.get(position);

        viewHolder.tvHeader.setText(currentClient.getName());
        String balanceOutput = "";
        HashMap<String, Double> availBals = currentClient.getBalances();
        if (availBals != null) {
            for(Map.Entry<String, Double> bal: availBals.entrySet()) {
                if (balanceOutput != "") {
                    balanceOutput += "\n";
                }
                balanceOutput += bal.getKey() + ": " + String.format("%.8f", bal.getValue());
            }
        } else {
            balanceOutput = "No Balance Information.";
        }
        viewHolder.tvBalances.setText(balanceOutput);
        viewHolder.btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentClient.UpdateBalances(viewHolder.context);
            }
        });

        return convertView;
    }

    private class ViewHolder {
        final TextView tvHeader;
        final TextView tvBalances;
        final ImageButton btnRefresh;
        final Context context;

        ViewHolder(View v) {
            this.tvHeader = v.findViewById(R.id.tvLiBalancesHeader);
            this.tvBalances = v.findViewById(R.id.tvLiBalances);
            this.btnRefresh = v.findViewById(R.id.btnLiBalancesRefresh);
            this.context = getContext();
        }
    }
}

