package com.cactusteam.money.data.filter

import com.cactusteam.money.data.dao.Transaction

import org.json.JSONException
import org.json.JSONObject

/**
 * @author vpotapenko
 */
interface ITransactionFilter {

    fun allow(transaction: Transaction): Boolean

    fun toJSON(): JSONObject
}
