package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Account

/**
 * @author vpotapenko
 */
class ExistAccountImportStrategy(var account: Account) : AccountImportStrategy() {

    override fun apply(currencyCode: String?): Account {
        return account
    }
}
