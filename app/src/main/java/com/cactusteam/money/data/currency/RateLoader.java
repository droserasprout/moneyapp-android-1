package com.cactusteam.money.data.currency;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vpotapenko
 */
public class RateLoader {

    private String code1;
    private String code2;
    private Double rate;

    private final List<IRateLoaderItem> chain = new ArrayList<>();

    public RateLoader() {
        chain.add(new StaticConvertRateLoaderItem());
        chain.add(new StaticMapRateLoaderItem());
        chain.add(new YahooRateLoaderItem());
    }

    public void initialize(String code1, String code2) {
        this.code1 = code1;
        this.code2 = code2;
    }

    public void load() {
        for (IRateLoaderItem item : chain) {
            item.initialize(code1, code2);
            item.load();
            if (item.hasRate()) {
                code1 = item.getResultCode1();
                code2 = item.getResultCode2();
                rate = item.getResultRate();
                break;
            }
        }
    }

    public String getCode1() {
        return code1;
    }

    public String getCode2() {
        return code2;
    }

    public Double getRate() {
        return rate;
    }
}
