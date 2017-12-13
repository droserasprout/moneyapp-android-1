package com.cactusteam.money.data.io

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.io.qif.QifAccount
import com.cactusteam.money.data.io.qif.QifParser
import com.cactusteam.money.data.io.qif.QifTransaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * @author vpotapenko
 */

internal class QifImporter : BaseImporter() {

    private val dateCSVFormat = SimpleDateFormat("dd/MM/yyyy")
    private val qifParser = QifParser()

    private var qifAccounts: List<QifAccount>? = null

    override fun analyse(sourceFile: File, listener: (progress: Int, max: Int) -> Unit, dataManager: DataManager): ImportSchema {
        currentSchema = ImportSchema()
        this.dataManager = dataManager

        parseFile(sourceFile)
        for (account in qifAccounts!!) {
            analyseQifAccount(account.name!!)

            for (t in account.transactions) {
                try {
                    val amount = java.lang.Double.parseDouble(t.amount)
                    if (!t.category.isNullOrBlank()) {
                        analyseCategory(t.category!!, amount < 0)
                    }
                    if (!t.subcategory.isNullOrBlank()) {
                        analyseSubcategory(t.category!!, t.subcategory!!, amount < 0)
                    }
                    if (!t.splitItems.isEmpty()) {
                        for (item in t.splitItems) {
                            val splitAmount = java.lang.Double.parseDouble(item.amount)
                            if (!item.category.isNullOrBlank()) {
                                analyseCategory(item.category!!, splitAmount < 0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // TODO make log and show it in UI
                    e.printStackTrace()
                    currentSchema!!.badLinesCount = currentSchema!!.badLinesCount + 1
                }

            }
        }

        return currentSchema!!
    }

    private fun analyseQifAccount(accountName: String) {
        var importAccount = currentSchema!!.getAccount(accountName, null)
        if (importAccount != null) return

        importAccount = ImportAccount(accountName, null, NewAccountImportStrategy(accountName))
        currentSchema!!.putAccount(accountName, null, importAccount)
    }

    private fun parseFile(sourceFile: File) {
        currentSchema!!.linesCount = 0

        qifAccounts = qifParser.parse(sourceFile)
        for (account in qifAccounts!!) {
            for (t in account.transactions) {
                if (t.transferAccount != null) continue

                if (t.category.isNullOrBlank() && t.splitItems.isEmpty())
                    t.category = "EMPTY"

                detectTransfer(t, account.name!!)
                detectSubcategory(t)
                currentSchema!!.linesCount = currentSchema!!.linesCount + 1
            }
        }
    }

    private fun detectSubcategory(transaction: QifTransaction) {
        if (!transaction.category.isNullOrBlank()) {
            val pair = transaction.category!!.split(":".toRegex(), 2).toTypedArray()
            if (pair.size == 2) {
                transaction.category = pair[0]
                transaction.subcategory = pair[1]
            }
        }
    }

    private fun detectTransfer(transaction: QifTransaction, accountName: String) {
        if (transaction.category == null) return

        val matcher = TRANSFER_CATEGORY_PATTERN.matcher(transaction.category)
        if (matcher.matches()) {
            val account2 = matcher.group(1)
            val account = findQifAccountByName(account2)
            if (account != null) {
                transaction.category = null
                transaction.transferAccount = account.name
                val correspondingTransaction = findCorrespondingTransaction(account, accountName,
                        transaction.date)
                if (correspondingTransaction != null) {
                    transaction.transferTransaction = correspondingTransaction

                    correspondingTransaction.transferTransaction = transaction
                    correspondingTransaction.transferAccount = accountName
                    correspondingTransaction.category = null
                }
            }
        }
    }

    private fun findCorrespondingTransaction(account: QifAccount, accountName: String, date: String?): QifTransaction? {
        for (t in account.transactions) {
            if (t.date != date || t.category == null) continue

            val matcher = TRANSFER_CATEGORY_PATTERN.matcher(t.category)
            if (matcher.matches() && matcher.group(1) == accountName) {
                return t
            }
        }
        return null
    }

    private fun findQifAccountByName(name: String): QifAccount? {
        for (account in qifAccounts!!) {
            if (account.name == name) return account
        }
        return null
    }

    @Throws(Exception::class)
    override fun doImport(schema: ImportSchema, sourceFile: File, listener: (progress: Int, max: Int) -> Unit, dataManager: DataManager): ImportResult {
        currentSchema = schema
        this.dataManager = dataManager

        parseFile(sourceFile)

        importResult = ImportResult()
        val database = dataManager.database
        try {
            listener(0, currentSchema!!.linesCount)

            var count = 0
            database.beginTransaction()
            for (qifAccount in qifAccounts!!) {
                for (t in qifAccount.transactions) {
                    importTransaction(t, qifAccount)

                    count++
                    if (count % 10 == 0) {
                        listener(count, currentSchema!!.linesCount)

                        // begin new transaction
                        database.setTransactionSuccessful()
                        database.endTransaction()

                        database.beginTransaction()
                    }
                }
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        return importResult!!
    }

    private fun importTransaction(transaction: QifTransaction, qifAccount: QifAccount) {
        if (transaction.transferAccount != null) {
            importTransfer(transaction, qifAccount)
        } else {
            importIOTransaction(transaction, qifAccount)
        }
    }

    private fun importIOTransaction(transaction: QifTransaction, qifAccount: QifAccount) {
        try {
            if (transaction.splitItems.isEmpty()) {
                val amount = java.lang.Double.parseDouble(transaction.amount)
                val category = transaction.category
                val subcategory = transaction.subcategory

                createCategoryTransaction(qifAccount, transaction, category!!, subcategory, amount)
                importResult!!.newTransactions = importResult!!.newTransactions + 1
            } else {
                for (item in transaction.splitItems) {
                    val amount = java.lang.Double.parseDouble(item.amount)
                    val category = item.category
                    createCategoryTransaction(qifAccount, transaction, category!!, null, amount)
                    importResult!!.newTransactions = importResult!!.newTransactions + 1
                }
            }
        } catch (e: Exception) {
            handleImportException(e, transaction)
        }

    }

    private fun createCategoryTransaction(qifAccount: QifAccount, transaction: QifTransaction, category: String, subcategory: String?, amount: Double) {
        val date = dateCSVFormat.parse(transaction.date)

        val b = dataManager!!.transactionService
                .newTransactionBuilder()
                .putType(if (amount > 0) Transaction.INCOME else Transaction.EXPENSE)
                .putDate(date)
                .putSourceAccountId(getAccountId(qifAccount.name!!, null)!!)
                .putAmount(Math.abs(amount))
        if (!!transaction.comment.isNullOrBlank())
            b.putComment(transaction.comment)

        if (!transaction.payee.isNullOrBlank()) {
            b.putTag(transaction.payee!!)
        }

        b.putCategoryId(getCategoryId(category, amount < 0))
        if (!subcategory.isNullOrBlank()) {
            b.putSubcategoryId(getSubcategoryId(category, subcategory!!, amount < 0))
        }
        b.createInternal()
    }

    private fun importTransfer(transaction: QifTransaction, qifAccount: QifAccount) {
        try {
            var amount = java.lang.Double.parseDouble(transaction.amount)
            if (amount > 0)
                return  // transfer has two transaction, so we need to handle only one with negative amount

            amount = Math.abs(amount)
            val srcAccount = currentSchema!!.getAccount(qifAccount.name!!, null)!!.getAccount()
            val dstAccount = currentSchema!!.getAccount(transaction.transferAccount!!, null)!!.getAccount()

            var destAmount: Double
            if (srcAccount.currencyCode == dstAccount.currencyCode) {
                destAmount = amount
            } else {
                if (transaction.transferTransaction != null) {
                    destAmount = java.lang.Double.parseDouble(transaction.transferTransaction!!.amount)
                    destAmount = Math.abs(destAmount)
                } else {
                    val currencyRate = dataManager!!.currencyService
                            .getRateInternal(srcAccount.currencyCode, dstAccount.currencyCode)
                    destAmount = currencyRate?.convertTo(amount, dstAccount.currencyCode) ?: amount
                }
            }
            val date = dateCSVFormat.parse(transaction.date)

            val b = dataManager!!.transactionService
                    .newTransactionBuilder()
                    .putType(Transaction.TRANSFER)
                    .putDate(date)
                    .putSourceAccountId(srcAccount.id!!)
                    .putAmount(amount)
            if (!transaction.comment.isNullOrBlank())
                b.putComment(transaction.comment)
            if (!transaction.payee.isNullOrBlank()) {
                b.putTag(transaction.payee!!)
            }

            b.putDestAccountId(dstAccount.id).putDestAmount(destAmount)

            b.createInternal()
            importResult!!.newTransactions = importResult!!.newTransactions + 1
        } catch (e: Exception) {
            handleImportException(e, transaction)
        }

    }

    private fun handleImportException(e: Exception, t: QifTransaction) {
        e.printStackTrace()
        log(e.message, t.startLine)
    }

    companion object {

        private val TRANSFER_CATEGORY_PATTERN = Pattern.compile("^\\[(.*)\\]$")
    }
}
