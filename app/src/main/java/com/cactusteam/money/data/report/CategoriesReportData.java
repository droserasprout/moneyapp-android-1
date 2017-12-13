package com.cactusteam.money.data.report;

/**
 * @author vpotapenko
 */

public class CategoriesReportData extends BaseReportData<CategoriesReportItem> {

    @Override
    protected CategoriesReportItem createOther(double amount, float percent) {
        CategoriesReportItem item = new CategoriesReportItem(null, amount);
        item.setPercent(percent);
        return item;
    }
}
