package com.cactusteam.money.data.io

import au.com.bytecode.opencsv.CSVReader
import com.cactusteam.money.data.DataManager
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*

/**
 * @author vpotapenko
 */
abstract class BaseCsvImporter(val csvCharset: String = "UTF-8", val csvSeparator: Char = ',') : BaseImporter() {

    var headerCount = 1

    override fun analyse(sourceFile: File, listener: (progress: Int, max: Int) -> Unit, dataManager: DataManager): ImportSchema {
        currentSchema = ImportSchema()
        this.dataManager = dataManager

        var csvReader: CSVReader? = null
        try {
            csvReader = createCSVReader(sourceFile)
            val all = csvReader.readAll()
            if (all.size > 0) detectCellsNumber(all[0])

            listener(0, all.size)

            currentSchema!!.linesCount = all.size - 1

            var count = 0
            for (line in all) {
                if (count < headerCount) {
                    count++
                    continue // skip header
                }

                if (line.size == currentSchema!!.cellsNumber) {
                    analyseCsvLine(line)
                } else {
                    currentSchema!!.badLinesCount++
                }

                count++
                if (count % 10 == 0) listener(count, all.size)
            }
        } finally {
            IOUtils.closeQuietly(csvReader)
        }

        return currentSchema!!
    }

    internal abstract fun detectCellsNumber(line: Array<String>)

    protected abstract fun analyseCsvLine(line: Array<String>)

    override fun doImport(schema: ImportSchema, sourceFile: File, listener: (progress: Int, max: Int) -> Unit, dataManager: DataManager): ImportResult {
        currentSchema = schema
        this.dataManager = dataManager

        importResult = ImportResult()

        var csvReader: CSVReader? = null
        val database = dataManager.database
        try {
            csvReader = createCSVReader(sourceFile)
            val all = csvReader.readAll()
            if (all.size > 0) detectCellsNumber(all[0])

            listener(0, all.size)

            database.beginTransaction()
            var count = 0
            for (line in all) {
                if (count < headerCount) {
                    count++
                    continue // skip header
                }

                if (line.size == currentSchema!!.cellsNumber) {
                    importLine(line, count)
                } else {
                    val s = line.joinToString(";")
                    log("Wrong cells number in line: " + s, count + 1)
                }

                count++
                if (count % 10 == 0) {
                    listener(count, all.size)

                    // begin new transaction
                    database.setTransactionSuccessful()
                    database.endTransaction()

                    database.beginTransaction()
                }
            }
            database.setTransactionSuccessful()
        } finally {
            IOUtils.closeQuietly(csvReader)
            database.endTransaction()
        }

        return importResult!!
    }

    private fun createCSVReader(sourceFile: File): CSVReader {
        return CSVReader(InputStreamReader(FileInputStream(sourceFile), csvCharset), csvSeparator)
    }

    protected abstract fun importLine(line: Array<String>, count: Int)

    fun mergeDate(date: Date, time: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = time

        val hh = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, hh)
        calendar.set(Calendar.MINUTE, minutes)
        calendar.set(Calendar.SECOND, seconds)

        return calendar.time
    }
}
