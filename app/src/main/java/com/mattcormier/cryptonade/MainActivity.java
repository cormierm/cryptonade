package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.Pair;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "MainActivity";
    public ArrayList<APIClient> apiClientArrayList;
    Spinner spnClients;
    Spinner spnPairs;
    APIClient selectedClient;
    Pair selectedPair;
    BalanceBarFragment fragBalanceBar;
    TickerBarFragment tickerBarFragment;
    CryptoDB db;
    SharedPreferences sharedPreferences;
    Fragment fragmentAPI;
    Fragment fragmentBalances;
    Fragment fragmentTicker;
    Fragment fragmentPairs;
    Fragment fragmentOrders;
    Fragment fragmentSettings;
    Fragment fragmentTrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spnClients = findViewById(R.id.spnClients);
        spnClients.setOnItemSelectedListener(this);
        spnPairs = findViewById(R.id.spnPairs);

        db = new CryptoDB(this);
        sharedPreferences = getSharedPreferences("main", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // initialize client spinner in action bar
        UpdateClientSpinner();
        selectedClient = (APIClient) spnClients.getSelectedItem();
        UpdatePairsSpinner();

        // initialize balance bar
        fragBalanceBar = new BalanceBarFragment();
        // initialize ticker bar
        tickerBarFragment = new TickerBarFragment();

        Log.d(TAG, "onCreate: implementing frags");
        getFragmentManager().beginTransaction().replace(R.id.flMainBalanceBar, fragBalanceBar, "balance_bar").commit();
        getFragmentManager().beginTransaction().replace(R.id.flMainTickerBar, tickerBarFragment, "ticker_bar").commit();
    }

    public void UpdateClientSpinner() {
        List<Exchange> exchangeList = db.getExchanges();
        if(exchangeList.isEmpty()) {
            Exchange exchange = new Exchange(1, 1, "Poloniex Demo", "", "", "", 1);
            long rowId = db.insertExchange(exchange);
            Exchange newEx = db.getExchange((int)rowId);
            APIClient newClient = Crypto.getAPIClient(newEx);
            newClient.RestorePairsInDB(this);
            UpdateClientSpinner();
        }
        ArrayList<APIClient> clientList = new ArrayList<>();
        for (Exchange e: exchangeList) {
            clientList.add(Crypto.getAPIClient(e));
        }
        apiClientArrayList = clientList;
        try {
            ArrayAdapter<APIClient> dataAdapter = new ArrayAdapter<>(this,
                    R.layout.spinner_exchange_layout, clientList);
            dataAdapter.setDropDownViewResource(R.layout.spinner_exchange_layout);
            spnClients.setAdapter(dataAdapter);
        } catch (Exception ex) {
            Log.d(TAG, "onCreate: " + ex.getMessage());
        }
    }

    @Override
    protected void onPause() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("clientSpinnerPosition", spnClients.getSelectedItemPosition());
        editor.putInt("pairsSpinnerPosition", spnPairs.getSelectedItemPosition());
        editor.commit();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        UpdateClientSpinner();
        spnClients.setSelection(sharedPreferences.getInt("clientSpinnerPosition", 0));
        UpdatePairsSpinner();
        spnPairs.setSelection(sharedPreferences.getInt("pairsSpinnerPosition", 0));

        switch (sharedPreferences.getString("currentScreen", "TradeFragment")) {
            case "APIFragment":
                getFragmentManager().beginTransaction().replace(R.id.content_frame, getFragment("api_keys"), "api_keys").commit();
                break;
            case "BalancesFragment":
                getFragmentManager().beginTransaction().replace(R.id.content_frame, getFragment("balances"), "balances").commit();
                break;
            case "TickerFragment":
                getFragmentManager().beginTransaction().replace(R.id.content_frame, getFragment("ticker"), "ticker").commit();
                break;
            case "PairsFragment":
                getFragmentManager().beginTransaction().replace(R.id.content_frame, getFragment("pairs"), "pairs").commit();
                break;
            case "OrdersFragment":
                getFragmentManager().beginTransaction().replace(R.id.content_frame, getFragment("orders"), "orders").commit();
                break;
            case "SettingsFragment":
                getFragmentManager().beginTransaction().replace(R.id.content_frame, getFragment("settings"), "settings").commit();
                break;
            default:
                getFragmentManager().beginTransaction().replace(R.id.content_frame, getFragment("trade"), "trade").commit();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menuAPISettings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, getFragment("api_keys"), "api_keys")
                    .addToBackStack("api_keys")
                    .commit();
            return true;
        }
        else if (id == R.id.menuRefreshBalances) {
            ((APIClient) spnClients.getSelectedItem()).UpdateBalances(this);
            return true;
        }
        else if (id == R.id.menuSettings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, getFragment("settings"), "settings")
                    .addToBackStack("settings")
                    .commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        android.app.FragmentManager fragmentManager = getFragmentManager();
        if (id == R.id.nav_trade) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, getFragment("trade"), "trade")
                    .addToBackStack("trade")
                    .commit();
        } else if (id == R.id.nav_orders) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, getFragment("orders"), "orders")
                    .addToBackStack("orders")
                    .commit();
        } else if (id == R.id.nav_balances) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, getFragment("balances"), "balances")
                    .addToBackStack("balances")
                    .commit();
        } else if (id == R.id.nav_ticker) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, getFragment("ticker"), "ticker")
                    .addToBackStack("ticker")
                    .commit();
        } else if (id == R.id.nav_pairs) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, getFragment("pairs"), "pairs")
                    .addToBackStack("pairs")
                    .commit();
        } else if (id == R.id.nav_api_settings) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, getFragment("api_keys"), "api_keys")
                    .addToBackStack("api_keys")
                    .commit();
        } else if (id == R.id.nav_settings) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, getFragment("settings"), "settings")
                    .addToBackStack("settings")
                    .commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedClient = (APIClient)((Spinner)findViewById(R.id.spnClients)).getSelectedItem();
        Log.d(TAG, "onItemSelected: selectedClient:" + selectedClient);
        if (selectedClient != null) {
            UpdatePairsSpinner();
            ((APIClient) spnClients.getSelectedItem()).UpdateBalances(this);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    public void UpdatePairsSpinner() {
        Log.d(TAG, "UpdatePairsSpinner: ");
        APIClient client = (APIClient) spnClients.getSelectedItem();
        if (client == null) {
            return;
        }

        List<Pair> pairsList = db.getPairs((int) (client).getId());
        Pair tmpPair = (Pair) spnPairs.getSelectedItem();
        String currentPair = "";
        if (tmpPair != null) {
            currentPair = tmpPair.getTradingPair();
        }
        try {
            ArrayAdapter<Pair> dataAdapter = new ArrayAdapter<>(this,
                    R.layout.spinner_pairs_layout, pairsList);
            dataAdapter.setDropDownViewResource(R.layout.spinner_pairs_layout);
            spnPairs.setAdapter(dataAdapter);

            // set pair to current pair of previous client
            for (int i = 0; i < dataAdapter.getCount(); i++) {
                String tradePair = ((Pair) spnPairs.getItemAtPosition(i)).getTradingPair();
                if (tradePair.equalsIgnoreCase(currentPair)) {
                    spnPairs.setSelection(i);
                    break;
                }
            }
        } catch (Exception ex) {
            Log.d("Crypto", "Error in UpdatePairsSpinner: " + ex.toString());
        }
        selectedPair = (Pair) spnPairs.getSelectedItem();
        if (selectedPair != null) {
            client.UpdateTickerInfo(this, selectedPair.getExchangePair());
        }
    }
    private Fragment getFragment(String fragName) {
        Log.d(TAG, "getFragment: fragName: " + fragName);
        switch (fragName) {
            case "api_keys":
                if (fragmentAPI == null) {
                    fragmentAPI = new APIFragment();
                }
                return fragmentAPI;
            case "balances":
                if (fragmentBalances == null) {
                    fragmentBalances = new BalancesFragment();
                }
                return fragmentBalances;
            case "ticker":
                if (fragmentTicker == null) {
                    fragmentTicker = new TickerFragment();
                }
                return fragmentTicker;
            case "pairs":
                if (fragmentPairs == null) {
                    fragmentPairs = new PairsFragment();
                }
                return fragmentPairs;
            case "orders":
                if (fragmentOrders == null) {
                    fragmentOrders = new OrdersFragment();
                }
                return fragmentOrders;
            case "settings":
                if (fragmentSettings == null) {
                    fragmentSettings = new SettingsFragment();
                }
                return fragmentSettings;
            case "trade":
                if (fragmentTrade == null) {
                    fragmentTrade = new TradeFragment();
                }
                return fragmentTrade;
            default:
                return null;
        }
    }
}
