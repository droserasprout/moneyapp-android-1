package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Transaction
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author vpotapenko
 */
internal class MonefyCsvImporter : BaseCsvImporter() {

    private val lineConverter = MonefyLineConverter()

    private val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun detectCellsNumber(line: Array<String>) {
        currentSchema!!.cellsNumber = if (line.size == 6) 6 else 8
        lineConverter.shortForm = line.size == 6
    }

    override fun analyseCsvLine(line: Array<String>) {
        lineConverter.extractFromLine(line)
        try {
            val amount = java.lang.Double.parseDouble(lineConverter.amount)
            analyseAccount(lineConverter.accountName!!, lineConverter.currencyCode)

            if (!lineConverter.category.isNullOrBlank()) {
                analyseCategory(lineConverter.category!!, amount < 0)
            }
            if (!lineConverter.destAccountName.isNullOrBlank()) {
                analyseAccount(lineConverter.destAccountName!!, lineConverter.currencyCode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            currentSchema!!.badLinesCount = currentSchema!!.badLinesCount + 1
        }

    }

    override fun importLine(line: Array<String>, count: Int) {
        lineConverter.extractFromLine(line)

        if (lineConverter.category.isNullOrBlank()) {
            importTransfer(count)
        } else {
            try {
                val amount = java.lang.Double.parseDouble(lineConverter.amount)

                val b = dataManager!!.transactionService.newTransactionBuilder()
                val date = dateFormat.parse(lineConverter.date)

                b.putType(if (amount > 0) Transaction.INCOME else Transaction.EXPENSE)
                        .putDate(date)
                        .putSourceAccountId(getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)!!)
                        .putAmount(Math.abs(amount))
                if (!lineConverter.comment.isNullOrBlank())
                    b.putComment(lineConverter.comment)

                b.putCategoryId(getCategoryId(lineConverter.category!!, amount < 0))
                b.createInternal()
                importResult!!.newTransactions = importResult!!.newTransactions + 1
            } catch (e: Exception) {
                handleImportException(e, count)
            }

        }
    }

    private fun importTransfer(count: Int) {
        if (lineConverter.destAccountName != null) {
            try {
                val sourceAccountId = getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)
                val destAccountId = getAccountId(lineConverter.destAccountName!!, lineConverter.currencyCode)
                val amount = Math.abs(java.lang.Double.parseDouble(lineConverter.amount))

                val b = dataManager!!.transactionService.newTransactionBuilder()
                val date = dateFormat.parse(lineConverter.date)

                b.putType(Transaction.TRANSFER)
                        .putDate(date)
                        .putSourceAccountId(sourceAccountId!!)
                        .putAmount(amount)
                if (!lineConverter.comment.isNullOrBlank())
                    b.putComment(lineConverter.comment)

                b.putDestAccountId(destAccountId).putDestAmount(amount)

                b.createInternal()
                importResult!!.newTransactions = importResult!!.newTransactions + 1
            } catch (e: Exception) {
                handleImportException(e, count)
            }

        }
    }
}
