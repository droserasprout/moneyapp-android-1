package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction

import org.json.JSONException
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class AccountTransactionFilter : ITransactionFilter {

    private val accountId: Long

    constructor(accountId: Long) {
        this.accountId = accountId
    }

    @Throws(JSONException::class)
    constructor(jsonObject: JSONObject) {
        this.accountId = jsonObject.getLong("id")
    }

    override fun allow(transaction: Transaction): Boolean {
        return transaction.sourceAccountId == accountId || transaction.destAccountId != null && transaction.destAccountId === accountId
    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)
        jsonObject.put("id", accountId)
        return jsonObject
    }

    companion object {

        val TYPE = "account"
    }
}
