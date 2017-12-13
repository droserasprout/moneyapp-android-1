package com.cactusteam.money.data.model

import android.view.View
import org.json.JSONObject

/**
 * @author vpotapenko
 */
class BalanceChange {

    val categoryId: Long
    val subcategoryId: Long?
    val type: Int
    val amount: Double

    var view: View? = null

    constructor(categoryId: Long, subcategoryId: Long?, type: Int, amount: Double) {
        this.categoryId = categoryId
        this.subcategoryId = subcategoryId
        this.type = type
        this.amount = amount
    }

    constructor(obj: JSONObject) {
        categoryId = obj.optLong("categoryId")
        subcategoryId = obj.optLong("subcategoryId")
        type = obj.optInt("type")
        amount = obj.optDouble("amount")
    }

    fun toJSONObject(): JSONObject {
        val obj = JSONObject()
        obj.put("categoryId", categoryId)
        if (subcategoryId != null) obj.put("subcategoryId", subcategoryId)

        obj.put("type", type)
        obj.put("amount", amount)
        return obj
    }
}