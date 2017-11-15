package com.mattcormier.cryptonade.clients;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.TradeFragment;
import com.mattcormier.cryptonade.adapters.OpenOrdersAdapter;
import com.mattcormier.cryptonade.adapters.OrderTransactionsAdapter;
import com.mattcormier.cryptonade.adapters.TickerAdapter;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.lib.Crypto;
import com.mattcormier.cryptonade.models.OpenOrder;
import com.mattcormier.cryptonade.models.OrderTransaction;
import com.mattcormier.cryptonade.models.Pair;
import com.mattcormier.cryptonade.models.Ticker;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CexioClient implements APIClient {
    private static final String TAG = "CexioClient";
    private static long typeId = 5;
    private long exchangeId;
    private HashMap<String, Double> balances;
    private HashMap<String, Double> availableBalances;
    private HashMap<String, String> tickerInfo;
    private String name;
    private String apiKey;
    private String apiSecret;
    private String apiUsername;
    private static String baseUrl = "https://cex.io/api";

    public CexioClient(int exchangeId, String name, String apiKey, String apiSecret, String apiUsername) {
        this.exchangeId = exchangeId;
        this.name = name;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiUsername = apiUsername;
    }

    private void publicRequest(String endpoint, HashMap<String, String> params, final Context c, final String cmd) {
        Log.d(TAG, "publicRequest: " + cmd);
        String url = baseUrl + endpoint + createBody(params);
        RequestQueue queue = Volley.newRequestQueue(c);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d(TAG, "onResponse: " + response);
                            if (cmd.equals("restorePairsInDB")) {
                                processRestorePairsInDB(response, c);
                            }
                            else if (cmd.equals("updateTickerActivity")) {
                                processUpdateTickerActivity(response, c);
                            }
                            else if (cmd.equals("updateTickerInfo")) {
                                processUpdateTickerInfo(response, c);
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
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null && networkResponse.data != null) {
                            String jsonError = new String(networkResponse.data);
                            Log.e(TAG, "onErrorResponse: " + jsonError);
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    private void privateRequest(String endpoint, HashMap<String, String> params, final Context c, final String cmd) {
        Log.d(TAG, "privateRequest: " + cmd);
        String url = baseUrl + endpoint;
        Log.d(TAG, "privateRequest: url: " +url);
        final String nonce = Long.toString(generateNonce());

        String msg = nonce + this.apiUsername + this.apiKey;
        final String signature = createSignature(msg).toUpperCase();

        if (params == null) {
            params = new HashMap<>();
        }
        params.put("key", this.apiKey);
        params.put("nonce", nonce);
        params.put("signature", signature);

        final String body = createBody(params);

        RequestQueue queue = Volley.newRequestQueue(c);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: response: " + response);
                        if (cmd.equals("updateBalances")) {
                            processUpdateBalances(response, c);
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
                        else if (cmd.equals("updateOrderTransactions")) {
                            processUpdateOrderTransactions(response, c);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        Log.d(TAG, "StringRequire.onErrorResponse: " + ex.getMessage());
                        NetworkResponse networkResponse = ex.networkResponse;
                        if (networkResponse != null && networkResponse.data != null) {
                            String jsonError = new String(networkResponse.data);
                            Log.e(TAG, "onErrorResponse: " + jsonError);
                        }
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

//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String> headers = new HashMap<>();
//                headers.put("apisign", signature);
//                return headers;
//            }
        };
        queue.add(stringRequest);
    }

    private String createSignature(String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(this.apiSecret.getBytes("utf-8"), "HmacSHA256"));
            final byte[] macData = mac.doFinal(msg.getBytes("utf-8"));
            return new String(Hex.encodeHex(macData));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private static long generateNonce() {
        Date d = new Date();
        return d.getTime() * 1000;
    }

    private static String createBody(HashMap<String, String> params) {
        if (params == null) {
            return "";
        }
        String body = "";
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (!body.isEmpty()) {
                body += "&";
            }
            body += param.getKey() + "=" + param.getValue();
        }
        return body;
    }

    private void processUpdateOrderTransactions(String response, Context c) {
        Log.d(TAG, "processUpdateOrderTransactions: response: " + response);
        ListView lvOrderTransactions = ((Activity) c).findViewById(R.id.lvOrdertransactions);
        try {
            ArrayList<OrderTransaction> orderTransactionsList = new ArrayList<>();
            JSONArray jsonResult = new JSONArray(response);
            for (int i=0; i < jsonResult.length(); i++){
                JSONObject json = jsonResult.getJSONObject(i);
                String orderNumber = json.getString("id");
                String timestamp = json.getString("time");
                String type = json.getString("type");
                String amount = json.getString("amount");
                String rate = json.getString("price");
                String fee = "";
                OrderTransaction order = new OrderTransaction(orderNumber, timestamp, type,
                        amount, rate, fee);
                Log.d(TAG, "processUpdateOrderTransactions: added: " + order.toString());
                orderTransactionsList.add(order);
            }

            OrderTransactionsAdapter orderTransactionsAdapter = new OrderTransactionsAdapter(c, R.layout.listitem_order_transaction, orderTransactionsList);
            lvOrderTransactions.setAdapter(orderTransactionsAdapter);
            UpdateOpenOrders(c);
        } catch (JSONException e) {
            Log.e(TAG, "processUpdateOrderTransactions: JSONException Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processUpdateOrderTransactions: Exception: " + e.getMessage());
        }
    }

    private void processUpdateBalances(String response, Context c) {
        Log.d(TAG, "processUpdateBalances: response: " + response);
        HashMap<String, Double> availableBalances = new HashMap<>();
        HashMap<String, Double> balances = new HashMap<>();
        try {
            JSONObject jsonResponse = new JSONObject(response);
            Iterator<String> keys = jsonResponse.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                if (key.equals("timestamp") || key.equals("username")) {
                    continue;
                }
                JSONObject jsonBalance = jsonResponse.getJSONObject(key);
                String currency = key;
                Double available = Double.parseDouble(jsonBalance.getString("available"));
                Double balance;
                if (jsonBalance.has("orders")) {
                    Double onOrders = Double.parseDouble(jsonBalance.getString("orders"));
                    if (onOrders > 0) {
                        balance = available + onOrders;
                    } else {
                        balance = available;
                    }
                } else {
                    balance = available;
                }
                if (balance > 0) {
                    availableBalances.put(currency, available);
                    balances.put(currency, balance);
                }
            }
            this.availableBalances = availableBalances;
            this.balances = balances;
        } catch (JSONException e) {
            Log.e(TAG, "processUpdateBalances: JSONException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processUpdateBalances: " + e.getMessage());
        }
    }

    private void processRestorePairsInDB(String response, Context c) {
        CryptoDB db = new CryptoDB(c);
        db.deletePairsByExchangeId(exchangeId);
        try {
            ArrayList<Pair> pairsList = new ArrayList<>();
            JSONObject jsonResponse = new JSONObject(response);
            JSONObject jsonData = jsonResponse.getJSONObject("data");
            JSONArray jsonArray = jsonData.getJSONArray("pairs");
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.optJSONObject(i);
                String tradingPair;
                if (json.getString("symbol2").equalsIgnoreCase("btc")) {
                    tradingPair = json.getString("symbol2") + "-" + json.getString("symbol1");
                } else {
                    tradingPair = json.getString("symbol1") + "-" + json.getString("symbol2");
                }
                String exchangePair = json.getString("symbol1") + "/" + json.getString("symbol2");;
                Pair pair = new Pair(0, (int)exchangeId, exchangePair, tradingPair);
                pairsList.add(pair);
            }
            db.insertPairs(pairsList);
        } catch (Exception ex) {
            Log.d(TAG, "Error in processTradingPairs: " + ex.toString());
        }
    }

    private void processPlacedOrder(String response, Context c) {
        Log.d(TAG, "processPlacedOrder: response");
        JSONObject jsonResponse;
        try {
            jsonResponse = new JSONObject(response);
            if (jsonResponse.has("id")) {
                Toast.makeText(c, c.getResources().getString(R.string.order_successfully_placed) +
                        jsonResponse.getString("id"), Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(c, jsonResponse.getString("message"), Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            Toast.makeText(c, "Unknown Error happened!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "JSONException error in processPlacedOrder: " + e.toString());
        }
    }

    private void processCancelOrder(String response, Context c) {
        Log.d(TAG, "processCancelOrder: response" + response);
        if (response.equals("true")) {
            Toast.makeText(c, c.getResources().getString(R.string.order_successfully_cancelled), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(c, "Error: " + response, Toast.LENGTH_LONG).show();
            Log.e(TAG, "processCancelOrder: Unknown response:" + response);
        }
        UpdateOpenOrders(c);
    }

    private void processUpdateOpenOrders(String response, Context c) {
        Log.d(TAG, "processUpdateOpenOrders: ");
        ListView lvOpenOrders = ((Activity) c).findViewById(R.id.lvOpenOrders);
        try {
            JSONArray jsonResult = new JSONArray(response);
            ArrayList<OpenOrder> openOrdersList = new ArrayList<>();
            for (int i=0; i < jsonResult.length(); i++){
                JSONObject json = jsonResult.getJSONObject(i);
                String orderNumber = json.getString("id");
                String orderPair = "";
                String orderType = json.getString("type");
                String orderRate = json.getString("price");
                String orderStartingAmount = json.getString("amount");
                String orderRemainingAmount = json.getString("pending");
                String orderDate = Crypto.formatDate(Long.toString(Long.parseLong(json.getString("time"))/1000));
                OpenOrder order = new OpenOrder(orderNumber, orderPair, orderType.toUpperCase(),
                        orderRate, orderStartingAmount, orderRemainingAmount, orderDate);
                openOrdersList.add(order);
            }
            OpenOrdersAdapter openOrdersAdapter = new OpenOrdersAdapter(c, R.layout.listitem_openorder, openOrdersList);
            lvOpenOrders.setAdapter(openOrdersAdapter);

        } catch (JSONException e) {
            Log.e(TAG, "processUpdateOpenOrders: JSONException Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processUpdateOpenOrders: Exception: " + e.getMessage());
        }
    }

    private static void processUpdateTickerActivity(String response, Context c) {
        Log.d(TAG, "processUpdateTickerActivity: " + response);
        ListView lvTickerList = ((Activity) c).findViewById(R.id.lvTickerList);
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray jsonResult = jsonResponse.getJSONArray("data");
            ArrayList<Ticker> tickerList = new ArrayList<>();
            for (int i=0; i < jsonResult.length(); i++) {
                JSONObject json = jsonResult.getJSONObject(i);
                String tickerPair = json.getString("pair");
                String []tickerSplit = tickerPair.split(":");
                if (tickerSplit[1].equals("BTC")) {
                    tickerPair = tickerSplit[1] + "-" + tickerSplit[0];
                } else {
                    tickerPair = tickerSplit[0] + "-" + tickerSplit[1];
                }
                String last = json.getString("last");
                String volume = json.getString("volume");
                String lowestAsk = "";
                if (json.has("ask")) {
                    lowestAsk = json.getString("ask");
                }
                String lowest24hr = json.getString("low");
                String highestBid = "";
                if (json.has("bid")) {
                    highestBid = json.getString("bid");
                }
                String highest24hr = json.getString("high");
                Ticker ticker = new Ticker(tickerPair, last, volume,
                        lowestAsk, lowest24hr, highestBid, highest24hr);
                tickerList.add(ticker);
            }
            TickerAdapter tickerAdapter = new TickerAdapter(c, R.layout.listitem_ticker, tickerList);
            lvTickerList.setAdapter(tickerAdapter);

        } catch (JSONException e) {
            Log.e(TAG, "processUpdateTickerActivity: JSONException Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processUpdateTickerActivity: Exception: " + e.getMessage());
        }
    }

    private void processUpdateTickerInfo(String response, Context c) {
        try {
            JSONObject jsonTicker = new JSONObject(response);
            tickerInfo = new HashMap<>();
            tickerInfo.put("Last", jsonTicker.getString("last"));
            tickerInfo.put("Bid", jsonTicker.getString("bid"));
            tickerInfo.put("Ask", jsonTicker.getString("ask"));
        } catch (JSONException ex) {
            Log.e(TAG, "Error in processUpdateTickerInfo: JSONException Error: " + ex.getMessage());
        } catch (Exception ex) {
            Log.e(TAG, "Error in processUpdateTickerInfo: Exception Error: " + ex.getMessage());
        }
    }

    public void RestorePairsInDB(Context c) {
        String endpoint = "/currency_limits";
        publicRequest(endpoint, null, c, "restorePairsInDB");
    }

    public void UpdateBalances(Context c) {
        String endpoint = "/balance/";
        privateRequest(endpoint, null, c, "updateBalances");
    }

    public void CancelOrder(Context c, String orderNumber) {
        Log.d(TAG, "CancelOrder: Order#: " + orderNumber);
        String endpoint = "/cancel_order/";
        HashMap<String, String> params = new HashMap<>();
        params.put("id", orderNumber);
        privateRequest(endpoint, params, c, "cancelOrder");
    }

    public void UpdateOpenOrders(Context c) {
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        String endpoint = "/open_orders/" + selectedPair.getExchangePair();
        privateRequest(endpoint, null, c, "updateOpenOrders");
    }

    public void UpdateOrderTransactions(Context c, String pair) {
        String endpoint = "/archived_orders/" + pair;
        HashMap<String, String> params = new HashMap<>();
        params.put("limit", "50");
        params.put("status", "d");
        privateRequest(endpoint, params, c, "updateOrderTransactions");
    }

    public void UpdateTickerActivity(Context c) {
        Log.d(TAG, "UpdateTickerActivity: ");
        String endpoint = "/tickers/BTC/ETH/CAD/RUB/USD/DASH/ZEC/BCH/GBP/EUR";
        publicRequest(endpoint, null, c, "updateTickerActivity");
    }

    public void UpdateTickerInfo(Context c, String pair) {
        String endpoint = "/ticker/" + pair;
        publicRequest(endpoint, null, c, "updateTickerInfo");
    }

    public void RefreshOrderBooks(Context c, String pair) {
        // TODO
    }

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType) {
        String endpoint = "/place_order/" + pair;
        HashMap<String, String> params = new HashMap<>();
        params.put("type", orderType);
        params.put("amount", amount);
        params.put("price", rate);
        privateRequest(endpoint, params, c, "placeOrder");
    }

    private static String createTradePair(String pair) {
        String[] parts = pair.split("/");
        if (parts[1].equals("BTC")) {
            return (parts[1] + "-" + parts[0]).toUpperCase();
        } else {
            return (parts[0] + "-" + parts[1]).toUpperCase();
        }

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

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public HashMap<String, Double> getBalances() {
        return balances;
    }

    public HashMap<String, Double> getAvailableBalances() {
        return availableBalances;
    }

    public HashMap<String, String> getTickerInfo() {
        return tickerInfo;
    }
}
