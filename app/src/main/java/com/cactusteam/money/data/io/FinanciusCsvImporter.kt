package com.cactusteam.money.data.io

import android.text.format.DateFormat
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Transaction
import java.text.SimpleDateFormat

/**
 * @author vpotapenko
 */

class FinanciusCsvImporter : BaseCsvImporter() {

    private val lineConverter = FinanciusLineConverter()
    private val timeFormat = SimpleDateFormat("HH:mm")
    private val dateFormat: java.text.DateFormat

    init {
        val moneyApp = MoneyApp.instance
        dateFormat = DateFormat.getLongDateFormat(moneyApp)

        headerCount = 0
    }

    override fun detectCellsNumber(line: Array<String>) {
        currentSchema!!.cellsNumber = 12
    }

    override fun analyseCsvLine(line: Array<String>) {
        lineConverter.extractFromLine(line)
        try {

            java.lang.Double.parseDouble(lineConverter.amount)

            if (lineConverter.type == FinanciusLineConverter.EXPENSE) {
                analyseAccount(lineConverter.srcAccount!!, lineConverter.currencyCode)
            } else if (lineConverter.type == FinanciusLineConverter.INCOME) {
                analyseAccount(lineConverter.dstAccount!!, lineConverter.currencyCode)
            } else if (lineConverter.type == FinanciusLineConverter.TRANSFER) {
                analyseAccount(lineConverter.srcAccount!!, lineConverter.currencyCode)

                val rate = java.lang.Double.parseDouble(lineConverter.rate)
                if (rate == 1.0) analyseAccount(lineConverter.dstAccount!!, lineConverter.currencyCode)
            }

            if (!lineConverter.category.isNullOrBlank())
                analyseCategory(lineConverter.category!!, lineConverter.type == FinanciusLineConverter.EXPENSE)
        } catch (e: Exception) {
            e.printStackTrace()
            currentSchema!!.badLinesCount++
        }

    }

    override fun importLine(line: Array<String>, count: Int) {
        lineConverter.extractFromLine(line)

        if (lineConverter.confirmation != CONFIRMED) {
            log("Skip not confirmed transaction", count + 1)
            return
        }

        if (lineConverter.type == FinanciusLineConverter.EXPENSE) {
            importExpense(count)
        } else if (lineConverter.type == FinanciusLineConverter.INCOME) {
            importIncome(count)
        } else if (lineConverter.type == FinanciusLineConverter.TRANSFER) {
            importTransfer(count)
        }
    }

    private fun importTransfer(count: Int) {
        val dstAccount = currentSchema!!.findAccountByName(lineConverter.dstAccount!!)
        if (dstAccount == null) {
            log("Cannot detect currency code for '" + lineConverter.dstAccount + "' account", count + 1)
            return
        }

        try {
            val date = dateFormat.parse(lineConverter.date)
            val time = timeFormat.parse(lineConverter.time)

            val b = dataManager!!.transactionService.newTransactionBuilder()
                    .putType(Transaction.TRANSFER)
                    .putDate(mergeDate(date, time))
                    .putSourceAccountId(getAccountId(lineConverter.srcAccount!!, lineConverter.currencyCode)!!)

            val amount = java.lang.Double.parseDouble(lineConverter.amount)
            b.putAmount(amount)

            if (!lineConverter.comment.isNullOrBlank())
                b.putComment(lineConverter.comment)
            for (tag in extractTags()) {
                b.putTag(tag)
            }

            b.putDestAccountId(dstAccount.accountId)
            val destAmount = java.lang.Double.parseDouble(lineConverter.rate) * amount
            b.putDestAmount(DataUtils.round(destAmount, 2))

            b.createInternal()
            importResult!!.newTransactions++
        } catch (e: Exception) {
            handleImportException(e, count)
        }

    }

    private fun importIncome(count: Int) {
        try {
            createIOTransaction(Transaction.INCOME)
            importResult!!.newTransactions++
        } catch (e: Exception) {
            handleImportException(e, count)
        }

    }

    private fun importExpense(count: Int) {
        try {
            createIOTransaction(Transaction.EXPENSE)
            importResult!!.newTransactions++
        } catch (e: Exception) {
            handleImportException(e, count)
        }

    }

    private fun createIOTransaction(type: Int) {
        val date = dateFormat.parse(lineConverter.date)
        val time = timeFormat.parse(lineConverter.time)

        val b = dataManager!!.transactionService
                .newTransactionBuilder()
                .putType(type)
                .putDate(mergeDate(date, time))
        val accountName = if (type == Transaction.EXPENSE) lineConverter.srcAccount else lineConverter.dstAccount
        b.putSourceAccountId(getAccountId(accountName!!, lineConverter.currencyCode)!!)

        b.putAmount(java.lang.Double.parseDouble(lineConverter.amount))
        b.putCategoryId(getCategoryId(lineConverter.category!!, type == Transaction.EXPENSE))

        if (!lineConverter.comment.isNullOrBlank())
                b.putComment(lineConverter.comment)
        for (tag in extractTags()) {
            b.putTag(tag)
        }

        b.createInternal()
    }

    private fun extractTags(): Array<String> {
        val tags = lineConverter.tags
        return if (tags == null || tags.isEmpty()) arrayOf<String>() else tags.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
    }

    companion object {

        private val CONFIRMED = "Confirmed"
    }
}
