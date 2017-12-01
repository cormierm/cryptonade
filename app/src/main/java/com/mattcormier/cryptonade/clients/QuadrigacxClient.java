package com.mattcormier.cryptonade.clients;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
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
import com.mattcormier.cryptonade.adapters.OpenOrdersAdapter;
import com.mattcormier.cryptonade.adapters.OrderTransactionsAdapter;
import com.mattcormier.cryptonade.adapters.TickerAdapter;
import com.mattcormier.cryptonade.databases.CryptoDB;
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

/**
 * Filename: QuadrigacxClient.java
 * Description: API Client for QuadrigaCX exchange API requests.
 * Created by Matt Cormier on 10/29/2017.
 **/

public class QuadrigacxClient implements APIClient {
    private static final String TAG = "QuadrigacxClient";
    private static long typeId = 2;
    private long exchangeId;
    private HashMap<String, Double> balances;
    private HashMap<String, Double> availableBalances;
    private HashMap<String, String> tickerInfo;
    private String name;
    private String apiKey;
    private String apiSecret;
    private String apiOther;
    private static String apiUrl = "https://api.quadrigacx.com/v2";

    public QuadrigacxClient(int exchangeId, String name, String apiKey, String apiSecret, String apiOther) {
        this.exchangeId = exchangeId;
        this.name = name;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiOther = apiOther;
    }

    private void publicRequest(HashMap<String, String> params, final Context c, String endpointUri, final String cmd) {
        Log.d(TAG, "publicRequest: " + cmd);
        String url = apiUrl + endpointUri + createBody(params);
        RequestQueue queue = Volley.newRequestQueue(c);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
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
                            Log.e(TAG, "Error in request: " + cmd);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "publicRequest.onErrorResponse: " + error.getMessage());
                    }
                }
        );
        queue.add(stringRequest);
    }

    private void privateRequest(HashMap<String, String> params, final Context c, String endpointUri, final String cmd) {
        String url = apiUrl + endpointUri;
        Log.d(TAG, "privateRequest: url: " + url + " cmd: " + cmd);
        if(apiKey.isEmpty() || apiSecret.isEmpty() || apiOther.isEmpty()) {
            Toast.makeText(c, c.getResources().getString(R.string.invalid_api_msg), Toast.LENGTH_SHORT).show();
            return;
        }

        final String nonce = Long.toString(generateNonce());
        final String signature = createSignature(nonce);

        if (params == null) {
            params = new HashMap<>();
        }
        params.put("key", apiKey);
        params.put("nonce", nonce);
        params.put("signature", signature);
        final String body = createJsonBody(params);

        RequestQueue queue = Volley.newRequestQueue(c);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "private.onResponse: " + response);
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
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

        };
        queue.add(stringRequest);
    }

    private void processUpdateOrderTransactions(String response, Context c) {
        Log.d(TAG, "processUpdateOrderTransactions: response: " + response);
        ListView lvOrderTransactions = ((Activity) c).findViewById(R.id.lvOrdertransactions);
        Spinner spnPairs = ((Activity) c).findViewById(R.id.spnPairs);
        String[] pair = ((Pair) spnPairs.getSelectedItem()).getTradingPair().split("-");
        try {
            ArrayList<OrderTransaction> orderTransactionsList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(response);
            for (int i=0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                int orderType = json.getInt("type");
                if (orderType == 2) {
                    String orderNumber = json.getString("id");
                    String timestamp = json.getString("datetime");
                    String type;
                    if (json.getDouble(pair[0].toLowerCase()) > 0) {
                        type = "Sell";
                    } else {
                        type = "Buy";
                    }
                    String rate = json.getString("rate");
                    String amount = json.getString(pair[1].toLowerCase());
                    if (amount.charAt(0) == '-') {
                        amount = amount.substring(1);
                    }
                    String fee = json.getString("fee");
                    OrderTransaction order = new OrderTransaction(orderNumber, timestamp, type,
                            amount, rate, fee);
                    orderTransactionsList.add(order);
                }
            }
            OrderTransactionsAdapter orderTransactionsAdapter = new OrderTransactionsAdapter(c, R.layout.listitem_order_transaction, orderTransactionsList);
            lvOrderTransactions.setAdapter(orderTransactionsAdapter);
        } catch (JSONException e) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(response);
                if (jsonObject.has("error")) {
                    JSONObject jsonError = jsonObject.getJSONObject("error");
                    if (jsonError.getInt("code") == 12) {
                        Toast.makeText(c, "Invalid API key/secret pair.", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(c, jsonError.getString("message"), Toast.LENGTH_LONG).show();
                    }
                }
            } catch (JSONException e1) {
                Log.d(TAG, "processUpdateOrderTransactions: "+ e1.getMessage());
            }
            Log.d(TAG, "Error in processUpdateOrderTransactions: " + e.toString());
        }
        UpdateOpenOrders(c);
    }

    private void processUpdateBalances(String response, Context c) {
        try {
            HashMap<String, Double> balances = new HashMap<>();
            HashMap<String, Double> availableBalances = new HashMap<>();

            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject != null && jsonObject.has("error")) {
                JSONObject jsonError = jsonObject.getJSONObject("error");
                if (jsonError.getInt("code") == 12) {
                    Toast.makeText(c, c.getResources().getString(R.string.invalid_api_msg), Toast.LENGTH_LONG).show();
                }
                else if (jsonError.getInt("code") == 104) {
                    UpdateBalances(c);
                }
                else {
                    Toast.makeText(c, jsonError.getString("message"), Toast.LENGTH_LONG).show();
                }
            }
            Iterator<?> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String[] splitKey = key.split("_");
                String value = jsonObject.get(key).toString();
                if (splitKey.length > 1 && splitKey[1].equals("available") && Double.parseDouble(value) > 0) {
                    availableBalances.put(splitKey[0].toUpperCase(), Double.parseDouble(value));
                } else if (splitKey.length > 1 && splitKey[1].equals("balance") && Double.parseDouble(value) > 0) {
                    balances.put(splitKey[0].toUpperCase(), Double.parseDouble(value));
                }
            }
            this.balances = balances;
            this.availableBalances = availableBalances;
        } catch (JSONException e) {
            Log.d(TAG, "processUpdateBalances: JSONException: " + e.getMessage());
        }
        BalancesFragment balFrag = (BalancesFragment)((Activity) c).getFragmentManager().findFragmentByTag("balances");
        if (balFrag != null && balFrag.isVisible()) {
            balFrag.refreshBalances();
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
            PairsFragment pairsFrag = (PairsFragment)((Activity)c).getFragmentManager().findFragmentByTag("pairs");
            if (pairsFrag != null) {
                pairsFrag.updatePairsListView();
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error in processTradingPairs: " + ex.toString());
        }
    }

    private void processPlacedOrder(String response, Context c) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
            if (jsonObject.has("error")) {
                processAPIJSONError(jsonObject,  c);
                return;
            }
            else if (jsonObject.has("id")) {
                Toast.makeText(c, "Trade placed successfully.\nOrder number: " +
                        jsonObject.getString("id"), Toast.LENGTH_LONG).show();
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
        if (response.equals("\"true\""))
            Toast.makeText(c, "Order successfully cancelled.", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(c, response, Toast.LENGTH_LONG).show();
        UpdateOpenOrders(c);
    }

    private void processUpdateOpenOrders(String response, Context c) {
        Log.d(TAG, "processUpdateOpenOrders: " + response);
        ListView lvOpenOrders = ((Activity) c).findViewById(R.id.lvOpenOrders);
        try {
            ArrayList<OpenOrder> openOrdersList = new ArrayList<>();
            JSONArray json = new JSONArray(response);
            for (int i=0; i < json.length(); i++) {
                JSONObject jsonOrder = json.getJSONObject(i);
                String orderNumber = jsonOrder.getString("id");
                String orderType = jsonOrder.getString("type");
                if (orderType.equals("1"))
                    orderType = "Sell";
                else
                    orderType = "Buy";
                String orderRate = jsonOrder.getString("price");
                String orderStartingAmount = "undef";
                String orderRemainingAmount = jsonOrder.getString("amount");
                String orderDate = jsonOrder.getString("datetime");
                OpenOrder order = new OpenOrder(orderNumber, "not set", orderType.toUpperCase(),
                        orderRate, orderStartingAmount, orderRemainingAmount, orderDate);
                openOrdersList.add(order);
            }

            OpenOrdersAdapter openOrdersAdapter = new OpenOrdersAdapter(c, R.layout.listitem_openorder, openOrdersList);
            lvOpenOrders.setAdapter(openOrdersAdapter);

        } catch (JSONException e) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(response);
                if (jsonObject != null && jsonObject.has("error")) {
                    JSONObject jsonError = jsonObject.getJSONObject("error");
                    if (jsonError.getInt("code") == 12) {
                        Toast.makeText(c, "Invalid API key/secret pair.", Toast.LENGTH_LONG).show();
                    }
                    else if (jsonError.getInt("code") == 104) {
                        UpdateOpenOrders(c);
                    }
                    else {
                        Toast.makeText(c, jsonError.getString("message"), Toast.LENGTH_LONG).show();
                    }
                }
            } catch (JSONException e1) {
                Log.d(TAG, "processUpdateOpenOrders: "+ e1.getMessage());
            }
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
                String volume = jsonTicker.getString("volume");
                String lowestAsk = jsonTicker.getString("ask");
                String lowest24hr = jsonTicker.getString("low");
                String highestBid = jsonTicker.getString("bid");
                String highest24hr = jsonTicker.getString("high");
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

    private void processUpdateTickerInfo(String response, Context c) {
        try {
            JSONObject jsonTicker = new JSONObject(response);
            tickerInfo = new HashMap<>();
            tickerInfo.put("Last", jsonTicker.getString("last"));
            tickerInfo.put("Bid", jsonTicker.getString("bid"));
            tickerInfo.put("Ask", jsonTicker.getString("ask"));
        } catch (JSONException ex) {
            Log.e(TAG, "Error in processUpdateTickerInfo: JSONException: " + ex.toString());
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

    public void UpdateBalances(Context c) {
        String endpointUri = "/balance";
        privateRequest(null, c, endpointUri, "updateBalances");
    }

    public void UpdateOrderTransactions(Context c, String pair) {
        String endpointUri = "/user_transactions";
        HashMap<String, String> params = new HashMap<>();
        params.put("book", pair);
        privateRequest(params, c, endpointUri, "updateOrderTransactions");
    }

    public void RestorePairsInDB(Context c) {
        String endpointUri = "/ticker?";
        HashMap<String, String> params = new HashMap<>();
        params.put("book", "all");
        publicRequest(params, c, endpointUri, "restorePairsInDB");
    }

    public void CancelOrder(Context c, String orderNumber) {
        String endpointUri = "/cancel_order";
        Log.d(TAG, "CancelOrder: Order#: " + orderNumber);
        HashMap<String, String> params = new HashMap<>();
        params.put("id", orderNumber);
        privateRequest(params, c, endpointUri, "cancelOrder");
    }

    public void UpdateOpenOrders(Context c) {
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        String endpointUri = "/open_orders";
        HashMap<String, String> params = new HashMap<>();
        params.put("book", selectedPair.getExchangePair());
        privateRequest(params, c, endpointUri, "updateOpenOrders");
    }

    public void UpdateTickerActivity(Context c) {
        String endpointUri = "/ticker?";
        HashMap<String, String> params = new HashMap<>();
        params.put("book", "all");
        publicRequest(params, c, endpointUri, "updateTickerActivity");
    }

    public void UpdateTickerInfo(Context c, String pair) {
        String endpointUri = "/ticker?";
        HashMap<String, String> params = new HashMap<>();
        params.put("book", pair);
        publicRequest(params, c, endpointUri, "updateTickerInfo");
    }

    public void RefreshOrderBooks(Context c, String pair) {
        String endpointUri = "/order_book?";
        HashMap<String, String> params = new HashMap<>();
        params.put("book", pair);
        publicRequest(params, c, endpointUri, "refreshOrderBooks");
    }

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType) {
        String endpointUri = "";
        HashMap<String, String> params = new HashMap<>();

        if (orderType.equals("sell")) {
            endpointUri = "/sell";
        } else {
            endpointUri = "/buy";
        }

        params.put("book", pair);
        params.put("price", rate);
        params.put("amount", amount);
        privateRequest(params, c, endpointUri, "placeOrder");
    }

    private static String createTradePair(String pair) {
        String[] parts = pair.split("_");
        String returnPair = (parts[0] + "-" + parts[1]).toUpperCase();
        if (returnPair.equals("ETH-BTC")) {
            returnPair = "BTC-ETH";
        }
        return returnPair;
    }

    private String createSignature(String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(this.apiSecret.getBytes("utf-8"), "HmacSHA256"));
            String msg = nonce + apiOther + apiKey;
            final byte[] macData = mac.doFinal(msg.getBytes("utf-8"));
            return new String(Hex.encodeHex(macData));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private long generateNonce() {
        Date d = new Date();
        return d.getTime() * 1000;
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

    private String createJsonBody(HashMap<String, String> params) {
        JSONObject jsonBody = new JSONObject();
        try{
            for(Map.Entry<String, String> param: params.entrySet()) {
                jsonBody.put(param.getKey(), param.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "createJsonBody: " + e.getMessage());
        }
        return jsonBody.toString();
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

    private void processAPIJSONError(JSONObject json, Context c) {
        try {
            JSONObject jsonError = json.getJSONObject("error");
            if (jsonError.getInt("code") == 12) {
                Toast.makeText(c, "Invalid API key/secret pair.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(c, jsonError.getString("message"), Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            Log.d(TAG, "processAPIError: " + e.getMessage());
        }
    }
}


