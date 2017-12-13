package com.cactusteam.money.data.model

import com.cactusteam.money.data.dao.Category
import org.json.JSONArray

/**
 * @author vpotapenko
 */
class BalanceChangeGroup {

    val changes: MutableList<BalanceChange> = mutableListOf()

    val expense: Double
        get() {
            return changes
                    .asIterable()
                    .filter { it.type == Category.EXPENSE }
                    .sumByDouble { it.amount }
        }

    val income: Double
        get() {
            return changes
                    .asIterable()
                    .filter { it.type == Category.INCOME }
                    .sumByDouble { it.amount }
        }

    fun asString(): String {
        val array = JSONArray()
        for (ch in changes) {
            array.put(ch.toJSONObject())
        }
        return array.toString()
    }

    fun extract(content: String) {
        changes.clear()

        val array = JSONArray(content)
        (0..array.length() - 1)
                .mapNotNull { array.optJSONObject(it) }
                .forEach {
                    changes.add(BalanceChange(it))
                }
    }
}