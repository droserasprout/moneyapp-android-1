package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Transaction
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author vpotapenko
 */
class TinkoffCsvImporter : BaseCsvImporter("Windows-1251", ';') {

    private val lineConverter = TinkoffLineConverter()
    private val formatter: DecimalFormat = DecimalFormat()

    private val dateFormat: DateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    init {
        headerCount = 0

        val symbols = DecimalFormatSymbols()
        symbols.decimalSeparator = ','
        formatter.decimalFormatSymbols = symbols
    }

    override fun detectCellsNumber(line: Array<String>) {
        currentSchema!!.cellsNumber = 12
    }

    override fun analyseCsvLine(line: Array<String>) {
        lineConverter.extractFromLine(line)
        if (lineConverter.status != "OK") return // skip FAILED

        try {
            val amount = formatter.parse(lineConverter.amount).toDouble()
            dateFormat.parse(lineConverter.date)

            analyseAccount(lineConverter.accountName!!, lineConverter.currencyCode)

            if (!lineConverter.category.isNullOrBlank()) {
                analyseCategory(lineConverter.category!!, amount < 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            currentSchema!!.badLinesCount++
        }
    }

    override fun importLine(line: Array<String>, count: Int) {
        lineConverter.extractFromLine(line)
        if (lineConverter.status != "OK") return // skip FAILED

        try {
            val amount = formatter.parse(lineConverter.amount).toDouble()
            val date = dateFormat.parse(lineConverter.date)

            val b = dataManager!!.transactionService.newTransactionBuilder()

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