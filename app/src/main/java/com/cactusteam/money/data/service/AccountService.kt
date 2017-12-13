package com.cactusteam.money.data.service

import com.cactusteam.money.data.model.AccountPeriodData
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.model.Totals
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.CurrencyRate
import rx.Observable
import rx.lang.kotlin.observable
import java.util.*

/**
 * @author vpotapenko
 */
class AccountService(dataManager: DataManager) : AccountInternalService(dataManager) {

    fun updateAccountsOrder(orders: Map<Long, Int>): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                updateAccountsOrderInternal(orders)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getTotals(date: Date? = null): Observable<Totals> {
        val o = observable<Totals> { s ->
            try {
                s.onNext(getTotalsInternal(date))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getAccounts(includeDeleted: Boolean = false, includeBalance: Boolean = false, excludeHidden: Boolean = false): Observable<List<Account>> {
        val o = observable<List<Account>> { s ->
            try {
                s.onNext(getAccountsInternal(includeDeleted, includeBalance, excludeHidden))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun changeAccountBalance(accountId: Long, newAmount: Double): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                changeAccountBalanceInternal(accountId, newAmount)
                dataManager.fireBalanceChanged()

                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getAccountPeriodData(accountId: Long, onlyCurrent: Boolean): Observable<List<AccountPeriodData>> {
        val o = observable<List<AccountPeriodData>> { s ->
            try {
                s.onNext(getAccountPeriodDataInternal(accountId, onlyCurrent))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun restoreAccount(accountId: Long): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                restoreAccountInternal(accountId)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getAccount(accountId: Long): Observable<Account> {
        val o = observable<Account> { s ->
            try {
                s.onNext(getAccountInternal(accountId))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun deleteAccount(accountId: Long): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteAccountInternal(accountId)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updateAccount(
            accountId: Long,
            name: String,
            type: Int,
            currencyCode: String,
            color: String?,
            skipInBalance: Boolean,
            rate: CurrencyRate?
    ): Observable<Account> {
        val o = observable<Account> { s ->
            try {
                val oldCurrencyCode = getAccountInternal(accountId).currencyCode
                val account = updateAccountInternal(accountId, name, type, currencyCode, color, skipInBalance)
                if (oldCurrencyCode != account.currencyCode) {
                    dataManager.daoSession.runInTx { convertTransactionAmountToNewCurrency(accountId, rate, currencyCode) }
                }

                s.onNext(account)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun createAccount(
            name: String,
            type: Int,
            currencyCode: String,
            color: String?,
            skipInBalance: Boolean,
            initialBalance: Double
    ): Observable<Account> {
        val o = observable<Account> { s ->
            try {
                val account = createAccountInternal(name,
                        type,
                        currencyCode,
                        color,
                        skipInBalance)
                updateInitialBalance(account, initialBalance)
                dataManager.fireBalanceChanged()

                s.onNext(account)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}