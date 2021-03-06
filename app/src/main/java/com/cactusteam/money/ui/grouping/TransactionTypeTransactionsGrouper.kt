package com.cactusteam.money.ui.grouping

import android.content.Context
import android.support.v4.util.ArrayMap
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Transaction
import java.util.*

/**
 * @author vpotapenko
 */
class TransactionTypeTransactionsGrouper(private val context: Context) : TransactionsGrouper() {

    override fun group(transactions: List<Transaction>): List<TransactionsGrouper.Item> {
        val groups = ArrayMap<String, MutableList<Transaction>>()
        for (transaction in transactions) {
            val groupName: String
            if (transaction.type == Transaction.EXPENSE) {
                groupName = context.getString(R.string.expense_label)
            } else if (transaction.type == Transaction.INCOME) {
                groupName = context.getString(R.string.income_label)
            } else {
                groupName = context.getString(R.string.transfer_label)
            }
            var transactionList: MutableList<Transaction>? = groups[groupName]
            if (transactionList == null) {
                transactionList = ArrayList<Transaction>()
                groups.put(groupName, transactionList)
            }
            transactionList.add(transaction)
        }

        val result = ArrayList<TransactionsGrouper.Item>()
        val groupNames = arrayOf(context.getString(R.string.expense_label), context.getString(R.string.income_label), context.getString(R.string.transfer_label))
        for (groupName in groupNames) {
            val transactionList = groups[groupName] ?: continue

            val currentGroup = TransactionsGrouper.Group(-1, groupName)
            result.add(TransactionsGrouper.Item(TransactionsGrouper.GROUP, currentGroup))
            for (transaction in transactionList) {
                result.add(TransactionsGrouper.Item(TransactionsGrouper.TRANSACTION, transaction))
                if (transaction.type == Transaction.EXPENSE) {
                    currentGroup.expense = currentGroup.expense - transaction.amountInMainCurrency
                } else if (transaction.type == Transaction.INCOME) {
                    currentGroup.income = currentGroup.income + transaction.amountInMainCurrency
                }
            }
        }
        return result
    }
}
