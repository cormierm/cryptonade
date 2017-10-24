package com.mattcormier.cryptonade.models;

/**
 * Created by matt on 10/24/2017.
 */

public class ExchangeType {
    private long typeId;
    private String name;
    private String apiOther;

    public ExchangeType() {
        typeId = 0;
        name = "";
        apiOther = "";
    }

    public ExchangeType(long typeId, String name, String apiOther) {
        this.typeId = typeId;
        this.name = name;
        this.apiOther = apiOther;
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

    public String getApiOther() {
        return apiOther;
    }

    public void setApiOther(String apiOther) {
        this.apiOther = apiOther;
    }

    @Override
    public String toString() {
        return name;
    }
}
