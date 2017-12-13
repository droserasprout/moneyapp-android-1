package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */

internal class FinancePMLineConverter {

    var type: String? = null
    var name: String? = null
    var accountName: String? = null
    var category: String? = null
    var amount: String? = null
    var currencyCode: String? = null
    var date: String? = null
    var comment: String? = null

    fun extractFromLine(line: Array<String>) {
        type = line[0]
        name = line[1]
        accountName = line[2]
        category = line[3]
        amount = line[4]
        currencyCode = line[5]
        date = line[6]
        comment = line[7]
    }
}
