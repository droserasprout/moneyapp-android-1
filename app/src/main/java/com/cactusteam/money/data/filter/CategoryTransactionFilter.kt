package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction

import org.json.JSONException
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class CategoryTransactionFilter : ITransactionFilter {

    private val categoryId: Long

    constructor(categoryId: Long) {
        this.categoryId = categoryId
    }

    constructor(jsonObject: JSONObject) {
        categoryId = jsonObject.getLong("id")
    }

    override fun allow(transaction: Transaction): Boolean {
        return transaction.type != Transaction.TRANSFER && transaction.categoryId === categoryId

    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)
        jsonObject.put("id", categoryId)
        return jsonObject
    }

    companion object {

        val TYPE = "category"
    }
}
