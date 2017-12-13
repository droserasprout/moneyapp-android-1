package com.cactusteam.money.data.currency;

/**
 * @author vpotapenko
 */
public interface IRateLoaderItem {

    void initialize(String code1, String code2);

    void load();
    boolean hasRate();

    String getResultCode1();

    String getResultCode2();

    Double getResultRate();
}
