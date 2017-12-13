package com.cactusteam.money.data.currency;

/**
 * @author vpotapenko
 */
public abstract class AbstractRateLoaderItem implements IRateLoaderItem {

    protected String code1;
    protected String code2;

    protected Double rate;

    @Override
    public void initialize(String code1, String code2) {
        this.code1 = code1;
        this.code2 = code2;
        this.rate = null;
    }

    @Override
    public boolean hasRate() {
        return rate != null;
    }

    @Override
    public String getResultCode1() {
        return code1;
    }

    @Override
    public String getResultCode2() {
        return code2;
    }

    @Override
    public Double getResultRate() {
        return rate;
    }
}
