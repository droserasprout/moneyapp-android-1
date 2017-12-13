package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction

import org.json.JSONException
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class SubcategoryTransactionFilter : ITransactionFilter {

    private val subcategoryId: Long

    constructor(subcategoryId: Long) {
        this.subcategoryId = subcategoryId
    }

    constructor(jsonObject: JSONObject) {
        subcategoryId = jsonObject.getLong("id")
    }

    override fun allow(transaction: Transaction): Boolean {
        if (transaction.type == Transaction.TRANSFER) return false

        if (subcategoryId < 0) { // transactions without subcategory
            return transaction.subcategoryId == null
        } else {
            return transaction.subcategoryId != null && transaction.subcategoryId === subcategoryId
        }
    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)
        jsonObject.put("id", subcategoryId)
        return jsonObject
    }

    companion object {

        val TYPE = "subcategory"
    }
}
