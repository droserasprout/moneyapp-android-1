package com.cactusteam.money.ui.grouping

import android.content.Context
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Transaction
import java.util.*

/**
 * @author vpotapenko
 */
class TagTransactionsGrouper(context: Context) : TransactionsGrouper() {

    private val withoutGroupName: String

    init {
        withoutGroupName = context.getString(R.string.without_tags)
    }

    override fun group(transactions: List<Transaction>): List<TransactionsGrouper.Item> {
        val groups = TreeMap<String, MutableList<Transaction>>()
        for (transaction in transactions) {
            if (transaction.tags.isEmpty()) {
                val groupName = withoutGroupName
                var transactionList: MutableList<Transaction>? = groups[groupName]
                if (transactionList == null) {
                    transactionList = ArrayList<Transaction>()
                    groups.put(groupName, transactionList)
                }
                transactionList.add(transaction)
            } else {
                for (transactionTag in transaction.tags) {
                    val groupName = transactionTag.tag.name
                    var transactionList: MutableList<Transaction>? = groups[groupName]
                    if (transactionList == null) {
                        transactionList = ArrayList<Transaction>()
                        groups.put(groupName, transactionList)
                    }
                    transactionList.add(transaction)
                }
            }
        }

        val withoutGroupGroup = groups.remove(withoutGroupName)

        val result = ArrayList<TransactionsGrouper.Item>()
        for (groupName in groups.keys) {
            val currentGroup = TransactionsGrouper.Group(-1, groupName)
            result.add(TransactionsGrouper.Item(TransactionsGrouper.GROUP, currentGroup))
            for (transaction in groups[groupName]!!) {
                result.add(TransactionsGrouper.Item(TransactionsGrouper.TRANSACTION, transaction))
                if (transaction.type == Transaction.EXPENSE) {
                    currentGroup.expense = currentGroup.expense - transaction.amountInMainCurrency
                } else if (transaction.type == Transaction.INCOME) {
                    currentGroup.income = currentGroup.income + transaction.amountInMainCurrency
                }
            }
        }

        if (withoutGroupGroup != null) {
            val currentGroup = TransactionsGrouper.Group(-1, withoutGroupName)
            result.add(TransactionsGrouper.Item(TransactionsGrouper.GROUP, currentGroup))
            for (transaction in withoutGroupGroup) {
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
