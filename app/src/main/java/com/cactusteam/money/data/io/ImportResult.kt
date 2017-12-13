package com.cactusteam.money.data.io

import java.util.ArrayList

/**
 * @author vpotapenko
 */
class ImportResult {

    var newTransactions: Int = 0

    val log: MutableList<ImportLogItem> = ArrayList()

    class ImportLogItem(var message: String?, var line: Int)
}
