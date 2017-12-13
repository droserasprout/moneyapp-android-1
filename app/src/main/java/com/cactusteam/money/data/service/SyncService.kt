package com.cactusteam.money.data.service

import com.cactusteam.money.R
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.Note
import rx.Observable
import rx.lang.kotlin.observable

/**
 * @author vpotapenko
 */
class SyncService(dataManager: DataManager) : SyncInternalService(dataManager) {

    fun loadSyncLog(): Observable<String> {
        val o = observable<String> { s ->
            try {
                s.onNext(loadSyncLogInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun disconnectSync(): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                disconnectSyncInternal()
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun syncAll(): Observable<Pair<Int, Int>> {
        val o = observable<Pair<Int, Int>> { s ->
            try {
                dataManager.noteService.deleteNoteByRefInternal(Note.SYNC_ERROR_REF)

                val syncManager = getApplication().syncManager

                syncManager.prepareSync(dataManager)

                val max = syncManager.jobsCount
                var progress = 0

                s.onNext(Pair(progress, max))
                while (syncManager.hasNextJob()) {
                    syncManager.executeNextJob()
                    s.onNext(Pair(++progress, max))
                }
                syncManager.saveLog()

                dataManager.fireBalanceChanged()
                s.onCompleted()
            } catch (e: Exception) {
                dataManager.noteService.createSyncErrorNoteInternal(getApplication().getString(R.string.sync_error, e.message))

                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun startCheckSync(): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                startCheckSyncInternal()
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun checkSync(): Observable<Int> {
        val o = observable<Int> { s ->
            try {
                s.onNext(checkSyncInternal())
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun joinToSyncRepository(type: Int, accessToken: String): Observable<Pair<Int, Int>> {
        val o = observable<Pair<Int, Int>> { s ->
            val appPreferences = getApplication().appPreferences
            try {
                appPreferences.syncType = type
                appPreferences.syncToken = accessToken

                val db = dataManager.createRestoreDatabase()
                val syncDataManager = DataManager(db)

                val syncManager = getApplication().syncManager
                syncManager.prepareSync(syncDataManager)
                val max = syncManager.jobsCount
                var progress = 0

                s.onNext(Pair(progress, max))
                while (syncManager.hasNextJob()) {
                    syncManager.executeNextJob()
                    s.onNext(Pair(++progress, max))
                }
                syncManager.saveLog()

                syncDataManager.closeDatabase()
                dataManager.replaceDataByRestored()

                s.onCompleted()
            } catch (e: Exception) {
                appPreferences.clearSync()

                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun createSyncRepository(type: Int, accessToken: String): Observable<Pair<Int, Int>> {
        val o = observable<Pair<Int, Int>> { s ->
            try {
                val appPreferences = getApplication().appPreferences
                appPreferences.syncType = type
                appPreferences.syncToken = accessToken

                val syncManager = getApplication().syncManager

                syncManager.initializeRepository()
                syncManager.prepareSync(dataManager)
                val max = syncManager.jobsCount
                var progress = 0

                s.onNext(Pair(progress, max))
                while (syncManager.hasNextJob()) {
                    syncManager.executeNextJob()
                    s.onNext(Pair(++progress, max))
                }
                syncManager.saveLog()

                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun checkDropboxRepository(token: String): Observable<Boolean> {
        val o = observable<Boolean> { s ->
            try {
                s.onNext(checkDropboxRepositoryInternal(token))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    companion object {
        val NEED_SYNC = 1
        val HAS_NEXT = 2
        val CHECK_FINISHED = 3
    }
}