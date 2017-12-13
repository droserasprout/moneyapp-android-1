package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Account

/**
 * @author vpotapenko
 */
class ImportAccount internal constructor(var name: String, var currencyCode: String?, var strategy: AccountImportStrategy) {

    private var account: Account? = null

    val displayName: String
        get() = if (currencyCode != null) name + " - " + currencyCode else name

    fun getAccount(): Account {
        checkAccount()
        return account!!
    }

    val accountId: Long?
        get() {
            checkAccount()
            return account!!.id
        }

    private fun checkAccount() {
        if (account == null) {
            account = strategy.apply(currencyCode)
        }
    }
}
