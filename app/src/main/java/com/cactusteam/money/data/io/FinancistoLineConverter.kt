package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */
class FinancistoLineConverter {

    var date: String? = null
    var time: String? = null
    var accountName: String? = null
    var amount: String? = null
    var currencyCode: String? = null
    var category: String? = null
    var subcategory: String? = null
    var payee: String? = null
    var transferDirection: String? = null
    var comment: String? = null

    fun extractFromLine(line: Array<String>) {
        date = line[0]
        time = line[1]
        accountName = line[2]
        amount = line[3]
        currencyCode = line[4]
        payee = line[9]
        transferDirection = line[10]
        comment = line[12]

        if (transferDirection.isNullOrBlank()) {
            category = line[7]
            val parent = line[8]
            if (!parent.isNullOrBlank()) {
                subcategory = category
                category = parent.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[0]
            } else {
                subcategory = ""
            }

            if (category.isNullOrBlank()) {
                // Financisto can have empty category, but MoneyApp not.
                category = "EMPTY"
            }
        } else {
            category = null
            subcategory = null
        }
    }

    companion object {

        val OUT = "Transfer Out"
        val IN = "Transfer In"
    }
}
