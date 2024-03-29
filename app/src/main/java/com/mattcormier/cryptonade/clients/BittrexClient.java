package com.mattcormier.cryptonade.clients;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Filename: BittrexClient.java
 * Description: API Client for Bittrex exchange API requests.
 * Created by Matt Cormier on 10/29/2017.
 **/

public class BittrexClient implements APIClient {
    private static final String TAG = "BittrexClient";
    private static long typeId = 4;
    private long exchangeId;
    private HashMap<String, Double> balances;
    private HashMap<String, Double> availableBalances;
    private HashMap<String, String> tickerInfo;
    private String name;
    private String apiKey;
    private String apiSecret;
    private static String baseUrl = "https://bittrex.com/api/v1.1";

    public BittrexClient(int exchangeId, String name, String apiKey, String apiSecret) {
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
                        Log.e(TAG, "publicRequest.onErrorResponse: " + error.getMessage());
                    }
                }
        );
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void privateRequest(String endpoint, HashMap<String, String> params, final Context c, final String cmd) {
        Log.d(TAG, "privateRequest: " + cmd);
        if(apiKey.isEmpty() || apiSecret.isEmpty()) {
            Toast.makeText(c, c.getResources().getString(R.string.invalid_api_msg), Toast.LENGTH_SHORT).show();
            return;
        }
        final String nonce = Long.toString(generateNonce());

        if (params == null) {
            params = new HashMap<>();
        }
        params.put("apikey", this.apiKey);
        params.put("nonce", nonce);
        String requestParams = createBody(params);
        String url = baseUrl + endpoint + "?" + requestParams;

        final String signature = createSignature(url);

        RequestQueue queue = Volley.newRequestQueue(c);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.has("success")) {
                                if (!jsonResponse.getBoolean("success")) {
                                    Toast.makeText(c, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "onResponse: JSONException: " + e.getMessage());
                        }
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
                            Log.e(TAG, "onErrorResponse: " + jsonError);
                        }
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("apisign", signature);
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private String createSignature(String url) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(this.apiSecret.getBytes("utf-8"), "HmacSHA512"));
            final byte[] macData = mac.doFinal(url.getBytes("utf-8"));
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
        Log.d(TAG, "processUpdateOrderTransactions: starts");
        try {
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.getBoolean("success")) {
                ArrayList<OrderTransaction> orderTransactionsList = new ArrayList<>();
                JSONArray jsonResult = jsonResponse.getJSONArray("result");
                for (int i=0; i < jsonResult.length(); i++){
                    JSONObject json = jsonResult.getJSONObject(i);
                    String orderNumber = json.getString("OrderUuid");
                    String timestamp = json.getString("TimeStamp");
                    String type = json.getString("OrderType");
                    if (type.equalsIgnoreCase("LIMIT_SELL")) {
                        type = "Sell";
                    } else if (type.equalsIgnoreCase("LIMIT_BUY")) {
                        type = "Buy";
                    }
                    String amount = String.format("%.8f", json.getDouble("Quantity"));
                    String rate = String.format("%.8f", json.getDouble("Price"));
                    String fee = String.format("%.8f", json.getDouble("Commission"));
                    OrderTransaction order = new OrderTransaction(orderNumber, timestamp, type,
                            amount, rate, fee);
                    Log.d(TAG, "processUpdateOrderTransactions: added: " + order.toString());
                    orderTransactionsList.add(order);
                }
                ((TransactionsFragment)((MainActivity) c).getFragment("transactions")).updateTransactionsList(orderTransactionsList);
            } else {
                String jsonMsg = jsonResponse.getString("message");
                Toast.makeText(c, jsonMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "processUpdateOrderTransactions: " + jsonMsg);
            }
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
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject jsonBalances = jsonArray.getJSONObject(i);
                String currency = jsonBalances.getString("Currency");
                Double available = jsonBalances.getDouble("Available");
                Double amount = jsonBalances.getDouble("Balance");
                if (amount > 0) {
                    availableBalances.put(currency, available);
                    balances.put(currency, amount);
                }
            }
            this.availableBalances = availableBalances;
            this.balances = balances;
        } catch (JSONException e) {
            Log.d(TAG, "processUpdateBalances: Exception error with json." + e.getMessage());
        }
        BalancesFragment balFrag = (BalancesFragment)((MainActivity)c).getSupportFragmentManager().findFragmentByTag("balances");
        if (balFrag != null && balFrag.isVisible()) {
            balFrag.refreshBalances();
        }
    }

    private void processRestorePairsInDB(String response, Context c) {
        CryptoDB db = new CryptoDB(c);
        db.deletePairsByExchangeId(exchangeId);
        try {
            ArrayList<Pair> pairsList = new ArrayList<>();
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.optJSONObject(i);
                String tradingPair = json.getString("MarketName");
                Pair pair = new Pair(0, (int)exchangeId, tradingPair, tradingPair);
                pairsList.add(pair);
            }
            db.insertPairs(pairsList);
            PairsFragment pairsFrag = (PairsFragment)((MainActivity)c).getSupportFragmentManager().findFragmentByTag("pairs");
            if (pairsFrag != null && pairsFrag.isVisible()) {
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
            if (jsonObject.getBoolean("success")) {
                JSONObject jsonResult = jsonObject.getJSONObject("result");
                Toast.makeText(c, c.getResources().getString(R.string.order_successfully_placed) +
                        jsonResult.getString("uuid"), Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(c, jsonObject.getString("message"), Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            Toast.makeText(c, "Unknown Error happened!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "JSONException error in processPlacedOrder: " + e.toString());
        }
    }

    private void processCancelOrder(String response, Context c) {
        Log.d(TAG, "processCancelOrder: ");
        JSONObject jsonResp;
        try {
            jsonResp = new JSONObject(response);
            if (jsonResp.getBoolean("success")) {
                Toast.makeText(c, c.getResources().getString(R.string.order_successfully_cancelled),
                        Toast.LENGTH_LONG).show();
            } else {
                String jsonMsg = jsonResp.getString("message");
                Toast.makeText(c, jsonMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "processCancelOrder: " + jsonMsg);
            }
        } catch (JSONException e) {
            Log.e(TAG, "processCancelOrder: JSONException Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processCancelOrder: Exception: " + e.getMessage());
        }
        UpdateOpenOrders(c);
    }

    private void processCheckOpenOrder(String response, Context c) {
        Log.d(TAG, "processCheckOpenOrder: ");
        JSONObject jsonResp;
        try {
            jsonResp = new JSONObject(response);
            if (jsonResp.getBoolean("success")) {
                JSONObject jsonResult = jsonResp.getJSONObject("result");
                if(!jsonResult.getBoolean("IsOpen")) {
                    Crypto.openOrderClosed(c, jsonResult.getString("OrderUuid"), this.getName(),
                            jsonResult.getString("Quantity"), jsonResult.getString("Price"),
                            jsonResult.getString("Exchange"));
                }
            } else {
                String jsonMsg = jsonResp.getString("message");
                Log.e(TAG, "processCheckOpenOrder: " + jsonMsg);
            }
        } catch (JSONException e) {
            Log.e(TAG, "processCheckOpenOrder: JSONException Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processCheckOpenOrder: Exception: " + e.getMessage());
        }
    }

    private void processUpdateOpenOrders(String response, Context c) {
        Log.d(TAG, "processUpdateOpenOrders: ");
        try {
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.getBoolean("success")) {
                JSONArray jsonResult = jsonResponse.getJSONArray("result");
                ArrayList<OpenOrder> openOrdersList = new ArrayList<>();
                for (int i=0; i < jsonResult.length(); i++){
                    JSONObject json = jsonResult.getJSONObject(i);
                    String orderNumber = json.getString("OrderUuid");
                    String orderPair = json.getString("Exchange");
                    String orderType = json.getString("OrderType");
                    if (orderType.equalsIgnoreCase("LIMIT_SELL")) {
                        orderType = "Sell";
                    } else if (orderType.equalsIgnoreCase("LIMIT_BUY")) {
                        orderType = "Buy";
                    }
                    String orderRate = String.format("%.8f", json.getDouble("Limit"));
                    String orderStartingAmount = String.format("%.8f", json.getDouble("Quantity"));
                    String orderRemainingAmount = String.format("%.8f", json.getDouble("QuantityRemaining"));
                    String orderDate = json.getString("Opened");
                    OpenOrder order = new OpenOrder((int)exchangeId, orderNumber, orderPair, orderType.toUpperCase(),
                            orderRate, orderStartingAmount, orderRemainingAmount, orderDate);
                    openOrdersList.add(order);
                }
                ((OpenOrdersFragment)((MainActivity) c).getFragment("open_orders")).updateOpenOrdersList(openOrdersList);
            } else {
                String jsonMsg = jsonResponse.getString("message");
                Toast.makeText(c, jsonMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "processUpdateOpenOrders: " + jsonMsg);
            }
        } catch (JSONException e) {
            Log.e(TAG, "processUpdateOpenOrders: JSONException Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processUpdateOpenOrders: Exception: " + e.getMessage());
        }
    }

    private static void processUpdateTickerActivity(String response, Context c) {
        Log.d(TAG, "processUpdateTickerActivity: ");
        ListView lvTickerList = ((Activity) c).findViewById(R.id.lvTickerList);
        try {
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.getBoolean("success")) {
                JSONArray jsonResult = jsonResponse.getJSONArray("result");
                ArrayList<Ticker> tickerList = new ArrayList<>();
                for (int i=0; i < jsonResult.length(); i++) {
                    JSONObject json = jsonResult.getJSONObject(i);
                    String tickerPair = json.getString("MarketName");
                    String last = String.format("%.8f", json.getDouble("Last"));
                    String volume = String.format("%.2f", json.getDouble("Volume"));
                    String lowestAsk = String.format("%.8f", json.getDouble("Ask"));
                    String lowest24hr = String.format("%.8f", json.getDouble("Low"));
                    String highestBid = String.format("%.8f", json.getDouble("Bid"));
                    String highest24hr = String.format("%.8f", json.getDouble("High"));
                    Ticker ticker = new Ticker(tickerPair, last, volume,
                            lowestAsk, lowest24hr, highestBid, highest24hr);
                    tickerList.add(ticker);
                }
                TickerAdapter tickerAdapter = new TickerAdapter(c, R.layout.listitem_ticker, tickerList);
                lvTickerList.setAdapter(tickerAdapter);
            } else {
                String jsonMsg = jsonResponse.getString("message");
                Toast.makeText(c, jsonMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "processUpdateTickerActivity: " + jsonMsg);
            }
        } catch (JSONException e) {
            Log.e(TAG, "processUpdateTickerActivity: JSONException Error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "processUpdateTickerActivity: Exception: " + e.getMessage());
        }
    }

    private void processUpdateTickerInfo(String response, Context c) {
        try {
            JSONObject jsonReponse = new JSONObject(response);
            if (jsonReponse.getBoolean("success")) {
                JSONObject jsonTicker = jsonReponse.getJSONObject("result");
                tickerInfo = new HashMap<>();
                tickerInfo.put("Last", String.format("%.8f", jsonTicker.getDouble("Last")));
                tickerInfo.put("Bid", String.format("%.8f", jsonTicker.getDouble("Bid")));
                tickerInfo.put("Ask", String.format("%.8f", jsonTicker.getDouble("Ask")));
            }
        } catch (JSONException ex) {
            Log.d(TAG, "Error in processUpdateTickerInfo: JSONException Error: " + ex.getMessage());
        } catch (Exception ex) {
            Log.d(TAG, "Error in processUpdateTickerInfo: Exception Error: " + ex.getMessage());
        }
    }

    private void processRefreshOrderBooks(String response, Context c) {
        Log.d(TAG, "refreshOrderBooks: start");
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (!jsonObject.has("success")) {
                Log.e(TAG, "processRefreshOrderBooks: Error in processRefreshOrderBooks: No success response.");
                return;
            }

            JSONObject jsonResult = jsonObject.getJSONObject("result");

            // Parse asks and update asks list
            JSONArray jsonAsks = jsonResult.getJSONArray("sell");
            ArrayList<HashMap<String, String>> asksList = new ArrayList<>();
            for(int i=0; i < jsonAsks.length(); i++) {
                JSONObject jsonAsk = jsonAsks.getJSONObject(i);
                String price = String.format("%.8f", jsonAsk.getDouble("Rate"));
                String amount = String.format("%.8f", jsonAsk.getDouble("Quantity"));
                HashMap<String, String> ask = new HashMap<>();
                ask.put("price", price);
                ask.put("amount", amount);
                asksList.add(ask);
            }

            // Parse bids and update bids list
            JSONArray jsonBids = jsonResult.getJSONArray("buy");
            ArrayList<HashMap<String, String>> bidsList = new ArrayList<>();
            for(int i=0; i < jsonBids.length(); i++) {
                JSONObject jsonBid = jsonBids.getJSONObject(i);
                String price = String.format("%.8f", jsonBid.getDouble("Rate"));
                String amount = String.format("%.8f", jsonBid.getDouble("Quantity"));
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
        String endpoint = "/public/getmarkets";
        publicRequest(endpoint, null, c, "restorePairsInDB");
    }

    public void UpdateBalances(Context c) {
        String endpoint = "/account/getbalances";
        privateRequest(endpoint, null, c, "updateBalances");
    }

    public void CancelOrder(Context c, String orderNumber) {
        Log.d(TAG, "CancelOrder: Order#: " + orderNumber);
        String endpoint = "/market/cancel";
        HashMap<String, String> params = new HashMap<>();
        params.put("uuid", orderNumber);
        privateRequest(endpoint, params, c, "cancelOrder");
    }

    public void UpdateOpenOrders(Context c) {
        String endpoint = "/market/getopenorders";
        Pair selectedPair = (Pair) ((Spinner)((Activity)c).findViewById(R.id.spnPairs)).getSelectedItem();
        HashMap<String, String> params = new HashMap<>();
        params.put("market", selectedPair.getExchangePair());
        privateRequest(endpoint, params, c, "updateOpenOrders");
    }

    public void UpdateOrderTransactions(Context c, String pair) {
        String endpoint = "/account/getorderhistory";
        HashMap<String, String> params = new HashMap<>();
        params.put("market", pair);
        params.put("count", "50");
        privateRequest(endpoint, params, c, "updateOrderTransactions");
    }

    public void UpdateTickerActivity(Context c) {
        String endpoint = "/public/getmarketsummaries";
        publicRequest(endpoint, null, c, "updateTickerActivity");
    }

    public void UpdateTickerInfo(Context c, String pair) {
        String endpoint = "/public/getticker?";
        HashMap<String, String> params = new HashMap<>();
        params.put("market", pair);
        publicRequest(endpoint, params, c, "updateTickerInfo");
    }

    public void RefreshOrderBooks(Context c, String pair) {
        String endpoint = "/public/getorderbook?";
        HashMap<String, String> params = new HashMap<>();
        params.put("market", pair);
        params.put("type", "both");
        publicRequest(endpoint, params, c, "refreshOrderBooks");
    }

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType) {
        String endpoint;
        if (orderType.equalsIgnoreCase("buy")) {
            endpoint = "/market/buylimit";
        } else {
            endpoint = "/market/selllimit";
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("market", pair);
        params.put("rate", rate);
        params.put("quantity", amount);
        privateRequest(endpoint, params, c, "placeOrder");
    }

    public void CheckOpenOrder(Context c, String orderId, String symbol) {
        Log.d(TAG, "CheckOpenOrder: " + orderId);
        String endpoint = "/account/getorder";
        HashMap<String, String> params = new HashMap<>();
        params.put("uuid", orderId);
        privateRequest(endpoint, params, c, "checkOpenOrder");
    }


    private static String createTradePair(String pair) {
        String[] parts = pair.split("_");
        return (parts[0] + "-" + parts[1]).toUpperCase();
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
