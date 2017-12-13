package com.cactusteam.money.ui.grouping

import android.content.Context
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Transaction
import java.util.*

/**
 * @author vpotapenko
 */
class OneGroupTransactionsGrouper(private val context: Context) : TransactionsGrouper() {

    override fun group(transactions: List<Transaction>): List<TransactionsGrouper.Item> {
        val result = ArrayList<TransactionsGrouper.Item>()
        val currentGroup = TransactionsGrouper.Group(-1, context.getString(R.string.grouping_without_group))
        result.add(TransactionsGrouper.Item(TransactionsGrouper.GROUP, currentGroup))
        for (transaction in transactions) {
            result.add(TransactionsGrouper.Item(TransactionsGrouper.TRANSACTION, transaction))
            if (transaction.type == Transaction.EXPENSE) {
                currentGroup.expense = currentGroup.expense - transaction.amountInMainCurrency
            } else if (transaction.type == Transaction.INCOME) {
                currentGroup.income = currentGroup.income + transaction.amountInMainCurrency
            }
        }
        return result
    }
}
