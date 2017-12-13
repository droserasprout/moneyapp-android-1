package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import rx.Observable
import rx.lang.kotlin.observable
import java.io.File

/**
 * @author vpotapenko
 */
class BackupService(dataManager: DataManager) : BackupInternalService(dataManager) {

    fun getLastBackups(maxCount: Int = 5): Observable<List<File>> {
        val o = observable<List<File>> { s ->
            try {
                s.onNext(getLastBackupsInternal(maxCount))
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun createBackup(file: File): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                createBackupInternal(file)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }

    fun restoreFromBackup(sourceFile: File): Observable<Unit> {
        val o = observable<Unit> { s ->
            try {
                restoreFromBackupInternal(sourceFile)
                s.onCompleted()
            } catch (e: Exception) {
                e.printStackTrace()
                s.onError(e)
            }
        }
        return prepareIOObservable(o)
    }
}