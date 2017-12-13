package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */
internal class ViZiBudgetLineConverter {

    var type: String? = null
    var date: String? = null
    var accountName: String? = null
    var category: String? = null
    var subcategory: String? = null
    var amount: String? = null
    var currencyCode: String? = null
    var accountName2: String? = null
    var amount2: String? = null
    var currencyCode2: String? = null
    var comment: String? = null

    fun extractFromLine(line: Array<String>) {
        type = line[0]
        date = line[1]
        accountName = line[2]
        category = line[3]
        subcategory = line[4]
        amount = line[5]
        currencyCode = line[6]
        accountName2 = line[7]
        amount2 = line[8]
        currencyCode2 = line[9]
        comment = line[10]
    }
}
