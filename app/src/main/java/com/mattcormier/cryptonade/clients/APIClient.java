package com.mattcormier.cryptonade.clients;

import android.content.Context;

/**
 * Created by matt on 10/17/2017.
 */

public interface APIClient {

    void RestorePairsInDB(Context c);

    void RefreshBalances(Context c);

    void UpdateBalanceBar(Context c);

    void CancelOrder(Context c, String orderNumber);

    void UpdateOpenOrders(Context c);

    void UpdateTickerActivity(Context c);

    void UpdateTradeTickerInfo(Context c);

    void PlaceOrder(Context c, String pair, String rate, String amount, String orderType);

    long getId();

    long getTypeId();

    String getName();

    String toString();
}
