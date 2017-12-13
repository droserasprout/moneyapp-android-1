package com.cactusteam.money.data.io.qif

import org.apache.commons.io.IOUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*

/**
 * @author vpotapenko
 */
class QifParser {

    private val accounts = ArrayList<QifAccount>()

    private var unnamedAccount: QifAccount? = null

    private var lastAccount: QifAccount? = null
    private var lastHeader: String? = null

    private var lineNumber: Int = 0

    @Throws(IOException::class)
    fun parse(sourceFile: File): List<QifAccount> {
        reset()

        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(FileReader(sourceFile))
            lineNumber = 0
            var record: QifRecord? = readRecord(reader)
            while (record != null) {
                handleRecord(record)
                record = readRecord(reader)
            }

            if (unnamedAccount != null) accounts.add(unnamedAccount!!)

            return accounts
        } finally {
            IOUtils.closeQuietly(reader)
        }
    }

    private fun handleRecord(record: QifRecord) {
        if (record.header == "!Account") {
            handleAccountRecord(record)
        } else if (record.header?.startsWith("!Type") ?: false) {
            handleTypeRecord(record)
        }
    }

    private fun handleTypeRecord(record: QifRecord) {
        val parts = record.header!!.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        if (parts.size < 2) return

        val type = parts[1]
        when (type) {
            "Cash", "Bank", "CCard" -> {
                val transaction = parseTransaction(record)
                addTransaction(transaction)
            }
        }
    }

    private fun addTransaction(transaction: QifTransaction) {
        if (lastAccount != null) {
            lastAccount!!.transactions.add(transaction)
        } else {
            if (unnamedAccount == null) {
                unnamedAccount = QifAccount()
                unnamedAccount!!.name = "Without Account"
                unnamedAccount!!.type = "Cash"
            }
            unnamedAccount!!.transactions.add(transaction)
        }
    }

    private fun parseTransaction(record: QifRecord): QifTransaction {
        val transaction = QifTransaction()
        transaction.startLine = record.startLine
        transaction.endLine = record.endLine

        for (line in record.lines) {
            if (line.startsWith("D")) {
                transaction.date = line.substring(1)
            } else if (line.startsWith("T")) {
                transaction.amount = line.substring(1)
            } else if (line.startsWith("L")) {
                transaction.category = line.substring(1)
            } else if (line.startsWith("P")) {
                transaction.payee = line.substring(1)
            } else if (line.startsWith("M")) {
                transaction.comment = line.substring(1)
            } else if (line.startsWith("S")) {
                val splitItem = QifSplitItem()
                splitItem.category = line.substring(1)
                transaction.splitItems.add(splitItem)
            } else if (line.startsWith("$")) {
                if (transaction.splitItems.size > 0) {
                    val splitItem = transaction.splitItems[transaction.splitItems.size - 1]
                    splitItem.amount = line.substring(1)
                }
            }
        }

        return transaction
    }

    private fun handleAccountRecord(record: QifRecord) {
        val account = parseAccount(record)
        if (account != null) lastAccount = account
    }

    private fun parseAccount(record: QifRecord): QifAccount? {
        val name: String =
                record.lines
                        .firstOrNull { it.startsWith("N") }
                        ?.substring(1) ?: return null

        var account = findAccountByName(name)
        if (account == null) {
            // create new
            account = QifAccount()
            account.startLine = record.startLine
            account.endLine = record.endLine

            account.name = name
            for (line in record.lines) {
                if (line.startsWith("T")) {
                    account.type = line.substring(1)
                    break
                }
            }
            accounts.add(account)
        }
        return account
    }

    private fun findAccountByName(name: String): QifAccount? {
        return accounts.firstOrNull { it.name == name }
    }

    @Throws(IOException::class)
    private fun readRecord(reader: BufferedReader): QifRecord? {
        var line: String? = reader.readLine()
        lineNumber++
        if (line == null) return null

        val record = QifRecord()
        line = line.trim { it <= ' ' }
        if (line.startsWith("!")) {
            record.header = line
            lastHeader = line
        } else {
            record.header = lastHeader
            record.lines.add(line)
        }
        record.endLine = lineNumber
        record.startLine = record.endLine

        if (line == "^") return record

        line = reader.readLine()
        while (line != null) {
            lineNumber++
            if (line == "^") break

            record.lines.add(line)
            line = reader.readLine()
        }
        record.endLine = lineNumber
        return record
    }

    private fun reset() {
        accounts.clear()
        lastAccount = null
        lastHeader = null
        unnamedAccount = null
    }

    private class QifRecord {
        internal var header: String? = null
        internal var startLine: Int = 0
        internal var endLine: Int = 0
        internal var lines: MutableList<String> = ArrayList()
    }
}
