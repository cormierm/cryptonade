package com.mattcormier.cryptonade.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.models.OrderTransaction;

import java.util.List;

/**
 * Filename: OrderTransactionsAdapter.java
 * Description: Adapter for order transactions ListView
 * Created by Matt Cormier on 10/21/2017.
 */

public class OrderTransactionsAdapter extends ArrayAdapter {
    private static final String TAG = "OrderTransactionsAdapter";
    private final int layoutResource;
    private final LayoutInflater layoutInflater;
    private List<OrderTransaction> orderTransactionsList;

    public OrderTransactionsAdapter(@NonNull Context context, @LayoutRes int resource, List<OrderTransaction> orderTransactionsList) {
        super(context, resource);
        this.layoutResource = resource;
        this.layoutInflater = LayoutInflater.from(context);
        this.orderTransactionsList = orderTransactionsList;
    }

    @Override
    public int getCount() {
        return orderTransactionsList.size();
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

        final OrderTransaction currentOrder = orderTransactionsList.get(position);

        viewHolder.tvLiOrderTransactionTimestamp.setText(currentOrder.getTimestamp());
        viewHolder.tvLiOrderTransactionType.setText(currentOrder.getType());
        if (currentOrder.getType().equalsIgnoreCase("sell")) {
            viewHolder.tvLiOrderTransactionType.setTextColor(parent.getResources().getColor(R.color.red));
        } else {
            viewHolder.tvLiOrderTransactionType.setTextColor(parent.getResources().getColor(R.color.green));
        }
        viewHolder.tvLiOrderTransactionAmount.setText(currentOrder.getAmount());
        viewHolder.tvLiOrderTransactionRate.setText(currentOrder.getRate());
        viewHolder.tvLiOrderTransactionFee.setText(currentOrder.getFee());

        return convertView;
    }

    private class ViewHolder {
        final TextView tvLiOrderTransactionTimestamp;
        final TextView tvLiOrderTransactionType;
        final TextView tvLiOrderTransactionAmount;
        final TextView tvLiOrderTransactionRate;
        final TextView tvLiOrderTransactionFee;

        ViewHolder(View v) {
            this.tvLiOrderTransactionTimestamp = v.findViewById(R.id.tvLiOrderTransactionTimestamp);
            this.tvLiOrderTransactionType = v.findViewById(R.id.tvLiOrderTransactionType);
            this.tvLiOrderTransactionAmount = v.findViewById(R.id.tvLiOrderTransactionAmount);
            this.tvLiOrderTransactionRate = v.findViewById(R.id.tvLiOrderTransactionRate);
            this.tvLiOrderTransactionFee = v.findViewById(R.id.tvLiOrderTransactionFee);
        }
    }
}

