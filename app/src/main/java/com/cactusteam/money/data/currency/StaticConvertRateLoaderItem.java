package com.cactusteam.money.data.currency;

import android.support.v4.util.ArrayMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * List of known static convert rates
 *
 * @author vpotapenko
 */
class StaticConvertRateLoaderItem extends AbstractRateLoaderItem {

    private final Map<String, List<StaticRate>> currencies = new ArrayMap<>();

    StaticConvertRateLoaderItem() {
        currencies.put("BYN", Collections.singletonList(new StaticRate("BYR", 10000.)));
    }

    @Override
    public void load() {
        Double staticRate = extractRate(code1, code2);
        if (staticRate != null) {
            rate = staticRate;
        } else {
            staticRate = extractRate(code2, code1);
            if (staticRate != null) {
                String tmp = code1;
                code1 = code2;
                code2 = tmp;
                rate = staticRate;
            }
        }
    }

    private Double extractRate(String code1, String code2) {
        List<StaticRate> staticRates = currencies.get(code1);
        if (staticRates == null) return null;

        for (StaticRate rate : staticRates) {
            if (rate.destCode.equals(code2)) return rate.rate;
        }
        return null;
    }

    private static class StaticRate {

        final String destCode;
        public final double rate;

        StaticRate(String destCode, double rate) {
            this.destCode = destCode;
            this.rate = rate;
        }
    }
}
