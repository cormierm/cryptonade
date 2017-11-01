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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class BitfinexClient implements APIClient {
    private static final String TAG = "BitfinexClient";
    private static final long TYPEID = 3;
    private long exchangeId;
    private HashMap<String, Double> balances;
    private HashMap<String, Double> availableBalances;
    private String name;
    private String apiKey;
    private String apiSecret;
    long prevNonce;
    private static String baseUrl = "https://api.bitfinex.com/v1";

    public BitfinexClient() {
        name = "";
        apiKey = "";
        apiSecret = "";
    }

    public BitfinexClient(int exchangeId, String name, String apiKey, String apiSecret) {
        this.exchangeId = exchangeId;
        this.name = name;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    private void publicRequest(String endpoint, final Context c, final String cmd) {
        Log.d(TAG, "publicRequest: " + cmd);
        String url = baseUrl + endpoint;
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
                    }
                }
        );
        queue.add(stringRequest);
    }

    private void privateRequest(JSONObject jsonPayload, String endpoint, final Context c, final String cmd) {
        Log.d(TAG, "privateRequest: " + cmd);
        String url = baseUrl + endpoint;
        String nonce = Long.toString(generateNonce());

        try{
            jsonPayload.put("nonce", nonce);
        } catch (JSONException e) {
            Log.e(TAG, "privateRequest: JSONException Error: " + e.getMessage());
            return;
        }
        Log.d(TAG, "privateRequest: jsonpayload " + jsonPayload.toString());
        final String base64_body =  Base64.encodeToString(jsonPayload.toString().getBytes(), Base64.NO_WRAP);
        final String signature = createSignature(base64_body);

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
                        Log.d(TAG, "StringRequire.onErrorResponse: " + ex.getLocalizedMessage());
                        NetworkResponse networkResponse = ex.networkResponse;
                        if (networkResponse != null && networkResponse.data != null) {
                            String jsonError = new String(networkResponse.data);
                            try {
                                JSONObject jsonObject = new JSONObject(jsonError);
                                Toast.makeText(c, jsonObject.getString("message"), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Log.d(TAG, "onErrorResponse: " + e.getMessage());
                                e.printStackTrace();
                            }
                            Log.e(TAG, "onErrorResponse: " + jsonError);
                        }
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-BFX-APIKEY", apiKey);
                headers.put("X-BFX-SIGNATURE", signature);
                headers.put("X-BFX-PAYLOAD", base64_body);
                return headers;
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
//            if (jsonObject.has("error")) {
//                Toast.makeText(c, jsonObject.getString("error"), Toast.LENGTH_LONG).show();
//                Log.d(TAG, "processUpdateBalances: " + jsonObject.getString("error"));
//                return;
//            }
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("type").equalsIgnoreCase("exchange") &&
                        !jsonObject.getString("amount").equals("0.0")) {
                    String currency = jsonObject.getString("currency").toUpperCase();
                    String available = jsonObject.getString("available");
                    String balance = jsonObject.getString("amount");
                    Double dblAvailable = Double.parseDouble(available);
                    Double dblBalance = Double.parseDouble(balance);

                    availableBalances.put(currency, dblAvailable);
                    balances.put(currency, dblBalance);
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

            JSONArray jsonArray = new JSONArray(response);
            Log.d(TAG, "processRestorePairsInDB: " + jsonArray.toString());
            ArrayList<Pair> pairsList = new ArrayList<>();
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

        try {
            JSONObject json = new JSONObject(response);
            tvLast.setText(json.getString("last_price"));
            tvHighest.setText(json.getString("high"));
            tvLowest.setText(json.getString("low"));
            edPrice.setText(json.getString("last_price"));
        } catch (Exception ex) {
            Log.d(TAG, "Error in processTradingPairs: " + ex.toString());
        }
        ((TradeFragment)((Activity) c).getFragmentManager().findFragmentByTag("trade")).updateAvailableInfo();
    }

    public void RestorePairsInDB(Context c) {
        String endpoint = "/symbols";
        publicRequest(endpoint, c, "restorePairsInDB");
    }

    public void UpdateBalances(Context c) {
        String endpoint = "/balances";
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("request", "/v1/balances");
            privateRequest(jsonPayload, endpoint, c, "updateBalances");
        } catch (JSONException e) {
            Log.e(TAG, "UpdateBalances: JSON Exception Error: " +e.getMessage());
        }
    }

    public void CancelOrder(Context c, String orderNumber) {
        Log.d(TAG, "CancelOrder: Order#: " + orderNumber);
        String endpoint = "/order/cancel";
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("order_id", Long.parseLong(orderNumber));
            jsonPayload.put("request", "/v1/order/cancel");
            privateRequest(jsonPayload, endpoint, c, "cancelOrder");
        } catch (JSONException e) {
            Log.e(TAG, "CancelOrder: JSON Exception Error: " +e.getMessage());
        }
    }

    public void UpdateOpenOrders(Context c) {
        String endpoint = "/orders";
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("request", "/v1/orders");
            privateRequest(jsonPayload, endpoint, c, "updateOpenOrders");
        } catch (JSONException e) {
            Log.e(TAG, "UpdateOpenOrders: JSON Exception Error: " +e.getMessage());
        }
    }

    public void UpdateOrderTransactions(Context c, String pair) {
        String endpoint = "/mytrades";
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("request", "/v1/mytrades");
            jsonPayload.put("symbol", pair);
            jsonPayload.put("timestamp", 0);
            privateRequest(jsonPayload, endpoint, c, "updateOrderTransactions");
        } catch (JSONException e) {
            Log.e(TAG, "UpdateOpenOrders: JSON Exception Error: " +e.getMessage());
        }
    }

    public void UpdateTickerActivity(Context c) {
        Toast.makeText(c, "Ticker is not supported by Bitfinex at this time.", Toast.LENGTH_LONG).show();
    }

    public void UpdateTradeTickerInfo(Context c, String pair) {
        String endpoint = "/pubticker" + "/" + pair;
        publicRequest(endpoint, c, "updateTradeTickerInfo");
    }

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType) {
        Log.d(TAG, "PlaceOrder: ");
        String endpoint = "/order/new";
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("request", "/v1/order/new");
            jsonPayload.put("symbol", pair);
            jsonPayload.put("amount", amount);
            jsonPayload.put("price", rate);
            jsonPayload.put("exchange", "bitfinex");
            jsonPayload.put("side", orderType);
            jsonPayload.put("type", "exchange limit");
            privateRequest(jsonPayload, endpoint, c, "placeOrder");
        } catch (JSONException e) {
            Log.e(TAG, "PlaceOrder: JSON Exception Error: " +e.getMessage());
        }
    }

    private static String createTradePair(String pair) {
        return (pair.substring(3) + "-" + pair.substring(0,3)).toUpperCase();
    }

    private String createSignature(String base64_body) {
        Log.d(TAG, "createSignature: start");
        try {
            Mac mac = Mac.getInstance("HmacSHA384");
            mac.init(new SecretKeySpec(this.apiSecret.getBytes("utf-8"), "HmacSHA384"));
            final byte[] macData = mac.doFinal(base64_body.getBytes());
            return new String(Hex.encodeHex(macData));
        } catch (Exception e) {
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

    public long getId() {
        return exchangeId;
    }

    public void setId(long exchangeId) {
        this.exchangeId = exchangeId;
    }

    public long getTypeId() {
        return TYPEID;
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
