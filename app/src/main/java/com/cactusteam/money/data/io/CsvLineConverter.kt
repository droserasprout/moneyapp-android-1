package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */
class CsvLineConverter {

    var date: String? = null
    var time: String? = null
    var accountName: String? = null
    var amount: String? = null
    var currencyCode: String? = null
    var amountInMain: String? = null
    var category: String? = null
    var subcategory: String? = null
    var tags: String? = null
    var comment: String? = null
    var transferDirection: String? = null

    init {
        clear()
    }

    fun extractFromLine(line: Array<String>) {
        date = line[0]
        time = line[1]
        accountName = line[2]
        amount = line[3]
        currencyCode = line[4]
        amountInMain = line[5]
        category = line[6]
        subcategory = line[7]
        tags = line[8]
        comment = line[9]
        transferDirection = line[10]
    }

    fun prepareLine(line: Array<String?>) {
        line[0] = date
        line[1] = time
        line[2] = accountName
        line[3] = amount
        line[4] = currencyCode
        line[5] = amountInMain
        line[6] = category
        line[7] = subcategory
        line[8] = tags
        line[9] = comment
        line[10] = transferDirection
    }

    fun clear() {
        date = ""
        time = ""
        accountName = ""
        amount = ""
        currencyCode = ""
        amountInMain = ""
        category = ""
        subcategory = ""
        tags = ""
        comment = ""
        transferDirection = ""
    }

    fun outDirection() {
        transferDirection = OUT
    }

    fun inDirection() {
        transferDirection = IN
    }

    companion object {

        val IN = "in"
        val OUT = "out"
    }
}
