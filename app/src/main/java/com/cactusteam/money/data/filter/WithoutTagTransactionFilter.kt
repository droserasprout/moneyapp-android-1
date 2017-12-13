package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class WithoutTagTransactionFilter private constructor() : ITransactionFilter {

    override fun allow(transaction: Transaction): Boolean {
        return transaction.type != Transaction.TRANSFER && transaction.tags.isEmpty()
    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)
        return jsonObject
    }

    companion object {

        val TYPE = "withoutTag"

        val instance: WithoutTagTransactionFilter by lazy { WithoutTagTransactionFilter() }
    }
}
