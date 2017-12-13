package com.cactusteam.money.data.service

import com.cactusteam.money.data.model.BalanceData
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.filter.ITransactionFilter
import com.cactusteam.money.data.report.CategoriesReportData
import com.cactusteam.money.data.report.TagsReportData
import rx.Observable
import rx.lang.kotlin.observable
import java.util.*

/**
 * @author vpotapenko
 */
class ReportService(dataManager: DataManager) : ReportInternalService(dataManager) {

    fun getCategoriesReportData(startTime: Date, endTime: Date, filter: ITransactionFilter?, type: Int): Observable<CategoriesReportData> {
        val o = observable<CategoriesReportData> { s ->
            try {
                s.onNext(getCategoriesReportDataInternal(startTime, endTime, filter, type))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getTagsReportData(startTime: Date, endTime: Date, filter: ITransactionFilter?, type: Int): Observable<TagsReportData> {
        val o = observable<TagsReportData> { s ->
            try {
                s.onNext(getTagsReportDataInternal(startTime, endTime, filter, type))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getBalanceData(filter: ITransactionFilter, full: Boolean): Observable<List<BalanceData>> {
        val o = observable<List<BalanceData>> { s ->
            try {
                s.onNext(getBalanceDataInternal(filter, full))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}