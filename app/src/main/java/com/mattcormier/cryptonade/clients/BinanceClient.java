package com.mattcormier.cryptonade.clients;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mattcormier.cryptonade.BalancesFragment;
import com.mattcormier.cryptonade.MainActivity;
import com.mattcormier.cryptonade.OpenOrdersFragment;
import com.mattcormier.cryptonade.OrderBookFragment;
import com.mattcormier.cryptonade.PairsFragment;
import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.TransactionsFragment;
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
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Filename: BinanceClient.java
 * Description: API Client for Binance exchange API requests.
 * Created by Matt Cormier on 10/29/2017.
**/

public class BinanceClient implements APIClient {
    private static final String TAG = "BinanceClient";
    private static long TYPE_ID = 9;
    private long exchangeId;
    private HashMap<String, Double> balances;
    private HashMap<String, Double> availableBalances;
    private HashMap<String, String> tickerInfo;
    private String name;
    private String apiKey;
    private String apiSecret;
    private static String baseUrl = "https://api.binance.com";

    public BinanceClient(int exchangeId, String name, String apiKey, String apiSecret) {
        this.exchangeId = exchangeId;
        this.name = name;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
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
        if(apiKey.isEmpty() || apiSecret.isEmpty()) {
            Toast.makeText(c, c.getResources().getString(R.string.invalid_api_msg), Toast.LENGTH_SHORT).show();
            return;
        }
        String url = baseUrl + endpoint;
        if (params == null) {
            params = new HashMap<>();
        }

        String timestamp = createTimestamp();
        params.put("timestamp", timestamp);
        params.put("recvWindow", "20000");

        String signature = createSignature(createBody(params));
        params.put("signature", signature);

        Log.d(TAG, "privateRequest: url: " + url);
        RequestQueue queue = Volley.newRequestQueue(c);

        int requestMethedInt = 0;
        String body = null;
        if (method.equals("GET")) {
            url += "?" + createBody(params);
            requestMethedInt = Request.Method.GET;
        }
        else if (method.equals("POST")){
            body = createBody(params);
            requestMethedInt = Request.Method.POST;
        }
        else if (method.equals("DELETE")) {
            url += "?" + createBody(params);
            requestMethedInt = Request.Method.DELETE;
        }
        final String getBody = body;

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
                        else if (cmd.equals("checkOpenOrder")) {
                            processCheckOpenOrder(response, c);
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
                                Log.e(TAG, "onErrorResponse: " + jsonObject.getString("msg"));
                                Toast.makeText(c, jsonObject.getString("msg"), Toast.LENGTH_LONG).show();
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
                    return getBody == null ? null : getBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.d("BalanceRequest", "Unsupported Encoding while trying to get the bytes of " + getBody + "using utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-MBX-APIKEY", apiKey);
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private String createSignature(String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256"));
            final byte[] macData = mac.doFinal(msg.getBytes());
            return new String(Hex.encodeHex(macData));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private static String createTimestamp() {
        Date d = new Date();
        return Long.toString(d.getTime());
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
        try {
            ArrayList<OrderTransaction> orderTransactionsList = new ArrayList<>();
            JSONArray jsonResult = new JSONArray(response);
            for (int i=0; i < jsonResult.length(); i++){
                JSONObject json = jsonResult.getJSONObject(i);
                String orderNumber = json.getString("id");

                String timestamp = Crypto.formatDate(Double.toString(Double.parseDouble(json.getString("time")) / 1000));
                String type;
                if (json.getBoolean("isBuyer")) {
                    type = "Buy";
                } else {
                    type = "Sell";
                }
                String amount = json.getString("qty");
                String rate = json.getString("price");
                String fee = json.getString("commission");
                OrderTransaction order = new OrderTransaction(orderNumber, timestamp, type,
                        amount, rate, fee);
                Log.d(TAG, "processUpdateOrderTransactions: added: " + order.toString());
                orderTransactionsList.add(order);
            }
            ((TransactionsFragment)((MainActivity) c).getFragment("transactions")).updateTransactionsList(orderTransactionsList);
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
            JSONArray jsonArray = jsonResponse.getJSONArray("balances");
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                String currency = json.getString("asset");
                Double available = json.getDouble("free");
                Double balance = available + json.getDouble("locked");
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
        BalancesFragment balFrag = (BalancesFragment)((MainActivity)c).getSupportFragmentManager().findFragmentByTag("balances");
        if (balFrag != null && balFrag.isVisible()) {
            balFrag.refreshBalances();
        }

    }

    private void processRestorePairsInDB(String response, Context c) {
        Log.d(TAG, "processRestorePairsInDB: response: " + response);
        CryptoDB db = new CryptoDB(c);
        db.deletePairsByExchangeId(exchangeId);
        try {
            ArrayList<Pair> pairsList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(response);
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.optJSONObject(i);
                String exchangePair = json.getString("symbol");
                String tradingPair = createTradePair(exchangePair);
                Pair pair = new Pair(0, (int)exchangeId, exchangePair, tradingPair);
                pairsList.add(pair);
            }
            db.insertPairs(pairsList);
            PairsFragment pairsFrag = (PairsFragment)((MainActivity)c).getSupportFragmentManager().findFragmentByTag("pairs");
            if (pairsFrag != null && pairsFrag.isVisible()) {
                pairsFrag.updatePairsListView();
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error in processRestorePairsInDB: " + ex.toString());
        }
    }

    private void processPlacedOrder(String response, Context c) {
        Log.d(TAG, "processPlacedOrder: response");
        try {
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.has("orderId")) {
                Toast.makeText(c, c.getResources().getString(R.string.order_successfully_placed) +
                        jsonResponse.getString("orderId"), Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(c, jsonResponse.getString("msg"), Toast.LENGTH_LONG).show();
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

    private void processCheckOpenOrder(String response, Context c) {
        Log.d(TAG, "processCancelOrder: response" + response);
        try {
            JSONObject jsonResponse = new JSONObject(response);
            String status = jsonResponse.getString("status");
            if (!status.equalsIgnoreCase("NEW") && !status.equalsIgnoreCase("PARTIALLY_FILLED")) {
                Crypto.openOrderClosed(c, jsonResponse.getString("orderId"), this.getName(),
                        jsonResponse.getString("origQty"), jsonResponse.getString("price"),
                        jsonResponse.getString("symbol"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "processCheckOpenOrder: " + e.getMessage());
        }
    }

    private void processUpdateOpenOrders(String response, Context c) {
        Log.d(TAG, "processUpdateOpenOrders: ");
        try {
            JSONArray jsonResult = new JSONArray(response);
            ArrayList<OpenOrder> openOrdersList = new ArrayList<>();
            for (int i=0; i < jsonResult.length(); i++){
                JSONObject json = jsonResult.getJSONObject(i);
                String orderNumber = json.getString("orderId");
                String orderPair = json.getString("symbol");
                String orderType = json.getString("side");
                String orderRate = json.getString("price");
                String orderStartingAmount = json.getString("origQty");
                String orderRemainingAmount = String.format("%.8f", Double.parseDouble(orderStartingAmount) - Double.parseDouble(json.getString("executedQty")));
                String orderDate = Crypto.formatDate(Double.toString(Double.parseDouble(json.getString("time")) / 1000));
                OpenOrder order = new OpenOrder((int)exchangeId, orderNumber, orderPair, orderType.toUpperCase(),
                        orderRate, orderStartingAmount, orderRemainingAmount, orderDate);
                openOrdersList.add(order);
            }
            ((OpenOrdersFragment)((MainActivity) c).getFragment("open_orders")).updateOpenOrdersList(openOrdersList);

        } catch (JSONException e) {
            Log.e(TAG, "processUpdateOpenOrders: JSONException Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processUpdateOpenOrders: Exception: " + e.getMessage());
        }
    }

    private static void processUpdateTickerActivity(String response, Context c) {
        ListView lvTickerList = ((Activity) c).findViewById(R.id.lvTickerList);
        try {
            JSONArray jsonResponse = new JSONArray(response);
            ArrayList<Ticker> tickerList = new ArrayList<>();

            for(int i=0; i < jsonResponse.length(); i++) {
                JSONObject json = jsonResponse.getJSONObject(i);
                String tickerPair = createTradePair(json.getString("symbol"));
                String last = "";
                String volume = "";
                String lowestAsk = json.getString("askPrice");
                String lowest24hr = "";
                String highestBid = json.getString("bidPrice");
                String highest24hr = "";
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
            tickerInfo.put("Last", jsonTicker.getString("lastPrice"));
            tickerInfo.put("Bid", jsonTicker.getString("bidPrice"));
            tickerInfo.put("Ask", jsonTicker.getString("askPrice"));
        } catch (JSONException ex) {
            Log.e(TAG, "Error in processUpdateTickerInfo: JSONException Error: " + ex.getMessage());
        } catch (Exception ex) {
            Log.e(TAG, "Error in processUpdateTickerInfo: Exception Error: " + ex.getMessage());
        }
    }

    private void processRefreshOrderBooks(String response, Context c) {
        Log.d(TAG, "processRefreshOrderBooks: starts");
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

            // Update order book list views
            OrderBookFragment orderBookFragment = (OrderBookFragment)((MainActivity)c).getSupportFragmentManager().findFragmentByTag("order_book");
            if(orderBookFragment != null && orderBookFragment.isVisible()) {
                orderBookFragment.updateAsksList(asksList);
                orderBookFragment.updateBidsList(bidsList);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "processRefreshOrderBooks: JSONException: " + e.getMessage());
        }
    }

    public void RestorePairsInDB(Context c) {
        String endpoint = "/api/v1/ticker/allPrices";
        publicRequest(endpoint, null, c, "restorePairsInDB");
    }

    public void UpdateBalances(Context c) {
        String endpoint = "/api/v3/account";
        String method = "GET";
        privateRequest(endpoint, null, method, c, "updateBalances");
    }

    public void CancelOrder(Context c, String orderNumber) {
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        Log.d(TAG, "CancelOrder: Order#: " + orderNumber);
        String endpoint = "/api/v3/order";

        HashMap<String, String> params = new HashMap<>();
        params.put("symbol", selectedPair.getExchangePair());
        params.put("orderId", orderNumber);

        String method = "DELETE";
        privateRequest(endpoint, params, method, c, "cancelOrder");
    }

    public void UpdateOpenOrders(Context c) {
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        String endpoint = "/api/v3/openOrders";

        HashMap<String, String> params = new HashMap<>();
        params.put("symbol", selectedPair.getExchangePair());

        String method = "GET";
        privateRequest(endpoint, params, method, c, "updateOpenOrders");
    }

    public void UpdateOrderTransactions(Context c, String pair) {
        String endpoint = "/api/v3/myTrades";

        HashMap<String, String> params = new HashMap<>();
        params.put("symbol", pair);

        String method = "GET";
        privateRequest(endpoint, params, method, c, "updateOrderTransactions");
    }

    public void UpdateTickerActivity(Context c) {
        String endpoint = "/api/v1/ticker/allBookTickers";
        Toast.makeText(c, "This feature is not fully support on this exchange", Toast.LENGTH_LONG).show();
        publicRequest(endpoint, null, c, "updateTickerActivity");
    }

    public void UpdateTickerInfo(Context c, String pair) {
        String endpoint = "/api/v1/ticker/24hr?symbol=" + pair;
        publicRequest(endpoint, null, c, "updateTickerInfo");
    }

    public void RefreshOrderBooks(Context c, String pair) {
        String endpoint = "/api/v1/depth?symbol=" + pair;
        publicRequest(endpoint, null, c, "refreshOrderBooks");
    }

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType) {
        String endpoint = "/api/v3/order";
        String method = "POST";
        HashMap<String, String> params = new HashMap<>();
        params.put("quantity", amount);
        params.put("price", rate);
        params.put("side", orderType.toUpperCase());
        params.put("symbol", pair);
        params.put("type", "LIMIT");
        params.put("timeInForce", "GTC");
        privateRequest(endpoint, params, method, c, "placeOrder");
    }

    public void CheckOpenOrder(Context c, String orderNumber, String symbol) {
        Log.d(TAG, "CheckOpenOrder: " + orderNumber);
        String endpoint = "/api/v3/order";

        HashMap<String, String> params = new HashMap<>();
        params.put("orderId", orderNumber);
        params.put("symbol", symbol);

        String method = "GET";
        privateRequest(endpoint, params, method, c, "checkOpenOrder");
    }

    private static String createTradePair(String pair) {
        String minor;
        String major = pair.substring(pair.length() - 3);
        if (major.equalsIgnoreCase("SDT")) {
            major = pair.substring(pair.length() - 4);
            minor = pair.substring(0, pair.length() - 4);
        }else {
            minor = pair.substring(0, pair.length() - 3);
        }
        return major + "-" + minor;
    }

    public long getId() {
        return exchangeId;
    }

    public void setId(long exchangeId) {
        this.exchangeId = exchangeId;
    }

    public long getTypeId() {
        return TYPE_ID;
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
