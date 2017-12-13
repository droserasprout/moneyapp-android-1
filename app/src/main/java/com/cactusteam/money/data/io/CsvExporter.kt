package com.cactusteam.money.data.io

import android.content.Context
import au.com.bytecode.opencsv.CSVWriter
import com.cactusteam.money.R
import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.dao.Transaction
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author vpotapenko
 */
class CsvExporter(context: Context) : BaseExporter(context) {

    private val transactions = ArrayList<Transaction>()

    private var csvWriter: CSVWriter? = null
    private val line = arrayOfNulls<String>(CELLS_NUMBER)

    private var mainCurrency: String? = null

    private val lineConverter = CsvLineConverter()

    private val dateCSVFormat: SimpleDateFormat
    private val timeCSVFormat: SimpleDateFormat

    private val numberFormat: NumberFormat

    init {

        dateCSVFormat = SimpleDateFormat("yyyy-MM-dd")
        timeCSVFormat = SimpleDateFormat("HH:mm:ss")

        numberFormat = NumberFormat.getNumberInstance()
    }

    override fun initialize(mainCurrency: String) {
        this.mainCurrency = mainCurrency

        transactions.clear()
    }

    override fun export(t: Transaction) {
        transactions.add(t)
    }

    override fun commit(exportFolder: File): String {
        try {
            val filePath = createCsvWriter(exportFolder)

            writeHeader()
            writeTransactions()

            return filePath
        } finally {
            IOUtils.closeQuietly(csvWriter)
        }
    }

    private fun writeTransactions() {
        for (t in transactions) {
            if (t.type == Transaction.EXPENSE) {
                writeExpense(t)
            } else if (t.type == Transaction.INCOME) {
                writeIncome(t)
            } else if (t.type == Transaction.TRANSFER) {
                writeTransfer(t)
            }
        }
    }

    private fun writeTransfer(t: Transaction) {
        lineConverter.date = dateCSVFormat.format(t.date)
        lineConverter.time = timeCSVFormat.format(t.date)
        lineConverter.accountName = t.sourceAccount.name
        lineConverter.amount = numberFormat.format(-t.amount)
        lineConverter.currencyCode = t.sourceAccount.currencyCode
        lineConverter.amountInMain = numberFormat.format(-t.amountInMainCurrency)
        fillTags(t)
        if (!t.comment.isNullOrBlank()) {
            lineConverter.comment = t.comment
        }
        lineConverter.outDirection()
        writeCSVLine()

        lineConverter.date = dateCSVFormat.format(t.date)
        lineConverter.time = timeCSVFormat.format(t.date)
        lineConverter.accountName = t.destAccount.name
        lineConverter.amount = numberFormat.format(t.destAmount)
        lineConverter.currencyCode = t.destAccount.currencyCode
        lineConverter.amountInMain = numberFormat.format(t.amountInMainCurrency)
        fillTags(t)
        if (!t.comment.isNullOrBlank()) {
            lineConverter.comment = t.comment
        }
        lineConverter.inDirection()
        writeCSVLine()
    }

    private fun writeIncome(t: Transaction) {
        fillIOTransaction(t)
        lineConverter.amount = numberFormat.format(t.amount)
        lineConverter.amountInMain = numberFormat.format(t.amountInMainCurrency)

        writeCSVLine()
    }

    private fun writeExpense(t: Transaction) {
        fillIOTransaction(t)
        lineConverter.amount = numberFormat.format(-t.amount)
        lineConverter.amountInMain = numberFormat.format(-t.amountInMainCurrency)

        writeCSVLine()
    }

    private fun fillIOTransaction(t: Transaction) {
        lineConverter.date = dateCSVFormat.format(t.date)
        lineConverter.time = timeCSVFormat.format(t.date)
        lineConverter.accountName = t.sourceAccount.name
        lineConverter.currencyCode = t.sourceAccount.currencyCode
        lineConverter.category = t.category.name
        if (t.subcategory != null) {
            lineConverter.subcategory = t.subcategory.name
        }
        fillTags(t)
        if (!t.comment.isNullOrBlank()) {
            lineConverter.comment = t.comment
        }
    }

    private fun fillTags(t: Transaction) {
        if (t.tags.isEmpty()) return

        val sb = StringBuilder()
        for (tag in t.tags) {
            if (sb.isNotEmpty()) sb.append(':')
            sb.append(tag.tag.name)
        }
        lineConverter.tags = sb.toString()
    }

    private fun writeHeader() {
        lineConverter.date = context.getString(R.string.date_label)
        lineConverter.time = context.getString(R.string.time_label)
        lineConverter.accountName = context.getString(R.string.account_label)
        lineConverter.amount = context.getString(R.string.amount_label)
        lineConverter.currencyCode = context.getString(R.string.currency_label)
        lineConverter.amountInMain = context.getString(R.string.amount_pattern, mainCurrency)
        lineConverter.category = context.getString(R.string.category_label)
        lineConverter.subcategory = context.getString(R.string.subcategory_label)
        lineConverter.tags = context.getString(R.string.tags)
        lineConverter.comment = context.getString(R.string.comment_label)
        lineConverter.transferDirection = context.getString(R.string.direction_label)

        writeCSVLine()
    }

    private fun writeCSVLine() {
        lineConverter.prepareLine(line)
        csvWriter!!.writeNext(line)
        lineConverter.clear()
    }

    private fun createCsvWriter(exportFolder: File): String {
        val dateFormat = SimpleDateFormat("yyyy_MM_dd")
        val fileNamePrefix = DataConstants.TRANSACTIONS_EXPORT_PREFIX + dateFormat.format(Date())

        var count = 0
        var newFile = File(exportFolder, fileNamePrefix + ".csv")
        while (newFile.exists()) {
            newFile = File(exportFolder, "$fileNamePrefix($count).csv")
            count++
        }

        csvWriter = CSVWriter(FileWriter(newFile))
        return newFile.path
    }

    companion object {

        val CELLS_NUMBER = 11
    }
}
