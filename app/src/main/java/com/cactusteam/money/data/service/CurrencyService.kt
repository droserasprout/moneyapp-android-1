package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.CurrencyRate
import rx.Observable
import rx.lang.kotlin.observable

/**
 * @author vpotapenko
 */
class CurrencyService(dataManager: DataManager) : CurrencyInternalService(dataManager) {

    fun getRates(): Observable<List<CurrencyRate>> {
        val o = observable<List<CurrencyRate>> { s ->
            try {
                s.onNext(getRatesInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun updateRate(sourceCode: String, destCode: String, rate: Double): Observable<CurrencyRate> {
        val o = observable<CurrencyRate> { s ->
            try {
                s.onNext(updateRateInternal(sourceCode, destCode, rate))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun deleteRate(rateId: Long): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                deleteRateInternal(rateId)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getRate(currencyCode1: String, currencyCode2: String): Observable<CurrencyRate?> {
        val o = observable<CurrencyRate?> { s ->
            try {
                s.onNext(getRateInternal(currencyCode1, currencyCode2))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}