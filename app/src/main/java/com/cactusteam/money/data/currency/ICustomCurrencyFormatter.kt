package com.cactusteam.money.data.currency

/**
 * @author vpotapenko
 */

interface ICustomCurrencyFormatter {

    fun format(amount: Double): String
}
