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

public class GeminiClient implements APIClient {
    private static final String TAG = "GeminiClient";
    private static long typeId = 7;
    private long exchangeId;
    private HashMap<String, Double> balances;
    private HashMap<String, Double> availableBalances;
    private String name;
    private String apiKey;
    private String apiSecret;
    private static String baseUrl = "https://api.gemini.com";

    public GeminiClient(int exchangeId, String name, String apiKey, String apiSecret) {
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

    private void privateRequest(String endpoint, HashMap<String, String> params, final Context c, final String cmd) {
        Log.d(TAG, "privateRequest: " + cmd);
        String url = baseUrl + endpoint;
        Log.d(TAG, "privateRequest: url: " +url);

        if (params == null) {
            params = new HashMap<>();
        }
        params.put("nonce", Long.toString(generateNonce()));
        JSONObject jsonPayload = new JSONObject(params);

        Log.d(TAG, "privateRequest: jsonPayload: " + jsonPayload.toString());

        final String b64_payload = Base64.encodeToString(jsonPayload.toString().getBytes(), Base64.NO_WRAP);
        Log.d(TAG, "privateRequest: b64_payload: " + b64_payload);

        final String signature = createSignature(b64_payload);

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
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-GEMINI-APIKEY", apiKey);
                headers.put("X-GEMINI-PAYLOAD", b64_payload);
                headers.put("X-GEMINI-SIGNATURE", signature);
                Log.d(TAG, "getHeaders: " + headers.toString());
                return headers;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };
        queue.add(stringRequest);
    }

    private String createSignature(String b64_payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA384");
            mac.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA384"));
            byte[] macData = mac.doFinal(b64_payload.getBytes());
            return new String(Hex.encodeHex(macData));
        } catch (Exception e) {
            Log.e(TAG, "createSignature: " + e.getMessage());
            e.printStackTrace();
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
            JSONArray jsonArray = new JSONArray(response);

            for (int i=0; i < jsonArray.length(); i++){
                JSONObject json = jsonArray.getJSONObject(i);
                String orderNumber = json.getString("order_id");
                String timestamp = Crypto.formatDate(json.getString("timestamp"));
                String type = json.getString("type");
                String amount = json.getString("amount");
                String rate = json.getString("price");
                String fee = json.getString("fee_amount");
                OrderTransaction order = new OrderTransaction(orderNumber, timestamp, type,
                        amount, rate, fee);
                Log.d(TAG, "processUpdateOrderTransactions: added: " + order.toString());
                orderTransactionsList.add(order);
            }

            OrderTransactionsAdapter orderTransactionsAdapter = new OrderTransactionsAdapter(c, R.layout.listitem_order_transaction, orderTransactionsList);
            lvOrderTransactions.setAdapter(orderTransactionsAdapter);

        } catch (JSONException e) {
            Log.d(TAG, "processUpdateOrderTransactions: "  + e.getMessage());
        }
        UpdateOpenOrders(c);
    }

    private void processUpdateBalances(String response, Context c) {
        Log.d(TAG, "processUpdateBalances: response: " + response);
        HashMap<String, Double> availableBalances = new HashMap<>();
        HashMap<String, Double> balances = new HashMap<>();
        try {
            JSONArray jsonArray = new JSONArray(response);
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("type").equalsIgnoreCase("exchange") &&
                        !jsonObject.getString("amount").equals("0.0")) {
                    String currency = jsonObject.getString("currency").toUpperCase();
                    String available = jsonObject.getString("available");
                    String balance = jsonObject.getString("amount");
                    Double dblAvailable = Double.parseDouble(available);
                    Double dblBalance = Double.parseDouble(balance);
                    if (dblBalance > 0) {
                        availableBalances.put(currency, dblAvailable);
                        balances.put(currency, dblBalance);
                    }
                }
            }
            this.availableBalances = availableBalances;
            this.balances = balances;
        } catch (JSONException e) {
            Log.d(TAG, "processUpdateBalances: Exception error with json." + e.getMessage());
        }
    }

    private void processRestorePairsInDB(String response, Context c) {
        CryptoDB db = new CryptoDB(c);
        db.deletePairsByExchangeId(exchangeId);
        try {
            ArrayList<Pair> pairsList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(response);
            for(int i=0; i < jsonArray.length(); i++) {
                String exchangePair = jsonArray.getString(i);
                String appPair = createTradePair(exchangePair);
                Pair pair = new Pair(0, (int)exchangeId, exchangePair, appPair);
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
            if (jsonResp.has("id")) {
                Toast.makeText(c, "Trade placed successfully.\nOrder number: " +
                        jsonResp.getString("id"), Toast.LENGTH_LONG).show();
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
    }

    private void processCancelOrder(String response, Context c) {
        JSONObject jsonResp;
        try {
            jsonResp = new JSONObject(response);
            if (jsonResp.has("id")) {
                Toast.makeText(c, c.getResources().getString(R.string.order_successfully_cancelled), Toast.LENGTH_LONG).show();
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

    private void processUpdateOpenOrders(String response, Context c) {
        Log.d(TAG, "processUpdateOpenOrders: ");
        ListView lvOpenOrders = ((Activity) c).findViewById(R.id.lvOpenOrders);
        try {
            String currentExchangePair = ((Pair)((Spinner)((Activity) c).findViewById(R.id.spnPairs)).getSelectedItem()).getExchangePair();
            ArrayList<OpenOrder> openOrdersList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(response);
            for (int i=0; i < jsonArray.length(); i++){
                JSONObject json = jsonArray.getJSONObject(i);
                String orderPair = json.getString("symbol");
                if (orderPair.equalsIgnoreCase(currentExchangePair)){
                    String orderNumber = json.getString("id");
                    String orderType = json.getString("side");
                    String orderRate = json.getString("price");
                    String orderStartingAmount = json.getString("original_amount");
                    String orderRemainingAmount = json.getString("remaining_amount");
                    String orderDate = Crypto.formatDate(json.getString("timestamp"));
                    OpenOrder order = new OpenOrder(orderNumber, createTradePair(orderPair), orderType.toUpperCase(),
                            orderRate, orderStartingAmount, orderRemainingAmount, orderDate);
                    openOrdersList.add(order);
                }
            }
            OpenOrdersAdapter openOrdersAdapter = new OpenOrdersAdapter(c, R.layout.listitem_openorder, openOrdersList);
            lvOpenOrders.setAdapter(openOrdersAdapter);

        } catch (JSONException e) {
            try {
                JSONObject json = new JSONObject(response);
                if (json.has("error")) {
                    Toast.makeText(c, json.getString("error"), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "processUpdateOpenOrders: " + json.getString("error"));
                } else {
                    Log.d(TAG, "Error in processUpdateOpenOrders: " + e.toString());
                }
            } catch (JSONException e1) {
                Log.d(TAG, "Error in processUpdateOpenOrders: " + e1.toString());
            }
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

    private static void processUpdateTradeTickerInfo(String response, Context c) {
        TextView tvLast = ((Activity) c).findViewById(R.id.tvTradeLastTrade);
        TextView tvHighest = ((Activity) c).findViewById(R.id.tvTradeHighestBid);
        TextView tvLowest = ((Activity) c).findViewById(R.id.tvTradeLowestAsk);
        TextView edPrice = ((Activity) c).findViewById(R.id.edTradePrice);

        try {
            JSONObject json = new JSONObject(response);
            tvLast.setText(json.getString("last"));
            tvHighest.setText(json.getString("bid"));
            tvLowest.setText(json.getString("ask"));
            edPrice.setText(json.getString("last"));
        } catch (Exception ex) {
            Log.e(TAG, "Error in processTradingPairs: " + ex.toString());
        }
        ((TradeFragment)((Activity) c).getFragmentManager().findFragmentByTag("trade")).updateAvailableInfo();
    }

    public void RestorePairsInDB(Context c) {
        String endpoint = "/v1/symbols";
        publicRequest(endpoint, null, c, "restorePairsInDB");
    }

    public void UpdateBalances(Context c) {
        String endpoint = "/v1/balances".replace("\\/", "/");
        HashMap<String, String> params = new HashMap<>();
        params.put("request", endpoint);
        privateRequest(endpoint, params, c, "updateBalances");
    }

    public void CancelOrder(Context c, String orderNumber) {
        Log.d(TAG, "CancelOrder: Order#: " + orderNumber);
        String endpoint = "/v1/order/cancel";
        HashMap<String, String> params = new HashMap<>();
        params.put("request", endpoint);
        params.put("order_id", orderNumber);
        privateRequest(endpoint, params, c, "cancelOrder");
    }

    public void UpdateOpenOrders(Context c) {
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        String endpoint = "/v1/orders";
        HashMap<String, String> params = new HashMap<>();
        params.put("request", endpoint);
        privateRequest(endpoint, params, c, "updateOpenOrders");
    }

    public void UpdateOrderTransactions(Context c, String pair) {
        String endpoint = "/v1/mytrades";
        HashMap<String, String> params = new HashMap<>();
        params.put("request", endpoint);
        params.put("symbol", pair);
        privateRequest(endpoint, params, c, "updateOrderTransactions");
    }

    public void UpdateTickerActivity(Context c) {
        Log.d(TAG, "UpdateTickerActivity: ");
        String endpoint = "/tickers/BTC/ETH/CAD/RUB/USD/DASH/ZEC/BCH/GBP/EUR";
        publicRequest(endpoint, null, c, "updateTickerActivity");
    }

    public void UpdateTradeTickerInfo(Context c, String pair) {
        String endpoint = "/v1/pubticker/" + pair;
        publicRequest(endpoint, null, c, "updateTradeTickerInfo");
    }

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType) {
        String endpoint = "/v1/order/new";
        HashMap<String, String> params = new HashMap<>();
        params.put("request", endpoint);
        params.put("symbol", pair);
        params.put("amount", amount);
        params.put("price", rate);
        params.put("side", orderType);
        params.put("type", "exchange limit");
        privateRequest(endpoint, params, c, "placeOrder");
    }

    private static String createTradePair(String pair) {
        if (pair.substring(3).equalsIgnoreCase("btc")) {
            return (pair.substring(3) + "-" + pair.substring(0,3)).toUpperCase();
        }
        return (pair.substring(0,3) + "-" + pair.substring(3)).toUpperCase();
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
}
