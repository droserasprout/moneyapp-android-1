package com.cactusteam.money.ui.grouping

import android.support.v4.util.ArrayMap
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import java.util.*

/**
 * @author vpotapenko
 */
class CategoriesReportTransactionsGrouper(private var type: Int) {

    private val GROUP_NAME_COMPARATOR = Comparator<TransactionsGrouper.Group> { lhs, rhs ->
        if (type == Category.EXPENSE) {
            java.lang.Double.compare(rhs.expense, lhs.expense)
        } else {
            java.lang.Double.compare(rhs.income, lhs.income)
        }
    }

    fun setType(type: Int) {
        this.type = type
    }

    fun group(transactions: List<Transaction>): List<TransactionsGrouper.Group> {
        val groups = TreeMap<String, MutableList<Transaction>>()
        val categories = ArrayMap<String, Category>()

        for (transaction in transactions) {
            if (transaction.type != type) continue

            val category = transaction.category
            val groupName = category.name
            var transactionList: MutableList<Transaction>? = groups[groupName]
            if (transactionList == null) {
                transactionList = ArrayList<Transaction>()
                groups.put(groupName, transactionList)
                categories.put(groupName, category)
            }
            transactionList.add(transaction)
        }

        val subGroups = ArrayMap<String, TransactionsGrouper.Group>()
        val result = ArrayList<TransactionsGrouper.Group>()
        for (groupName in groups.keys) {
            val category = categories[groupName]
            val currentGroup: TransactionsGrouper.Group
            if (category == null) {
                currentGroup = TransactionsGrouper.Group(-1, groupName)
            } else {
                currentGroup = TransactionsGrouper.Group(category.id!!, groupName)
                currentGroup.icon = category.icon
            }
            result.add(currentGroup)

            subGroups.clear()
            for (transaction in groups[groupName]!!) {
                if (transaction.type == Transaction.EXPENSE) {
                    currentGroup.expense = currentGroup.expense + transaction.amountInMainCurrency
                } else if (transaction.type == Transaction.INCOME) {
                    currentGroup.income = currentGroup.income + transaction.amountInMainCurrency
                }
                handleSubGroup(subGroups, transaction)
            }
            applySubGroups(subGroups, currentGroup)
        }

        Collections.sort(result, GROUP_NAME_COMPARATOR)
        return result
    }

    private fun applySubGroups(subGroups: Map<String, TransactionsGrouper.Group>, currentGroup: TransactionsGrouper.Group) {
        if (subGroups.isEmpty()) return

        currentGroup.subGroups.clear()
        currentGroup.subGroups.addAll(subGroups.values)
        currentGroup.subGroups.sortWith(GROUP_NAME_COMPARATOR)
    }

    private fun handleSubGroup(subGroups: MutableMap<String, TransactionsGrouper.Group>, transaction: Transaction) {
        val subcategory = transaction.subcategory ?: return

        var group: TransactionsGrouper.Group? = subGroups[subcategory.name]
        if (group == null) {
            group = TransactionsGrouper.Group(subcategory.id!!, subcategory.name)
            subGroups.put(subcategory.name, group)
        }
        if (transaction.type == Transaction.EXPENSE) {
            group.expense = group.expense + transaction.amountInMainCurrency
        } else if (transaction.type == Transaction.INCOME) {
            group.income = group.income + transaction.amountInMainCurrency
        }
    }
}
