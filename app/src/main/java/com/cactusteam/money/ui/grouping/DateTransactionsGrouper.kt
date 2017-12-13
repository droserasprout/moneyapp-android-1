package com.cactusteam.money.ui.grouping

import android.content.Context
import android.text.format.DateUtils
import com.cactusteam.money.data.dao.Transaction
import java.util.*

/**
 * @author vpotapenko
 */
class DateTransactionsGrouper(private val context: Context) : TransactionsGrouper() {

    override fun group(transactions: List<Transaction>): List<TransactionsGrouper.Item> {
        if (transactions.isEmpty()) return emptyList()

        Collections.sort(transactions, TRANSACTION_COMPARATOR)

        val result = ArrayList<TransactionsGrouper.Item>()

        var currentDate: Date? = null
        var currentGroup: TransactionsGrouper.Group? = null
        for (transaction in transactions) {
            if (currentDate == null || !org.apache.commons.lang3.time.DateUtils.isSameDay(currentDate, transaction.date)) {
                currentDate = transaction.date
                currentGroup = TransactionsGrouper.Group(-1, DateUtils.formatDateTime(context, currentDate!!.time, DateUtils.FORMAT_SHOW_DATE))
                result.add(TransactionsGrouper.Item(TransactionsGrouper.GROUP, currentGroup))
            }
            result.add(TransactionsGrouper.Item(TransactionsGrouper.TRANSACTION, transaction))
            if (transaction.type == Transaction.EXPENSE) {
                currentGroup!!.expense = currentGroup.expense - transaction.amountInMainCurrency
            } else if (transaction.type == Transaction.INCOME) {
                currentGroup!!.income = currentGroup.income + transaction.amountInMainCurrency
            }
        }
        return result
    }

    companion object {

        private val TRANSACTION_COMPARATOR = Comparator<Transaction> { lhs, rhs -> rhs.date.compareTo(lhs.date) }
    }
}
