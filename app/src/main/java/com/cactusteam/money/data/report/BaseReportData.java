package com.cactusteam.money.data.report;

import com.cactusteam.money.ui.UiConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vpotapenko
 */

public abstract class BaseReportData<T extends BaseReportItem> {

    private static final float MIN_CHART_PERCENT = 5f;
    private static final int MIN_CHART_ITEMS = 3;

    public double total;

    public final List<T> allItems = new ArrayList<>();
    public final List<T> chartItems = new ArrayList<>();

    public void prepareChartItems() {
        calculatePercents();

        List<T> acceptedItems = new ArrayList<>();
        List<T> declinedItems = new ArrayList<>();

        // split on two collections
        for (T item : chartItems) {
            if (item.getPercent() < MIN_CHART_PERCENT) {
                declinedItems.add(item);
            } else {
                acceptedItems.add(item);
            }
        }

        // increase accepted to min item count
        while (!declinedItems.isEmpty() && acceptedItems.size() < MIN_CHART_ITEMS) {
            acceptedItems.add(declinedItems.remove(0));
        }

        if (!declinedItems.isEmpty()) {
            if (declinedItems.size() == 1) {
                acceptedItems.add(declinedItems.remove(0)); // just copy if only one declined item
            } else {
                double amount = 0;
                float percent = 0;
                for (T item : declinedItems) {
                    amount += item.getAmount();
                    percent += item.getPercent();

                    item.setPercent(null);
                }
                acceptedItems.add(createOther(amount, percent));
            }
        }

        chartItems.clear();
        chartItems.addAll(acceptedItems);
    }

    protected abstract T createOther(double amount, float percent);

    private void calculatePercents() {
        total = 0;
        for (T item : allItems) {
            total += item.getAmount();
        }

        double totalChart = 0;
        for (T item : allItems) {
            if (chartItems.size() >= UiConstants.INSTANCE.getUI_COLORS().length) break;

            chartItems.add(item);
            totalChart += item.getAmount();
        }

        for (T item : chartItems) {
            item.setPercent(((float) item.getAmount()) / ((float) totalChart) * 100f);
        }
    }

}
