package com.cactusteam.money.data.filter

import org.json.JSONException
import org.json.JSONObject

/**
 * @author vpotapenko
 */
object FilterFactory {

    fun deserialize(content: String): ITransactionFilter? {
        try {
            val jsonObject = JSONObject(content)
            return deserialize(jsonObject)
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }

    }

    fun deserialize(jsonObject: JSONObject): ITransactionFilter? {
        val type = jsonObject.getString("type")
        when (type) {
            AndTransactionFilters.TYPE -> return AndTransactionFilters(jsonObject)
            OrTransactionFilters.TYPE -> return OrTransactionFilters(jsonObject)
            AccountTransactionFilter.TYPE -> return AccountTransactionFilter(jsonObject)
            AllowAllTransactionFilter.TYPE -> return AllowAllTransactionFilter.instance
            CategoryNameTransactionFilter.TYPE -> return CategoryNameTransactionFilter(jsonObject)
            CategoryTransactionFilter.TYPE -> return CategoryTransactionFilter(jsonObject)
            SubcategoryTransactionFilter.TYPE -> return SubcategoryTransactionFilter(jsonObject)
            TagTransactionFilter.TYPE -> return TagTransactionFilter(jsonObject)
            WithoutTagTransactionFilter.TYPE -> return WithoutTagTransactionFilter.instance
            TypeTransactionFilter.TYPE -> return TypeTransactionFilter(jsonObject)
            TransactionExtendedFilter.TYPE -> return TransactionExtendedFilter(jsonObject)
            TagNameTransactionFilter.TYPE -> return TagNameTransactionFilter(jsonObject)
            else -> return null
        }
    }

    fun serialize(filter: ITransactionFilter): String? {
        try {
            val jsonObject = filter.toJSON()
            return jsonObject.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }

    }
}
