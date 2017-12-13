package com.cactusteam.money.data.io.qif

import java.util.*

/**
 * @author vpotapenko
 */
class QifAccount {

    var startLine: Int = 0
    var endLine: Int = 0

    var name: String? = null
    var type: String? = null

    val transactions: MutableList<QifTransaction> = ArrayList()
}
