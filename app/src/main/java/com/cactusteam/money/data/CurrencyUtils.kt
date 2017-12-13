package com.cactusteam.money.data

import java.util.Currency
import java.util.Locale

/**
 * @author vpotapenko
 */
object CurrencyUtils {

    val localeCurrencyCode: String
        get() {
            val locale = Locale.getDefault()
            var currencyCode = getCurrencyCode(locale)
            if (currencyCode != null && !currencyCode.isEmpty()) return currencyCode

            for (availableLocale in Locale.getAvailableLocales()) {
                if (availableLocale.language == locale.language) {
                    currencyCode = getCurrencyCode(availableLocale)
                    if (!currencyCode.isNullOrBlank()) return currencyCode!!
                }
            }
            return Currency.getInstance("USD").currencyCode
        }

    private fun getCurrencyCode(locale: Locale): String? {
        try {
            var currencyCode = Currency.getInstance(locale).currencyCode
            if ("BYR" == currencyCode) currencyCode = "BYN" // uses new bel ruble workaround

            return currencyCode
        } catch (e: Exception) {
            return null
        }

    }
}
