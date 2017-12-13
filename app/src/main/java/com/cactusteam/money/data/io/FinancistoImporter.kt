package com.cactusteam.money.data.io

import com.cactusteam.money.data.dao.Transaction
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * @author vpotapenko
 */
class FinancistoImporter : BaseCsvImporter() {

    private val lineConverter = FinancistoLineConverter()

    private val dateCSVFormat = SimpleDateFormat("yyyy-MM-dd")
    private val timeCSVFormat = SimpleDateFormat("HH:mm:ss")

    override fun detectCellsNumber(line: Array<String>) {
        currentSchema!!.cellsNumber = 13
    }

    override fun analyseCsvLine(line: Array<String>) {
        lineConverter.extractFromLine(line)
        try {
            val amount = java.lang.Double.parseDouble(lineConverter.amount)

            analyseAccount(lineConverter.accountName!!, lineConverter.currencyCode)

            if (!lineConverter.category.isNullOrBlank())
                analyseCategory(lineConverter.category!!, amount < 0)
            if (!lineConverter.subcategory.isNullOrBlank())
                analyseSubcategory(lineConverter.category!!, lineConverter.subcategory!!, amount < 0)

        } catch (e: ParseException) {
            e.printStackTrace()
            currentSchema!!.badLinesCount++
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
                val amount = java.lang.Double.parseDouble(lineConverter.amount)

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
                if (!lineConverter.payee.isNullOrBlank()) {
                    b.putTag(lineConverter.payee!!)
                }

                b.putCategoryId(getCategoryId(lineConverter.category!!, amount < 0))
                if (lineConverter.subcategory.isNullOrBlank()) {
                    b.putSubcategoryId(getSubcategoryId(lineConverter.category!!, lineConverter.subcategory!!, amount < 0))
                }
                b.createInternal()
                importResult!!.newTransactions++
            } catch (e: Exception) {
                handleImportException(e, count)
            }

        }
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

                if (FinancistoLineConverter.IN == previousTransferPart!!.direction && FinancistoLineConverter.OUT == lineConverter.transferDirection) {
                    sourceAccountId = getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)
                    destAccountId = getAccountId(previousTransferPart!!.accountName!!, previousTransferPart!!.currencyCode)

                    amount = Math.abs(java.lang.Double.parseDouble(lineConverter.amount))
                    destAmount = Math.abs(java.lang.Double.parseDouble(previousTransferPart!!.amount))
                } else if (FinancistoLineConverter.IN == lineConverter.transferDirection && FinancistoLineConverter.OUT == previousTransferPart!!.direction) {
                    sourceAccountId = getAccountId(previousTransferPart!!.accountName!!, previousTransferPart!!.currencyCode)
                    destAccountId = getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)

                    amount = Math.abs(java.lang.Double.parseDouble(previousTransferPart!!.amount))
                    destAmount = Math.abs(java.lang.Double.parseDouble(lineConverter.amount))
                } else {
                    log("Wrong transfer direction values in lines [" + (previousTransferPart!!.count + 1) + ", " + (count + 1) + "]", count + 1)
                    return
                }

                val b = dataManager!!.transactionService.newTransactionBuilder()
                val date = dateCSVFormat.parse(lineConverter.date)
                val time = timeCSVFormat.parse(lineConverter.time)

                b.putType(Transaction.TRANSFER)
                        .putDate(mergeDate(date, time))
                        .putSourceAccountId(sourceAccountId!!)
                        .putAmount(amount)
                if (lineConverter.comment != null && lineConverter.comment!!.isNotBlank())
                    b.putComment(lineConverter.comment)

                if (!lineConverter.payee.isNullOrBlank()) {
                    b.putTag(lineConverter.payee!!)
                }

                b.putDestAccountId(destAccountId).putDestAmount(destAmount)

                b.createInternal()
                importResult!!.newTransactions++
            } catch (e: Exception) {
                handleImportException(e, count)
            }

            previousTransferPart = null
        }
    }
}
