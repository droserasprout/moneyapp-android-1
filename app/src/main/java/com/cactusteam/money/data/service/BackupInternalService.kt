package com.cactusteam.money.data.service

import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.JsonConverter
import com.cactusteam.money.data.dao.*
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.*

/**
 * @author vpotapenko
 */
abstract class BackupInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun createAutoBackupInternal() {
        val dataDir = DataUtils.backupFolder
        var version = 0
        var backupFile = File(dataDir, DataUtils.getAutoBackupFileName(version))
        while (backupFile.exists()) {
            version++
            backupFile = File(dataDir, DataUtils.getAutoBackupFileName(version))
        }

        createBackupInternal(backupFile)

        val dataFolder = DataUtils.backupFolder
        val files = dataFolder.listFiles(FileFilter { path ->
            if (path.isDirectory) return@FileFilter false

            val name = path.name
            name.startsWith(DataConstants.BACKUP_FILENAME_PREFIX) && name.endsWith(DataConstants.AUTO_BACKUP_FILENAME_SUFFIX)
        }) ?: return

        val maxBackupNumber = getApplication().appPreferences.backupMaxNumber
        if (files.size <= maxBackupNumber) return

        files.sortBy(File::lastModified)
        val list = files.toMutableList()
        while (list.size > maxBackupNumber) {
            val file = list.removeAt(0)
            FileUtils.deleteQuietly(file)
        }
    }

    fun getLastBackupsInternal(maxCount: Int = 5): List<File> {
        val dataDir = DataUtils.backupFolder

        val files = dataDir.listFiles { pathname -> pathname.name.startsWith(DataConstants.BACKUP_FILENAME_PREFIX) } ?: return emptyList()
        files.sortByDescending(File::lastModified)

        val result: MutableList<File> = mutableListOf()
        for (file in files) {
            result.add(file)

            if (result.size >= maxCount) break
        }
        return result
    }

    fun createBackupInternal(file: File) {
        var archiveOutputStream: ZipArchiveOutputStream? = null
        try {
            archiveOutputStream = ZipArchiveOutputStream(BufferedOutputStream(FileOutputStream(file)))
            val converter = JsonConverter()

            val daoSession = dataManager.daoSession

            var array = JSONArray()
            for (account in daoSession.accountDao.loadAll()) {
                array.put(converter.toJson(account))
            }
            if (array.length() > 0) savePart(DataConstants.ACCOUNTS, array.toString(2), archiveOutputStream)

            array = JSONArray()
            for (plan in daoSession.budgetPlanDao.loadAll()) {
                array.put(converter.toJson(plan))
            }
            if (array.length() > 0) savePart(DataConstants.BUDGET, array.toString(2), archiveOutputStream)

            array = JSONArray()
            for (category in daoSession.categoryDao.loadAll()) {
                array.put(converter.toJson(category))
            }
            if (array.length() > 0) savePart(DataConstants.CATEGORIES, array.toString(2), archiveOutputStream)

            array = JSONArray()
            for (rate in daoSession.currencyRateDao.loadAll()) {
                array.put(converter.toJson(rate))
            }
            if (array.length() > 0) savePart(DataConstants.RATES, array.toString(2), archiveOutputStream)

            array = JSONArray()
            for (debt in daoSession.debtDao.loadAll()) {
                array.put(converter.toJson(debt))
            }
            if (array.length() > 0) savePart(DataConstants.DEBTS, array.toString(2), archiveOutputStream)

            array = JSONArray()
            for (debtNote in daoSession.debtNoteDao.loadAll()) {
                array.put(converter.toJson(debtNote))
            }
            if (array.length() > 0) savePart(DataConstants.DEBT_NOTES, array.toString(2), archiveOutputStream)

            array = JSONArray()
            for (t in daoSession.transactionDao.loadAll()) {
                array.put(converter.toJson(t))
            }
            if (array.length() > 0) savePart(DataConstants.TRANSACTIONS, array.toString(2), archiveOutputStream)

            array = JSONArray()
            for (pattern in daoSession.transactionPatternDao.loadAll()) {
                array.put(converter.toJson(pattern))
            }
            if (array.length() > 0) savePart(DataConstants.PATTERNS, array.toString(2), archiveOutputStream)

            array = JSONArray()
            for (syncLog in daoSession.syncLogDao.loadAll()) {
                array.put(converter.toJson(syncLog))
            }
            if (array.length() > 0) savePart(DataConstants.SYNC_LOGS, array.toString(2), archiveOutputStream)

            array = JSONArray()
            for (trash in daoSession.trashDao.loadAll()) {
                array.put(converter.toJson(trash))
            }
            if (array.length() > 0) savePart(DataConstants.TRASH, array.toString(2), archiveOutputStream)

            val appPreferences = getApplication().appPreferences

            val obj = JSONObject()
            obj.put(DataConstants.CURRENCY_CODE, appPreferences.mainCurrencyCode)
            obj.put(DataConstants.START, appPreferences.isFirstStart)
            obj.put(DataConstants.PERIOD, appPreferences.period.toSerializeString())

            val syncType = appPreferences.syncType
            if (syncType >= 0) obj.put(DataConstants.SYNC_TYPE, syncType)

            val syncToken = appPreferences.syncToken
            if (syncToken != null) obj.put(DataConstants.SYNC_TOKEN, syncToken)

            savePart(DataConstants.PREFS, obj.toString(2), archiveOutputStream)
        } finally {
            IOUtils.closeQuietly(archiveOutputStream)
        }
    }

    fun restoreFromBackupInternal(sourceFile: File) {
        val jsonConverter = JsonConverter()
        val db = dataManager.createRestoreDatabase()
        val master = DaoMaster(db)

        val daoSession = master.newSession()
        var zipArchiveInputStream: ZipArchiveInputStream? = null
        try {
            zipArchiveInputStream = ZipArchiveInputStream(BufferedInputStream(FileInputStream(sourceFile)))
            var entry: ArchiveEntry? = zipArchiveInputStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    handlePart(entry, daoSession, jsonConverter, zipArchiveInputStream)
                }
                entry = zipArchiveInputStream.nextEntry
            }
        } finally {
            IOUtils.closeQuietly(zipArchiveInputStream)
            db.close()
        }

        dataManager.replaceDataByRestored()
    }

    private fun handlePart(entry: ArchiveEntry, daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val entryName = entry.name
        if (entryName == DataConstants.ACCOUNTS) {
            daoSession.runInTx { createAccounts(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.BUDGET) {
            daoSession.runInTx { createBudgets(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.CATEGORIES) {
            daoSession.runInTx { createCategories(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.RATES) {
            daoSession.runInTx { createRates(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.DEBTS) {
            daoSession.runInTx { createDebts(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.DEBT_NOTES) {
            daoSession.runInTx { createDebtNotes(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.TRANSACTIONS) {
            daoSession.runInTx { createTransactions(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.PATTERNS) {
            daoSession.runInTx { createPatterns(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.SYNC_LOGS) {
            daoSession.runInTx { createSyncLogs(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.TRASH) {
            daoSession.runInTx { createTrash(daoSession, jsonConverter, zipArchiveInputStream) }
        } else if (entryName == DataConstants.PREFS) {
            val prefs = createJsonObject(zipArchiveInputStream)
            restorePrefs(prefs)

        }
    }

    private fun restorePrefs(prefs: JSONObject) {
        val appPreferences = getApplication().appPreferences
        appPreferences.mainCurrencyCode = prefs.optString(DataConstants.CURRENCY_CODE)
        appPreferences.isFirstStart = prefs.optBoolean(DataConstants.START)

        appPreferences.setPeriodStr(prefs.optString(DataConstants.PERIOD))
        getApplication().resetPeriod()

        val syncType = prefs.optInt(DataConstants.SYNC_TYPE, -1)
        appPreferences.syncType = syncType

        val syncToken = prefs.optString(DataConstants.SYNC_TOKEN)
        appPreferences.syncToken = syncToken
    }

    private fun createTrash(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        (0..array.length() - 1)
                .mapNotNull { array.optJSONObject(it) }
                .map { jsonConverter.createTrash(it) }
                .forEach { daoSession.insert<Trash>(it) }
    }

    private fun createSyncLogs(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        (0..array.length() - 1)
                .mapNotNull { array.optJSONObject(it) }
                .map { jsonConverter.createSyncLog(it) }
                .forEach { daoSession.insert<SyncLog>(it) }
    }

    private fun createPatterns(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        for (i in 0..array.length() - 1) {
            val jsonObject = array.optJSONObject(i)
            if (jsonObject != null) {
                val pattern = jsonConverter.createPattern(jsonObject)
                daoSession.insert<TransactionPattern>(pattern)

                val sub = jsonObject.optJSONArray(DataConstants.TAGS)
                (0..sub.length() - 1)
                        .mapNotNull { sub.optString(it) }
                        .forEach { createPatternTag(it, pattern, daoSession) }
            }
        }
    }

    private fun createTransactions(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        for (i in 0..array.length() - 1) {
            val jsonObject = array.optJSONObject(i)
            if (jsonObject != null) {
                val transaction = jsonConverter.createTransaction(jsonObject)
                daoSession.insert<Transaction>(transaction)

                val sub = jsonObject.optJSONArray(DataConstants.TAGS)
                (0..sub.length() - 1)
                        .mapNotNull { sub.optString(it) }
                        .forEach { createTransactionTag(it, transaction, daoSession) }
            }
        }
    }

    private fun createDebts(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        (0..array.length() - 1)
                .mapNotNull { array.optJSONObject(it) }
                .map { jsonConverter.createDebt(it) }
                .forEach { daoSession.insert<Debt>(it) }
    }

    private fun createDebtNotes(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        (0..array.length() - 1)
                .mapNotNull { array.optJSONObject(it) }
                .map { jsonConverter.createDebtNote(it) }
                .forEach { daoSession.insert<DebtNote>(it) }
    }

    private fun createRates(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        (0..array.length() - 1)
                .mapNotNull { array.optJSONObject(it) }
                .map { jsonConverter.createRate(it) }
                .forEach { daoSession.insert<CurrencyRate>(it) }
    }

    private fun createCategories(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        for (i in 0..array.length() - 1) {
            val jsonObject = array.optJSONObject(i)
            if (jsonObject != null) {
                val category = jsonConverter.createCategory(jsonObject)
                daoSession.insert<Category>(category)

                val sub = jsonObject.optJSONArray(DataConstants.SUBCATEGORIES)
                for (j in 0..sub.length() - 1) {
                    val subObject = sub.optJSONObject(j)
                    val subcategory = jsonConverter.createSubcategory(subObject)
                    subcategory.categoryId = category.id

                    daoSession.insert<Subcategory>(subcategory)
                }
            }
        }
    }

    private fun createBudgets(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        for (i in 0..array.length() - 1) {
            val jsonObject = array.optJSONObject(i)
            if (jsonObject != null) {
                val plan = jsonConverter.createPlan(jsonObject)
                daoSession.insert<BudgetPlan>(plan)

                val sub = jsonObject.optJSONArray(DataConstants.DEPENDENCIES)
                for (j in 0..sub.length() - 1) {
                    val subObject = sub.optJSONObject(j)
                    val dependency = jsonConverter.createPlanDependency(subObject)
                    dependency.planId = plan.id

                    daoSession.insert<BudgetPlanDependency>(dependency)
                }
            }
        }
    }

    private fun createAccounts(daoSession: DaoSession, jsonConverter: JsonConverter, zipArchiveInputStream: ZipArchiveInputStream) {
        val array = createJsonArray(zipArchiveInputStream)
        (0..array.length() - 1)
                .mapNotNull { array.optJSONObject(it) }
                .map { jsonConverter.createAccount(it) }
                .forEach { daoSession.insert<Account>(it) }
    }

    private fun createPatternTag(tagName: String, pattern: TransactionPattern, daoSession: DaoSession) {
        val list = daoSession.tagDao.queryBuilder().where(TagDao.Properties.Name.eq(tagName)).list()
        val tag: Tag
        if (list.isEmpty()) {
            tag = Tag()
            tag.name = tagName
            tag.updated = Date()

            daoSession.insert(tag)
        } else {
            tag = list[0]
        }

        val patternTag = PatternTag()
        patternTag.tagId = tag.id!!
        patternTag.patternId = pattern.id!!

        daoSession.insert(patternTag)
    }

    private fun createTransactionTag(tagName: String, transaction: Transaction, daoSession: DaoSession) {
        val list = daoSession.tagDao.queryBuilder().where(TagDao.Properties.Name.eq(tagName)).list()
        val tag: Tag
        if (list.isEmpty()) {
            tag = Tag()
            tag.name = tagName
            tag.updated = Date()

            daoSession.insert(tag)
        } else {
            tag = list[0]
        }

        val transactionTag = TransactionTag()
        transactionTag.tagId = tag.id!!
        transactionTag.transactionId = transaction.id!!

        daoSession.insert(transactionTag)
    }

    private fun createJsonArray(zipArchiveInputStream: ZipArchiveInputStream): JSONArray {
        val out = ByteArrayOutputStream()

        try {
            val buffer = ByteArray(DataConstants.DEFAULT_COPY_BUFFER_SIZE)
            var bytes = zipArchiveInputStream.read(buffer)
            while (bytes != -1) {
                out.write(buffer, 0, bytes)
                bytes = zipArchiveInputStream.read(buffer)
            }

            return JSONArray(out.toString())
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    private fun createJsonObject(zipArchiveInputStream: ZipArchiveInputStream): JSONObject {
        val out = ByteArrayOutputStream()

        val buffer = ByteArray(DataConstants.DEFAULT_COPY_BUFFER_SIZE)
        var bytes = zipArchiveInputStream.read(buffer)
        while (bytes != -1) {
            out.write(buffer, 0, bytes)
            bytes = zipArchiveInputStream.read(buffer)
        }

        return JSONObject(out.toString())
    }

    private fun savePart(entryName: String, content: String, archiveOutputStream: ZipArchiveOutputStream) {
        val entry = ZipArchiveEntry(entryName)
        archiveOutputStream.putArchiveEntry(entry)

        val input = ByteArrayInputStream(content.toByteArray())

        val buffer = ByteArray(DataConstants.DEFAULT_COPY_BUFFER_SIZE)
        var bytes = input.read(buffer)
        while (bytes != -1) {
            archiveOutputStream.write(buffer, 0, bytes)
            bytes = input.read(buffer)
        }

        archiveOutputStream.closeArchiveEntry()
    }
}