package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction

import org.json.JSONException
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class TypeTransactionFilter : ITransactionFilter {

    private var type: Int = 0

    constructor(type: Int) {
        this.type = type
    }

    constructor(jsonObject: JSONObject) {
        type = jsonObject.getInt("allowType")
    }

    override fun allow(transaction: Transaction): Boolean {
        return transaction.type == type
    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)
        jsonObject.put("allowType", type)
        return jsonObject
    }

    companion object {

        val TYPE = "type"
    }
}
