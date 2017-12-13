package com.cactusteam.money.sync.model;

/**
 * @author vpotapenko
 */
public class SyncAccount extends SyncObject {

    public int type;

    public String name;
    public String currencyCode;
    public String color;
    public boolean skipInBalance;

    public boolean deleted;
}
