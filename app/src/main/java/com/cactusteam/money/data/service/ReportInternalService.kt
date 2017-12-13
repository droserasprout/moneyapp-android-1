package com.cactusteam.money.data.service

import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.model.BalanceData
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.ITransactionFilter
import com.cactusteam.money.data.report.CategoriesReportData
import com.cactusteam.money.data.report.CategoriesReportItem
import com.cactusteam.money.data.report.TagsReportData
import com.cactusteam.money.data.report.TagsReportItem
import com.cactusteam.money.ui.grouping.CategoriesReportTransactionsGrouper
import com.cactusteam.money.ui.grouping.TagsReportTransactionsGrouper
import org.apache.commons.lang3.time.DateUtils
import java.util.*

/**
 * @author vpotapenko
 */
abstract class ReportInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun getCategoriesReportDataInternal(startTime: Date, endTime: Date, filter: ITransactionFilter?, type: Int): CategoriesReportData {
        val b = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(startTime)
                .putTo(endTime)
                .putConvertToMain(true)
        if (filter != null) b.putTransactionFilter(filter)

        val transactions = b.listInternal()

        val data = CategoriesReportData()

        val grouper = CategoriesReportTransactionsGrouper(type)
        val groups = grouper.group(transactions)
        for (g in groups) {
            data.allItems.add(CategoriesReportItem(g, g.getAmount(type)))
        }
        data.prepareChartItems()
        return data
    }

    fun getTagsReportDataInternal(startTime: Date, endTime: Date, filter: ITransactionFilter?, type: Int): TagsReportData {
        val b = dataManager.transactionService.newListTransactionsBuilder()
                .putFrom(startTime)
                .putTo(endTime)
                .putConvertToMain(true)
        if (filter != null) b.putTransactionFilter(filter)

        val transactions = b.listInternal()

        val data = TagsReportData()

        val grouper = TagsReportTransactionsGrouper(MoneyApp.instance, type)
        data.total = grouper.calculateTotal(transactions)

        val groups = grouper.group(transactions)
        for (g in groups) {
            data.allItems.add(TagsReportItem(g, g.getAmount(type)))
        }
        data.prepareChartItems()

        return data
    }

    fun getBalanceDataInternal(filter: ITransactionFilter, full: Boolean): List<BalanceData> {
        val start = if (full)
            System.currentTimeMillis() - MILLIS_PER_YEAR
        else
            System.currentTimeMillis() - MILLIS_PER_SIX_MONTH

        val list = LinkedList<BalanceData>()
        val period = MoneyApp.instance.appPreferences.period

        var datePair = period.current
        while (start < datePair.second.time) {
            list.add(0, BalanceData(datePair.first, datePair.second))

            datePair = period.getPrevious(datePair)
        }

        for (data in list) {
            val transactions = dataManager.transactionService
                    .newListTransactionsBuilder()
                    .putFrom(data.from)
                    .putTo(data.to)
                    .putTransactionFilter(filter)
                    .putConvertToMain(true)
                    .listInternal()
            for (t in transactions) {
                when (t.type) {
                    Transaction.EXPENSE -> {
                        data.expense += t.amount
                    }
                    Transaction.INCOME -> {
                        data.income += t.amount
                    }
                }
            }
            data.profit = data.income - data.expense
        }

        if (full) {
            for (d in list) {
                val totals = dataManager.accountService.getTotalsInternal(d.to)
                d.balance = totals.total
            }
        }

        return list
    }

    companion object {
        private val MILLIS_PER_YEAR = DateUtils.MILLIS_PER_DAY * 365
        private val MILLIS_PER_SIX_MONTH = DateUtils.MILLIS_PER_DAY * 180
    }
}