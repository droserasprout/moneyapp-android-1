package com.cactusteam.money.data.model

import com.cactusteam.money.data.dao.Transaction

/**
 * @author vpotapenko
 */
class TransactionSearch(q: String) {

    val query: String = "%${q.replace(Regex("[;%]"), "")}%"

    var offset: Int = 0
    var limit: Int = 10

    var hasMore: Boolean = true

    val transactions = mutableListOf<Transaction>()

}