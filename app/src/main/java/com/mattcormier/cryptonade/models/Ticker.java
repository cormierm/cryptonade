package com.mattcormier.cryptonade.models;

/**
 * Created by matt on 10/23/2017.
 */

public class Ticker {
    private String pair;
    private String last;
    private String volume;
    private String lowestAsk;
    private String lowest24hr;
    private String highestBid;
    private String highest24hr;

    public Ticker(String pair, String last, String volume, String lowestAsk, String lowest24hr, String highestBid, String highest24hr) {
        this.pair = pair;
        this.last = last;
        this.volume = volume;
        this.lowestAsk = lowestAsk;
        this.lowest24hr = lowest24hr;
        this.highestBid = highestBid;
        this.highest24hr = highest24hr;
    }

    public String getPair() {
        return pair;
    }

    public void setPair(String pair) {
        this.pair = pair;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getLowestAsk() {
        return lowestAsk;
    }

    public void setLowestAsk(String lowestAsk) {
        this.lowestAsk = lowestAsk;
    }

    public String getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(String highestBid) {
        this.highestBid = highestBid;
    }

    public String getLowest24hr() {
        return lowest24hr;
    }

    public void setLowest24hr(String lowest24hr) {
        this.lowest24hr = lowest24hr;
    }

    public String getHighest24hr() {
        return highest24hr;
    }

    public void setHighest24hr(String highest24hr) {
        this.highest24hr = highest24hr;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getBuyPair() {
        String buyPair = pair.split("-")[0];
        if (buyPair != null)
            return buyPair;
        else
            return "";
    }

    public String getSellPair() {
        String sellPair = pair.split("-")[1];
        if (sellPair != null)
            return sellPair;
        else
            return "";
    }

    @Override
    public String toString() {
        return "pair=" + pair;
    }
}
