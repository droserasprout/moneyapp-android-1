package com.cactusteam.money.data

import android.database.sqlite.SQLiteDatabase
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.DaoMaster
import com.cactusteam.money.data.dao.DaoSession
import com.cactusteam.money.data.dao.DbOpenHelper
import com.cactusteam.money.data.service.*
import org.apache.commons.io.FileUtils
import java.util.*

/**
 * @author vpotapenko
 */
class DataManager(db: SQLiteDatabase?) {

    val daoSession: DaoSession
        get() {
            ensureOpenedSession()
            return _daoSession!!
        }
    val database: SQLiteDatabase
        get() {
            ensureOpenedSession()
            return _database!!
        }

    private var _daoSession: DaoSession? = null
    private var _database: SQLiteDatabase? = null

    init {
        if (db != null) {
            _database = db
            _daoSession = DaoMaster(db).newSession()
        }
    }

    private val listeners = HashSet<IBalanceListener>()

    val accountService: AccountService by lazy { AccountService(this) }
    val categoryService: CategoryService by lazy { CategoryService(this) }
    val transactionService: TransactionService by lazy { TransactionService(this) }
    val patternService: PatternService by lazy { PatternService(this) }
    val debtService: DebtService by lazy { DebtService(this) }
    val budgetService: BudgetService by lazy { BudgetService(this) }
    val currencyService: CurrencyService by lazy { CurrencyService(this) }
    val reportService: ReportService by lazy { ReportService(this) }
    val tagService: TagService by lazy { TagService(this) }
    val noteService: NoteService by lazy { NoteService(this) }

    val syncService: SyncService by lazy { SyncService(this) }

    val systemService: SystemService by lazy { SystemService(this) }
    val backupService: BackupService by lazy { BackupService(this) }
    val fileService: FileService by lazy { FileService(this) }

    fun replaceDataByRestored() {
        if (_database != null) closeDatabase()

        val context = MoneyApp.instance

        val temp = context.getFileStreamPath("tempFile")
        if (temp.exists()) FileUtils.forceDelete(temp)

        val mainDb = context.getDatabasePath(DataManager.DB_NAME)
        if (mainDb.exists()) FileUtils.copyFile(mainDb, temp)

        val restoreDb = context.getDatabasePath(DataManager.RESTORE_DB_NAME)
        try {
            FileUtils.copyFile(restoreDb, mainDb)
        } catch (e: Exception) {
            if (temp.exists()) FileUtils.copyFile(temp, mainDb)
            throw e
        }

        FileUtils.deleteQuietly(temp)
        FileUtils.deleteQuietly(restoreDb)

        ensureOpenedSession()
        fireBalanceChanged()
    }

    fun closeDatabase() {
        _database?.close()

        _daoSession = null
        _database = null
    }

    private fun ensureOpenedSession() {
        if (_daoSession != null) return

        val helper = DbOpenHelper(MoneyApp.instance, DataManager.DB_NAME, null)
        _database = helper.writableDatabase
        val master = DaoMaster(_database)
        _daoSession = master.newSession()

        if (!hasDbData()) initializeData()

        fireBalanceChanged()
    }

    private fun initializeData() {
        _daoSession!!.callInTx {
            DbInitializer().execute(_daoSession)
            null
        }
    }

    private fun hasDbData(): Boolean {
        return _daoSession!!.categoryDao.count() > 0
    }

    fun addBalanceListener(l: IBalanceListener) {
        listeners.add(l)
    }

    fun removeBalanceListener(l: IBalanceListener) {
        listeners.remove(l)
    }

    fun fireBalanceChanged() {
        for (l in listeners) {
            l.balanceChanged()
        }
    }

    fun createRestoreDatabase(): SQLiteDatabase {
        val context = MoneyApp.instance
        val dbFile = context.getDatabasePath(RESTORE_DB_NAME)
        if (dbFile.exists()) FileUtils.forceDelete(dbFile)

        val helper = DbOpenHelper(context, RESTORE_DB_NAME, null)
        return helper.writableDatabase
    }

    fun clearAllData() {
        closeDatabase()

        val context = MoneyApp.instance
        val mainDb = context.getDatabasePath(DB_NAME)

        FileUtils.forceDelete(mainDb)
    }

    companion object {

        val DB_NAME = "money-app-db"
        val RESTORE_DB_NAME = "restore-money-app-db"
    }
}
