package com.mattcormier.cryptonade.clients;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
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
import com.mattcormier.cryptonade.OrderBookFragment;
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

public class GDAXClient implements APIClient {
    private static final String TAG = "GDAXClient";
    private static long typeId = 5;
    private long exchangeId;
    private HashMap<String, Double> balances;
    private HashMap<String, Double> availableBalances;
    private HashMap<String, String> tickerInfo;
    private String name;
    private String apiKey;
    private String apiSecret;
    private String apiPassphrase;
    private static String baseUrl = "https://api.gdax.com";

    public GDAXClient(int exchangeId, String name, String apiKey, String apiSecret, String apiPassphrase) {
        this.exchangeId = exchangeId;
        this.name = name;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiPassphrase = apiPassphrase;
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
                            else if (cmd.equals("refreshOrderBooks")) {
                                processRefreshOrderBooks(response, c);
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
                            try {
                                JSONObject jsonObject = new JSONObject(jsonError);
                                Log.e(TAG, "onErrorResponse: " + jsonObject.getString("message"));
                                Toast.makeText(c, jsonObject.getString("message"), Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Log.e(TAG, "onErrorResponse: " + jsonError);
                                Toast.makeText(c, jsonError, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
        );
        queue.add(stringRequest);
    }

    private void privateRequest(String endpoint, HashMap<String, String> params, String method, final Context c, final String cmd) {
        Log.d(TAG, "privateRequest: " + cmd);
        String url = baseUrl + endpoint;
        Log.d(TAG, "privateRequest: url: " + url);

        final String timestamp = createTimestamp();
        final String getBody = createBody(params);
        String json = "";
        if (params != null) {
            json = new JSONObject(params).toString();
        }
        final String jsonBody = json;

        Log.d(TAG, "privateRequest: body: " + jsonBody);
        String msg = timestamp + method + endpoint + jsonBody;
        final String signature = createSignature(msg);

        RequestQueue queue = Volley.newRequestQueue(c);

        int requestMethedInt = Request.Method.GET;
        if (method.equals("POST")){
            requestMethedInt = Request.Method.POST;
        }
        else if (method.equals("DELETE")) {
            requestMethedInt = Request.Method.DELETE;
        }
        StringRequest stringRequest = new StringRequest(requestMethedInt, url,
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
                            try {
                                JSONObject jsonObject = new JSONObject(jsonError);
                                Log.e(TAG, "onErrorResponse: " + jsonObject.getString("message"));
                                Toast.makeText(c, jsonObject.getString("message"), Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Log.e(TAG, "onErrorResponse: " + jsonError);
                                Toast.makeText(c, jsonError, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return jsonBody == null ? null : jsonBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.d("BalanceRequest", "Unsupported Encoding while trying to get the bytes of " + getBody + "using utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("CB-ACCESS-SIGN", signature);
                headers.put("CB-ACCESS-TIMESTAMP", timestamp);
                headers.put("CB-ACCESS-KEY", apiKey);
                headers.put("CB-ACCESS-PASSPHRASE", apiPassphrase);
                headers.put("Content-Type", "Application/JSON");
                Log.d(TAG, "getHeaders: " + headers.toString());
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private String createSignature(String msg) {
        try {
            byte[] secretDecoded = Base64.decode(apiSecret, Base64.DEFAULT);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretDecoded, "HmacSHA256"));
            final byte[] macData = mac.doFinal(msg.getBytes());
            return Base64.encodeToString(macData, Base64.NO_WRAP);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private static String createTimestamp() {
        Date d = new Date();
        return Long.toString(d.getTime() / 1000);
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
                String timestamp = json.getString("done_at");
                String type = json.getString("side");
                String amount = json.getString("size");
                String rate = json.getString("price");
                String fee = json.getString("fill_fees").substring(0, json.getString("fill_fees").indexOf('.') + 9);
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
            JSONArray jsonArray = new JSONArray(response);
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                String currency = json.getString("currency");
                Double available = json.getDouble("available");
                Double balance = json.getDouble("balance");
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
            JSONArray jsonArray = new JSONArray(response);
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.optJSONObject(i);
                String exchangePair = json.getString("id");
                String tradingPair = createTradePair(exchangePair);

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
        Toast.makeText(c, c.getResources().getString(R.string.order_successfully_cancelled), Toast.LENGTH_LONG).show();
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
                String orderType = json.getString("side");
                String orderRate = json.getString("price");
                String orderStartingAmount = json.getString("size");
                String orderRemainingAmount = String.format("%.8f", Double.parseDouble(json.getString("size")) - Double.parseDouble(json.getString("filled_size")));
                String orderDate = json.getString("created_at");
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
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        Log.d(TAG, "processUpdateTickerActivity: " + response);
        ListView lvTickerList = ((Activity) c).findViewById(R.id.lvTickerList);
        try {
            JSONObject jsonResponse = new JSONObject(response);
            ArrayList<Ticker> tickerList = new ArrayList<>();
            String tickerPair = selectedPair.getTradingPair();
            String last = jsonResponse.getString("price");
            String volume = jsonResponse.getString("volume");
            String lowestAsk = jsonResponse.getString("ask");
            String lowest24hr = "";
            String highestBid = jsonResponse.getString("bid");
            String highest24hr = "";
            Ticker ticker = new Ticker(tickerPair, last, volume,
                    lowestAsk, lowest24hr, highestBid, highest24hr);
            tickerList.add(ticker);
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
            tickerInfo.put("Last", jsonTicker.getString("price"));
            tickerInfo.put("Bid", jsonTicker.getString("bid"));
            tickerInfo.put("Ask", jsonTicker.getString("ask"));
        } catch (JSONException ex) {
            Log.e(TAG, "Error in processUpdateTickerInfo: JSONException Error: " + ex.getMessage());
        } catch (Exception ex) {
            Log.e(TAG, "Error in processUpdateTickerInfo: Exception Error: " + ex.getMessage());
        }
    }

    private void processRefreshOrderBooks(String response, Context c) {
        Log.d(TAG, "processRefreshOrderBooks: response: " + response);
        try {
            JSONObject jsonObject = new JSONObject(response);

            // Parse asks and update asks list
            JSONArray jsonAsks = jsonObject.getJSONArray("asks");
            ArrayList<HashMap<String, String>> asksList = new ArrayList<>();
            for(int i=0; i < jsonAsks.length(); i++) {
                JSONArray jsonAsk = jsonAsks.getJSONArray(i);
                String price = jsonAsk.getString(0);
                String amount = jsonAsk.getString(1);
                HashMap<String, String> ask = new HashMap<>();
                ask.put("price", price);
                ask.put("amount", amount);
                asksList.add(ask);
            }
            ((OrderBookFragment)((Activity)c).getFragmentManager().findFragmentByTag("order_book")).updateAsksList(asksList);

            // Parse bids and update bids list
            JSONArray jsonBids = jsonObject.getJSONArray("bids");
            ArrayList<HashMap<String, String>> bidsList = new ArrayList<>();
            for(int i=0; i < jsonBids.length(); i++) {
                JSONArray jsonBid = jsonBids.getJSONArray(i);
                String price = jsonBid.getString(0);
                String amount = jsonBid.getString(1);
                HashMap<String, String> bid = new HashMap<>();
                bid.put("price", price);
                bid.put("amount", amount);
                bidsList.add(bid);
            }
            ((OrderBookFragment)((Activity)c).getFragmentManager().findFragmentByTag("order_book")).updateBidsList(bidsList);
            Log.d(TAG, "processRefreshOrderBooks: arraylist " + bidsList.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "processRefreshOrderBooks: JSONException: " + e.getMessage());
        }
    }

    public void RestorePairsInDB(Context c) {
        String endpoint = "/products";
        publicRequest(endpoint, null, c, "restorePairsInDB");
    }

    public void UpdateBalances(Context c) {
        String endpoint = "/accounts/";
        String method = "GET";
        privateRequest(endpoint, null, method, c, "updateBalances");
    }

    public void CancelOrder(Context c, String orderNumber) {
        Log.d(TAG, "CancelOrder: Order#: " + orderNumber);
        String endpoint = "/orders/" + orderNumber;
        String method = "DELETE";
        privateRequest(endpoint, null, method, c, "cancelOrder");
    }

    public void UpdateOpenOrders(Context c) {
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        String endpoint = "/orders/" + "?product_id=" + selectedPair.getExchangePair();
        String method = "GET";
        privateRequest(endpoint, null, method, c, "updateOpenOrders");
    }

    public void UpdateOrderTransactions(Context c, String pair) {
        String endpoint = "/orders?status=done&status=pending";
        String method = "GET";
        privateRequest(endpoint, null, method, c, "updateOrderTransactions");
    }

    public void UpdateTickerActivity(Context c) {
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        Log.d(TAG, "UpdateTickerActivity: ");
        String endpoint = "/products/" + selectedPair.getExchangePair() + "/ticker";
        Toast.makeText(c, "This exchange only shows ticker information for select pair.", Toast.LENGTH_LONG).show();
        publicRequest(endpoint, null, c, "updateTickerActivity");
    }

    public void UpdateTickerInfo(Context c, String pair) {
        String endpoint = "/products/" + pair + "/ticker";
        publicRequest(endpoint, null, c, "updateTickerInfo");
    }

    public void RefreshOrderBooks(Context c, String pair) {
        String endpoint = "/products/" + pair + "/book?level=2";
        publicRequest(endpoint, null, c, "refreshOrderBooks");
    }

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType) {
        String endpoint = "/orders";
        String method = "POST";
        HashMap<String, String> params = new HashMap<>();
        params.put("size", amount);
        params.put("price", rate);
        params.put("side", orderType);
        params.put("product_id", pair);
        privateRequest(endpoint, params, method, c, "placeOrder");
    }

    private static String createTradePair(String pair) {
        String[] parts = pair.split("-");
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
