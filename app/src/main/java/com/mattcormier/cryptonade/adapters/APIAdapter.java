package com.mattcormier.cryptonade.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.ExchangeType;

import java.util.List;

/**
 * Filename: APIAdapter.java
 * Description: Adapter for API Key ListView
 * Created by Matt Cormier on 10/21/2017.
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

    @Override
    public Exchange getItem(int position) {
        return exchangeList.get(position);
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
        final long exchangeId = currentExchange.getId();
        ExchangeType exType = db.getExchangeTypeById((int)currentExchange.getTypeId());

        viewHolder.tvLiExchangeProfileName.setText(currentExchange.getName());
        viewHolder.tvLiExchangeType.setText(exType.getName());
        viewHolder.tvLiExchangeAPIKey.setText(currentExchange.getAPIKey());
        viewHolder.tvLiExchangeAPISecret.setText(currentExchange.getAPISecret());
        if (exType.getApiOther().isEmpty()) {
            viewHolder.lblLiExchangeAPIOther.setVisibility(View.GONE);
            viewHolder.tvLiExchangeAPIOther.setVisibility(View.GONE);
        } else {
            viewHolder.lblLiExchangeAPIOther.setVisibility(View.VISIBLE);
            viewHolder.tvLiExchangeAPIOther.setVisibility(View.VISIBLE);
            viewHolder.lblLiExchangeAPIOther.setText(exType.getApiOther());
            viewHolder.tvLiExchangeAPIOther.setText(currentExchange.getAPIOther());
        }
        Log.d(TAG, "getView: active: " + currentExchange.getActive());
        if(currentExchange.getActive() == 0) {
            viewHolder.clLiApiKey.setBackgroundColor(getContext().getResources().getColor(R.color.colorLightGray));
            viewHolder.tvLiExchangeDisabled.setVisibility(View.VISIBLE);
        } else {
            viewHolder.clLiApiKey.setBackgroundColor(getContext().getResources().getColor(R.color.white));
            viewHolder.tvLiExchangeDisabled.setVisibility(View.GONE);
        }

        return convertView;
    }

    private class ViewHolder {
        final TextView tvLiExchangeProfileName;
        final TextView tvLiExchangeType;
        final TextView tvLiExchangeAPIKey;
        final TextView tvLiExchangeAPISecret;
        final TextView lblLiExchangeAPIOther;
        final TextView tvLiExchangeAPIOther;
        final TextView tvLiExchangeDisabled;
        final ConstraintLayout clLiApiKey;

        ViewHolder(View v) {
            this.tvLiExchangeProfileName = v.findViewById(R.id.tvLiExchangeProfileName);
            this.tvLiExchangeType = v.findViewById(R.id.tvLiExchangeType);
            this.tvLiExchangeAPIKey = v.findViewById(R.id.tvLiExchangeAPIKey);
            this.tvLiExchangeAPISecret = v.findViewById(R.id.tvLiExchangeAPISecret);
            this.lblLiExchangeAPIOther = v.findViewById(R.id.lblLiExchangeAPIOther);
            this.tvLiExchangeAPIOther = v.findViewById(R.id.tvLiExchangeAPIOther);
            this.tvLiExchangeDisabled = v.findViewById(R.id.tvLiExchangeDisabled);
            this.clLiApiKey = v.findViewById(R.id.clListItemAPIKey);
        }
    }
}

