package com.mattcormier.cryptonade;

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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "MainActivity";
    Spinner spnClients;
    Spinner spnPairs;
    APIClient selectedClient;
    BalanceBarFragment fragBalanceBar;
    CryptoDB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spnClients = (Spinner) findViewById(R.id.spnClients);
        spnClients.setOnItemSelectedListener(this);
        spnPairs = (Spinner) findViewById(R.id.spnPairs);

        db = new CryptoDB(this);

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
        fragBalanceBar.setClient(selectedClient);

        Log.d(TAG, "onCreate: implementing frags");
        getFragmentManager().beginTransaction().replace(R.id.flMainBalanceBar, fragBalanceBar).commit();
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new HomeFragment()).commit();
    }

    public void UpdateClientSpinner() {
        List<Exchange> exchangeList = db.getExchanges();
        ArrayList<APIClient> clientList = new ArrayList<APIClient>();
        for (Exchange e: exchangeList) {
            clientList.add(Crypto.getAPIClient(e));
        }

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
            fragBalanceBar.UpdateBalanceBar();
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
                    .replace(R.id.content_frame, new TradeFragment())
                    .addToBackStack("trade")
                    .commit();
        } else if (id == R.id.nav_orders) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new OrdersFragment())
                    .addToBackStack("orders")
                    .commit();
        } else if (id == R.id.nav_ticker) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new TickerFragment())
                    .addToBackStack("ticker")
                    .commit();
        } else if (id == R.id.nav_pairs) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new PairsFragment())
                    .addToBackStack("pairs")
                    .commit();
        } else if (id == R.id.nav_api_settings) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, new APIFragment())
                    .addToBackStack("api_settings")
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
            fragBalanceBar.setClient(selectedClient);
            fragBalanceBar.UpdateBalanceBar();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void UpdatePairsSpinner() {
        List<Pair> pairsList = db.getPairs((int)selectedClient.getId());
        try {
            ArrayAdapter<Pair> dataAdapter = new ArrayAdapter<>(this,
                    R.layout.spinner_exchange_layout, pairsList);
            dataAdapter.setDropDownViewResource(R.layout.spinner_exchange_layout);
            spnPairs.setAdapter(dataAdapter);
        } catch (Exception ex) {
            Log.d("Crypto", "Error in UpdatePairsSpinner: " + ex.toString());
        }
    }
}
