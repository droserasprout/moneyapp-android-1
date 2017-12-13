package com.cactusteam.money.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.text.format.DateUtils
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataManager
import com.google.android.gms.analytics.HitBuilders
import rx.subscriptions.CompositeSubscription

/**
 * @author vpotapenko
 */
class SyncService : Service() {

    private var syncManager: SyncManager? = null
    private var handler: Handler? = null

    private var working: Boolean = false

    private var compositeSubscription: CompositeSubscription? = null
    private val dataManager: DataManager
        get() = MoneyApp.instance.dataManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        compositeSubscription = CompositeSubscription()
        if (intent.action == START_SYNC_ACTION) {
            startSync(true)
            startWork()
        } else if (intent.action == STOP_SYNC_ACTION) {
            if (working) {
                stopSelf()
            }
        } else {
            startWork()
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        working = false
        compositeSubscription?.unsubscribe()

        super.onDestroy()
    }

    private fun startWork() {
        if (working) return

        syncManager = MoneyApp.instance.syncManager
        handler = Handler()

        working = true

        startDataChecking()
    }

    private fun startDataChecking() {
        if (syncManager!!.isAutoSync && isTimeToCheck) {
            val s = dataManager.syncService
                    .startCheckSync()
                    .subscribe(
                            {},
                            { e ->
                                sendErrorEvent(e.message)
                                postDataCheckingLater()
                            },
                            {
                                saveTryTime()
                                doDataChecking()
                            }
                    )
            compositeSubscription!!.add(s)
        } else {
            postDataCheckingLater()
        }
    }

    private fun saveTryTime() {
        val appPreferences = MoneyApp.instance.appPreferences
        appPreferences.syncLastTry = System.currentTimeMillis()
    }

    private val isTimeToCheck: Boolean
        get() {
            val appPreferences = MoneyApp.instance.appPreferences
            val lastTry = appPreferences.syncLastTry
            if (lastTry < 0) return true

            val syncPeriod: Long
            when (appPreferences.syncPeriod) {
                ONE_PER_TEN_MINUTE_PERIOD -> syncPeriod = 10 * DateUtils.MINUTE_IN_MILLIS
                ONE_PER_HOUR_PERIOD -> syncPeriod = DateUtils.HOUR_IN_MILLIS
                else -> syncPeriod = DateUtils.DAY_IN_MILLIS
            }
            val actualPeriod = System.currentTimeMillis() - lastTry
            return actualPeriod > syncPeriod
        }

    private fun doDataChecking() {
        val eventsController = syncManager!!.eventsController
        if (eventsController.isSyncing) {
            postDataCheckingLater()
        } else {
            val s = dataManager.syncService
                    .checkSync()
                    .subscribe(
                            { r ->
                                if (r == com.cactusteam.money.data.service.SyncService.NEED_SYNC) {
                                    startSync(false)
                                } else if (r == com.cactusteam.money.data.service.SyncService.HAS_NEXT) {
                                    doDataChecking()
                                } else if (r == com.cactusteam.money.data.service.SyncService.CHECK_FINISHED) {
                                    postDataCheckingLater()
                                }

                            },
                            { e ->
                                sendErrorEvent(e.message)
                                postDataCheckingLater()
                            }
                    )
            compositeSubscription!!.add(s)
        }
    }

    private fun startSync(force: Boolean) {
        val eventsController = syncManager!!.eventsController
        if (eventsController.isSyncing) {
            if (!force) postDataCheckingLater()
        } else {
            eventsController.setState(SyncEventsController.SYNCING)
            val s = dataManager.syncService
                    .syncAll()
                    .subscribe(
                            { p ->
                                eventsController.onProgressUpdated(p.first, p.second)
                            },
                            { e ->
                                eventsController.setState(SyncEventsController.IDLE)
                                sendErrorEvent(e.message)
                                postDataCheckingLater()
                            },
                            {
                                eventsController.setState(SyncEventsController.IDLE)
                                saveTryTime()
                                postDataCheckingLater()
                            }
                    )
            compositeSubscription!!.add(s)
        }
    }

    private fun postDataCheckingLater() {
        if (working) {
            handler!!.postDelayed({ startDataChecking() }, DELAY)
        }
    }

    private fun sendErrorEvent(description: String?) {
        val t = MoneyApp.instance.tracker

        t.setScreenName(javaClass.name)
        t.send(HitBuilders.EventBuilder().setCategory("error").setAction("Sync Service: ").setLabel(description).build())
    }

    companion object {

        val ONE_PER_DAY_PERIOD = 0
        val ONE_PER_HOUR_PERIOD = 1
        val ONE_PER_TEN_MINUTE_PERIOD = 2

        val DELAY = 5 * DateUtils.MINUTE_IN_MILLIS // 5 minutes
        val LOCK_LIVE = 15 * DateUtils.MINUTE_IN_MILLIS // 15 minutes

        private val START_SYNC_ACTION = "startSync"
        private val STOP_SYNC_ACTION = "stopSync"

        fun actionStart(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            context.startService(intent)
        }

        fun actionSyncImmediately(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            intent.action = START_SYNC_ACTION
            context.startService(intent)
        }

        fun actionStop(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            intent.action = STOP_SYNC_ACTION
            context.startService(intent)
        }
    }
}
