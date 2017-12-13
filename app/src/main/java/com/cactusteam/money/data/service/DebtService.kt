package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Debt
import com.cactusteam.money.data.dao.DebtNote
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.service.builder.DebtBuilder
import rx.Observable
import rx.lang.kotlin.observable
import java.util.*

/**
 * @author vpotapenko
 */
class DebtService(dataManager: DataManager) : DebtInternalService(dataManager) {

    fun deleteDebtNote(debtNoteId: Long): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteDebtNoteInternal(debtNoteId, true)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updateDebtNote(debtNoteId: Long, text: String): Observable<DebtNote> {
        val o = observable<DebtNote> { s ->
            try {
                s.onNext(updateDebtNoteInternal(debtNoteId, text))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun createDebtNote(debtId: Long, text: String): Observable<DebtNote> {
        val o = observable<DebtNote> { s ->
            try {
                s.onNext(createDebtNoteInternal(debtId, text, true))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getDebts(): Observable<List<Debt>> {
        val o = observable<List<Debt>> { s ->
            try {
                s.onNext(getDebtsInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updateDebtTime(debtId: Long, till: Date): Observable<Debt> {
        val o = observable<Debt> { s ->
            try {
                s.onNext(updateDebtTimeInternal(debtId, till))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun changeDebtAmount(debtId: Long, amount: Double): Observable<Debt> {
        val o = observable<Debt> { s ->
            try {
                val debt = changeDebtAmountInternal(debtId, amount)
                dataManager.fireBalanceChanged()

                s.onNext(debt)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun closeDebt(debtId: Long): Observable<Debt> {
        val o = observable<Debt> { s ->
            try {
                val debt = closeDebtInternal(debtId)
                dataManager.fireBalanceChanged()

                s.onNext(debt)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updateDebt(debtBuilder: DebtBuilder): Observable<Debt> {
        val o = observable<Debt> { s ->
            try {
                s.onNext(updateDebtInternal(debtBuilder))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun createDebt(debtBuilder: DebtBuilder, type: Int, amount: Double): Observable<Debt> {
        val o = observable<Debt> { s ->
            try {
                val result = findActiveDebtsByNameInternal(debtBuilder.name!!)
                val debt = if (result.size == 1) {
                    result[0]
                } else {
                    createDebtInternal(debtBuilder)
                }
                if (amount != 0.0) {
                    val categoryId = findDebtCategory(if (type == Transaction.INCOME) Category.INCOME else Category.EXPENSE)
                    dataManager.transactionService
                            .newTransactionBuilder()
                            .putType(type)
                            .putAmount(amount)
                            .putCategoryId(categoryId)
                            .putSourceAccountId(debtBuilder.accountId!!)
                            .putDate(Date())
                            .putTag(debt.name)
                            .putRef(String.format(Debt.DEBT_REF_PATTERN, debt.id))
                            .createInternal()
                    dataManager.fireBalanceChanged()
                }
                s.onNext(debt)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun newDebtBuilder(): DebtBuilder {
        return DebtBuilder(this)
    }

    fun deleteDebt(id: Long, removeTransactions: Boolean): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteDebtInternal(id, removeTransactions)
                s.onCompleted()
            } catch (e: Exception) {
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getDebt(id: Long): Observable<Debt> {
        val o = observable<Debt> { s ->
            try {
                s.onNext(getDebtInternal(id))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}