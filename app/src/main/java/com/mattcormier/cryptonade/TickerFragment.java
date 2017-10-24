package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.PoloniexClient;
import com.mattcormier.cryptonade.models.Exchange;

public class TickerFragment extends Fragment {
    ListView lvTickers;
    CryptoDB db;
    PoloniexClient exchange;
    View tickerView;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        tickerView = inflater.inflate(R.layout.ticker_layout, container, false);
        context = getActivity();
        lvTickers = (ListView) tickerView.findViewById(R.id.lvTickerList);

        db = new CryptoDB(context);
        Exchange ex = db.getExchange(1);
        exchange = new PoloniexClient((int)ex.getId(), ex.getName(), ex.getAPIKey(), ex.getAPISecret(), ex.getAPIOther());
        exchange.UpdateTickerActivity(context);
        return tickerView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRefresh:
                exchange.UpdateTickerActivity(context);
                return true;
            case R.id.menuSettings:
                getFragmentManager().beginTransaction().replace(
                        R.id.content_frame, new APISettingsFragment()).commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
