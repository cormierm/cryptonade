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
import com.mattcormier.cryptonade.BalancesFragment;
import com.mattcormier.cryptonade.OrderBookFragment;
import com.mattcormier.cryptonade.PairsFragment;
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

public class HitBTCClient implements APIClient {
    private static final String TAG = "HitBTCClient";
    private static long typeId = 8;
    private long exchangeId;
    private HashMap<String, Double> balances;
    private HashMap<String, Double> availableBalances;
    private HashMap<String, String> tickerInfo;
    private String name;
    private String apiKey;
    private String apiSecret;
    private static String baseUrl = "https://api.hitbtc.com";

    public HitBTCClient(int exchangeId, String name, String apiKey, String apiSecret) {
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

    private void privateRequest(String endpoint, HashMap<String, String> params, int method, final Context c, final String cmd) {
        Log.d(TAG, "privateRequest: " + cmd);
        if (params == null) {
            params = new HashMap<>();
        }
        final String body = createBody(params);
        String nonce = Long.toString(generateNonce());

        params.put("nonce", nonce);
        params.put("apikey", this.apiKey);

        String uri;
        String msg;
        if (method == Request.Method.GET)
        {
            uri = endpoint + "?" + createBody(params);
            msg = uri;

        } else {
            uri = endpoint + "?apikey=" + apiKey + "&nonce=" + nonce + "";
            msg = uri + "" + body;
        }

        String url = baseUrl + uri;
        Log.d(TAG, "privateRequest: msg: " + msg);
        final String signature = createSignature(msg).toLowerCase();
        Log.d(TAG, "privateRequest: body: " + body);
        Log.d(TAG, "privateRequest: uri: " + uri);

        RequestQueue queue = Volley.newRequestQueue(c);
        StringRequest stringRequest = new StringRequest(method, url,
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
                    return body == null ? null : body.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.d("BalanceRequest", "Unsupported Encoding while trying to get the bytes of " + body + "using utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-Signature", signature);
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private String createSignature(String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(this.apiSecret.getBytes("utf-8"), "HmacSHA512"));
            final byte[] macData = mac.doFinal(msg.getBytes("utf-8"));
            return new String(Hex.encodeHex(macData));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "createSignature: " + e.getMessage());
        }
        return null;
    }

    private static long generateNonce() {
        Date d = new Date();
        return d.getTime();
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
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray jsonResult = jsonResponse.getJSONArray("orders");
            for (int i=0; i < jsonResult.length(); i++){
                JSONObject json = jsonResult.getJSONObject(i);
                String orderNumber = json.getString("clientOrderId");
                String timestamp = Crypto.formatDate(Long.toString(Long.parseLong(json.getString("lastTimestamp"))/1000));
                String type = json.getString("side");
                String amount = String.format("%.8f", Double.parseDouble(json.getString("orderQuantity")) / 1000);
                String rate = json.getString("orderPrice");
                String fee = "";
                OrderTransaction order = new OrderTransaction(orderNumber, timestamp, type,
                        amount, rate, fee);
                Log.d(TAG, "processUpdateOrderTransactions: added: " + order.toString());
                orderTransactionsList.add(order);
            }

            OrderTransactionsAdapter orderTransactionsAdapter = new OrderTransactionsAdapter(c, R.layout.listitem_order_transaction, orderTransactionsList);
            lvOrderTransactions.setAdapter(orderTransactionsAdapter);

        } catch (JSONException e) {
            Log.e(TAG, "processUpdateOrderTransactions: JSONException Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processUpdateOrderTransactions: Exception: " + e.getMessage());
        }
    }

    private void processUpdateBalances(String response, Context c) {
        Log.d(TAG, "processUpdateBalances:" );
        HashMap<String, Double> availableBalances = new HashMap<>();
        HashMap<String, Double> balances = new HashMap<>();
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray jsonArray = jsonResponse.getJSONArray("balance");
            for (int i=0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.optJSONObject(i);
                String currency = json.getString("currency_code");
                Double available = Double.parseDouble(json.getString("cash"));
                Double balance = available + Double.parseDouble(json.getString("reserved"));
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
        BalancesFragment balFrag = (BalancesFragment)((Activity) c).getFragmentManager().findFragmentByTag("balances");
        if (balFrag != null) {
            balFrag.refreshBalances();
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
            PairsFragment pairsFrag = (PairsFragment)((Activity)c).getFragmentManager().findFragmentByTag("pairs");
            if (pairsFrag != null) {
                pairsFrag.updatePairsListView();
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error in processTradingPairs: " + ex.toString());
        }
    }

    private void processPlacedOrder(String response, Context c) {
        Log.d(TAG, "processPlacedOrder: response");
        JSONObject jsonResponse;
        try {
            jsonResponse = new JSONObject(response);
            if (jsonResponse.has("ExecutionReport")) {
                JSONObject jsonReport = jsonResponse.getJSONObject("ExecutionReport");
                if (jsonReport.has("orderRejectReason")) {
                    Toast.makeText(c, jsonReport.getString("orderRejectReason"), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "processPlacedOrder: ORDER REJECTED: " + jsonReport.getString("orderRejectReason"));
                } else {
                    Toast.makeText(c, c.getResources().getString(R.string.order_successfully_placed) +
                            jsonReport.getString("orderId"), Toast.LENGTH_LONG).show();
                }

            }
            else {
                Toast.makeText(c, "Unknown Error", Toast.LENGTH_LONG).show();
                Log.e(TAG, "processPlacedOrder: Unexpected Response: " + response);
            }
        } catch (JSONException e) {
            Toast.makeText(c, "Unknown Error happened!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "JSONException error in processPlacedOrder: " + e.toString());
        }
    }

    private void processCancelOrder(String response, Context c) {
        Log.d(TAG, "processCancelOrder: response" + response);
        try {
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.has("ExecutionReport")) {
                Toast.makeText(c, c.getResources().getString(R.string.order_successfully_cancelled), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(c, "Error: " + response, Toast.LENGTH_LONG).show();
                Log.e(TAG, "processCancelOrder: Unknown response:" + response);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "processCancelOrder: " + e.getMessage());
        }
        UpdateOpenOrders(c);
    }

    private void processUpdateOpenOrders(String response, Context c) {
        Log.d(TAG, "processUpdateOpenOrders: ");
        ListView lvOpenOrders = ((Activity) c).findViewById(R.id.lvOpenOrders);
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray jsonResult = jsonResponse.getJSONArray("orders");
            ArrayList<OpenOrder> openOrdersList = new ArrayList<>();
            for (int i=0; i < jsonResult.length(); i++){
                JSONObject json = jsonResult.getJSONObject(i);
                String orderNumber = json.getString("clientOrderId");
                String orderPair = json.getString("symbol");
                String orderType = json.getString("side");
                String orderRate = json.getString("orderPrice");
                String orderStartingAmount = String.format("%.8f", Double.parseDouble(json.getString("orderQuantity")) / 1000);
                String orderRemainingAmount = String.format("%.8f", Double.parseDouble(json.getString("quantityLeaves")) / 1000);
                String orderDate = Crypto.formatDate(Long.toString(Long.parseLong(json.getString("lastTimestamp"))/1000));
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
            JSONArray jsonArray = new JSONArray(response);
            ArrayList<Ticker> tickerList = new ArrayList<>();
            for (int i=0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                String tickerPair = json.getString("symbol");
                if (tickerPair.substring(3).equals("BTC")) {
                    tickerPair = tickerPair.substring(3) + "-" + tickerPair.substring(0,3);
                } else {
                    tickerPair = tickerPair.substring(0,3) + "-" + tickerPair.substring(3);
                }
                String last = json.getString("last");
                String volume = json.getString("volume");
                String lowest24hr = json.getString("low");
                String lowestAsk = json.getString("ask");
                String highestBid = json.getString("bid");
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

    private void processRefreshOrderBooks(String response, Context c) {
        Log.d(TAG, "refreshOrderBooks: starts");
        try {
            JSONObject jsonObject = new JSONObject(response);

            // Parse asks and update asks list
            JSONArray jsonAsks = jsonObject.getJSONArray("ask");
            ArrayList<HashMap<String, String>> asksList = new ArrayList<>();
            for(int i=0; i < jsonAsks.length(); i++) {
                JSONObject jsonAsk = jsonAsks.getJSONObject(i);
                String price = jsonAsk.getString("price");
                String amount = jsonAsk.getString("size");
                HashMap<String, String> ask = new HashMap<>();
                ask.put("price", price);
                ask.put("amount", amount);
                asksList.add(ask);
            }
            ((OrderBookFragment)((Activity)c).getFragmentManager().findFragmentByTag("order_book")).updateAsksList(asksList);

            // Parse bids and update bids list
            JSONArray jsonBids = jsonObject.getJSONArray("bid");
            ArrayList<HashMap<String, String>> bidsList = new ArrayList<>();
            for(int i=0; i < jsonBids.length(); i++) {
                JSONObject jsonBid = jsonBids.getJSONObject(i);
                String price = jsonBid.getString("price");
                String amount = jsonBid.getString("size");
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
        String endpoint = "/api/2/public/symbol";
        publicRequest(endpoint, null, c, "restorePairsInDB");
    }

    public void UpdateBalances(Context c) {
        String endpoint = "/api/1/trading/balance";
        int method = Request.Method.GET;
        privateRequest(endpoint, null, method, c, "updateBalances");
    }

    public void CancelOrder(Context c, String orderNumber) {
        Log.d(TAG, "CancelOrder: Order#: " + orderNumber);
        String endpoint = "/api/1/trading/cancel_order";
        HashMap<String, String> params = new HashMap<>();
        params.put("clientOrderId", orderNumber);
        privateRequest(endpoint, params, Request.Method.POST, c, "cancelOrder");
    }

    public void UpdateOpenOrders(Context c) {
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        String endpoint = "/api/1/trading/orders/active";
        HashMap<String, String> params = new HashMap<>();
        params.put("symbols", selectedPair.getExchangePair());
        privateRequest(endpoint, params, Request.Method.GET, c, "updateOpenOrders");
    }

    public void UpdateOrderTransactions(Context c, String pair) {
        String endpoint = "/api/1/trading/orders/recent";
        HashMap<String, String> params = new HashMap<>();
        params.put("max_results", "50");
        params.put("symbols", pair);
        UpdateOpenOrders(c);
        privateRequest(endpoint, params, Request.Method.GET, c, "updateOrderTransactions");
    }

    public void UpdateTickerActivity(Context c) {
        Log.d(TAG, "UpdateTickerActivity: ");
        String endpoint = "/api/2/public/ticker";
        publicRequest(endpoint, null, c, "updateTickerActivity");
    }

    public void UpdateTickerInfo(Context c, String pair) {
        String endpoint = "/api/2/public/ticker/" + pair;
        publicRequest(endpoint, null, c, "updateTickerInfo");
    }

    public void RefreshOrderBooks(Context c, String pair) {
        String endpoint = "/api/2/public/orderbook/" + pair;
        publicRequest(endpoint, null, c, "refreshOrderBooks");
    }

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType) {
        String endpoint = "/api/1/trading/new_order";
        int method = Request.Method.POST;
        HashMap<String, String> params = new HashMap<>();
        params.put("clientOrderId", Long.toString(generateNonce()));
        amount = String.format("%.0f", Double.parseDouble(amount) * 1000);
        params.put("quantity", amount);
        Log.d(TAG, "PlaceOrder: amount " + amount);
        params.put("symbol", pair);
        params.put("side", orderType);
        params.put("price", rate);
        params.put("type", "limit");
        privateRequest(endpoint, params, method, c, "placeOrder");
    }

    private static String createTradePair(String pair) {
        if (pair.substring(3).equalsIgnoreCase("BTC")){
            return pair.substring(3) + "-" + pair.substring(0,3);
        }
        return pair.substring(0,3) + "-" + pair.substring(3);

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
