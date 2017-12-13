package com.cactusteam.money.data.io.qif

import java.util.ArrayList

/**
 * @author vpotapenko
 */
class QifTransaction {

    var startLine: Int = 0
    var endLine: Int = 0

    var date: String? = null
    var amount: String? = null
    var category: String? = null
    var subcategory: String? = null
    var payee: String? = null
    var transferAccount: String? = null
    var comment: String? = null

    val splitItems: MutableList<QifSplitItem> = ArrayList()

    var transferTransaction: QifTransaction? = null

}
