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
import com.mattcormier.cryptonade.MainActivity;
import com.mattcormier.cryptonade.OrdersFragment;
import com.mattcormier.cryptonade.R;
import com.mattcormier.cryptonade.TradeFragment;
import com.mattcormier.cryptonade.adapters.OpenOrdersAdapter;
import com.mattcormier.cryptonade.adapters.OrderTransactionsAdapter;
import com.mattcormier.cryptonade.adapters.TickerAdapter;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.models.Exchange;
import com.mattcormier.cryptonade.models.OpenOrder;
import com.mattcormier.cryptonade.models.OrderTransaction;
import com.mattcormier.cryptonade.models.Pair;
import com.mattcormier.cryptonade.models.Ticker;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class QuadrigacxClient implements APIClient {
    private static final String TAG = "QuadrigacxClient";
    private static long typeId = 2;
    private long exchangeId;
    private HashMap<String, Double> balances;
    private HashMap<String, Double> availableBalances;
    private String name;
    private String apiKey;
    private String apiSecret;
    private String apiOther;
    private static String apiUrl = "https://api.quadrigacx.com/v2";

    public QuadrigacxClient() {
        name = "";
        apiKey = "";
        apiSecret = "";
        apiOther = "";
    }

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

    private void privateRequest(HashMap<String, String> params, final Context c, String endpointUri, final String cmd) {
        String url = apiUrl + endpointUri;
        Log.d(TAG, "privateRequest: url: " + url + " cmd: " + cmd);

        final String nonce = new BigDecimal(generateNonce()).toString();
        final String signature = createSignature(nonce);
        final String body = createJsonBody(nonce, signature, params);

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
            } catch (JSONException e1) {
                Log.d(TAG, "processUpdateOrderTransactions: "+ e1.getMessage());
            }
            if (jsonObject != null && jsonObject.has("error")) {
                processAPIJSONError(jsonObject,  c);
                return;
            }
            Log.d(TAG, "Error in processUpdateOrderTransactions: " + e.toString());
        }
    }

    private void processUpdateBalances(String response, Context c) {
        try {
            HashMap<String, Double> balances = new HashMap<>();
            HashMap<String, Double> availableBalances = new HashMap<>();

            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.has("error")) {
                processAPIJSONError(jsonObject,  c);
                return;
            }
            Iterator<?> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String[] splitKey = key.split("_");
                String value = jsonObject.get(key).toString();
                if (splitKey.length > 1 && splitKey[1].equals("available") && Double.parseDouble(value) > 0) {
                    availableBalances.put(splitKey[0].toUpperCase(), Double.parseDouble(value));
                } else if (splitKey.length > 1 && splitKey[1].equals("available") && Double.parseDouble(value) > 0) {
                    balances.put(splitKey[0].toUpperCase(), Double.parseDouble(value));
                }
            }
            this.balances = balances;
            this.availableBalances = availableBalances;
        } catch (JSONException e) {
            Log.d(TAG, "processUpdateBalances: JSONException: " + e.getMessage());
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
            } catch (JSONException e1) {
                Log.d(TAG, "processUpdateOpenOrders: "+ e1.getMessage());
            }
            if (jsonObject != null && jsonObject.has("error")) {
                processAPIJSONError(jsonObject,  c);
                return;
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

    private static void processUpdateTradeTickerInfo(String response, Context c) {
        TextView tvLast = ((Activity) c).findViewById(R.id.tvTradeLastTrade);
        TextView tvHighest = ((Activity) c).findViewById(R.id.tvTradeHighestBid);
        TextView tvLowest = ((Activity) c).findViewById(R.id.tvTradeLowestAsk);
        TextView edPrice = ((Activity) c).findViewById(R.id.edTradePrice);
        Spinner spnPairs = ((Activity) c).findViewById(R.id.spnPairs);
        String pair = ((Pair) spnPairs.getSelectedItem()).getExchangePair();

        try {
            JSONObject data = new JSONObject(response);
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.equals(pair)) {
                    JSONObject tickerInfo = data.getJSONObject(key);
                    tvLast.setText(tickerInfo.getString("last"));
                    tvHighest.setText(tickerInfo.getString("bid"));
                    tvLowest.setText(tickerInfo.getString("ask"));
                    edPrice.setText(tickerInfo.getString("last"));
                    break;
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error in processTradingPairs: " + ex.toString());
        }
        ((TradeFragment)((Activity) c).getFragmentManager().findFragmentByTag("trade")).updateAvailableInfo();
    }

    public void UpdateBalances(Context c) {
        String endpointUri = "/balance";
        privateRequest(null, c, endpointUri, "updateBalances");
    }

    public void UpdateOrderTransactions(Context c) {
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        String endpointUri = "/user_transactions";
        HashMap<String, String> params = new HashMap<>();
        params.put("book", selectedPair.getExchangePair());
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

    public void UpdateTradeTickerInfo(Context c) {
        String endpointUri = "/ticker?";
        HashMap<String, String> params = new HashMap<>();
        params.put("book", "all");
        publicRequest(params, c, endpointUri, "updateTradeTickerInfo");
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
            Log.d(TAG, "createSignature: msg: " + msg);
            final byte[] macData = mac.doFinal(msg.getBytes("utf-8"));
            return new String(Hex.encodeHex(macData));

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private static int generateNonce() {
        Date d = new Date();
        return (int)d.getTime();
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

    private String createJsonBody(String nonce, String signature, HashMap<String, String> params) {
        JSONObject jsonBody = new JSONObject();
        try{
            if (params != null) {
                for(Map.Entry<String, String> param: params.entrySet()) {
                    jsonBody.put(param.getKey(), param.getValue());
                }
            }
            jsonBody.put("key", apiKey);
            jsonBody.put("nonce", Integer.parseInt(nonce));
            jsonBody.put("signature", signature);
        } catch (JSONException e) {
            e.printStackTrace();
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

    public String getAPIOther() {
        return apiOther;
    }

    public void setAPIOther(String apiOther) {
        this.apiOther = apiOther;
    }

    @Override
    public String toString() {
        return this.name.toString();
    }

    public HashMap<String, Double> getBalances() {
        return balances;
    }

    public void setBalances(HashMap<String, Double> balances) {
        this.balances = balances;
    }

    public HashMap<String, Double> getAvailableBalances() {
        return availableBalances;
    }

    public void setAvailableBalances(HashMap<String, Double> availableBalances) {
        this.availableBalances = availableBalances;
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


