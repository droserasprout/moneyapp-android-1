package com.cactusteam.money.data.io

import android.text.format.DateFormat
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Transaction

/**
 * @author vpotapenko
 */
internal class ViZiBudgetImporter : BaseCsvImporter() {

    private val lineConverter = ViZiBudgetLineConverter()
    private val dateFormat: java.text.DateFormat

    private val expenseType: String
    private val incomeType: String
    private val transferType: String

    init {
        headerCount = 2

        val moneyApp = MoneyApp.instance
        dateFormat = DateFormat.getDateFormat(moneyApp)

        expenseType = moneyApp.getString(R.string.expense_csv_type)
        incomeType = moneyApp.getString(R.string.income_csv_type)
        transferType = moneyApp.getString(R.string.transfer_csv_type)
    }

    override fun detectCellsNumber(line: Array<String>) {
        currentSchema!!.cellsNumber = 11
    }

    override fun analyseCsvLine(line: Array<String>) {
        lineConverter.extractFromLine(line)
        try {

            java.lang.Double.parseDouble(lineConverter.amount)
            analyseAccount(lineConverter.accountName!!, lineConverter.currencyCode)

            if (!lineConverter.accountName2.isNullOrBlank())
                analyseAccount(lineConverter.accountName2!!, lineConverter.currencyCode2)

            if (!lineConverter.category.isNullOrBlank())
                analyseCategory(lineConverter.category!!, lineConverter.type == expenseType)
            if (!lineConverter.subcategory.isNullOrBlank())
                analyseSubcategory(lineConverter.category!!, lineConverter.subcategory!!, lineConverter.type == expenseType)
        } catch (e: Exception) {
            e.printStackTrace()
            currentSchema!!.badLinesCount = currentSchema!!.badLinesCount + 1
        }

    }

    override fun importLine(line: Array<String>, count: Int) {
        lineConverter.extractFromLine(line)

        if (lineConverter.type == expenseType) {
            importExpense(count)
        } else if (lineConverter.type == incomeType) {
            importIncome(count)
        } else if (lineConverter.type == transferType) {
            importTransfer(count)
        }
    }

    private fun importTransfer(count: Int) {
        try {
            val date = dateFormat.parse(lineConverter.date)

            val b = dataManager!!.transactionService
                    .newTransactionBuilder()
                    .putType(Transaction.TRANSFER)
                    .putDate(date)
                    .putSourceAccountId(getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)!!)
                    .putAmount(java.lang.Double.parseDouble(lineConverter.amount))
            if (!lineConverter.comment.isNullOrBlank())
                b.putComment(lineConverter.comment)
            b.putDestAccountId(getAccountId(lineConverter.accountName2!!, lineConverter.currencyCode2))

            b.putDestAmount(java.lang.Double.parseDouble(lineConverter.amount2))

            b.createInternal()
            importResult!!.newTransactions = importResult!!.newTransactions + 1
        } catch (e: Exception) {
            handleImportException(e, count)
        }

    }

    private fun importIncome(count: Int) {
        try {
            createIOTransaction(Transaction.INCOME)
            importResult!!.newTransactions = importResult!!.newTransactions + 1
        } catch (e: Exception) {
            handleImportException(e, count)
        }

    }

    private fun importExpense(count: Int) {
        try {
            createIOTransaction(Transaction.EXPENSE)
            importResult!!.newTransactions = importResult!!.newTransactions + 1
        } catch (e: Exception) {
            handleImportException(e, count)
        }

    }

    private fun createIOTransaction(type: Int) {
        val date = dateFormat.parse(lineConverter.date)

        val b = dataManager!!.transactionService
                .newTransactionBuilder()
                .putType(type)
                .putDate(date)
                .putSourceAccountId(getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)!!)
                .putAmount(java.lang.Double.parseDouble(lineConverter.amount))
        if (!lineConverter.comment.isNullOrBlank())
            b.putComment(lineConverter.comment)

        b.putCategoryId(getCategoryId(lineConverter.category!!, type == Transaction.EXPENSE))
        if (!lineConverter.subcategory.isNullOrBlank()) {
            b.putSubcategoryId(getSubcategoryId(lineConverter.category!!, lineConverter.subcategory!!, type == Transaction.EXPENSE))
        }
        b.createInternal()
    }
}
