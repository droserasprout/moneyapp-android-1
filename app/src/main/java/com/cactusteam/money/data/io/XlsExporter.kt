package com.cactusteam.money.data.io

import android.content.Context
import com.cactusteam.money.R
import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.dao.Transaction
import org.apache.commons.io.IOUtils
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author vpotapenko
 */
class XlsExporter(context: Context) : BaseExporter(context) {

    private var mainCurrency: String? = null

    private var wb: Workbook? = null
    private var sheet: Sheet? = null

    private var sheetCount: Int = 0
    private var rowCount: Int = 0
    private var dateCellStyle: CellStyle? = null

    override fun initialize(mainCurrency: String) {
        this.mainCurrency = mainCurrency

        wb = HSSFWorkbook()

        createNewSheet()
    }

    private fun createNewSheet() {
        val sheetName = context.getString(R.string.transactions_title) + if (sheetCount > 0) sheetCount.toString() else ""
        sheet = wb!!.createSheet(sheetName)

        createHeader(sheet!!, mainCurrency!!)

        rowCount = 1
    }

    override fun export(t: Transaction) {
        if (t.type == Transaction.EXPENSE) {
            saveExpense(t)
        } else if (t.type == Transaction.INCOME) {
            saveIncome(t)
        } else if (t.type == Transaction.TRANSFER) {
            saveTransfer(t)
        }

        if (rowCount > MAX_ROWS) {
            sheetCount++
            createNewSheet()
        }
    }

    private fun saveTransfer(t: Transaction) {
        var row = sheet!!.createRow(rowCount)
        rowCount++
        createDateCell(row, t.date)
        row.createCell(1, Cell.CELL_TYPE_STRING).setCellValue(t.sourceAccount.name)
        row.createCell(2, Cell.CELL_TYPE_NUMERIC).setCellValue(-t.amount)
        row.createCell(3, Cell.CELL_TYPE_STRING).setCellValue(t.sourceAccount.currencyCode)
        row.createCell(4, Cell.CELL_TYPE_NUMERIC).setCellValue(-t.amountInMainCurrency)
        createTagsCell(row, t)
        if (!t.comment.isNullOrBlank())
            row.createCell(8, Cell.CELL_TYPE_STRING).setCellValue(t.comment)
        row.createCell(9, Cell.CELL_TYPE_STRING).setCellValue("out")

        row = sheet!!.createRow(rowCount)
        rowCount++
        createDateCell(row, t.date)
        row.createCell(1, Cell.CELL_TYPE_STRING).setCellValue(t.destAccount.name)
        row.createCell(2, Cell.CELL_TYPE_NUMERIC).setCellValue(t.destAmount!!)
        row.createCell(3, Cell.CELL_TYPE_STRING).setCellValue(t.destAccount.currencyCode)
        row.createCell(4, Cell.CELL_TYPE_NUMERIC).setCellValue(t.amountInMainCurrency)
        createTagsCell(row, t)
        if (!t.comment.isNullOrBlank())
            row.createCell(8, Cell.CELL_TYPE_STRING).setCellValue(t.comment)
        row.createCell(9, Cell.CELL_TYPE_STRING).setCellValue("in")
    }

    private fun saveIoTransaction(t: Transaction): Row {
        val row = sheet!!.createRow(rowCount)
        rowCount++

        createDateCell(row, t.date)
        row.createCell(1, Cell.CELL_TYPE_STRING).setCellValue(t.sourceAccount.name)
        row.createCell(3, Cell.CELL_TYPE_STRING).setCellValue(t.sourceAccount.currencyCode)
        row.createCell(5, Cell.CELL_TYPE_STRING).setCellValue(t.category.name)
        if (t.subcategory != null) {
            row.createCell(6, Cell.CELL_TYPE_STRING).setCellValue(t.subcategory.name)
        }
        createTagsCell(row, t)

        if (!t.comment.isNullOrBlank())
            row.createCell(8, Cell.CELL_TYPE_STRING).setCellValue(t.comment)

        return row
    }

    private fun saveIncome(t: Transaction) {
        val row = saveIoTransaction(t)
        row.createCell(2, Cell.CELL_TYPE_NUMERIC).setCellValue(t.amount)
        row.createCell(4, Cell.CELL_TYPE_NUMERIC).setCellValue(t.amountInMainCurrency)
    }

    private fun saveExpense(t: Transaction) {
        val row = saveIoTransaction(t)
        row.createCell(2, Cell.CELL_TYPE_NUMERIC).setCellValue(-t.amount)
        row.createCell(4, Cell.CELL_TYPE_NUMERIC).setCellValue(-t.amountInMainCurrency)
    }

    private fun createTagsCell(row: Row, t: Transaction) {
        if (t.tags.isEmpty()) return

        val sb = StringBuilder()
        for (tag in t.tags) {
            if (sb.isNotEmpty()) sb.append(':')
            sb.append(tag.tag.name)
        }
        row.createCell(7, Cell.CELL_TYPE_STRING).setCellValue(sb.toString())
    }

    private fun createHeader(sheet: Sheet, mainCurrency: String) {
        val row = sheet.createRow(0)


        row.createCell(0, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.date_label))
        row.createCell(1, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.account_label))
        row.createCell(2, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.amount_label))
        row.createCell(3, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.currency_label))
        row.createCell(4, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.amount_pattern, mainCurrency))
        row.createCell(5, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.category_label))
        row.createCell(6, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.subcategory_label))
        row.createCell(7, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.tags))
        row.createCell(8, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.comment_label))
        row.createCell(9, Cell.CELL_TYPE_STRING).setCellValue(context.getString(R.string.direction_label))
    }

    private fun createDateCell(row: Row, date: Date) {
        val dateCell = row.createCell(0)
        dateCell.cellStyle = getDateCellStyle()
        dateCell.setCellValue(date)
    }

    private fun getDateCellStyle(): CellStyle {
        if (dateCellStyle == null) {
            val formatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
            val pattern = (formatter as SimpleDateFormat).toPattern()

            dateCellStyle = wb!!.createCellStyle()
            val dataFormat = wb!!.creationHelper.createDataFormat()
            dateCellStyle!!.dataFormat = dataFormat.getFormat(pattern)
        }
        return dateCellStyle!!
    }

    override fun commit(exportFolder: File): String {
        val dateFormat = SimpleDateFormat("yyyy_MM_dd")
        val fileNamePrefix = DataConstants.TRANSACTIONS_EXPORT_PREFIX + dateFormat.format(Date())

        var count = 0
        var newFile = File(exportFolder, fileNamePrefix + ".xls")
        while (newFile.exists()) {
            newFile = File(exportFolder, "$fileNamePrefix($count).xls")
            count++
        }

        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(newFile)
            wb!!.write(out)
        } finally {
            IOUtils.closeQuietly(out)
        }

        return newFile.path
    }

    companion object {

        private val MAX_ROWS = 39998
    }
}
