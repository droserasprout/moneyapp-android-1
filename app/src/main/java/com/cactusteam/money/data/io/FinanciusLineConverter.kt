package com.cactusteam.money.data.io

/**
 * @author vpotapenko
 */

internal class FinanciusLineConverter {

    var date: String? = null
    var time: String? = null
    var type: String? = null
    var confirmation: String? = null
    var comment: String? = null
    var srcAccount: String? = null
    var dstAccount: String? = null
    var category: String? = null
    var tags: String? = null
    var amount: String? = null
    var currencyCode: String? = null
    var rate: String? = null

    fun extractFromLine(line: Array<String>) {
        date = line[0]
        time = line[1]
        type = line[2]
        confirmation = line[3]
        comment = line[4]
        srcAccount = line[5]
        dstAccount = line[6]
        category = line[7]
        tags = line[8]
        amount = line[9]
        currencyCode = line[10]
        rate = line[11]

        if (type == EXPENSE || type == INCOME) {
            if (category.isNullOrBlank()) {
                category = if (!comment.isNullOrBlank()) comment else "EMPTY"
            }
        }
    }

    companion object {

        val EXPENSE = "Expense"
        val INCOME = "Income"
        val TRANSFER = "Transfer"
    }
}
