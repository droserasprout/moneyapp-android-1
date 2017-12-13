package com.cactusteam.money.data.currency

import java.text.NumberFormat
import java.util.Currency
import java.util.HashMap

/**
 * @author vpotapenko
 */
internal class CurrencyFormatter {

    private val map = HashMap<String, ICustomCurrencyFormatter>()

    init {
        map.put("BYN", BelRubleCustomFormatter())
        map.put("BYR", OldBelRubleCustomFormatter())
        map.put("RUP", TransnistrianRubleFormatter())
    }

    fun formatCurrency(amount: Double, currencyCode: String): String {
        val customFormatter = map[currencyCode]
        if (customFormatter != null) {
            return customFormatter.format(amount)
        } else {
            try {
                val currency = Currency.getInstance(currencyCode)

                val numberFormat = NumberFormat.getCurrencyInstance()
                numberFormat.currency = currency
                return numberFormat.format(amount)
            } catch (ignore: IllegalArgumentException) {
                return amount.toString()
            }

        }
    }
}
