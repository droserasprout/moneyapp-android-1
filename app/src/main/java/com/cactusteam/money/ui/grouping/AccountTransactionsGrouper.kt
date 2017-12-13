package com.cactusteam.money.ui.grouping

import android.support.v4.util.ArrayMap
import com.cactusteam.money.data.dao.Transaction
import java.util.*

/**
 * @author vpotapenko
 */
class AccountTransactionsGrouper : TransactionsGrouper() {

    override fun group(transactions: List<Transaction>): List<TransactionsGrouper.Item> {
        val groups = TreeMap<String, MutableList<Transaction>>()
        val accountIds = ArrayMap<String, Long>()
        for (transaction in transactions) {
            val sourceAccount = transaction.sourceAccount
            val accountName = sourceAccount.name
            var transactionList: MutableList<Transaction>? = groups[accountName]
            if (transactionList == null) {
                transactionList = ArrayList<Transaction>()
                groups.put(accountName, transactionList)
                accountIds.put(accountName, sourceAccount.id)
            }
            transactionList.add(transaction)
        }

        val result = ArrayList<TransactionsGrouper.Item>()
        for (groupName in groups.keys) {
            val id = accountIds[groupName]
            val currentGroup = TransactionsGrouper.Group(id ?: -1, groupName)
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
        return result
    }
}
