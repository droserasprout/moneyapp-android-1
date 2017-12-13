package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction

import org.json.JSONException
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class TransactionExtendedFilter : ITransactionFilter {

    private var accountFilter: ITransactionFilter? = null
    private var categoryFilter: ITransactionFilter? = null
    private var additionalFilter: ITransactionFilter? = null

    private var accountName: String? = null
    private var categoryName: String? = null
    private var additionalName: String? = null

    constructor() {
    }

    constructor(jsonObject: JSONObject) {
        accountFilter = FilterFactory.deserialize(jsonObject.getString("accountFilter"))
        accountName = jsonObject.getString("accountName")

        categoryFilter = FilterFactory.deserialize(jsonObject.getString("categoryFilter"))
        categoryName = jsonObject.getString("categoryName")

        if (jsonObject.has("additionalFilter")) {
            additionalFilter = FilterFactory.deserialize(jsonObject.getString("additionalFilter"))
            additionalName = jsonObject.getString("additionalName")
        }
    }

    fun subtractWithCategory(transactionFilter: ITransactionFilter, description: String): TransactionExtendedFilter {
        val newFilter = TransactionExtendedFilter()
        newFilter.accountFilter = accountFilter
        newFilter.accountName = accountName
        newFilter.categoryFilter = transactionFilter
        newFilter.categoryName = description
        newFilter.additionalFilter = additionalFilter
        newFilter.additionalName = additionalName

        return newFilter
    }

    fun subtractWithAdditional(transactionFilter: ITransactionFilter, description: String): TransactionExtendedFilter {
        val newFilter = TransactionExtendedFilter()
        newFilter.accountFilter = accountFilter
        newFilter.accountName = accountName
        newFilter.categoryFilter = categoryFilter
        newFilter.categoryName = categoryName
        newFilter.additionalFilter = transactionFilter
        newFilter.additionalName = description

        return newFilter
    }

    fun setAccountName(accountName: String) {
        this.accountName = accountName
    }

    fun setAdditionalName(additionalName: String) {
        this.additionalName = additionalName
    }

    val displayName: String
        get() {
            val sb = StringBuilder()
            sb.append(accountName)

            if (!categoryName.isNullOrBlank()) {
                if (sb.isNotEmpty()) sb.append("<br/>")
                sb.append(categoryName)
            }

            if (!additionalName.isNullOrBlank()) {
                if (sb.isNotEmpty()) sb.append("<br/>")
                sb.append(additionalName)
            }
            return sb.toString()
        }

    fun setAccountFilter(accountFilter: ITransactionFilter) {
        this.accountFilter = accountFilter
    }

    fun setAdditionalFilter(additionalFilter: ITransactionFilter) {
        this.additionalFilter = additionalFilter
    }

    fun setCategoryFilter(categoryFilter: ITransactionFilter) {
        this.categoryFilter = categoryFilter
    }

    fun setCategoryName(categoryName: String) {
        this.categoryName = categoryName
    }

    override fun allow(transaction: Transaction): Boolean {
        if (additionalFilter != null) {
            return accountFilter!!.allow(transaction) && categoryFilter!!.allow(transaction)
                    && additionalFilter!!.allow(transaction)
        } else {
            return accountFilter!!.allow(transaction) && categoryFilter!!.allow(transaction)
        }
    }

    @Throws(JSONException::class)
    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()

        jsonObject.put("type", TYPE)
        jsonObject.put("accountFilter", FilterFactory.serialize(accountFilter!!))
        jsonObject.put("accountName", accountName)

        jsonObject.put("categoryFilter", FilterFactory.serialize(categoryFilter!!))
        jsonObject.put("categoryName", categoryName)

        if (additionalFilter != null) {
            jsonObject.put("additionalFilter", FilterFactory.serialize(additionalFilter!!))
            jsonObject.put("additionalName", additionalName)
        }

        return jsonObject
    }

    companion object {

        val TYPE = "extended"
    }
}
