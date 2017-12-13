package com.cactusteam.money.data.model

import android.support.v4.util.ArrayMap

import java.util.Date

/**
 * @author vpotapenko
 */
class CategoryPeriodData {

    var from: Date? = null
    var to: Date? = null

    var amount: Double = 0.0

    var subcategoryAmounts: MutableMap<Long, Double> = ArrayMap()

    val maxSubcategoryAmount: Double
        get() {
            var max = 0.0
            for ((key, value) in subcategoryAmounts) {
                max = Math.max(max, value)
            }
            return max
        }

    fun handleSubcategoryAmount(subcategoryId: Long, amount: Double) {
        var currentAmount = amount
        val oldAmount = subcategoryAmounts[subcategoryId]
        if (oldAmount != null) currentAmount += oldAmount

        subcategoryAmounts.put(subcategoryId, currentAmount)
    }
}
