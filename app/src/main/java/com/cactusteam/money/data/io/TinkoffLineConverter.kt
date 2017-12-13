package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */
class TinkoffLineConverter {

    var date: String? = null
    var accountName: String? = null
    var status: String? = null
    var amount: String? = null
    var currencyCode: String? = null
    var category: String? = null
    var comment: String? = null

    fun extractFromLine(line: Array<String>) {
        date = line[0]
        accountName = line[2]
        status = line[3]
        amount = line[6]
        currencyCode = line[7]
        category = line[8]
        comment = line[10]

        if (accountName.isNullOrBlank()) {
            accountName = "EMPTY"
        }
    }
}