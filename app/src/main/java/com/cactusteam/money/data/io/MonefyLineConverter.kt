package com.cactusteam.money.data.io

import java.util.regex.Pattern

/**
 * @author vpotapenko
 */
internal class MonefyLineConverter {

    var shortForm: Boolean = false

    var date: String? = null
    var accountName: String? = null
    var category: String? = null
    var amount: String? = null
    var currencyCode: String? = null
    var comment: String? = null
    var destAccountName: String? = null

    fun extractFromLine(line: Array<String>) {
        date = line[0]
        accountName = line[1]
        category = line[2]
        amount = line[3].replace("\\s".toRegex(), "")
        currencyCode = line[4]
        comment = if (shortForm) line[5] else line[7]

        handleCategory()
    }

    private fun handleCategory() {
        var matcher = FROM_PATTERN.matcher(category!!)
        if (matcher.matches()) {
            category = null
            destAccountName = null // just skip in transfers
            return
        }

        matcher = TO_PATTERN.matcher(category!!)
        if (matcher.matches()) {
            category = null
            destAccountName = matcher.group(1)
        }
    }

    companion object {

        private val FROM_PATTERN = Pattern.compile("From \'(.*)\'")
        private val TO_PATTERN = Pattern.compile("To \'(.*)\'")
    }
}
