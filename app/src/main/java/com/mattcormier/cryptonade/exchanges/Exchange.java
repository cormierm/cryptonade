package com.mattcormier.cryptonade.exchanges;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.util.HashMap;

/**
 * Created by matt on 10/17/2017.
 */

public class Exchange {
    private long exchangeId;
    private long typeId;
    private String name;
    private String apiKey;
    private String apiSecret;
    private String apiOther;

    public Exchange() {
        typeId = 0;
        name = "";
        apiKey = "";
        apiSecret = "";
        apiOther = "";
    }

    public Exchange(int exchangeId, int typeId, String name, String apiKey, String apiSecret,
                    String apiOther) {
        this.exchangeId = exchangeId;
        this.typeId = typeId;
        this.name = name;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiOther = apiOther;
    }

    public void RefreshBalances(Context c) {}

    public void UpdateBalanceBar(Context c) {}

    public void CancelOrder(Context c, String orderNumber) {}

    public void UpdateOpenOrders(Context c) {}

    public void UpdateTickerActivity(Context c) {}

    public void UpdateTradeTickerInfo(Context c){}

    public void PlaceOrder(Context c, String pair, String rate, String amount, String orderType, TextView tv) {}

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
}
