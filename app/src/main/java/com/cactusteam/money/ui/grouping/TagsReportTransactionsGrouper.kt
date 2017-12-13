package com.cactusteam.money.ui.grouping

import android.content.Context
import android.support.v4.util.ArrayMap
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import java.util.*

/**
 * @author vpotapenko
 */
class TagsReportTransactionsGrouper(context: Context, private val type: Int) {

    private val GROUP_NAME_COMPARATOR = Comparator<TransactionsGrouper.Group> { lhs, rhs ->
        if (type == Category.EXPENSE) {
            java.lang.Double.compare(rhs.expense, lhs.expense)
        } else {
            java.lang.Double.compare(rhs.income, lhs.income)
        }
    }

    private val withoutGroupName: String

    init {
        withoutGroupName = context.getString(R.string.without_tags)
    }

    fun group(transactions: List<Transaction>): List<TransactionsGrouper.Group> {
        val groups = TreeMap<String, MutableList<Transaction>>()
        val tagIds = ArrayMap<String, Long>()

        for (transaction in transactions) {
            if (transaction.type != type) continue

            val tags = transaction.tags
            for (tag in tags) {
                val t = tag.tag
                handleTag(transaction, t.name, groups)
                tagIds.put(t.name, t.id)
            }
            if (tags.isEmpty()) handleTag(transaction, withoutGroupName, groups)
        }

        val result = ArrayList<TransactionsGrouper.Group>()
        for (groupName in groups.keys) {
            val tagId = tagIds[groupName]
            val currentGroup = TransactionsGrouper.Group(tagId ?: -1, groupName)
            result.add(currentGroup)

            for (transaction in groups[groupName]!!) {
                if (transaction.type == Transaction.EXPENSE) {
                    currentGroup.expense = currentGroup.expense + transaction.amountInMainCurrency
                } else if (transaction.type == Transaction.INCOME) {
                    currentGroup.income = currentGroup.income + transaction.amountInMainCurrency
                }
            }
        }
        result.sortWith(GROUP_NAME_COMPARATOR)
        return result
    }

    private fun handleTag(transaction: Transaction, tag: String, groups: MutableMap<String, MutableList<Transaction>>) {
        var transactionList: MutableList<Transaction>? = groups[tag]
        if (transactionList == null) {
            transactionList = ArrayList<Transaction>()
            groups.put(tag, transactionList)
        }
        transactionList.add(transaction)
    }

    fun calculateTotal(transactions: List<Transaction>): Double {
        return transactions
                .filter { it.type == type }
                .sumByDouble { it.amountInMainCurrency }
    }
}
