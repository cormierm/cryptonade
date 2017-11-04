package com.mattcormier.cryptonade.models;

import android.content.Context;

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
    private int active;

    public Exchange() {
        typeId = 0;
        name = "";
        apiKey = "";
        apiSecret = "";
        apiOther = "";
        active = 0;
    }

    public Exchange(int exchangeId, int typeId, String name, String apiKey, String apiSecret,
                    String apiOther, int active) {
        this.exchangeId = exchangeId;
        this.typeId = typeId;
        this.name = name;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiOther = apiOther;
        this.active = active;
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

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
