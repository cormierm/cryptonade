package com.mattcormier.cryptonade.clients;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.mattcormier.cryptonade.MainActivity;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.Ticker;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.adapters.OpenOrdersAdapter;
import com.mattcormier.cryptonade.models.Pair;
import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.adapters.TickerAdapter;

import com.mattcormier.cryptonade.models.OpenOrder;

public class PoloniexClient implements APIClient {
    private static final String TAG = "PoloniexClient";
    private long exchangeId;
    private static long typeId = 1;
    private String name;
    private String apiKey;
    private String apiSecret;
    private static String publicUrl = "https://poloniex.com/public?";
    private static String privateUrl = "https://poloniex.com/tradingApi";

    public PoloniexClient() {
        name = "";
        apiKey = "";
        apiSecret = "";
    }

    public PoloniexClient(int exchangeId, String name, String apiKey, String apiSecret) {
        this.exchangeId = exchangeId;
        this.name = name;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    private void publicRequest(HashMap<String, String> params, final Context c, final String cmd) {
        Log.d(TAG, "publicRequest: " + cmd);
        String url = publicUrl + createBody(params);
        RequestQueue queue = Volley.newRequestQueue(c);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            if (cmd.equals("restorePairsInDB")) {
                                processRestorePairsInDB(response, c);
                            }
                            else if (cmd.equals("updateTradeTickerInfo")) {
                                processUpdateTradeTickerInfo(response, c);
                            }
                            else if (cmd.equals("updateTickerActivity")) {
                                processUpdateTickerActivity(response, c);
                            }

                        } catch (Exception e) {
                            Log.d(TAG, "Error in request: " + cmd);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "publicRequest.onErrorResponse: " + error.getMessage());
                        Toast.makeText(c, "Currency update failed.", Toast.LENGTH_LONG).show();
                    }
                }
        );
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void privateRequest(final HashMap<String, String> params, final Context c, final String cmd) {
        Log.d(TAG, "privateRequest: " + cmd);
        final String nonce = new BigDecimal(generateNonce()).toString();
        params.put("nonce", nonce);
        final String body = createBody(params);
        final String signature = createSignature(body);

        RequestQueue queue = Volley.newRequestQueue(c);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, privateUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (cmd.equals("refreshBalances")) {
                            processRefreshBalances(response, c);
                        }
                        else if (cmd.equals("updateBalanceBar")) {
                            processUpdateBalanceBar(response, c);
                        }
                        else if (cmd.equals("placeOrder")) {
                            processPlacedOrder(response, c);
                        }
                        else if (cmd.equals("cancelOrder")) {
                            processCancelOrder(response, c);
                        }
                        else if (cmd.equals("updateOpenOrders")) {
                            processUpdateOpenOrders(response, c);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        Log.d(TAG, "StringRequire.onErrorResponse: " + ex.getMessage());
                    }
                }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return body == null ? null : body.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.d("BalanceRequest", "Unsupported Encoding while trying to get the bytes of " + body + "using utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Key", apiKey);
                headers.put("Sign", signature);
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private static void processRefreshBalances(String response, Context c) {
        try {
            TextView tvHeaderRight = ((Activity) c).findViewById(R.id.tvTradeHeaderRight);
            TextView tvHeaderLeft = ((Activity) c).findViewById(R.id.tvTradeHeaderLeft);
            String[] pairs = ((Spinner) ((Activity) c).findViewById(R.id.spnTradeCurrencyPairs)).getSelectedItem().toString().split("-");
            String orderType = tvHeaderLeft.getText().toString().split(" ")[0].toLowerCase();
            String pair;
            if (orderType.equals("buy")) {
                pair = pairs[0];
            } else {
                pair = pairs[1];
            }
            JSONObject data = new JSONObject(response);
            Iterator<?> keys = data.keys();
            String output = "";
            String headerValue = "0";
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String value = (String) data.get(key);
                if (!value.equals("0.00000000")) {
                    if (key.equals(pair)) {
                        headerValue = value;
                    }
                    output += key + ":" + value + "        ";
                }
            }
            tvHeaderRight.setText(headerValue + " " + pair + " Available");
        } catch (Exception ex) {
            Log.d(TAG, "Error in processRequestBalances.");
        }
    }

    private static void processUpdateBalanceBar(String response, Context c) {
        TextView tvBalanceBar = ((Activity) c).findViewById(R.id.tvBalanceBar);
        try {
            JSONObject data = new JSONObject(response);
            Iterator<?> keys = data.keys();
            String output = "";
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String value = (String) data.get(key);
                if (!value.equals("0.00000000")) {
                    output += key + ":" + value + "        ";
                }
            }
            tvBalanceBar.setText(output);
        } catch (Exception ex) {
            tvBalanceBar.setText("Error updating balances.");
            Log.d(TAG, "Error in processUpdateBalanceBar.");
        }
    }

    private void processRestorePairsInDB(String response, Context c) {
        CryptoDB db = new CryptoDB(c);
        db.deletePairsByExchangeId(exchangeId);
        try {
            JSONObject data = new JSONObject(response);
            ArrayList<Pair> pairsList = new ArrayList<>();
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String tradingPair = createTradePair(key);
                Pair pair = new Pair(0, (int)exchangeId, key, tradingPair);
                pairsList.add(pair);
            }

            db.insertPairs(pairsList);
        } catch (Exception ex) {
            Log.d(TAG, "Error in processTradingPairs: " + ex.toString());
        }
    }

    private void processPlacedOrder(String response, Context c) {
        JSONObject jsonResp;
        try {
            jsonResp = new JSONObject(response);
            if (jsonResp.has("error")) {
                Toast.makeText(c, jsonResp.getString("error"), Toast.LENGTH_LONG).show();
            }
            else if (jsonResp.has("orderNumber")) {
                Toast.makeText(c, "Trade placed successfully.\nOrder number: " +
                        jsonResp.getString("orderNumber"), Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(c, response, Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            Toast.makeText(c, "Error happened!", Toast.LENGTH_LONG).show();
            Log.d(this.name, "JSONException error in processPlacedOrder: " + e.toString());
        }
    }

    private void processCancelOrder(String response, Context c) {
        JSONObject jsonResp;
        try {
            jsonResp = new JSONObject(response);
            if (jsonResp.has("error")) {
                Toast.makeText(c, jsonResp.getString("error"), Toast.LENGTH_LONG).show();
            }
            else if (jsonResp.has("message")) {
                Toast.makeText(c, jsonResp.getString("message"), Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(c, response, Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            Toast.makeText(c, "Error happened!", Toast.LENGTH_LONG).show();
            Log.d(this.name, "JSONException error in processPlacedOrder: " + e.toString());
        }
        UpdateOpenOrders(c);
    }

    private static void processUpdateOpenOrders(String response, Context c) {
        ListView lvOpenOrders = ((Activity) c).findViewById(R.id.lvOpenOrders);
        try {
            ArrayList<OpenOrder> openOrdersList = new ArrayList<>();
            JSONObject json = new JSONObject(response);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray ordersList = json.getJSONArray(key);
                if (ordersList.length() != 0) {
                    for (int i=0; i < ordersList.length(); i++) {
                        JSONObject jsonOrder = ordersList.getJSONObject(i);
                        String orderNumber = jsonOrder.getString("orderNumber");
                        String orderType = jsonOrder.getString("type");
                        String orderRate = jsonOrder.getString("rate");
                        String orderStartingAmount = jsonOrder.getString("startingAmount");
                        String orderRemainingAmount = jsonOrder.getString("amount");
                        String orderDate = jsonOrder.getString("date");
                        OpenOrder order = new OpenOrder(orderNumber, key, orderType,
                                orderRate, orderStartingAmount, orderRemainingAmount, orderDate);
                        openOrdersList.add(order);
                    }
                }
            }

            OpenOrdersAdapter openOrdersAdapter = new OpenOrdersAdapter(c, R.layout.listitem_openorder, openOrdersList);
            lvOpenOrders.setAdapter(openOrdersAdapter);

        } catch (JSONException e) {
            Log.d(TAG, "Error in processUpdateOpenOrders: " + e.toString());
        }
    }

    private static void processUpdateTickerActivity(String response, Context c) {
        ListView lvTickerList = ((Activity) c).findViewById(R.id.lvTickerList);
        try {
            ArrayList<Ticker> tickerList = new ArrayList<>();
            JSONObject json = new JSONObject(response);
            Iterator<String> keys = json.keys();
            Log.d(TAG, "processUpdateTickerActivity: created tickers json object");
            while (keys.hasNext()) {
                String pair = keys.next();
                Log.d(TAG, "processUpdateTickerActivity: processing " + pair);
                JSONObject jsonTicker = json.getJSONObject(pair);
                String tickerPair = createTradePair(pair);
                String last = jsonTicker.getString("last");
                String volume = jsonTicker.getString("baseVolume");
                String lowestAsk = jsonTicker.getString("lowestAsk");
                String lowest24hr = jsonTicker.getString("low24hr");
                String highestBid = jsonTicker.getString("highestBid");
                String highest24hr = jsonTicker.getString("high24hr");
                Ticker ticker = new Ticker(tickerPair, last, volume,
                        lowestAsk, lowest24hr, highestBid, highest24hr);
                tickerList.add(ticker);
            }
            Log.d(TAG, "processUpdateTickerActivity: adding ticker list to adapter");
            TickerAdapter tickerAdapter = new TickerAdapter(c, R.layout.listitem_ticker, tickerList);
            lvTickerList.setAdapter(tickerAdapter);

        } catch (JSONException e) {
            Log.d(TAG, "Error in processUpdateTickerActivity: " + e.toString());
        }
    }

    private static void processUpdateTradeTickerInfo(String response, Context c) {
        TextView tvLast = ((Activity) c).findViewById(R.id.tvTradeLastTrade);
        TextView tvHighest = ((Activity) c).findViewById(R.id.tvTradeHighestBid);
        TextView tvLowest = ((Activity) c).findViewById(R.id.tvTradeLowestAsk);
        TextView edPrice = ((Activity) c).findViewById(R.id.edTradePrice);
        Spinner spnPairs = ((Activity) c).findViewById(R.id.spnTradeCurrencyPairs);
        String pair = ((Pair) spnPairs.getSelectedItem()).getExchangePair();

        try {
            JSONObject data = new JSONObject(response);
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.equals(pair)) {
                    JSONObject tickerInfo = data.getJSONObject(key);
                    tvLast.setText(tickerInfo.getString("last"));
                    tvHighest.setText(tickerInfo.getString("highestBid"));
                    tvLowest.setText(tickerInfo.getString("lowestAsk"));
                    edPrice.setText(tickerInfo.getString("last"));
                    break;
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error in processTradingPairs: " + ex.toString());
        }
    }

    public void RestorePairsInDB(Context c) {
        HashMap<String, String> params = new HashMap<>();
        params.put("command", "returnTicker");
        publicRequest(params, c, "restorePairsInDB");
    }

    public void RefreshBalances(Context c) {
        HashMap<String, String> params = new HashMap<>();
        params.put("command", "returnBalances");
        privateRequest(params, c, "refreshBalances");
    }

    public void UpdateBalanceBar(Context c) {
        HashMap<String, String> params = new HashMap<>();
        params.put("command", "returnBalances");
        privateRequest(params, c, "updateBalanceBar");
    }

    public void CancelOrder(Context c, String orderNumber) {
        Log.d(TAG, "CancelOrder: Order#: " + orderNumber);
        HashMap<String, String> params = new HashMap<>();
        params.put("command", "cancelOrder");
        params.put("orderNumber", orderNumber);
        privateRequest(params, c, "cancelOrder");
    }

    public void UpdateOpenOrders(Context c) {
        HashMap<String, String> params = new HashMap<>();
        params.put("command", "returnOpenOrders");
        params.put("currencyPair", "all");
        privateRequest(params, c, "updateOpenOrders");
    }

    public void UpdateTickerActivity(Context c) {
        HashMap<String, String> params = new HashMap<>();
        params.put("command", "returnTicker");
        publicRequest(params, c, "updateTickerActivity");
    }

    public void UpdateTradeTickerInfo(Context c) {
        HashMap<String, String> params = new HashMap<>();
        params.put("command", "returnTicker");
        publicRequest(params, c, "updateTradeTickerInfo");
    }

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType) {
        HashMap<String, String> params = new HashMap<>();
        params.put("command", orderType);
        params.put("currencyPair", pair);
        params.put("rate", rate);
        params.put("amount", amount);
        privateRequest(params, c, "placeOrder");
    }

    private static String createTradePair(String pair) {
        String[] parts = pair.split("_");
        return (parts[0] + "-" + parts[1]).toUpperCase();
    }

    private String createSignature(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(this.apiSecret.getBytes(), "HmacSHA512"));
            final byte[] macData = mac.doFinal(body.getBytes());
            return new String(Hex.encodeHex(macData));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private static long generateNonce() {
        Date d = new Date();
        return d.getTime();
    }

    private static String createBody(HashMap<String, String> params) {
        String body = "";
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (!body.isEmpty()) {
                body += "&";
            }
            body += param.getKey() + "=" + param.getValue();
        }
        return body;
    }

    public long getId() {
        return exchangeId;
    }

    public void setId(long exchangeId) {
        this.exchangeId = exchangeId;
    }

    public long getTypeId() {
        return typeId;
    }

    public void setTypeId(long typeId) {
        this.typeId = typeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAPIKey() {
        return apiKey;
    }

    public void setAPIKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAPISecret() {
        return apiSecret;
    }

    public void setAPISecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    @Override
    public String toString() {
        return this.name.toString();
    }
}