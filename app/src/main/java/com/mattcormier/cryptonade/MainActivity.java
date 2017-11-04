package com.mattcormier.cryptonade;

import android.content.Intent;
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
import android.widget.Toast;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.clients.PoloniexClient;
import com.mattcormier.cryptonade.clients.QuadrigacxClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.Pair;

import java.lang.reflect.Array;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spnClients = (Spinner) findViewById(R.id.spnClients);
        spnClients.setOnItemSelectedListener(this);
        spnPairs = (Spinner) findViewById(R.id.spnPairs);

        db = new CryptoDB(this);
        sharedPreferences = getSharedPreferences("main", MODE_PRIVATE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // initialize client spinner in action bar
        UpdateClientSpinner();
        selectedClient = (APIClient) spnClients.getSelectedItem();
        Log.d(TAG, "onCreate: selectedClient: " + selectedClient);

        UpdatePairsSpinner();

        Log.d(TAG, "onCreate: init balance bar");
        // initialize balance bar
        fragBalanceBar = new BalanceBarFragment();
        tickerBarFragment = new TickerBarFragment();

        Log.d(TAG, "onCreate: implementing frags");
        getFragmentManager().beginTransaction().replace(R.id.flMainBalanceBar, fragBalanceBar, "balance_bar").commit();
        getFragmentManager().beginTransaction().replace(R.id.flMainTickerBar, tickerBarFragment, "ticker_bar").commit();
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new HomeFragment()).commit();
    }

    public void UpdateClientSpinner() {
        Intent intent = getIntent();
        String password = intent.getStringExtra("password");
        Toast.makeText(this, password, Toast.LENGTH_LONG).show();
        List<Exchange> exchangeList = db.getExchanges();
        ArrayList<APIClient> clientList = new ArrayList<APIClient>();
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
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menuAPISettings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new APIFragment())
                    .addToBackStack("api_settings")
                    .commit();
            return true;
        }
        else if (id == R.id.menuRefreshBalances) {
            ((APIClient) spnClients.getSelectedItem()).UpdateBalances(this);
            return true;
        }
        else if (id == R.id.menuSettings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new SettingsFragment())
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
                    .replace(R.id.content_frame, new TradeFragment(), "trade")
                    .addToBackStack("trade")
                    .commit();
        } else if (id == R.id.nav_orders) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new OrdersFragment(), "orders")
                    .addToBackStack("orders")
                    .commit();
        } else if (id == R.id.nav_balances) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new BalancesFragment(), "balances")
                    .addToBackStack("balances")
                    .commit();
        } else if (id == R.id.nav_ticker) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new TickerFragment(), "ticker")
                    .addToBackStack("ticker")
                    .commit();
        } else if (id == R.id.nav_pairs) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new PairsFragment(), "pairs")
                    .addToBackStack("pairs")
                    .commit();
        } else if (id == R.id.nav_api_settings) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new APIFragment(), "api")
                    .addToBackStack("api")
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
        List<Pair> pairsList = db.getPairs((int)((APIClient)spnClients.getSelectedItem()).getId());
        Pair tmpPair = (Pair)spnPairs.getSelectedItem();
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
            for(int i=0; i < dataAdapter.getCount(); i++) {
                String tradePair = ((Pair)spnPairs.getItemAtPosition(i)).getTradingPair();
                if (tradePair.equalsIgnoreCase(currentPair)) {
                    spnPairs.setSelection(i);
                    break;
                }
            }
        } catch (Exception ex) {
            Log.d("Crypto", "Error in UpdatePairsSpinner: " + ex.toString());
        }
        selectedPair = (Pair)spnPairs.getSelectedItem();
    }
}
