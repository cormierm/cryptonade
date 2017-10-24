package com.mattcormier.cryptonade.models;

/**
 * Created by matt on 10/17/2017.
 */

public class Pair {
    private long pairId;
    private long exchangeId;
    private String exchangePair;
    private String tradingPair;

    public Pair() {
        exchangeId = 0;
        exchangePair = "";
        tradingPair = "";
    }

    public Pair(int pairId, int exchangeId, String exchangePair, String tradingPair) {
        this.pairId = pairId;
        this.exchangeId = exchangeId;
        this.exchangePair = exchangePair;
        this.tradingPair = tradingPair;
    }

    public long getId() {
        return pairId;
    }

    public void setId(long pairId) {
        this.pairId = pairId;
    }

    public long getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(long exchangeId) {
        this.exchangeId = exchangeId;
    }

    public String getExchangePair() {
        return exchangePair;
    }

    public void setExchangePair(String exchangePair) {
        this.exchangePair = exchangePair;
    }

    public String getTradingPair() {
        return tradingPair;
    }

    public void setTradingPair(String tradingPair) {
        this.tradingPair = tradingPair;
    }

    @Override
    public String toString() {
        return tradingPair;
    }
}
