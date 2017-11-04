package com.mattcormier.cryptonade.clients;

import android.content.Context;

import java.util.HashMap;

/**
 * Created by matt on 10/17/2017.
 */

public interface APIClient {

    void UpdateBalances(Context c);

    void RestorePairsInDB(Context c);

    void CancelOrder(Context c, String orderNumber);

    void UpdateOpenOrders(Context c);

    void UpdateOrderTransactions(Context c, String pair);

    void UpdateTickerActivity(Context c);

    void UpdateTradeTickerInfo(Context c, String pair);

    void UpdateTickerInfo(Context c, String pair);

    void PlaceOrder(Context c, String pair, String rate, String amount, String orderType);

    long getId();

    long getTypeId();

    String getName();

    String toString();

    public HashMap<String, Double> getBalances();

    public HashMap<String, Double> getAvailableBalances();

    public HashMap<String, String> getTickerInfo();
}
