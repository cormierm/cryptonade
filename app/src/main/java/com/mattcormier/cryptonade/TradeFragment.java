package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

        mainActivity.selectedClient.RefreshBalances(context);
        mainActivity.selectedClient.UpdateTradeTickerInfo(context);

        orderType = "buy";
        updatePage();

        return tradeView;
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
                        edAmount.setText(Float.toString(available / price));
                    } else {
                        edAmount.setText(Float.toString(available));
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
        Pair selectedPair = (Pair) spnPairs.getSelectedItem();
        ((APIClient)spnClients.getSelectedItem()).UpdateTradeTickerInfo(context);
        String[] pair = selectedPair.toString().split("-");
        String msg = orderType.toUpperCase() + " " + pair[1];
        btnPlaceOrder.setText(msg);
        tvHeaderLeft.setText(msg);
        if (orderType.equals("buy")) {
            btnPlaceOrder.setBackgroundResource(R.color.green);
            updateAvailable(pair[0]);
        }
        else {
            btnPlaceOrder.setBackgroundResource(R.color.red);
            updateAvailable(pair[1]);
        }
    }

    private void updateAvailable(String pair) {
        TextView tvBalances = (TextView) mainActivity.findViewById(R.id.tvBalanceBar);
        String[] balances = tvBalances.getText().toString().split("        ");
        String available = "0";
        for (String bal: balances) {
            String[] b = bal.split(":");
            if (b[0].equals(pair)) {
                available = b[1];
                break;
            }
        }
        tvHeaderRight.setText(available + " " + pair + " Available");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
        if (parent.getId() == spnClients.getId()) {
            mainActivity.UpdatePairsSpinner();
            ((APIClient)spnClients.getSelectedItem()).UpdateBalances(context);
        }
        updatePage();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED ||
                actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_NONE){
            String amount = edAmount.getText().toString();
            String price = edPrice.getText().toString();
            String total = "0";
            if (!amount.isEmpty() && !price.isEmpty()) {
                try {
                    total = Float.toString(Float.parseFloat(amount) * Float.parseFloat(price));
                } catch (Exception ex) {
                    Log.d(TAG, "onEditorAction: " + ex.getMessage());
                }
            }
            edTotal.setText(total);
        }
        return false;
    }
}
