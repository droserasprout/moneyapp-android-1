package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class TagNameTransactionFilter : ITransactionFilter {

    private val tagName: String

    constructor(tagName: String) {
        this.tagName = tagName
    }

    constructor(jsonObject: JSONObject) {
        tagName = jsonObject.getString("name")
    }

    override fun allow(transaction: Transaction): Boolean {
        if (transaction.type == Transaction.TRANSFER) return false

        val tags = transaction.tags
        return tags.any { it.tag.name == tagName }
    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)
        jsonObject.put("name", tagName)
        return jsonObject
    }

    companion object {

        val TYPE = "tagName"
    }
}
