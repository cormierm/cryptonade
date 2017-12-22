package com.mattcormier.cryptonade.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.models.Coin;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Filename: BookRecyclerViewAdapter.java
 * Description: Adapter for handling order books RecyclerView
 * Created by Matt Cormier on 11/15/2017.
 */

public class MarketCapRecyclerViewAdapter extends RecyclerView.Adapter<MarketCapRecyclerViewAdapter.CoinViewHolder>{
    private static final String TAG = "MarketCapRecyclerViewAd";
    private List<Coin> mCoinsList;
    private Context mContext;

    public MarketCapRecyclerViewAdapter(Context context, List<Coin> coinsList) {
        mContext = context;
        mCoinsList = coinsList;
    }

    @Override
    public CoinViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_market_cap, parent, false);
        return new CoinViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CoinViewHolder holder, int position) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        Coin coin = mCoinsList.get(position);
        holder.name.setText(coin.getName());
        holder.symbol.setText("(" + coin.getSymbol() + ")");
        holder.rank.setText(coin.getRank() + ".");
        holder.priceBTC.setText(String.format("%.8f", coin.getPriceBTC()));
        holder.priceUSD.setText("$" + numberFormat.format(coin.getPriceUSD()));
        holder.cap.setText("$" + numberFormat.format(coin.getCap()));
        holder.volume.setText("$" + numberFormat.format(coin.getVolume()));

        if (coin.getOneHour() < 0) {
            holder.oneHour.setTextColor(mContext.getResources().getColor(R.color.red));
        } else {
            holder.oneHour.setTextColor(mContext.getResources().getColor(R.color.green));
        }
        holder.oneHour.setText(numberFormat.format(coin.getOneHour()));

        if (coin.getTwentyFourHour() < 0) {
            holder.twentyFourHour.setTextColor(mContext.getResources().getColor(R.color.red));
        } else {
            holder.twentyFourHour.setTextColor(mContext.getResources().getColor(R.color.green));
        }
        holder.twentyFourHour.setText("(" + numberFormat.format(coin.getTwentyFourHour()) + "%)");

        if (coin.getSevenDay() < 0) {
            holder.sevenDay.setTextColor(mContext.getResources().getColor(R.color.red));
        } else {
            holder.sevenDay.setTextColor(mContext.getResources().getColor(R.color.green));
        }
        holder.sevenDay.setText(numberFormat.format(coin.getSevenDay()));
    }

    public int getItemCount() {
        return ((mCoinsList != null) && (mCoinsList.size() != 0) ? mCoinsList.size() : 0);
    }

    public void loadNewData(List<Coin> newCoins) {
        Log.d(TAG, "loadNewData: ");
        mCoinsList = newCoins;
        notifyDataSetChanged();
    }

    public Coin getCoin(int position) {
        return ((mCoinsList != null) && (mCoinsList.size() != 0) ? mCoinsList.get(position) : null);
    }

    static class CoinViewHolder extends RecyclerView.ViewHolder {
        TextView name = null;
        TextView symbol = null;
        TextView rank = null;
        TextView priceBTC = null;
        TextView priceUSD = null;
        TextView cap = null;
        TextView volume = null;
        TextView oneHour = null;
        TextView twentyFourHour = null;
        TextView sevenDay = null;

        public CoinViewHolder(View itemView) {
            super(itemView);
            this.name = itemView.findViewById(R.id.tvLiMarketCapName);
            this.symbol = itemView.findViewById(R.id.tvLiMarketCapSymbol);
            this.rank = itemView.findViewById(R.id.tvLiMarketCapRank);
            this.priceBTC = itemView.findViewById(R.id.tvLiMarketCapRate);
            this.priceUSD = itemView.findViewById(R.id.tvLiMarketCapPrice);
            this.cap = itemView.findViewById(R.id.tvLiMarketCapCap);
            this.volume = itemView.findViewById(R.id.tvLiMarketCapVolume);
            this.oneHour = itemView.findViewById(R.id.tvLiMarketCap1H);
            this.twentyFourHour = itemView.findViewById(R.id.tvLiMarketCap24H);
            this.sevenDay = itemView.findViewById(R.id.tvLiMarketCap7D);
        }
    }
}
