package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.DaoSession
import com.cactusteam.money.data.dao.ISyncObject
import com.cactusteam.money.sync.dropbox.DropboxClient
import com.cactusteam.money.sync.dropbox.DropboxConstants

/**
 * @author vpotapenko
 */
abstract class SyncInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun loadSyncLogInternal(): String {
        val syncManager = getApplication().syncManager
        val lines = syncManager.loadLog()
        return lines.joinToString("<br>")
    }

    fun disconnectSyncInternal() {
        val appPreferences = getApplication().appPreferences
        appPreferences.clearSync()

        clearTrash()
        clearSyncLog()

        val daoSession = dataManager.daoSession
        daoSession.runInTx({
            clearSyncObjects(daoSession.accountDao.loadAll(), daoSession)
            clearSyncObjects(daoSession.categoryDao.loadAll(), daoSession)
            clearSyncObjects(daoSession.subcategoryDao.loadAll(), daoSession)
            clearSyncObjects(daoSession.debtDao.loadAll(), daoSession)
            clearSyncObjects(daoSession.transactionDao.loadAll(), daoSession)
            clearSyncObjects(daoSession.transactionPatternDao.loadAll(), daoSession)
            clearSyncObjects(daoSession.budgetPlanDao.loadAll(), daoSession)
        })
    }

    fun startCheckSyncInternal() {
        val syncManager = getApplication().syncManager
        syncManager.prepareCheck(dataManager)
    }

    fun checkSyncInternal(): Int {
        val syncManager = getApplication().syncManager
        val result = syncManager.executeCheckJob()
        if (result != null && result) {
            return SyncService.NEED_SYNC
        } else if (syncManager.hasNextCheckJob()) {
            return SyncService.HAS_NEXT
        } else {
            return SyncService.CHECK_FINISHED
        }
    }

    fun checkDropboxRepositoryInternal(accessToken: String): Boolean {
        return DropboxClient(accessToken).exist(DropboxConstants.SYNC_PATH)
    }

    private fun clearSyncLog() {
        val daoSession = dataManager.daoSession
        daoSession.syncLogDao.deleteAll()
    }

    private fun clearSyncObjects(list: List<ISyncObject>, daoSession: DaoSession) {
        for (obj in list) {
            obj.globalId = null
            obj.synced = null
            daoSession.update(obj)
        }
    }

    private fun clearTrash() {
        val daoSession = dataManager.daoSession
        daoSession.trashDao.deleteAll()
    }
}