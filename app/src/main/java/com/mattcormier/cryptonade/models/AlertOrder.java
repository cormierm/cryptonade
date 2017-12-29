package com.mattcormier.cryptonade.models;

/**
 * Created by matt on 12/29/2017.
 */

public class AlertOrder {
    private String orderId;
    private int exchangeId;
    private String symbol;

    public AlertOrder() {
        this.orderId = "";
        this.exchangeId = 0;
        this.symbol = "";
    }

    public AlertOrder(String orderId, int exchangeId, String symbol) {
        this.orderId = orderId;
        this.exchangeId = exchangeId;
        this.symbol = symbol;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(int exchangeId) {
        this.exchangeId = exchangeId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
