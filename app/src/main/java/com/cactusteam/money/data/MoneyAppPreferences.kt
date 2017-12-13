package com.cactusteam.money.data

import android.content.Context
import android.content.SharedPreferences

import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.period.Period
import com.cactusteam.money.sync.SyncService
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.format.DateTimeFormatter

/**
 * @author vpotapenko
 */
class MoneyAppPreferences(context: Context) {

    private val preferences: SharedPreferences

    var lastLoginTime: Long? = null

    init {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    val lastDailyWork: Long?
        get() {
            val value = preferences.getLong(LAST_DAILY_WORK, -1)
            return if (value == -1L) null else value
        }

    fun setLastDailyWork(time: Long) {
        preferences.edit().putLong(LAST_DAILY_WORK, time).apply()
    }

    var mainCurrencyCode: String
        get() {
            val value = preferences.getString(MAIN_CURRENCY_CODE, null)
            return value ?: CurrencyUtils.localeCurrencyCode
        }
        set(currencyCode) = preferences.edit().putString(MAIN_CURRENCY_CODE, currencyCode).apply()

    var syncDeviceId: String?
        get() = preferences.getString(SYNC_DEVICE_ID, null)
        set(deviceId) = preferences.edit().putString(SYNC_DEVICE_ID, deviceId).apply()

    var backupMaxNumber: Int
        get() = preferences.getInt(BACKUP_MAX_NUMBER, DataConstants.DEFAULT_BACKUP_NUMBERS)
        set(max) = preferences.edit().putInt(BACKUP_MAX_NUMBER, max).apply()

    var backupPath: String
        get() {
            var path = ""
            try {
                path = DataUtils.initialBackupFolder.path
            } catch (ignore: Exception) {
            }

            return preferences.getString(BACKUP_PATH, path)
        }
        set(path) = preferences.edit().putString(BACKUP_PATH, path).apply()

    var isFirstStart: Boolean
        get() = preferences.getBoolean(FIRST_START, true)
        set(b) = preferences.edit().putBoolean(FIRST_START, b).apply()

    var isOpenAmountTransaction: Boolean
        get() = preferences.getBoolean(OPEN_AMOUNT_TRANSACTION, true)
        set(b) = preferences.edit().putBoolean(OPEN_AMOUNT_TRANSACTION, b).apply()

    var isAutoBackup: Boolean
        get() = preferences.getBoolean(AUTO_BACKUP, true)
        set(b) = preferences.edit().putBoolean(AUTO_BACKUP, b).apply()

    var isManualSyncMode: Boolean
        get() = preferences.getBoolean(SYNC_MANUAL_MODE, false)
        set(b) = preferences.edit().putBoolean(SYNC_MANUAL_MODE, b).apply()

    var syncPeriod: Int
        get() = preferences.getInt(SYNC_PERIOD, SyncService.ONE_PER_DAY_PERIOD)
        set(period) = preferences.edit().putInt(SYNC_PERIOD, period).apply()

    var syncLastTry: Long
        get() = preferences.getLong(SYNC_LAST_TRY, -1)
        set(timeInMillis) = preferences.edit().putLong(SYNC_LAST_TRY, timeInMillis).apply()

    var period: Period
        get() {
            val serializeString = preferences.getString(PERIOD, null)
            return if (serializeString == null) Period(Period.MONTH_TYPE, 1) else Period(serializeString)
        }
        set(period) = preferences.edit().putString(PERIOD, period.toSerializeString()).apply()

    fun setPeriodStr(periodStr: String) {
        preferences.edit().putString(PERIOD, periodStr).apply()
    }

    var lastAccountId: Long
        get() = preferences.getLong(LAST_ACCOUNT, -1)
        set(accountId) = preferences.edit().putLong(LAST_ACCOUNT, accountId).apply()

    var lastCategoryId: Long
        get() = preferences.getLong(LAST_CATEGORY, -1)
        set(categoryId) = preferences.edit().putLong(LAST_CATEGORY, categoryId).apply()

    var mainBalanceType: Int
        get() = preferences.getInt(MAIN_BALANCE_TYPE, 0)
        set(type) = preferences.edit().putInt(MAIN_BALANCE_TYPE, type).apply()

    var isMainShowPatterns: Boolean
        get() = preferences.getBoolean(MAIN_SHOW_PATTERNS, true)
        set(value) = preferences.edit().putBoolean(MAIN_SHOW_PATTERNS, value).apply()

    var isMainShowTotalBalance: Boolean
        get() = preferences.getBoolean(MAIN_SHOW_TOTAL_BALANCE, true)
        set(show) = preferences.edit().putBoolean(MAIN_SHOW_TOTAL_BALANCE, show).apply()

    var isMainShowBudget: Boolean
        get() = preferences.getBoolean(MAIN_SHOW_BUDGET, false)
        set(show) = preferences.edit().putBoolean(MAIN_SHOW_BUDGET, show).apply()

    var isMainShowTransactions: Boolean
        get() = preferences.getBoolean(MAIN_SHOW_TRANSACTIONS, true)
        set(show) = preferences.edit().putBoolean(MAIN_SHOW_TRANSACTIONS, show).apply()

    var isMainShowAccounts: Boolean
        get() = preferences.getBoolean(MAIN_SHOW_ACCOUNTS, false)
        set(show) = preferences.edit().putBoolean(MAIN_SHOW_ACCOUNTS, show).apply()

    var isMainShowBalanceChart: Boolean
        get() = preferences.getBoolean(MAIN_SHOW_BALANCE_CHART, false)
        set(show) = preferences.edit().putBoolean(MAIN_SHOW_BALANCE_CHART, show).apply()

    var isMainShowExpenseChart: Boolean
        get() = preferences.getBoolean(MAIN_SHOW_EXPENSE_CHART, false)
        set(show) = preferences.edit().putBoolean(MAIN_SHOW_EXPENSE_CHART, show).apply()

    var isMainShowIncomeChart: Boolean
        get() = preferences.getBoolean(MAIN_SHOW_INCOME_CHART, false)
        set(show) = preferences.edit().putBoolean(MAIN_SHOW_INCOME_CHART, show).apply()

    var isMainShowBudgetSummary: Boolean
        get() = preferences.getBoolean(MAIN_SHOW_BUDGET_SUMMARY, true)
        set(b) = preferences.edit().putBoolean(MAIN_SHOW_BUDGET_SUMMARY, b).apply()

    var mainBlockOrder: String
        get() = preferences.getString(MAIN_BLOCK_ORDER, UiConstants.DEFAULT_BLOCK_ORDER)
        set(blockOrder) = preferences.edit().putString(MAIN_BLOCK_ORDER, blockOrder).apply()

    var transactionFormatDateMode: Int
        get() = preferences.getInt(TRANSACTION_FORMAT_DATE_MODE, DateTimeFormatter.RELATIVE)
        set(mode) = preferences.edit().putInt(TRANSACTION_FORMAT_DATE_MODE, mode).apply()

    var accountSortType: Int
        get() = preferences.getInt(ACCOUNT_SORT_TYPE, Account.TYPE_NAME_SORT)
        set(sortType) = preferences.edit().putInt(ACCOUNT_SORT_TYPE, sortType).apply()

    var expenseSortType: Int
        get() = preferences.getInt(EXPENSE_SORT_TYPE, Category.NAME_SORT)
        set(sortType) = preferences.edit().putInt(EXPENSE_SORT_TYPE, sortType).apply()

    var incomeSortType: Int
        get() = preferences.getInt(INCOME_SORT_TYPE, Category.NAME_SORT)
        set(sortType) = preferences.edit().putInt(INCOME_SORT_TYPE, sortType).apply()

    var syncType: Int
        get() = preferences.getInt(SYNC_TYPE, -1)
        set(type) = preferences.edit().putInt(SYNC_TYPE, type).apply()

    fun clearSync() {
        preferences.edit().remove(SYNC_TYPE).remove(SYNC_TOKEN).remove(SYNC_DEVICE_ID).apply()
    }

    var syncToken: String?
        get() = preferences.getString(SYNC_TOKEN, null)
        set(token) = preferences.edit().putString(SYNC_TOKEN, token).apply()

    var password: String?
        get() = preferences.getString(PASSWORD, null)
        set(newPassword) = preferences.edit().putString(PASSWORD, newPassword).apply()

    fun clearPassword() {
        preferences.edit().remove(PASSWORD).apply()
    }

    fun clearAllInterfaceSettings() {
        preferences.edit().remove(AUTO_BACKUP).remove(BACKUP_PATH).remove(BACKUP_MAX_NUMBER).remove(PASSWORD).remove(SYNC_MANUAL_MODE).remove(SYNC_PERIOD).remove(TRANSACTION_FORMAT_DATE_MODE).remove(MAIN_BALANCE_TYPE).remove(MAIN_BLOCK_ORDER).remove(MAIN_SHOW_PATTERNS).remove(MAIN_SHOW_TOTAL_BALANCE).remove(MAIN_SHOW_BUDGET).remove(MAIN_SHOW_TRANSACTIONS).remove(MAIN_SHOW_ACCOUNTS).remove(MAIN_SHOW_BALANCE_CHART).remove(MAIN_SHOW_EXPENSE_CHART).remove(MAIN_SHOW_INCOME_CHART).remove(MAIN_SHOW_BUDGET_SUMMARY).remove(ACCOUNT_SORT_TYPE).remove(EXPENSE_SORT_TYPE).remove(INCOME_SORT_TYPE).remove(OPEN_AMOUNT_TRANSACTION).apply()
    }

    fun clearAllDataSettings() {
        preferences.edit().remove(MAIN_CURRENCY_CODE).remove(PERIOD).remove(LAST_ACCOUNT).remove(LAST_CATEGORY).remove(SYNC_TYPE).remove(SYNC_TOKEN).remove(SYNC_DEVICE_ID).apply()
    }

    companion object {

        private val PREF_NAME = "m_app"

        // Internal
        private val FIRST_START = "firstStart"
        private val LAST_DAILY_WORK = "lastDailyWork"
        private val SYNC_LAST_TRY = "syncLastTry"

        // Data settings
        private val MAIN_CURRENCY_CODE = "mainCurrencyCode"
        private val PERIOD = "period"
        private val LAST_ACCOUNT = "lastAccount"
        private val LAST_CATEGORY = "lastCategory"
        private val SYNC_TYPE = "syncType"
        private val SYNC_TOKEN = "syncToken"
        private val SYNC_DEVICE_ID = "syncDeviceId"

        // Interface settings
        private val AUTO_BACKUP = "autoBackup"
        private val BACKUP_PATH = "backupPath"
        private val BACKUP_MAX_NUMBER = "backupMaxNumber"
        private val PASSWORD = "longTPart"
        private val SYNC_MANUAL_MODE = "syncManualMode"
        private val SYNC_PERIOD = "syncPeriod"
        private val TRANSACTION_FORMAT_DATE_MODE = "transactionsFormatDateMode"

        private val MAIN_BALANCE_TYPE = "mainBalanceType"
        private val MAIN_BLOCK_ORDER = "mainBlockOrder"
        private val MAIN_SHOW_PATTERNS = "mainShowPatterns"
        private val MAIN_SHOW_TOTAL_BALANCE = "mainShowTotalBalance"
        private val MAIN_SHOW_BUDGET = "mainShowBudget"
        private val MAIN_SHOW_TRANSACTIONS = "mainShowTransactions"
        private val MAIN_SHOW_ACCOUNTS = "mainShowAccounts"
        private val MAIN_SHOW_BALANCE_CHART = "mainShowBalanceChart"
        private val MAIN_SHOW_EXPENSE_CHART = "mainShowExpenseChart"
        private val MAIN_SHOW_INCOME_CHART = "mainShowIncomeChart"
        private val MAIN_SHOW_BUDGET_SUMMARY = "mainShowBudgetSummary"

        private val ACCOUNT_SORT_TYPE = "accountSortType"
        private val EXPENSE_SORT_TYPE = "expenseSortType"
        private val INCOME_SORT_TYPE = "incomeSortType"

        private val OPEN_AMOUNT_TRANSACTION = "openAmountTransaction"
    }
}
