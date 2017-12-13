package com.cactusteam.money.data.currency;

/**
 * @author vpotapenko
 */
public class MCurrency {

    public final String currencyCode;
    public final String displayName;
    public final int fraction;

    public MCurrency(String currencyCode, String displayName, int fraction) {
        this.currencyCode = currencyCode;
        this.displayName = displayName;
        this.fraction = fraction;
    }

    public static MCurrency fromResourceItem(String item) {
        String[] parts = item.split(":");
        return parts.length == 3 ?
                new MCurrency(parts[0], parts[1], Integer.parseInt(parts[2])) : null;
    }

    public String getDisplayString() {
        return String.format("%s - %s", currencyCode, displayName);
    }

    @Override
    public String toString() {
        return getDisplayString();
    }
}
