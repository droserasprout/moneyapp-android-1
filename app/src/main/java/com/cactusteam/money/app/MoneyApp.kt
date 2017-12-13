package com.cactusteam.money.app

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex

import com.cactusteam.money.R
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.MoneyAppPreferences
import com.cactusteam.money.data.currency.CurrencyManager
import com.cactusteam.money.data.period.Period
import com.cactusteam.money.scheduler.MoneyAppScheduler
import com.cactusteam.money.sync.SyncManager
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker

/**
 * @author vpotapenko
 */
class MoneyApp : Application() {

    val appPreferences: MoneyAppPreferences by lazy { MoneyAppPreferences(this) }
    val syncManager: SyncManager by lazy { SyncManager(this, appPreferences) }
    val currencyManager: CurrencyManager by lazy { CurrencyManager(this) }
    val scheduler: MoneyAppScheduler by lazy { MoneyAppScheduler(this) }

    val dataManager: DataManager get() {
        if (_dataManager == null) {
            _dataManager = DataManager(null)
        }
        return _dataManager!!
    }
    val period: Period get() {
        if (_period == null) {
            _period = appPreferences.period
        }
        return _period!!
    }

    val tracker: Tracker by lazy {
        val analytics = GoogleAnalytics.getInstance(this)
        val tracker = analytics.newTracker(R.xml.global_tracker)
        tracker.enableExceptionReporting(true)
        tracker.enableAdvertisingIdCollection(true)
        tracker.enableAutoActivityTracking(true)
        tracker
    }

    private var _period: Period? = null
    private var _dataManager: DataManager? = null

    fun resetPeriod() {
        _period = null
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this

        tracker // initialize GA
    }

    override fun onTerminate() {
        _dataManager = null
        _instance = null
        super.onTerminate()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {

        val instance: MoneyApp get() = _instance!!

        private var _instance: MoneyApp? = null
    }
}
