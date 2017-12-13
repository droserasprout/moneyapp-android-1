package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Transaction
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * @author vpotapenko
 */
internal class MoneyAppCsvImporter : BaseCsvImporter() {

    private val lineConverter = CsvLineConverter()

    private val dateCSVFormat = SimpleDateFormat("yyyy-MM-dd")
    private val timeCSVFormat = SimpleDateFormat("HH:mm:ss")

    private val numberFormat = NumberFormat.getNumberInstance()

    override fun detectCellsNumber(line: Array<String>) {
        currentSchema!!.cellsNumber = CsvExporter.CELLS_NUMBER
    }

    @Throws(Exception::class)
    override fun analyseCsvLine(line: Array<String>) {
        lineConverter.extractFromLine(line)
        try {
            val amount = numberFormat.parse(lineConverter.amount).toDouble()

            analyseAccount(lineConverter.accountName!!, lineConverter.currencyCode)

            if (!lineConverter.category.isNullOrBlank()) analyseCategory(lineConverter.category!!, amount < 0)
            if (!lineConverter.subcategory.isNullOrBlank())
                analyseSubcategory(lineConverter.category!!, lineConverter.subcategory!!, amount < 0)

        } catch (e: ParseException) {
            e.printStackTrace()
            currentSchema!!.badLinesCount = currentSchema!!.badLinesCount + 1
        }

    }

    override fun importLine(line: Array<String>, count: Int) {
        lineConverter.extractFromLine(line)

        if (lineConverter.category.isNullOrBlank()) {
            importTransfer(count)
        } else {
            if (previousTransferPart != null) {
                log("Found transfer without pair", previousTransferPart!!.count + 1)
                previousTransferPart = null
            }
            try {
                val amount = numberFormat.parse(lineConverter.amount).toDouble()

                val date = dateCSVFormat.parse(lineConverter.date)
                val time = timeCSVFormat.parse(lineConverter.time)

                val b = dataManager!!.transactionService
                        .newTransactionBuilder()
                        .putType(if (amount > 0) Transaction.INCOME else Transaction.EXPENSE)
                        .putDate(mergeDate(date, time))
                        .putSourceAccountId(getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)!!)
                        .putAmount(Math.abs(amount))
                if (!lineConverter.comment.isNullOrBlank())
                    b.putComment(lineConverter.comment)
                for (tag in extractTags()) {
                    b.putTag(tag)
                }

                b.putCategoryId(getCategoryId(lineConverter.category!!, amount < 0))
                if (!lineConverter.subcategory.isNullOrBlank()) {
                    b.putSubcategoryId(getSubcategoryId(lineConverter.category!!, lineConverter.subcategory!!, amount < 0))
                }
                b.createInternal()
                importResult!!.newTransactions = importResult!!.newTransactions + 1
            } catch (e: Exception) {
                handleImportException(e, count)
            }

        }
    }

    private fun extractTags(): Array<String> {
        val tags = lineConverter.tags
        return if (tags == null || tags.isEmpty()) arrayOf<String>() else tags.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
    }

    private fun importTransfer(count: Int) {
        if (previousTransferPart == null) {
            previousTransferPart = BaseImporter.TransferPart()
            previousTransferPart!!.accountName = lineConverter.accountName
            previousTransferPart!!.currencyCode = lineConverter.currencyCode
            previousTransferPart!!.amount = lineConverter.amount
            previousTransferPart!!.direction = lineConverter.transferDirection
            previousTransferPart!!.count = count
        } else {
            try {
                val sourceAccountId: Long?
                val destAccountId: Long?
                val amount: Double
                val destAmount: Double

                if (CsvLineConverter.IN == previousTransferPart!!.direction && CsvLineConverter.OUT == lineConverter.transferDirection) {
                    sourceAccountId = getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)
                    destAccountId = getAccountId(previousTransferPart!!.accountName!!, previousTransferPart!!.currencyCode)

                    amount = Math.abs(numberFormat.parse(lineConverter.amount).toDouble())
                    destAmount = Math.abs(numberFormat.parse(previousTransferPart!!.amount).toDouble())
                } else if (CsvLineConverter.IN == lineConverter.transferDirection && CsvLineConverter.OUT == previousTransferPart!!.direction) {
                    sourceAccountId = getAccountId(previousTransferPart!!.accountName!!, previousTransferPart!!.currencyCode)
                    destAccountId = getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)

                    amount = Math.abs(numberFormat.parse(previousTransferPart!!.amount).toDouble())
                    destAmount = Math.abs(numberFormat.parse(lineConverter.amount).toDouble())
                } else {
                    log("Wrong transfer direction values in lines [" + (previousTransferPart!!.count + 1) + ", " + (count + 1) + "]", count + 1)
                    return
                }

                val date = dateCSVFormat.parse(lineConverter.date)
                val time = timeCSVFormat.parse(lineConverter.time)

                val b = dataManager!!.transactionService.newTransactionBuilder()
                b.putType(Transaction.TRANSFER)
                        .putDate(mergeDate(date, time))
                        .putSourceAccountId(sourceAccountId!!)
                        .putAmount(amount)

                if (!lineConverter.comment.isNullOrBlank())
                    b.putComment(lineConverter.comment)
                for (tag in extractTags()) {
                    b.putTag(tag)
                }
                b.putDestAccountId(destAccountId).putDestAmount(destAmount)

                b.createInternal()
                importResult!!.newTransactions = importResult!!.newTransactions + 1
            } catch (e: Exception) {
                handleImportException(e, count)
            }

            previousTransferPart = null
        }
    }
}
