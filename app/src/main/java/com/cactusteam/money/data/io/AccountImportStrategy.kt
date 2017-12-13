package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Account

/**
 * @author vpotapenko
 */
abstract class AccountImportStrategy {

    abstract fun apply(currencyCode: String?): Account
}
