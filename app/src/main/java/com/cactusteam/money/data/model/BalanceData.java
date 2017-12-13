package com.cactusteam.money.data.model;

import java.util.Date;

/**
 * @author vpotapenko
 */
public class BalanceData {

    public Date from;
    public Date to;

    public double expense;
    public double income;
    public double profit;
    public double balance;

    public BalanceData(Date from, Date to) {
        this.from = from;
        this.to = to;
    }
}
