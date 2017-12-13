package com.cactusteam.money.data.io

import android.text.format.DateFormat
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Transaction
import java.text.DecimalFormat

/**
 * @author vpotapenko
 */

internal class FinancePMImporter : BaseCsvImporter() {

    private val lineConverter = FinancePMLineConverter()
    private val dateFormat: java.text.DateFormat
    private val formatter = DecimalFormat("#,##0.##")

    private val expenseType: String
    private val incomeType: String

    init {
        headerCount = 1

        val moneyApp = MoneyApp.instance
        dateFormat = DateFormat.getDateFormat(moneyApp)

        expenseType = moneyApp.getString(R.string.expense_csv_type)
        incomeType = moneyApp.getString(R.string.income_csv_type)
    }

    override fun detectCellsNumber(line: Array<String>) {
        currentSchema!!.cellsNumber = 8
    }

    override fun analyseCsvLine(line: Array<String>) {
        lineConverter.extractFromLine(line)
        try {

            formatter.parse(lineConverter.amount)
            analyseAccount(lineConverter.accountName!!, lineConverter.currencyCode)

            if (!lineConverter.category.isNullOrBlank())
                analyseCategory(lineConverter.category!!, lineConverter.type == expenseType)
        } catch (e: Exception) {
            e.printStackTrace()
            currentSchema!!.badLinesCount++
        }

    }

    override fun importLine(line: Array<String>, count: Int) {
        lineConverter.extractFromLine(line)

        if (lineConverter.type == expenseType) {
            importExpense(count)
        } else if (lineConverter.type == incomeType) {
            importIncome(count)
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
        val b = dataManager!!.transactionService.newTransactionBuilder()
        val date = dateFormat.parse(lineConverter.date)

        b.putType(type)
                .putDate(date)
                .putSourceAccountId(getAccountId(lineConverter.accountName!!, lineConverter.currencyCode)!!)
                .putAmount(Math.abs(formatter.parse(lineConverter.amount).toDouble()))

        if (!lineConverter.comment.isNullOrBlank())
            b.putComment(lineConverter.comment)

        b.putCategoryId(getCategoryId(lineConverter.category!!, type == Transaction.EXPENSE))
        b.createInternal()
    }
}
