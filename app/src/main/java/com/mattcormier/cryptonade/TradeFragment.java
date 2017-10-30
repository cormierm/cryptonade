package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.models.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class TradeFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener, TextView.OnEditorActionListener {
    private static final String TAG = "TradeFragment";
    TextView tvHeaderLeft;
    TextView tvHeaderRight;
    TextView tvLast;
    TextView tvHighest;
    TextView tvLowest;
    EditText edPrice;
    EditText edAmount;
    EditText edTotal;
    Button btnBuy;
    Button btnSell;
    Button btnMax;
    Button btnLast;
    Button btnHighest;
    Button btnLowest;
    Button btnPlaceOrder;
    String orderType;
    MainActivity mainActivity;
    Spinner spnPairs;
    Spinner spnClients;

    CryptoDB db;
    View tradeView;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        tradeView = inflater.inflate(R.layout.trade_layout, container, false);
        context = getActivity();
        mainActivity = (MainActivity)getActivity();

        tvHeaderLeft = tradeView.findViewById(R.id.tvTradeHeaderLeft);
        tvHeaderRight = tradeView.findViewById(R.id.tvTradeHeaderRight);
        tvLast = tradeView.findViewById(R.id.tvTradeLastTrade);
        tvHighest = tradeView.findViewById(R.id.tvTradeHighestBid);
        tvLowest = tradeView.findViewById(R.id.tvTradeLowestAsk);
        edPrice = tradeView.findViewById(R.id.edTradePrice);
        edAmount = tradeView.findViewById(R.id.edTradeAmount);
        edTotal = tradeView.findViewById(R.id.edTradeTotal);
        btnBuy = tradeView.findViewById(R.id.btnTradeBuy);
        btnSell  = tradeView.findViewById(R.id.btnTradeSell);
        btnMax = tradeView.findViewById(R.id.btnTradeMax);
        btnLast = tradeView.findViewById(R.id.btnTradeLast);
        btnHighest = tradeView.findViewById(R.id.btnTradeBuyHighestBid);
        btnLowest = tradeView.findViewById(R.id.btnTradeBuyLowestAsk);
        btnPlaceOrder = tradeView.findViewById(R.id.btnTradePlaceOrder);

        spnPairs = (Spinner) mainActivity.findViewById(R.id.spnPairs);
        spnPairs.setOnItemSelectedListener(this);
        spnClients = (Spinner) mainActivity.findViewById(R.id.spnClients);
        spnClients.setOnItemSelectedListener(this);

        db = new CryptoDB(context);

        btnBuy.setOnClickListener(this);
        btnSell.setOnClickListener(this);
        btnMax.setOnClickListener(this);
        btnLast.setOnClickListener(this);
        btnHighest.setOnClickListener(this);
        btnLowest.setOnClickListener(this);
        btnPlaceOrder.setOnClickListener(this);

        edAmount.setOnEditorActionListener(this);
        edPrice.setOnEditorActionListener(this);

        mainActivity.selectedClient.UpdateTradeTickerInfo(context);

        orderType = "buy";
        updatePage();

        setHasOptionsMenu(true);

        return tradeView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuRefresh) {
            updatePage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == btnPlaceOrder.getId()) {
            String pair = ((Pair) spnPairs.getSelectedItem()).getExchangePair();
            String amount = edAmount.getText().toString();
            String price = edPrice.getText().toString();
            ((APIClient)spnClients.getSelectedItem()).PlaceOrder(context, pair, price, amount, orderType);
        }
        else if (view.getId() == btnBuy.getId()) {
            orderType = "buy";
            updatePage();
        }
        else if (view.getId() == btnSell.getId()) {
            orderType = "sell";
            updatePage();
        }
        else if (view.getId() == btnMax.getId()) {
            try{
                float price = Float.parseFloat(edPrice.getText().toString());
                float available = Float.parseFloat(tvHeaderRight.getText().toString().split(" ")[0]);
                if (available > 0) {
                    if(orderType.equals("buy")) {
                        edAmount.setText(String.format("%.8f", (available / price)));
                    } else {
                        edAmount.setText(String.format("%.8f", (available)));
                    }
                }
            } catch (Exception ex) {
                Log.d(TAG, "onClick: Error with Max button click " + ex.getMessage());
                Toast.makeText(context, "Error setting max amount. Check your price amount.", Toast.LENGTH_SHORT).show();
            }
        }
        else if (view.getId() == btnHighest.getId()) {
            edPrice.setText(tvHighest.getText().toString());
        }
        else if (view.getId() == btnLowest.getId()) {
            edPrice.setText(tvLowest.getText().toString());
        }
        else if (view.getId() == btnLast.getId()) {
            edPrice.setText(tvLast.getText().toString());
        }
    }

    private void updatePage() {
        Log.d(TAG, "updatePage: start");
        Pair selectedPair = (Pair) spnPairs.getSelectedItem();
        ((APIClient)spnClients.getSelectedItem()).UpdateTradeTickerInfo(context);
        String[] pair = selectedPair.toString().split("-");
        String leftHeaderText = orderType.toUpperCase() + " " + pair[1];
        btnPlaceOrder.setText(leftHeaderText);
        tvHeaderLeft.setText(leftHeaderText);
        if (orderType.equals("buy")) {
            btnPlaceOrder.setBackgroundResource(R.color.green);
        }
        else {
            btnPlaceOrder.setBackgroundResource(R.color.red);
        }
        updateAvailableInfo();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
        edAmount.setText("");
        edTotal.setText("");
        if (parent.getId() == spnClients.getId()) {
            mainActivity.UpdatePairsSpinner();
            ((APIClient)spnClients.getSelectedItem()).UpdateBalances(context);
        }
        updatePage();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED ||
                actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_NONE){
            String amount = edAmount.getText().toString();
            String price = edPrice.getText().toString();
            String total = "";
            if (!amount.isEmpty() && !price.isEmpty()) {
                try {
                    total = String.format("%.8f", (Float.parseFloat(amount) * Float.parseFloat(price)));
                } catch (Exception ex) {
                    Log.d(TAG, "onEditorAction: " + ex.getMessage());
                }
            }
            edTotal.setText(total);
        }
        return false;
    }

    public void updateAvailableInfo() {
        Log.d(TAG, "updateAvailableInfo: start");
        HashMap<String, Double> availableBalances = ((APIClient)spnClients.getSelectedItem()).getAvailableBalances();
        if (availableBalances != null) {
            String headerValue = "0";
            String[] pairs = spnPairs.getSelectedItem().toString().split("-");
            String pair;
            if (orderType.equals("buy")) {
                pair = pairs[0];
            } else {
                pair = pairs[1];
            }
            for (Map.Entry<String, Double> bal: availableBalances.entrySet()) {
                Double balAmount = bal.getValue();
                if (balAmount > 0 && bal.getKey().equals(pair)) {
                    headerValue = String.format("%.8f", balAmount);
                }
            }
            tvHeaderRight.setText(headerValue + " " + pair + " Available");
        }
        else {
            tvHeaderRight.setText("");
        }

    }
}
