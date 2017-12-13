package com.cactusteam.money.data.model

import android.support.v4.util.ArrayMap

/**
 * @author vpotapenko
 */
class CategoryAmounts {
    val categoryAmounts: MutableMap<Long, Double> = ArrayMap()
    val subcategoryAmounts: MutableMap<Long, Double> = ArrayMap()

    fun putAmount(categoryId: Long, subcategoryId: Long?, amount: Double) {
        var oldAmount: Double? = categoryAmounts[categoryId]
        if (oldAmount != null) {
            categoryAmounts.put(categoryId, amount + oldAmount)
        } else {
            categoryAmounts.put(categoryId, amount)
        }

        if (subcategoryId != null) {
            oldAmount = subcategoryAmounts[subcategoryId]
            if (oldAmount != null) {
                subcategoryAmounts.put(subcategoryId, amount + oldAmount)
            } else {
                subcategoryAmounts.put(subcategoryId, amount)
            }
        }
    }
}