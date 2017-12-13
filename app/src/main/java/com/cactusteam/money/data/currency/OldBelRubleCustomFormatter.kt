package com.cactusteam.money.data.currency

import java.text.DecimalFormat

/**
 * @author vpotapenko
 */

class OldBelRubleCustomFormatter : ICustomCurrencyFormatter {

    private val formatter = DecimalFormat("#,##0.## BYR")

    override fun format(amount: Double): String {
        return formatter.format(amount)
    }
}
