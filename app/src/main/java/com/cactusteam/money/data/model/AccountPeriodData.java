package com.cactusteam.money.data.model;

import com.cactusteam.money.data.DataUtils;

import java.util.Date;

/**
 * @author vpotapenko
 */
public class AccountPeriodData {

    public Date from;
    public Date to;

    public double expense;
    public double income;
    public double transfer;
    public double initial;

    public String currencyCode;

    public double getTotal() {
        return DataUtils.INSTANCE.round(initial - expense + income + transfer, 2);
    }
}
