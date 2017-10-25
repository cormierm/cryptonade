package com.mattcormier.cryptonade;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new HomeFragment()).commit();
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

        if (id == R.id.action_settings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new APISettingsFragment())
                    .addToBackStack("api_settings")
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
}
