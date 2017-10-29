package com.mattcormier.cryptonade.models;

/**
 * Created by matt on 10/29/2017.
 */

public class OrderTransaction {
    private String orderNumber;
    private String timestamp;
    private String type;
    private String amount;
    private String rate;
    private String fee;

    public OrderTransaction() {
        orderNumber = "";
        timestamp = "";
        type = "";
        amount = "";
        rate = "";
        fee = "";
    }

    public OrderTransaction(String orderNumber, String timestamp, String type, String amount, String rate, String fee) {
        this.orderNumber = orderNumber;
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.rate = rate;
        this.fee = fee;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }
}
