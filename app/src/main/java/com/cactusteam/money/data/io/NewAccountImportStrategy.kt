package com.cactusteam.money.data.io

import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.ui.UiConstants

import java.util.Random

/**
 * @author vpotapenko
 */
class NewAccountImportStrategy(var name: String) : AccountImportStrategy() {

    @Suppress("DEPRECATION")
    override fun apply(currencyCode: String?): Account {
        val colorIndex = Random().nextInt(UiConstants.UI_COLORS.size)
        val uiColor = UiConstants.UI_COLORS[colorIndex]

        val moneyApp = MoneyApp.instance
        val color = moneyApp.resources.getColor(uiColor)
        val hexColor = String.format("#%06X", 0xFFFFFF and color)

        return moneyApp.dataManager.accountService
                .createAccountInternal(name, Account.CASH_TYPE,
                        currencyCode ?: moneyApp.appPreferences.mainCurrencyCode,
                        hexColor, false, null, null)
    }
}
