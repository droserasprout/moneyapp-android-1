package com.cactusteam.money.data.currency;

import android.content.Context;
import android.text.TextUtils;

import com.cactusteam.money.R;
import com.cactusteam.money.data.CurrencyUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author vpotapenko
 */
public class CurrencyManager {

    private static final Comparator<MCurrency> CURRENCY_COMPARATOR = new Comparator<MCurrency>() {
        @Override
        public int compare(MCurrency c1, MCurrency c2) {
            return c1.currencyCode.compareTo(c2.currencyCode);
        }
    };

    private final Context context;

    private final CurrencyFormatter currencyFormatter;

    public CurrencyManager(Context context) {
        this.context = context;
        this.currencyFormatter = new CurrencyFormatter();
    }

    public CurrencyFormatter getCurrencyFormatter() {
        return currencyFormatter;
    }

    public MCurrency getLocaleCurrency() {
        String localeCurrencyCode = CurrencyUtils.INSTANCE.getLocaleCurrencyCode();
        return getCurrencyByCode(localeCurrencyCode);
    }

    public MCurrency getCurrencyByCode(String code) {
        String[] array = context.getResources().getStringArray(R.array.all_currencies);
        for (String item : array) {
            MCurrency currency = MCurrency.fromResourceItem(item);
            if (currency != null && TextUtils.equals(currency.currencyCode, code)) {
                return currency;
            }
        }
        return new MCurrency(code, code, 2);
    }

    public MCurrency findCurrencyByCode(String code) {
        String[] array = context.getResources().getStringArray(R.array.all_currencies);
        for (String item : array) {
            MCurrency currency = MCurrency.fromResourceItem(item);
            if (currency != null && TextUtils.equals(currency.currencyCode, code)) {
                return currency;
            }
        }
        return null;
    }

    public MCurrency findCurrencyByName(String name) {
        String[] array = context.getResources().getStringArray(R.array.all_currencies);
        for (String item : array) {
            MCurrency currency = MCurrency.fromResourceItem(item);
            if (currency != null && TextUtils.equals(currency.displayName, name)) {
                return currency;
            }
        }
        return null;
    }

    public List<MCurrency> loadAll() {
        return loadCurrencies(R.array.all_currencies, false);
    }

    public List<MCurrency> loadShort() {
        return loadCurrencies(R.array.short_currencies, true);
    }

    private List<MCurrency> loadCurrencies(int resId, boolean addLocalCurrency) {
        List<MCurrency> result = new LinkedList<>();

        String[] array = context.getResources().getStringArray(resId);
        for (String item : array) {
            MCurrency currency = MCurrency.fromResourceItem(item);
            if (currency != null) result.add(currency);
        }
        if (addLocalCurrency) addLocaleCurrency(result);

        Collections.sort(result, CURRENCY_COMPARATOR);
        return result;
    }

    private void addLocaleCurrency(List<MCurrency> currencies) {
        String localeCurrencyCode = CurrencyUtils.INSTANCE.getLocaleCurrencyCode();
        MCurrency localCurrency = null;
        for (MCurrency currency : currencies) {
            if (TextUtils.equals(currency.currencyCode, localeCurrencyCode)) {
                localCurrency = currency;
                break;
            }
        }

        if (localCurrency == null) {
            currencies.add(getCurrencyByCode(localeCurrencyCode));
        }
    }

}
