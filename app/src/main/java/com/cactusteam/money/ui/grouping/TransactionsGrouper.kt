package com.cactusteam.money.ui.grouping

import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction

/**
 * @author vpotapenko
 */
abstract class TransactionsGrouper {

    abstract fun group(transactions: List<Transaction>): List<Item>

    class Item(val type: Int, val obj: Any)

    class Group(val id: Long, val title: String) {

        var icon: String? = null

        var expense: Double = 0.0
        var income: Double = 0.0

        val subGroups = mutableListOf<Group>()

        var opened: Boolean = false
        val items = mutableListOf<TransactionsGrouper.Item>()

        fun getAmount(type: Int): Double {
            return if (type == Category.EXPENSE) expense else income
        }
    }

    companion object {

        val TRANSACTION = 0
        val GROUP = 1
    }

}
