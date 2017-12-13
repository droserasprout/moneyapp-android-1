package com.cactusteam.money.data.model;

import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author vpotapenko
 */
public class Totals {

    public final List<String> currencyCodes = new ArrayList<>();
    public final Map<String, Double> map = new ArrayMap<>();

    public String mainCurrencyCode;
    public double total;

    public void prepare(String mainCurrencyCode) {
        this.mainCurrencyCode = mainCurrencyCode;

        currencyCodes.add(mainCurrencyCode);
        if (!map.containsKey(mainCurrencyCode)) map.put(mainCurrencyCode, 0.);

        for (String k : map.keySet()) {
            if (!TextUtils.equals(k, mainCurrencyCode)) currencyCodes.add(k);
        }
    }

    public void putAmount(String currencyCode, double amount) {
        Double total = map.get(currencyCode);
        map.put(currencyCode, total == null ? amount : total + amount);
    }
}
