package com.cactusteam.money.data.service

import android.graphics.Bitmap
import android.net.Uri
import com.cactusteam.money.data.model.Contact
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.model.Icons
import com.cactusteam.money.data.dao.CurrencyRate
import com.cactusteam.money.data.io.ExporterFactory
import com.cactusteam.money.data.io.ImportSchema
import com.cactusteam.money.data.io.ImporterFactory
import com.cactusteam.money.data.period.Period
import rx.Observable
import rx.lang.kotlin.observable
import java.io.File
import java.util.*

/**
 * @author vpotapenko
 */
class SystemService(dataManager: DataManager) : SystemInternalService(dataManager) {

    fun resetInterfaceSettings(): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                resetInterfaceSettingsInternal()
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun importTransactions(sourceFile: File, schema: ImportSchema): Observable<Any> {
        val o = observable<Any> { s ->
            try {
                val importer = ImporterFactory.create(schema.format)
                val r = importer.doImport(schema, sourceFile,
                        { progress, max -> s.onNext(Pair(progress, max)) },
                        dataManager)
                dataManager.fireBalanceChanged()

                s.onNext(r)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o.onBackpressureLatest())
    }

    fun analyseImportFile(sourceFile: File, format: Int): Observable<Any> {
        val o = observable<Any> { s ->
            try {
                val importer = ImporterFactory.create(format)
                val schema = importer.analyse(sourceFile, { progress, max ->
                    s.onNext(Pair(progress, max))
                }, dataManager)
                schema.format = format

                s.onNext(schema)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o.onBackpressureLatest())
    }

    fun initializeModel(period: Period, currencyCode: String): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                initializeModelInternal(period, currencyCode)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getIconPaths(): Observable<Icons> {
        val o = observable<Icons> { s ->
            try {
                s.onNext(getIconPathsInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun prepareMoneyAppContext(): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                prepareMoneyAppContextInternal()
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun exportTransactions(from: Date, to: Date,
                           type: Int = ExporterFactory.XLS_TYPE,
                           expense: Boolean = false,
                           income: Boolean = false,
                           transfer: Boolean = false): Observable<String> {
        val o = observable<String> { s ->
            try {
                s.onNext(exportTransactionsInternal(from, to, type, expense, income, transfer))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun changeFinancialOptions(currencyCode: String, period: Period, rate: CurrencyRate?): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                changeFinancialOptionsInternal(currencyCode, period, rate)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun resetAllData(): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                resetAllDataInternal()
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getIconImage(name: String, bigImage: Boolean = false): Observable<Bitmap?> {
        val o = observable<Bitmap?> { s ->
            try {
                s.onNext(getIconImageInternal(name, bigImage))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getContactImage(contactId: Long): Observable<Bitmap?> {
        val o = observable<Bitmap?> { s ->
            try {
                s.onNext(getContactImageInternal(contactId))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun getContact(contactUri: Uri): Observable<Contact?> {
        val o = observable<Contact?> { s ->
            try {
                s.onNext(getContactInternal(contactUri))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}