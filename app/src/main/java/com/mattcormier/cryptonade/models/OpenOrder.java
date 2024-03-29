package com.mattcormier.cryptonade.models;

/**
 * Filename: OpenOrder.java
 * Description: Model for open orders
 * Created by Matt Cormier on 10/18/2017.
 */

public class OpenOrder {
    private int exchangeId;
    private String orderNumber;
    private String tradePair;
    private String type;
    private String rate;
    private String startAmount;
    private String remainingAmount;
    private String date;

    public OpenOrder() {
        exchangeId = 0;
        orderNumber = "";
        tradePair = "";
        type = "";
        rate = "";
        startAmount = "";
        remainingAmount = "";
        date = "";
    }

    public OpenOrder(int exchangeId, String orderNumber, String tradePair, String type, String rate,
                     String startAmount, String remainingAmount, String date) {
        this.exchangeId = exchangeId;
        this.orderNumber = orderNumber;
        this.tradePair = tradePair;
        this.type = type;
        this.rate = rate;
        this.startAmount = startAmount;
        this.remainingAmount = remainingAmount;
        this.date = date;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(int exchangeId) {
        this.exchangeId = exchangeId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getTradePair() {
        return tradePair;
    }

    public void setTradePair(String tradePair) {
        this.tradePair = tradePair;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public String getStartAmount() {
        return startAmount;
    }

    public void setStartAmount(String startAmount) {
        this.startAmount = startAmount;
    }

    public String getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(String remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Order Number: " + orderNumber;
    }
}
