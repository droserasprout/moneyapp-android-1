package com.cactusteam.money.data.service

import android.util.Pair
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.model.AccountPeriodData
import com.cactusteam.money.data.AccountTotalLoader
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.model.Totals
import com.cactusteam.money.data.dao.*
import java.util.*

/**
 * @author vpotapenko
 */
abstract class AccountInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun updateAccountsOrderInternal(orders: Map<Long, Int>) {
        val daoSession = dataManager.daoSession
        daoSession.runInTx({
            val queryBuilder = daoSession.accountDao.queryBuilder()
            for (account in queryBuilder.list()) {
                val order = orders[account.id]
                if (order != null) {
                    account.customOrder = order
                    daoSession.update(account)
                }
            }
        })
    }

    fun getTotalsInternal(date: Date? = null): Totals {
        val totals = Totals()
        val accounts = dataManager.daoSession.accountDao.loadAll()
        for (account in accounts) {
            if (account.skipInBalance) continue // skip not in balance

            val total = getAccountTotalInternal(account.id, date)
            if (total == 0.0) continue

            totals.putAmount(account.currencyCode, total)
        }
        val mainCurrencyCode = getApplication().appPreferences.mainCurrencyCode
        totals.prepare(mainCurrencyCode)

        for (currencyCode in totals.currencyCodes) {
            var amount = totals.map[currencyCode]
            if (currencyCode != mainCurrencyCode) {
                val rate = dataManager.currencyService.getRateInternal(currencyCode, mainCurrencyCode)
                amount = rate?.convertTo(amount!!, mainCurrencyCode) ?: amount
            }

            totals.total += amount!!
        }

        return totals
    }

    fun getAccountsInternal(includeDeleted: Boolean = false, includeBalance: Boolean = false, excludeHidden: Boolean = false): List<Account> {
        val queryBuilder = dataManager.daoSession.accountDao.queryBuilder()

        val accountSortType = MoneyApp.instance.appPreferences.accountSortType
        when (accountSortType) {
            Account.TYPE_NAME_SORT -> queryBuilder.orderAsc(AccountDao.Properties.Type, AccountDao.Properties.Name)
            Account.NAME_SORT -> queryBuilder.orderAsc(AccountDao.Properties.Name)
            Account.CUSTOM_SORT -> queryBuilder.orderAsc(AccountDao.Properties.CustomOrder)
        }

        if (!includeDeleted) queryBuilder.where(AccountDao.Properties.Deleted.eq(false))
        if (excludeHidden) queryBuilder.where(AccountDao.Properties.SkipInBalance.eq(false))

        val list = queryBuilder.list()
        if (includeBalance) {
            val mainCurrencyCode = getApplication().appPreferences.mainCurrencyCode
            for (account in list) {
                account.balance = getAccountTotalInternal(account.id)
                if (account.currencyCode != mainCurrencyCode) {
                    val rate = dataManager.currencyService.getRateInternal(account.currencyCode, mainCurrencyCode)
                    account.balanceInMain = rate?.convertTo(account.balance ?: 0.0, mainCurrencyCode) ?: account.balance
                }
            }
        }

        return list
    }

    fun changeAccountBalanceInternal(accountId: Long, newAmount: Double) {
        val currentAmount = getAccountTotalInternal(accountId)
        var delta = newAmount - currentAmount
        if (delta == 0.0) return

        val type: Int
        if (delta > 0) {
            type = Transaction.INCOME
        } else {
            type = Transaction.EXPENSE
            delta = -delta
        }
        val categoryId = findChangeBalanceCategory(if (type == Transaction.INCOME) Category.INCOME else Category.EXPENSE)

        dataManager.transactionService
                .newTransactionBuilder()
                .putDate(Date())
                .putType(type)
                .putAmount(delta)
                .putSourceAccountId(accountId)
                .putCategoryId(categoryId)
                .putComment(getApplication().getString(R.string.fix_balance_comment))
                .createInternal()
    }

    fun getAccountTotalInternal(accountId: Long, date: Date? = null): Double {
        val daoSession = dataManager.daoSession

        val loader = AccountTotalLoader(accountId, daoSession)
        loader.load(null, date ?: Date())

        return loader.total
    }

    fun getAccountPeriodDataInternal(accountId: Long, onlyCurrent: Boolean): List<AccountPeriodData> {
        val period = getApplication().period
        val datePair = period.current

        val result = ArrayList<AccountPeriodData>()

        val accountTotalLoader = AccountTotalLoader(accountId, dataManager.daoSession)
        val current = createPeriodData(datePair, accountTotalLoader)

        var previousDates = period.getPrevious(datePair)
        val previous = createPeriodData(previousDates, accountTotalLoader)

        previousDates = period.getPrevious(previousDates)
        val allBefore = createPeriodData(Pair<Date, Date>(null, previousDates.second), accountTotalLoader)

        val account = dataManager.daoSession.accountDao.load(accountId)
        val currencyCode = account.currencyCode

        previous.initial = allBefore.total
        previous.currencyCode = currencyCode

        current.initial = previous.total
        current.currencyCode = currencyCode

        result.add(current)
        if (!onlyCurrent) result.add(previous)

        return result
    }

    fun restoreAccountInternal(accountId: Long) {
        val daoSession = dataManager.daoSession
        val account = daoSession.accountDao.load(accountId)

        account.deleted = false
        if (account.globalId != null) account.synced = false

        daoSession.update(account)
    }

    fun getAccountInternal(accountId: Long): Account {
        val daoSession = dataManager.daoSession
        return daoSession.accountDao.load(accountId)
    }

    fun deleteAccountInternal(accountId: Long) {
        val daoSession = dataManager.daoSession
        val account = daoSession.accountDao.load(accountId)

        if (hasDependencies(account)) {
            account.deleted = true
            if (account.globalId != null) account.synced = false

            daoSession.update(account)
        } else {
            daoSession.delete(account)
        }
    }

    private fun hasDependencies(account: Account?): Boolean {
        if (account!!.globalId != null) return true

        val daoSession = dataManager.daoSession
        val transactions = daoSession.transactionDao.queryBuilder()
                .whereOr(
                        TransactionDao.Properties.SourceAccountId.eq(account.id),
                        TransactionDao.Properties.DestAccountId.eq(account.id)).limit(1).list()
        if (!transactions.isEmpty()) return true

        val patterns = daoSession.transactionPatternDao.queryBuilder()
                .whereOr(
                        TransactionPatternDao.Properties.SourceAccountId.eq(account.id),
                        TransactionPatternDao.Properties.DestAccountId.eq(account.id)).limit(1).list()
        if (!patterns.isEmpty()) return true

        val debts = daoSession.debtDao.queryBuilder().where(
                DebtDao.Properties.AccountId.eq(account.id)).limit(1).list()
        return !debts.isEmpty()
    }

    fun updateAccountInternal(
            accountId: Long,
            name: String,
            type: Int,
            currencyCode: String,
            color: String?,
            skipInBalance: Boolean,
            globalId: Long? = null,
            synced: Boolean? = null
    ): Account {
        val daoSession = dataManager.daoSession
        val account = daoSession.accountDao.load(accountId)
        account.name = name
        account.type = type
        account.currencyCode = currencyCode
        account.color = color
        account.skipInBalance = skipInBalance

        if (globalId != null) {
            account.globalId = globalId
            account.synced = synced
        } else if (account.globalId != null) {
            account.synced = false
        }

        daoSession.update(account)
        return account
    }

    fun createAccountInternal(
            name: String,
            type: Int,
            currencyCode: String,
            color: String?,
            skipInBalance: Boolean,
            globalId: Long? = null,
            synced: Boolean? = null
    ): Account {
        val account = Account()
        account.name = name
        account.type = type
        account.currencyCode = currencyCode
        account.color = color
        account.skipInBalance = skipInBalance
        if (globalId != null) {
            account.globalId = globalId
            account.synced = synced
        }
        dataManager.daoSession.insert(account)

        return account
    }

    protected fun updateInitialBalance(account: Account, initialBalance: Double) {
        if (initialBalance != 0.0) {
            val current = getApplication().appPreferences.period.current
            val cal = Calendar.getInstance()
            cal.time = current.first
            cal.add(Calendar.HOUR, -1)

            val transactionBuilder = dataManager.transactionService.newTransactionBuilder()
            val type = if (initialBalance > 0) {
                Transaction.INCOME
            } else {
                Transaction.EXPENSE
            }
            val categoryId = findChangeBalanceCategory(if (type == Transaction.INCOME) Category.INCOME else Category.EXPENSE)

            transactionBuilder
                    .putType(type)
                    .putAmount(Math.abs(initialBalance))
                    .putCategoryId(categoryId)
                    .putSourceAccountId(account.id)
                    .putDate(cal.time)
                    .putComment(getApplication().getString(R.string.start_balance))
                    .createInternal()
        }
    }

    private fun findChangeBalanceCategory(type: Int): Long {
        val name = getApplication().getString(R.string.fix_balance_category)
        return findCategoryByName(type, name, "basic_026.png")
    }

    protected fun convertTransactionAmountToNewCurrency(accountId: Long, rate: CurrencyRate?, currencyCode: String) {
        val transactions = dataManager.transactionService
                .newListTransactionsBuilder().putAccountId(accountId).listInternal()

        val daoSession = dataManager.daoSession
        for (t in transactions) {
            if (t.type != Transaction.TRANSFER) {
                t.amount = rate!!.convertTo(t.amount, currencyCode)
                if (t.globalId != null) t.synced = false

                daoSession.update(t)
            } else {
                if (t.sourceAccountId == accountId) {
                    t.amount = rate!!.convertTo(t.amount, currencyCode)
                } else {
                    t.destAmount = rate!!.convertTo(t.destAmount!!, currencyCode)
                }

                val sourceCurrencyCode = t.sourceAccount.currencyCode
                val destCurrencyCode = t.destAccount.currencyCode
                if (sourceCurrencyCode == destCurrencyCode) {
                    t.destAmount = t.amount
                }
                if (t.globalId != null) t.synced = false
                daoSession.update(t)
            }
        }
    }

    private fun createPeriodData(datePair: Pair<Date, Date>, accountTotalLoader: AccountTotalLoader): AccountPeriodData {
        accountTotalLoader.load(datePair.first, datePair.second)

        val data = AccountPeriodData()
        data.from = datePair.first
        data.to = datePair.second
        data.income = accountTotalLoader.income
        data.expense = accountTotalLoader.expense
        data.transfer = accountTotalLoader.transfer

        return data
    }
}