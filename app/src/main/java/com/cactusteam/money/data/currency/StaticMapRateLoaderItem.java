package com.cactusteam.money.data.currency;

import android.util.Pair;

import com.cactusteam.money.data.DataUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts rates from unknown currency to known currency
 * @author vpotapenko
 */
class StaticMapRateLoaderItem extends AbstractRateLoaderItem {

    private final Map<String, Pair<String, Double>> map = new HashMap<>();

    private final YahooRateLoaderItem yahooRateLoaderItem = new YahooRateLoaderItem();

    StaticMapRateLoaderItem() {
        map.put("BYR", new Pair<>("BYN", 10000.));
    }

    @Override
    public void load() {
        Pair<String, Double> mapValue = map.get(code1);
        if (mapValue != null) {
            yahooRateLoaderItem.initialize(mapValue.first, code2);
            yahooRateLoaderItem.loadRate();
            if (yahooRateLoaderItem.hasRate()) {
                rate = DataUtils.INSTANCE.round(yahooRateLoaderItem.getResultRate() / mapValue.second, 4);
                normalizeRate();
            }
        } else {
            mapValue = map.get(code2);
            if (mapValue != null) {
                yahooRateLoaderItem.initialize(code1, mapValue.first);
                yahooRateLoaderItem.loadRate();
                if (yahooRateLoaderItem.hasRate()) {
                    rate = DataUtils.INSTANCE.round(yahooRateLoaderItem.getResultRate() * mapValue.second, 4);
                    normalizeRate();
                }
            }
        }
    }

    private void normalizeRate() {
        if (rate < 1 && rate > 0) {
            rate = DataUtils.INSTANCE.round(1 / rate, 4);
            String tmp = code2;
            code2 = code1;
            code1 = tmp;
        }
    }
}
