package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Spinner;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.clients.PoloniexClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.clients.QuadrigacxClient;
import com.mattcormier.cryptonade.models.Pair;

public class OrdersFragment extends Fragment {
    private static final String TAG = "OrdersFragment";
    ListView lvOpenOrders;
    CryptoDB db;
    APIClient client;
    View ordersView;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        ordersView = inflater.inflate(R.layout.orders_layout, container, false);
        context = getActivity();
        lvOpenOrders = (ListView) ordersView.findViewById(R.id.lvOpenOrders);

        db = new CryptoDB(context);
        client = (APIClient) ((Spinner)getActivity().findViewById(R.id.spnClients)).getSelectedItem();

        client.UpdateOpenOrders(context);
        return ordersView;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menuRefresh:
//                client.UpdateOpenOrders(context);
//                return true;
////            case R.id.menuSettings:
////                getFragmentManager().beginTransaction()
////                        .replace(R.id.content_frame, new APISettingsFragment())
////                        .addToBackStack("api_settings")
////                        .commit();
////                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

}
