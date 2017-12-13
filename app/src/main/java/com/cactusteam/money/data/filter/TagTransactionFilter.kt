package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class TagTransactionFilter : ITransactionFilter {

    private val tagId: Long

    private var withTransfer: Boolean = false

    constructor(tagId: Long) {
        this.tagId = tagId
    }

    constructor(jsonObject: JSONObject) {
        tagId = jsonObject.getLong("id")
        withTransfer = jsonObject.getBoolean("transfer")
    }

    override fun allow(transaction: Transaction): Boolean {
        if (transaction.type == Transaction.TRANSFER && !withTransfer) return false

        val tags = transaction.tags
        return tags.any { it.tagId == tagId }
    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)
        jsonObject.put("id", tagId)
        jsonObject.put("transfer", withTransfer)
        return jsonObject
    }

    companion object {

        val TYPE = "tag"
    }
}
