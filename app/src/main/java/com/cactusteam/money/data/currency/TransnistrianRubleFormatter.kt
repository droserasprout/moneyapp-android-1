package com.cactusteam.money.data.currency

import java.text.DecimalFormat

/**
 * @author vpotapenko
 */
class TransnistrianRubleFormatter : ICustomCurrencyFormatter {

    private val formatter = DecimalFormat("#,##0.00 RUP")

    override fun format(amount: Double): String {
        return formatter.format(amount)
    }
}