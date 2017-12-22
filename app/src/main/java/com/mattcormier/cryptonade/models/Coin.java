package com.mattcormier.cryptonade.models;

/**
 * Created by matt on 12/22/2017.
 */

public class Coin {
    private String name;
    private String symbol;
    private int rank;
    private double priceBTC;
    private double priceUSD;
    private double cap;
    private double volume;
    private double oneHour;
    private double twentyFourHour;
    private double sevenDay;

    public Coin(String name, String symbol, int rank, double priceBTC, double priceUSD, double cap, double volume, double oneHour, double twentyFourHour, double sevenDay) {
        this.name = name;
        this.symbol = symbol;
        this.rank = rank;
        this.priceBTC = priceBTC;
        this.priceUSD = priceUSD;
        this.cap = cap;
        this.volume = volume;
        this.oneHour = oneHour;
        this.twentyFourHour = twentyFourHour;
        this.sevenDay = sevenDay;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getPriceBTC() {
        return priceBTC;
    }

    public void setPriceBTC(double priceBTC) {
        this.priceBTC = priceBTC;
    }

    public double getPriceUSD() {
        return priceUSD;
    }

    public void setPriceUSD(double priceUSD) {
        this.priceUSD = priceUSD;
    }

    public double getCap() {
        return cap;
    }

    public void setCap(double cap) {
        this.cap = cap;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getOneHour() {
        return oneHour;
    }

    public void setOneHour(double oneHour) {
        this.oneHour = oneHour;
    }

    public double getTwentyFourHour() {
        return twentyFourHour;
    }

    public void setTwentyFourHour(double twentyFourHour) {
        this.twentyFourHour = twentyFourHour;
    }

    public double getSevenDay() {
        return sevenDay;
    }

    public void setSevenDay(double sevenDay) {
        this.sevenDay = sevenDay;
    }
}
