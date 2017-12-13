package com.cactusteam.money.data.currency;

import com.cactusteam.money.data.DataUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionContext;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author vpotapenko
 */
class YahooRateLoaderItem extends AbstractRateLoaderItem {

    private static final String URL_PATTERN = "http://finance.yahoo.com/d/quotes.csv?s=%s%s=X&f=sl1d1t1ba&e=.csv";

    @Override
    public void initialize(String code1, String code2) {
        super.initialize(code1, code2);
    }

    public void load() {
        loadRate();
        if (rate == null || rate < 1) {
            String tmp = code2;
            code2 = code1;
            code1 = tmp;
            loadRate();
        }
    }

    void loadRate() {
        try {
            String url = String.format(URL_PATTERN, code1, code2);

            URL requestUrl = new URL(url);
            URLConnection connection = requestUrl.openConnection();

            String content = IOUtils.toString(connection.getInputStream());

            parseContent(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseContent(String content) {
        String[] parts = content.split(",");
        if (parts.length < 2) return;

        String rateStr = parts[1];

        try {
            rate = Double.parseDouble(rateStr);
            rate = DataUtils.INSTANCE.round(rate, 4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
