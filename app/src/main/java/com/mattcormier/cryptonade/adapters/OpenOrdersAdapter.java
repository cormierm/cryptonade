package com.mattcormier.cryptonade.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mattcormier.cryptonade.MainActivity;
import com.mattcormier.cryptonade.OpenOrdersService;
import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.AlertOrder;
import com.mattcormier.cryptonade.models.OpenOrder;

import java.util.List;

/**
 * Filename: OpenOrdersAdapter.java
 * Description: Adapter for open orders ListView
 * Created by Matt Cormier on 10/21/2017.
 */

public class OpenOrdersAdapter extends ArrayAdapter {
    private static final String TAG = "OpenOrdersAdapter";
    private final int layoutResource;
    private final LayoutInflater layoutInflater;
    private List<OpenOrder> openOrders;
    APIClient client;

    public OpenOrdersAdapter(@NonNull Context context, @LayoutRes int resource, List<OpenOrder> openOrders) {
        super(context, resource);
        this.layoutResource = resource;
        this.layoutInflater = LayoutInflater.from(context);
        this.openOrders = openOrders;

        client = (APIClient) ((Spinner)((MainActivity)getContext()).findViewById(R.id.spnClients)).getSelectedItem();
    }

    @Override
    public int getCount() {
        return openOrders.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(layoutResource, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final OpenOrder currentOrder = openOrders.get(position);

        viewHolder.ivLiOpenOrdersAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CryptoDB db = new CryptoDB(getContext());
                AlertOrder alertOrder = new AlertOrder(currentOrder.getOrderNumber(), currentOrder.getExchangeId(), currentOrder.getTradePair());
                db.deleteAlertOrder(alertOrder.getOrderId());
                db.insertAlertOrder(alertOrder);
                APIClient client = Crypto.getAPIClient(db.getExchange(alertOrder.getExchangeId()));
                client.CheckOpenOrder(getContext(), alertOrder.getOrderId(), alertOrder.getSymbol());
                Toast.makeText(getContext(), "Added alert for Order number: " + currentOrder.getOrderNumber(), Toast.LENGTH_SHORT).show();
                getContext().startService(new Intent(getContext(), OpenOrdersService.class));
            }
        });

        viewHolder.tvliOpenOrdersId.setText(currentOrder.getOrderNumber());
        viewHolder.tvliOpenOrdersType.setText(currentOrder.getType());
        if (currentOrder.getType().equalsIgnoreCase("sell")) {
            viewHolder.tvliOpenOrdersType.setTextColor(parent.getResources().getColor(R.color.red));
        } else {
            viewHolder.tvliOpenOrdersType.setTextColor(parent.getResources().getColor(R.color.green));
        }
        viewHolder.tvliOpenOrdersAmount.setText(currentOrder.getRemainingAmount());
        viewHolder.tvliOpenOrdersRate.setText(currentOrder.getRate());
        viewHolder.tvliOpenOrdersTimestamp.setText(currentOrder.getDate());
        viewHolder.ivLiOpenOrdersCancel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String orderId = currentOrder.getOrderNumber();
                client.CancelOrder(getContext(), orderId);
                return true;
            }
        });

        return convertView;
    }

    private class ViewHolder {
        final TextView tvliOpenOrdersId;
        final TextView tvliOpenOrdersType;
        final TextView tvliOpenOrdersAmount;
        final TextView tvliOpenOrdersRate;
        final TextView tvliOpenOrdersTimestamp;
        final ImageView ivLiOpenOrdersCancel;
        final ImageView ivLiOpenOrdersAlert;

        ViewHolder(View v) {
            this.tvliOpenOrdersId = (TextView) v.findViewById(R.id.tvliOpenOrdersId);
            this.tvliOpenOrdersType = (TextView) v.findViewById(R.id.tvliOpenOrdersType);
            this.tvliOpenOrdersAmount = (TextView) v.findViewById(R.id.tvliOpenOrdersAmount);
            this.tvliOpenOrdersRate = (TextView) v.findViewById(R.id.tvLiOpenOrdersRate);
            this.tvliOpenOrdersTimestamp = (TextView) v.findViewById(R.id.tvliOpenOrdersTimestamp);
            this.ivLiOpenOrdersCancel = (ImageView) v.findViewById(R.id.ivLiOpenOrdersCancel);
            this.ivLiOpenOrdersAlert = (ImageView) v.findViewById(R.id.ivLiOpenOrdersAlert);
        }
    }
}

