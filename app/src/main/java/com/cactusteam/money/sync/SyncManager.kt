package com.cactusteam.money.sync

import android.content.Context
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.MoneyAppPreferences
import com.cactusteam.money.sync.changes.IChangesStorage
import com.cactusteam.money.sync.dropbox.DropboxClient
import com.cactusteam.money.sync.dropbox.DropboxLogStorage
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.*

/**
 * @author vpotapenko
 */
class SyncManager(private val context: Context, private val appPreferences: MoneyAppPreferences) {
    val eventsController: SyncEventsController

    private var currentJobs: MutableList<SyncJob<Any>>? = null
    private var currentCheckJobs: MutableList<SyncJob<Boolean>>? = null
    private var currentLogger: SyncLogger? = null

    init {
        this.eventsController = SyncEventsController()
    }

    val isAutoSync: Boolean
        get() = isSyncConnected && !appPreferences.isManualSyncMode

    val isSyncConnected: Boolean
        get() = syncType >= 0

    val syncType: Int
        get() = appPreferences.syncType

    fun initializeRepository() {
        val syncLogStorage = createLogStorage()
        syncLogStorage.initialize()
    }

    fun prepareCheck(dataManager: DataManager) {
        val syncLogStorage = createLogStorage()
        val proxyDatabase = SyncProxyDatabase(dataManager)

        currentLogger = createLogger()

        val syncJobsExecutor = SyncJobsExecutor(proxyDatabase, syncLogStorage, currentLogger, deviceId)
        currentCheckJobs = syncJobsExecutor.createCheckJobs()
    }

    private val deviceId: String
        get() {
            var deviceId: String? = appPreferences.syncDeviceId
            if (!deviceId.isNullOrBlank()) return deviceId!!

            deviceId = UUID.randomUUID().toString()
            appPreferences.syncDeviceId = deviceId

            return deviceId
        }

    fun hasNextCheckJob(): Boolean {
        return currentCheckJobs != null && !currentCheckJobs!!.isEmpty()
    }

    fun executeCheckJob(): Boolean? {
        if (currentCheckJobs == null || currentCheckJobs!!.isEmpty()) return false

        val job = currentCheckJobs!!.removeAt(0)
        return job.execute()
    }

    fun prepareSync(dataManager: DataManager) {
        val syncLogStorage = createLogStorage()
        val proxyDatabase = SyncProxyDatabase(dataManager)

        currentLogger = createLogger()

        val syncJobsExecutor = SyncJobsExecutor(proxyDatabase, syncLogStorage, currentLogger, deviceId)
        currentJobs = syncJobsExecutor.createSyncJobs()
    }

    private fun createLogger(): SyncLogger {
        return SyncLogger(emptyList<String>())
    }

    fun loadLog(): List<String> {
        val logFile = File(context.filesDir, SYNC_LOG_FILENAME)
        if (logFile.exists()) {
            try {
                return FileUtils.readLines(logFile)
            } catch (e: IOException) {
                e.printStackTrace()
                return emptyList()
            }

        } else {
            return emptyList()
        }
    }

    fun saveLog() {
        if (currentLogger != null) {
            val lines = currentLogger!!.getLines()

            val logFile = File(context.filesDir, SYNC_LOG_FILENAME)
            try {
                FileUtils.writeLines(logFile, lines)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    fun hasNextJob(): Boolean {
        return currentJobs != null && !currentJobs!!.isEmpty()
    }

    fun executeNextJob() {
        if (currentJobs == null || currentJobs!!.isEmpty()) return

        val job = currentJobs!!.removeAt(0)
        job.execute()
        if (job.error) {
            currentLogger!!.print("<strong>ERROR: " + job.errorMessage + "</strong>")
            currentJobs!!.clear()
            throw RuntimeException(job.errorMessage)
        }
    }

    private fun createLogStorage(): IChangesStorage {
        when (appPreferences.syncType) {
            DROPBOX_TYPE -> return createDropboxLogStorage()
            else -> throw RuntimeException("Not implemented")
        }
    }

    private fun createDropboxLogStorage(): IChangesStorage {
        val client = DropboxClient(appPreferences.syncToken!!)
        return DropboxLogStorage(client)
    }

    val jobsCount: Int
        get() = currentJobs!!.size

    fun resetSyncLog() {
        val logFile = File(context.filesDir, SYNC_LOG_FILENAME)
        FileUtils.deleteQuietly(logFile)
    }

    companion object {

        val DROPBOX_TYPE = 0

        val SYNC_LOG_FILENAME = "sync.log"
    }
}
