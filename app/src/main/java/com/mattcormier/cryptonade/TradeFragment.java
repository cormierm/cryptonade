package com.mattcormier.cryptonade;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.exchanges.PoloniexClient;
import com.mattcormier.cryptonade.exchanges.Exchange;
import com.mattcormier.cryptonade.models.Pair;

public class TradeFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener, TextView.OnEditorActionListener {
    private static final String TAG = "TradeFragment";
    TextView tvBalances;
    TextView tvHeaderLeft;
    TextView tvHeaderRight;
    TextView tvLast;
    TextView tvHighest;
    TextView tvLowest;
    Spinner spnPair;
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

    CryptoDB db;
    PoloniexClient exchange;
    View tradeView;
    Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        tradeView = inflater.inflate(R.layout.trade_layout, container, false);
        context = getActivity();

        tvBalances = (TextView) tradeView.findViewById(R.id.tvTradeBalances);
        tvHeaderLeft = (TextView) tradeView.findViewById(R.id.tvTradeHeaderLeft);
        tvHeaderRight = (TextView) tradeView.findViewById(R.id.tvTradeHeaderRight);
        tvLast = (TextView) tradeView.findViewById(R.id.tvTradeLastTrade);
        tvHighest = (TextView) tradeView.findViewById(R.id.tvTradeHighestBid);
        tvLowest = (TextView) tradeView.findViewById(R.id.tvTradeLowestAsk);
        spnPair = (Spinner) tradeView.findViewById(R.id.spnTradeCurrencyPairs);
        edPrice = (EditText) tradeView.findViewById(R.id.edTradePrice);
        edAmount = (EditText) tradeView.findViewById(R.id.edTradeAmount);
        edTotal = (EditText) tradeView.findViewById(R.id.edTradeTotal);
        btnBuy = (Button) tradeView.findViewById(R.id.btnTradeBuy);
        btnSell  = (Button) tradeView.findViewById(R.id.btnTradeSell);
        btnMax = (Button) tradeView.findViewById(R.id.btnTradeMax);
        btnLast = (Button) tradeView.findViewById(R.id.btnTradeLast);
        btnHighest = (Button) tradeView.findViewById(R.id.btnTradeBuyHighestBid);
        btnLowest = (Button) tradeView.findViewById(R.id.btnTradeBuyLowestAsk);
        btnPlaceOrder = (Button) tradeView.findViewById(R.id.btnTradePlaceOrder);

        db = new CryptoDB(context);
        Exchange ex = db.getExchange(1);
        exchange = new PoloniexClient((int)ex.getId(), ex.getName(), ex.getAPIKey(), ex.getAPISecret(), ex.getAPIOther());

        spnPair.setOnItemSelectedListener(this);
        btnBuy.setOnClickListener(this);
        btnSell.setOnClickListener(this);
        btnMax.setOnClickListener(this);
        btnLast.setOnClickListener(this);
        btnHighest.setOnClickListener(this);
        btnLowest.setOnClickListener(this);
        btnPlaceOrder.setOnClickListener(this);

        edAmount.setOnEditorActionListener(this);
        edPrice.setOnEditorActionListener(this);

        exchange.RefreshBalances(context);
        Crypto.UpdatePairsSpinner(context, spnPair, db);
        exchange.UpdateTradeTickerInfo(context);

        orderType = "buy";
        updatePage();

        return tradeView;
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_main, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRefresh:
                exchange.RefreshBalances(context);
                exchange.UpdateTradeTickerInfo(context);
                return true;
            case R.id.menuSettings:
                getFragmentManager().beginTransaction().replace(
                        R.id.content_frame, new APISettingsFragment()).commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == btnPlaceOrder.getId()) {
            String pair = ((Pair) spnPair.getSelectedItem()).getExchangePair();
            String amount = edAmount.getText().toString();
            String price = edPrice.getText().toString();
            exchange.PlaceOrder(context, pair, price, amount, orderType, tvBalances);
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
        String[] pair = spnPair.getSelectedItem().toString().split("-");
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
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        exchange.UpdateTradeTickerInfo(context);
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
