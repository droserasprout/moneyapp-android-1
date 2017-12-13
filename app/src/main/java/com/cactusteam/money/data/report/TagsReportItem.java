package com.cactusteam.money.data.report;

import com.cactusteam.money.ui.grouping.TransactionsGrouper;

/**
 * @author vpotapenko
 */

public class TagsReportItem extends BaseReportItem {

    public final TransactionsGrouper.Group group;

    public TagsReportItem(TransactionsGrouper.Group group, double amount) {
        super(amount);
        this.group = group;
    }
}
