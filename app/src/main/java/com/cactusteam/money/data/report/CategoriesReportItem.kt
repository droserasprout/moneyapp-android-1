package com.cactusteam.money.data.report

import com.cactusteam.money.ui.grouping.TransactionsGrouper

/**
 * @author vpotapenko
 */

class CategoriesReportItem(val group: TransactionsGrouper.Group?, amount: Double) : BaseReportItem(amount)
