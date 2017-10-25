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
import android.widget.TextView;

import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.PoloniexClient;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.ExchangeType;
import com.mattcormier.cryptonade.models.OpenOrder;

import java.util.List;

/**
 * Created by matt on 10/21/2017.
 */

public class APIAdapter extends ArrayAdapter {
    private static final String TAG = "APIAdapter";
    private final int layoutResource;
    private final LayoutInflater layoutInflater;
    private List<Exchange> exchangeList;
    CryptoDB db;

    public APIAdapter(@NonNull Context context, @LayoutRes int resource, List<Exchange> exchangeList) {
        super(context, resource);
        this.layoutResource = resource;
        this.layoutInflater = LayoutInflater.from(context);
        this.exchangeList = exchangeList;
        db = new CryptoDB(context);
    }

    @Override
    public int getCount() {
        return exchangeList.size();
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

        final Exchange currentExchange = exchangeList.get(position);
        ExchangeType exType = db.getExchangeTypeById((int)currentExchange.getTypeId());

        viewHolder.tvLiExchangeProfileName.setText(currentExchange.getName());
        viewHolder.tvLiExchangeType.setText(exType.getName());
        viewHolder.tvLiExchangeAPIKey.setText(currentExchange.getAPIKey());
        viewHolder.tvLiExchangeAPISecret.setText(currentExchange.getAPISecret());
//        viewHolder.ivLiOpenOrdersCancel.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                String orderId = currentOrder.getOrderNumber();
//                exchange.CancelOrder(getContext(), orderId);
//                return true;
//            }
//        });

        return convertView;
    }

    private class ViewHolder {
        final TextView tvLiExchangeProfileName;
        final TextView tvLiExchangeType;
        final TextView tvLiExchangeAPIKey;
        final TextView tvLiExchangeAPISecret;

        ViewHolder(View v) {
            this.tvLiExchangeProfileName = v.findViewById(R.id.tvLiExchangeProfileName);
            this.tvLiExchangeType = v.findViewById(R.id.tvLiExchangeType);
            this.tvLiExchangeAPIKey = v.findViewById(R.id.tvLiExchangeAPIKey);
            this.tvLiExchangeAPISecret = v.findViewById(R.id.tvLiExchangeAPISecret);
        }
    }
}

