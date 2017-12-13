package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.TransactionPattern
import com.cactusteam.money.data.service.builder.PatternBuilder
import rx.Observable
import rx.lang.kotlin.observable

/**
 * @author vpotapenko
 */
class PatternService(dataManager: DataManager) : PatternInternalService(dataManager) {

    fun getPatterns(): Observable<List<TransactionPattern>> {
        val o = observable<List<TransactionPattern>> { s ->
            try {
                s.onNext(getPatternsInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getPattern(id: Long): Observable<TransactionPattern> {
        val o = observable<TransactionPattern> { s ->
            try {
                s.onNext(getPatternInternal(id))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun deletePattern(id: Long): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deletePatternInternal(id)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun newPatternBuilder(): PatternBuilder {
        return PatternBuilder(this)
    }

    fun createPatternFromTransaction(transactionId: Long, name: String): Observable<TransactionPattern> {
        val o = observable<TransactionPattern> { s ->
            try {
                s.onNext(createPatternFromTransactionInternal(transactionId, name))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun createPattern(builder: PatternBuilder): Observable<TransactionPattern> {
        val o = observable<TransactionPattern> { s ->
            try {
                s.onNext(createPatternInternal(builder))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updatePattern(builder: PatternBuilder): Observable<TransactionPattern> {
        val o = observable<TransactionPattern> { s ->
            try {
                s.onNext(updatePatternInternal(builder))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}