package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class CategoryNameTransactionFilter : ITransactionFilter {

    private val categoryName: String
    private val categoryType: Int?

    constructor(categoryName: String, categoryType: Int?) {
        this.categoryName = categoryName
        this.categoryType = categoryType
    }

    constructor(jsonObject: JSONObject) {
        categoryName = jsonObject.getString("name")
        categoryType = jsonObject.getInt("categoryType")
    }

    override fun allow(transaction: Transaction): Boolean {
        if (transaction.type == Transaction.TRANSFER) return false

        if (categoryType != null) {
            val transactionType = if (categoryType === Category.EXPENSE) Transaction.EXPENSE else Transaction.INCOME
            return transaction.type == transactionType && transaction.category.name == categoryName
        } else {
            return transaction.category.name == categoryName
        }
    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)
        jsonObject.put("name", categoryName)
        jsonObject.put("categoryType", categoryType)
        return jsonObject
    }

    companion object {

        val TYPE = "categoryName"
    }
}
