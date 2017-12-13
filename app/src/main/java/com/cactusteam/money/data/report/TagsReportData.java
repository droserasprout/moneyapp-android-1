package com.cactusteam.money.data.report;

/**
 * @author vpotapenko
 */

public class TagsReportData extends BaseReportData<TagsReportItem> {


    @Override
    protected TagsReportItem createOther(double amount, float percent) {
        TagsReportItem item = new TagsReportItem(null, amount);
        item.setPercent(percent);
        return item;
    }
}
