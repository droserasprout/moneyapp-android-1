package com.cactusteam.money.data.io

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.support.v4.util.ArrayMap
import android.text.TextUtils
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.currency.MCurrency
import com.cactusteam.money.data.dao.Transaction
import java.io.File
import java.util.*

/**
 * @author vpotapenko
 */
class MyMoneyDbImporter : BaseImporter() {

    private var categories: Map<Long, String>? = null
    private var accounts: Map<Long, String>? = null
    private var currencies: Map<Long, MyMoneyCurrency>? = null

    override fun analyse(sourceFile: File, listener: (progress: Int, max: Int) -> Unit, dataManager: DataManager): ImportSchema {
        currentSchema = ImportSchema()
        this.dataManager = dataManager

        val db = SQLiteDatabase.openDatabase(sourceFile.path, null, SQLiteDatabase.OPEN_READONLY)
        loadStructure(db)

        currentSchema!!.linesCount = 0
        analyseOutsideTransactions(db)
        analyseMoving(db)
        analyseExchange(db)

        db.close()

        return currentSchema!!
    }

    private fun loadStructure(db: SQLiteDatabase) {
        categories = loadCategories(db)
        accounts = loadAccounts(db)
        currencies = loadCurrencies(db)
    }

    private fun analyseExchange(db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT * from obmen", arrayOf<String>())
        while (cursor.moveToNext()) {
            try {
                val accountId = cursor.getLong(cursor.getColumnIndex("id_schet"))
                var valId = cursor.getLong(cursor.getColumnIndex("id_val1"))
                analyseAccount(accountId, valId)

                valId = cursor.getLong(cursor.getColumnIndex("id_val2"))
                analyseAccount(accountId, valId)
            } catch (e: Exception) {
                e.printStackTrace()
                currentSchema!!.badLinesCount = currentSchema!!.badLinesCount + 1
            }

            currentSchema!!.linesCount = currentSchema!!.linesCount + 1
        }
        cursor.close()
    }

    private fun analyseMoving(db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT * from moving", arrayOf<String>())
        while (cursor.moveToNext()) {
            try {
                var accountId = cursor.getLong(cursor.getColumnIndex("id_schet1"))
                val valId = cursor.getLong(cursor.getColumnIndex("id_val"))
                analyseAccount(accountId, valId)

                accountId = cursor.getLong(cursor.getColumnIndex("id_schet2"))
                analyseAccount(accountId, valId)

            } catch (e: Exception) {
                e.printStackTrace()
                currentSchema!!.badLinesCount = currentSchema!!.badLinesCount + 1
            }

            currentSchema!!.linesCount = currentSchema!!.linesCount + 1
        }
        cursor.close()
    }

    private fun analyseOutsideTransactions(db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT * from rashodi", arrayOf<String>())
        while (cursor.moveToNext()) {
            try {
                val accountId = cursor.getLong(cursor.getColumnIndex("id_schet"))
                val valId = cursor.getLong(cursor.getColumnIndex("id_val"))
                analyseAccount(accountId, valId)

                val type = cursor.getInt(cursor.getColumnIndex("key"))

                val catId = cursor.getLong(cursor.getColumnIndex("id_cat"))
                val categoryName = categories!![catId]
                analyseCategory(categoryName!!, type == 0)

                val subCatId = cursor.getLong(cursor.getColumnIndex("id_podCat"))
                if (subCatId > 0) {
                    val subcategoryName = categories!![subCatId]
                    analyseSubcategory(categoryName, subcategoryName!!, type == 0)
                }
            } catch (e: Exception) {
                currentSchema!!.badLinesCount = currentSchema!!.badLinesCount + 1
            }

            currentSchema!!.linesCount = currentSchema!!.linesCount + 1
        }
        cursor.close()
    }

    private fun analyseAccount(accountId: Long, valId: Long) {
        val accountName = accounts!![accountId]
        val currency = currencies!![valId]

        val newAccountName = accountName + "_" + currency!!.name
        var importAccount = currentSchema!!.getAccount(newAccountName, currency.currencyCode)
        if (importAccount == null) {
            importAccount = ImportAccount(newAccountName, currency.currencyCode, NewAccountImportStrategy(newAccountName))
            currentSchema!!.putAccount(newAccountName, currency.currencyCode, importAccount)
        }
    }

    private fun loadAccounts(db: SQLiteDatabase): Map<Long, String> {
        val cursor = db.rawQuery("SELECT * from schet", arrayOf<String>())
        val accounts = ArrayMap<Long, String>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex("_id"))
            val accountName = cursor.getString(cursor.getColumnIndex("name"))

            accounts.put(id, accountName)
        }
        cursor.close()

        return accounts
    }

    private fun loadCurrencies(db: SQLiteDatabase): Map<Long, MyMoneyCurrency> {
        val cursor = db.rawQuery("SELECT * from valuta", arrayOf<String>())
        val currencies = ArrayMap<Long, MyMoneyCurrency>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex("_id"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            val currencyCode = detectCurrencyCode(name)

            currencies.put(id, MyMoneyCurrency(name, currencyCode))
        }
        cursor.close()
        return currencies
    }

    private fun detectCurrencyCode(name: String): String {
        val moneyApp = MoneyApp.instance
        val currencyManager = moneyApp.currencyManager
        var currency: MCurrency? = currencyManager.findCurrencyByCode(name)
        if (currency != null) return currency.currencyCode

        currency = currencyManager.findCurrencyByName(name)
        if (currency != null) return currency.currencyCode

        if ("$" == name) return "USD"

        return moneyApp.appPreferences.mainCurrencyCode
    }

    private fun loadCategories(db: SQLiteDatabase): Map<Long, String> {
        val cursor = db.rawQuery("SELECT * from category", arrayOf<String>())
        val categories = ArrayMap<Long, String>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex("_id"))
            val categoryName = cursor.getString(cursor.getColumnIndex("name"))

            categories.put(id, categoryName)
        }
        cursor.close()
        return categories
    }

    override fun doImport(schema: ImportSchema, sourceFile: File, listener: (progress: Int, max: Int) -> Unit, dataManager: DataManager): ImportResult {
        currentSchema = schema
        this.dataManager = dataManager

        importResult = ImportResult()

        listener(0, currentSchema!!.linesCount)

        val db = SQLiteDatabase.openDatabase(sourceFile.path, null, SQLiteDatabase.OPEN_READONLY)
        loadStructure(db)

        var count = 0
        var cursor = db.rawQuery("SELECT * from rashodi", arrayOf<String>())
        while (cursor.moveToNext()) {
            try {
                importOutsideTransaction(cursor)
                importResult!!.newTransactions = importResult!!.newTransactions + 1
            } catch (e: Exception) {
                handleImportException(e, count)
            }

            count++
            if (count % 10 == 0) listener(count, currentSchema!!.linesCount)
        }
        cursor.close()

        cursor = db.rawQuery("SELECT * from moving", arrayOf<String>())
        while (cursor.moveToNext()) {
            try {
                importMovingTransaction(cursor)
                importResult!!.newTransactions = importResult!!.newTransactions + 1
            } catch (e: Exception) {
                handleImportException(e, count)
            }

            count++
            if (count % 10 == 0) listener(count, currentSchema!!.linesCount)
        }
        cursor.close()

        cursor = db.rawQuery("SELECT * from obmen", arrayOf<String>())
        while (cursor.moveToNext()) {
            try {
                importExchangeTransaction(cursor)
                importResult!!.newTransactions = importResult!!.newTransactions + 1
            } catch (e: Exception) {
                handleImportException(e, count)
            }

            count++
            if (count % 10 == 0) listener(count, currentSchema!!.linesCount)
        }
        cursor.close()

        db.close()

        return importResult!!
    }

    private fun importExchangeTransaction(cursor: Cursor) {
        val b = dataManager!!.transactionService
                .newTransactionBuilder()
                .putType(Transaction.TRANSFER)

        val accountId = cursor.getLong(cursor.getColumnIndex("id_schet"))
        var valId = cursor.getLong(cursor.getColumnIndex("id_val2"))
        b.putSourceAccountId(getAccountId(accountId, valId))

        valId = cursor.getLong(cursor.getColumnIndex("id_val1"))
        b.putDestAccountId(getAccountId(accountId, valId))

        var amount = cursor.getDouble(cursor.getColumnIndex("summa2"))
        b.putAmount(amount)

        amount = cursor.getDouble(cursor.getColumnIndex("summa1"))
        b.putDestAmount(amount)

        val time = cursor.getLong(cursor.getColumnIndex("date"))
        b.putDate(Date(time))

        val description = cursor.getString(cursor.getColumnIndex("opis"))
        if (!description.isNullOrBlank()) b.putComment(description)

        b.createInternal()
    }

    private fun importMovingTransaction(cursor: Cursor) {
        val b = dataManager!!.transactionService
                .newTransactionBuilder()
                .putType(Transaction.TRANSFER)

        var accountId = cursor.getLong(cursor.getColumnIndex("id_schet1"))
        val valId = cursor.getLong(cursor.getColumnIndex("id_val"))
        b.putSourceAccountId(getAccountId(accountId, valId))

        accountId = cursor.getLong(cursor.getColumnIndex("id_schet2"))
        b.putDestAccountId(getAccountId(accountId, valId))

        val amount = cursor.getDouble(cursor.getColumnIndex("summa"))
        b.putAmount(amount).putDestAmount(amount)

        val time = cursor.getLong(cursor.getColumnIndex("date"))
        b.putDate(Date(time))

        val description = cursor.getString(cursor.getColumnIndex("opis"))
        if (!description.isNullOrBlank()) b.putComment(description)

        b.createInternal()
    }

    private fun importOutsideTransaction(cursor: Cursor) {
        val b = dataManager!!.transactionService.newTransactionBuilder()

        val type = cursor.getInt(cursor.getColumnIndex("key"))
        b.putType(if (type == 0) Transaction.EXPENSE else Transaction.INCOME)

        val accountId = cursor.getLong(cursor.getColumnIndex("id_schet"))
        val valId = cursor.getLong(cursor.getColumnIndex("id_val"))
        b.putSourceAccountId(getAccountId(accountId, valId))

        val catId = cursor.getLong(cursor.getColumnIndex("id_cat"))
        val categoryName = categories!![catId]
        b.putCategoryId(getCategoryId(categoryName!!, type == 0))

        val subCatId = cursor.getLong(cursor.getColumnIndex("id_podCat"))
        if (subCatId > 0) {
            val subcategoryName = categories!![subCatId]
            b.putSubcategoryId(getSubcategoryId(categoryName, subcategoryName!!, type == 0))
        }

        val amount = cursor.getDouble(cursor.getColumnIndex("summa"))
        b.putAmount(amount)

        val time = cursor.getLong(cursor.getColumnIndex("date"))
        b.putDate(Date(time))

        val description = cursor.getString(cursor.getColumnIndex("opis"))
        if (!description.isNullOrBlank()) b.putComment(description)

        b.createInternal()
    }

    private fun getAccountId(accountId: Long, valId: Long): Long {
        val accountName = accounts!![accountId]
        val currency = currencies!![valId]

        val newAccountName = accountName + "_" + currency!!.name
        return getAccountId(newAccountName, currency.currencyCode)!!
    }

    private class MyMoneyCurrency(val name: String, val currencyCode: String)
}
