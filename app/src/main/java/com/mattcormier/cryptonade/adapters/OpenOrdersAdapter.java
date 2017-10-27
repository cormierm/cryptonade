package com.mattcormier.cryptonade.adapters;

import android.content.Context;
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

import com.mattcormier.cryptonade.MainActivity;
import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.clients.PoloniexClient;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.OpenOrder;

import java.util.List;

/**
 * Created by matt on 10/21/2017.
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

        viewHolder.tvliOpenOrdersId.setText(currentOrder.getOrderNumber());
        viewHolder.tvliOpenOrdersPair.setText(currentOrder.getTradePair());
        viewHolder.tvliOpenOrdersType.setText(currentOrder.getType());
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
        final TextView tvliOpenOrdersPair;
        final TextView tvliOpenOrdersType;
        final TextView tvliOpenOrdersAmount;
        final TextView tvliOpenOrdersRate;
        final TextView tvliOpenOrdersTimestamp;
        final ImageView ivLiOpenOrdersCancel;

        ViewHolder(View v) {
            this.tvliOpenOrdersId = (TextView) v.findViewById(R.id.tvliOpenOrdersId);
            this.tvliOpenOrdersPair = (TextView) v.findViewById(R.id.tvLiOpenOrdersPair);
            this.tvliOpenOrdersType = (TextView) v.findViewById(R.id.tvliOpenOrdersType);
            this.tvliOpenOrdersAmount = (TextView) v.findViewById(R.id.tvliOpenOrdersAmount);
            this.tvliOpenOrdersRate = (TextView) v.findViewById(R.id.tvLiOpenOrdersRate);
            this.tvliOpenOrdersTimestamp = (TextView) v.findViewById(R.id.tvliOpenOrdersTimestamp);
            this.ivLiOpenOrdersCancel = (ImageView) v.findViewById(R.id.ivLiOpenOrdersCancel);
        }
    }
}

