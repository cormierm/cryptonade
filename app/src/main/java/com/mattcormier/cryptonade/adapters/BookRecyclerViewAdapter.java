package com.mattcormier.cryptonade.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mattcormier.cryptonade.R;

import java.util.HashMap;
import java.util.List;

/**
 * Created by matt on 11/15/2017.
 */

public class BookRecyclerViewAdapter extends RecyclerView.Adapter<BookRecyclerViewAdapter.BookViewHolder>{
    private List<HashMap<String, String>> mBooksList;
    private Context mContext;

    public BookRecyclerViewAdapter(Context context, List<HashMap<String, String>> booksList) {
        mContext = context;
        mBooksList = booksList;
    }

    @Override
    public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_order_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookViewHolder holder, int position) {
        HashMap<String, String> ask = mBooksList.get(position);
        holder.price.setText(ask.get("price"));
        holder.amount.setText(ask.get("amount"));
    }

    public int getItemCount() {
        return ((mBooksList != null) && (mBooksList.size() != 0) ? mBooksList.size() : 0);
    }

    public void loadNewData(List<HashMap<String, String>> newBooks) {
        mBooksList = newBooks;
        notifyDataSetChanged();
    }

    public HashMap<String, String> getAsk(int position) {
        return ((mBooksList != null) && (mBooksList.size() != 0) ? mBooksList.get(position) : null);
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView price = null;
        TextView amount = null;

        public BookViewHolder(View itemView) {
            super(itemView);
            this.price = itemView.findViewById(R.id.tvLiOrderBookPrice);
            this.amount = itemView.findViewById(R.id.tvLiOrderBookAmount);
        }
    }
}
