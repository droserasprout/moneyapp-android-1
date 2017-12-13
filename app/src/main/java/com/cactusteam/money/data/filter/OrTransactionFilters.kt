package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * @author vpotapenko
 */
class OrTransactionFilters : ITransactionFilter {

    private val filters = LinkedList<ITransactionFilter>()

    constructor() {
    }

    constructor(jsonObject: JSONObject) {
        val array = jsonObject.getJSONArray("filters")
        (0..array.length() - 1)
                .map { array.getJSONObject(it) }
                .mapTo(filters) { FilterFactory.deserialize(it)!! }
    }

    fun addFilter(filter: ITransactionFilter) {
        filters.add(filter)
    }

    override fun allow(transaction: Transaction): Boolean {
        return filters.any { it.allow(transaction) }
    }

    override fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("type", TYPE)

        val array = JSONArray()
        for (filter in filters) {
            array.put(filter.toJSON())
        }
        jsonObject.put("filters", array)
        return jsonObject
    }

    companion object {

        val TYPE = "or"
    }
}
