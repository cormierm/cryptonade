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
import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.clients.PoloniexClient;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.Ticker;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by matt on 10/21/2017.
 */

public class TickerAdapter extends ArrayAdapter {
    private static final String TAG = "TickerAdapter";
    private final int layoutResource;
    private final LayoutInflater layoutInflater;
    private List<Ticker> tickerList;

    public TickerAdapter(@NonNull Context context, @LayoutRes int resource, List<Ticker> tickerList) {
        super(context, resource);
        this.layoutResource = resource;
        this.layoutInflater = LayoutInflater.from(context);
        // sort tickerList by pair
        Collections.sort(tickerList, new Comparator<Ticker>(){
            public int compare(Ticker o1, Ticker o2) {
                return o1.getPair().compareTo(o2.getPair());
            }
        });
        this.tickerList = tickerList;
    }

    @Override
    public int getCount() {
        return tickerList.size();
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

        final Ticker currentTicker = tickerList.get(position);

        viewHolder.tvLiTickerPair.setText(currentTicker.getPair());
        viewHolder.tvLiTickerLast.setText(currentTicker.getLast());
        viewHolder.tvLiTickerVolume.setText(currentTicker.getVolume());
        viewHolder.tvLiTickerVolumeCurrency.setText(currentTicker.getSellPair());
        viewHolder.tvLiTickerLowestAsk.setText(currentTicker.getLowestAsk());
        viewHolder.tvLiTickerLowest24hr.setText(currentTicker.getLowest24hr());
        viewHolder.tvLiTickerHighestBid.setText(currentTicker.getHighestBid());
        viewHolder.tvLiTickerHighest24hr.setText(currentTicker.getHighest24hr());

        return convertView;
    }

    private class ViewHolder {
        final TextView tvLiTickerPair;
        final TextView tvLiTickerLast;
        final TextView tvLiTickerVolume;
        final TextView tvLiTickerVolumeCurrency;
        final TextView tvLiTickerLowestAsk;
        final TextView tvLiTickerLowest24hr;
        final TextView tvLiTickerHighestBid;
        final TextView tvLiTickerHighest24hr;

        ViewHolder(View v) {
            this.tvLiTickerPair = (TextView) v.findViewById(R.id.tvLiTickerPair);
            this.tvLiTickerLast = (TextView) v.findViewById(R.id.tvLiTickerLast);
            this.tvLiTickerVolume = (TextView) v.findViewById(R.id.tvLiTickerVolume);
            this.tvLiTickerVolumeCurrency = (TextView) v.findViewById(R.id.tvLiTickerVolumeCurrency);
            this.tvLiTickerLowestAsk = (TextView) v.findViewById(R.id.tvLiTickerLowestAsk);
            this.tvLiTickerLowest24hr = (TextView) v.findViewById(R.id.tvLiTickerLowest24hr);
            this.tvLiTickerHighestBid = (TextView) v.findViewById(R.id.tvLiTickerHighestBid);
            this.tvLiTickerHighest24hr = (TextView) v.findViewById(R.id.tvLiTickerHighest24hr);
        }
    }
}

