package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class AllowAllTransactionFilter : ITransactionFilter {

    override fun allow(transaction: Transaction): Boolean {
        return true
    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)
        return jsonObject
    }

    companion object {

        val TYPE = "allowAll"

        val instance: AllowAllTransactionFilter by lazy { AllowAllTransactionFilter() }
    }
}
