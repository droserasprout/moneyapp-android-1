package com.cactusteam.money.data.currency

import java.text.DecimalFormat

/**
 * @author vpotapenko
 */

class BelRubleCustomFormatter : ICustomCurrencyFormatter {

    private val formatter = DecimalFormat("#,##0.00 BYN")

    override fun format(amount: Double): String {
        return formatter.format(amount)
    }
}
