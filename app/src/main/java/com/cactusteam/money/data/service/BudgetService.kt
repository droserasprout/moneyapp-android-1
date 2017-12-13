package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.data.service.builder.BudgetBuilder
import rx.Observable
import rx.lang.kotlin.observable

/**
 * @author vpotapenko
 */
class BudgetService(dataManager: DataManager) : BudgetInternalService(dataManager) {

    fun getCurrentBudgets(): Observable<List<BudgetPlan>> {
        val o = observable<List<BudgetPlan>> { s ->
            try {
                s.onNext(getCurrentBudgetsInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getBudgets(fillExpense: Boolean = false): Observable<List<BudgetPlan>> {
        val o = observable<List<BudgetPlan>> { s ->
            try {
                s.onNext(getBudgetsInternal(fillExpense))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun newBudgetBuilder(): BudgetBuilder {
        return BudgetBuilder(this)
    }

    fun updateBudgetLimit(id: Long, limit: Double): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                updateBudgetLimitInternal(id, limit)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getBudgetAmount(id: Long): Observable<Double> {
        val o = observable<Double> { s ->
            try {
                s.onNext(getBudgetAmountInternal(id))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getBudget(id: Long): Observable<BudgetPlan> {
        val o = observable<BudgetPlan> { s ->
            try {
                s.onNext(getBudgetInternal(id))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun deleteBudget(id: Long): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteBudgetInternal(id)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun createBudget(builder: BudgetBuilder): Observable<BudgetPlan> {
        val o = observable<BudgetPlan> { s ->
            try {
                s.onNext(createBudgetInternal(builder))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updateBudget(builder: BudgetBuilder): Observable<BudgetPlan> {
        val o = observable<BudgetPlan> { s ->
            try {
                s.onNext(updateBudgetInternal(builder))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}