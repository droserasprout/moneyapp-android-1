package com.cactusteam.money.data.service

import android.content.ContentResolver
import android.content.ContentUris
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.*
import com.cactusteam.money.data.dao.CurrencyRate
import com.cactusteam.money.data.dao.Note
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.io.ExporterFactory
import com.cactusteam.money.data.model.Contact
import com.cactusteam.money.data.model.Icons
import com.cactusteam.money.data.period.Period
import com.cactusteam.money.ui.BitmapUtils
import org.apache.commons.lang3.time.DateUtils
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

/**
 * @author vpotapenko
 */
abstract class SystemInternalService(dataManager: DataManager) : BaseService(dataManager) {

    fun resetInterfaceSettingsInternal() {
        getApplication().appPreferences.clearAllInterfaceSettings()
    }

    fun getIconPathsInternal(): Icons {
        val application = getApplication()
        val icons = application.assets.list(ICONS_PATH)
        val result = Icons()
        if (icons == null) return result

        for (group in groups) {
            val resId = application.resources.getIdentifier(group, "string", application.packageName)
            result.groups.add(Pair(group, application.getString(resId)))
        }

        for (icon in icons) {
            for (group in result.groups) {
                if (icon.startsWith(group.first)) {
                    var list: MutableList<String>? = result.icons[group.first]
                    if (list == null) {
                        list = mutableListOf()
                        result.icons.put(group.first, list)
                    }
                    list.add(icon)
                    break
                }
            }
        }

        return result
    }

    fun prepareMoneyAppContextInternal() {
        val preferences = getApplication().appPreferences
        val lastDailyWork = preferences.lastDailyWork
        val now = Date()
        if (lastDailyWork != null && DateUtils.isSameDay(Date(lastDailyWork), now)) {
            dataManager.daoSession
        } else {
            DatabaseOpener(dataManager).prepareDatabase()
            preferences.setLastDailyWork(now.time)
        }

        getApplication().scheduler.updateAlarm()
    }

    fun exportTransactionsInternal(from: Date, to: Date,
                                   type: Int = ExporterFactory.XLS_TYPE,
                                   expense: Boolean = false,
                                   income: Boolean = false,
                                   transfer: Boolean = false): String {
        val application = getApplication()

        val exporter = ExporterFactory.create(application, type)

        val mainCurrencyCode = application.appPreferences.mainCurrencyCode
        val mainCurrency = getApplication().currencyManager.getCurrencyByCode(mainCurrencyCode).displayName
        exporter.initialize(mainCurrency)

        val transactions = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(from)
                .putTo(to)
                .putConvertToMain(true)
                .listInternal()
        transactions.sortedBy { t -> t.date }
        for (t in transactions) {
            if (t.type == Transaction.EXPENSE && expense) {
                exporter.export(t)
            }
            if (t.type == Transaction.INCOME && income) {
                exporter.export(t)
            }
            if (t.type == Transaction.TRANSFER && transfer) {
                exporter.export(t)
            }
        }

        val exportFolder = DataUtils.exportFolder
        return exporter.commit(exportFolder)
    }

    fun changeFinancialOptionsInternal(currencyCode: String, period: Period, rate: CurrencyRate?) {
        val application = getApplication()

        val appPreferences = application.appPreferences
        appPreferences.period = period
        application.resetPeriod()

        val oldCurrencyCode = appPreferences.mainCurrencyCode
        if (oldCurrencyCode != currencyCode) {
            appPreferences.mainCurrencyCode = currencyCode

            val daoSession = dataManager.daoSession
            daoSession.runInTx({
                val budgetPlans = daoSession.budgetPlanDao.loadAll()
                for (plan in budgetPlans) {
                    val limit = plan.limit
                    plan.limit = rate!!.convertTo(limit, currencyCode)

                    daoSession.update(plan)
                }
            })
        }
    }

    fun resetAllDataInternal() {
        dataManager.clearAllData()

        val preferences = getApplication().appPreferences
        val period = preferences.period
        val currencyCode = preferences.mainCurrencyCode

        preferences.clearAllDataSettings()

        val syncManager = getApplication().syncManager
        syncManager.resetSyncLog()

        initializeModelInternal(period, currencyCode)
    }

    fun initializeModelInternal(period: Period, currencyCode: String) {
        val preferences = getApplication().appPreferences

        preferences.mainCurrencyCode = currencyCode
        preferences.period = period

        preferences.isFirstStart = false

        MoneyApp.instance.dataManager.daoSession // initialize base
    }

    fun getIconImageInternal(name: String, bigImage: Boolean = false): Bitmap? {
        try {
            val resources = getApplication().resources
            val size = if (bigImage) resources.getDimensionPixelSize(R.dimen.category_big_icon) else resources.getDimensionPixelSize(R.dimen.category_icon)

            val assetsPath = ICONS_PATH + '/' + name
            val inputStream = getApplication().assets.open(assetsPath)
            return BitmapUtils.decodeBitmapFromInputStream(inputStream, size, size)
        } catch (e: IOException) {
            return null
        }
    }

    fun getContactImageInternal(contactId: Long): Bitmap? {
        // Instantiates a ContentResolver for retrieving the Uri of the image
        val context = getApplication()

        val imageSize = context.resources.getDimensionPixelSize(R.dimen.contact_icon)
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        val contentResolver = context.contentResolver

        // Instantiates an AssetFileDescriptor. Given a content Uri pointing to an image file, the
        // ContentResolver can return an AssetFileDescriptor for the file.
        var afd: AssetFileDescriptor? = null

        if (DataUtils.hasICS()) {
            // On platforms running Android 4.0 (API version 14) and later, a high resolution image
            // is available from Photo.DISPLAY_PHOTO.
            try {
                // Constructs the content Uri for the image
                val displayImageUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO)

                // Retrieves an AssetFileDescriptor from the Contacts Provider, using the
                // constructed Uri
                afd = contentResolver.openAssetFileDescriptor(displayImageUri, "r")
                // If the file exists
                if (afd != null) {
                    // Reads and decodes the file to a Bitmap and scales it to the desired size
                    return BitmapUtils.decodeBitmapFromDescriptor(afd.fileDescriptor, imageSize, imageSize)
                }
            } catch (e: FileNotFoundException) {
                // Catches file not found exceptions
            } finally {
                // Once the decode is complete, this closes the file. You must do this each time
                // you access an AssetFileDescriptor; otherwise, every image load you do will open
                // a new descriptor.
                if (afd != null) {
                    try {
                        afd.close()
                    } catch (e: IOException) {
                        // Closing a file descriptor might cause an IOException if the file is
                        // already closed. Nothing extra is needed to handle this.
                    }

                }
            }
        }

        // If the platform version is less than Android 4.0 (API Level 14), use the only available
        // image URI, which points to a normal-sized image.
        try {
            // Constructs the image Uri from the contact Uri and the directory twig from the
            // Contacts.Photo table
            val imageUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)

            // Retrieves an AssetFileDescriptor from the Contacts Provider, using the constructed
            // Uri
            afd = context.contentResolver.openAssetFileDescriptor(imageUri, "r")

            // If the file exists
            if (afd != null) {
                // Reads the image from the file, decodes it, and scales it to the available screen
                // area
                return BitmapUtils.decodeBitmapFromDescriptor(afd.fileDescriptor, imageSize, imageSize)
            }
        } catch (e: FileNotFoundException) {
            // Catches file not found exceptions
        } finally {
            // Once the decode is complete, this closes the file. You must do this each time you
            // access an AssetFileDescriptor; otherwise, every image load you do will open a new
            // descriptor.
            if (afd != null) {
                try {
                    afd.close()
                } catch (e: IOException) {
                    // Closing a file descriptor might cause an IOException if the file is
                    // already closed. Ignore this.
                }

            }
        }

        // If none of the case selectors match, returns null.
        return null
    }

    fun getContactInternal(contactUri: Uri): Contact? {
        val contentResolver = getApplication().contentResolver

        val c = contentResolver.query(contactUri, null, null, null, null) ?: return null

        val contact = Contact()
        if (c.moveToFirst()) {
            val id = c.getLong(c.getColumnIndex(ContactsContract.Contacts._ID))
            val displayName = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

            contact.id = id
            contact.name = displayName

            fetchPhone(contact, contentResolver)
        }
        c.close()
        return contact
    }

    fun handleLatePlanningTransactions(): Boolean {
        var hasTransaction = false
        dataManager.transactionService
                .newListTransactionsBuilder()
                .putStatus(Transaction.STATUS_PLANNING)
                .putTo(Date())
                .listInternal()
                .forEach {
                    handleLatePlanningTransaction(it)
                    hasTransaction = true
                }
        return hasTransaction
    }

    private fun handleLatePlanningTransaction(transaction: Transaction) {
        val builder = dataManager.transactionService
                .newTransactionBuilder(transaction)
                .putStatus(Transaction.STATUS_WAITING_CONFIRMATION)

        if (builder.globalId != null) builder.putSynced(false)
        builder.updateInternal()

        dataManager.noteService.createTransactionNoteInternal(transaction)
    }

    private fun createNote(description: String?, ref: String?) {
        try {
            val daoSession = dataManager.daoSession

            val note = Note()
            note.description = description
            note.ref = ref

            daoSession.insert(note)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun fetchPhone(contact: Contact, contentResolver: ContentResolver) {
        val cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf(contact.id.toString()), null) ?: return

        if (cursor.moveToNext()) {
            val phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            contact.phone = phoneNumber
        }
        cursor.close()
    }

    companion object {
        val ICONS_PATH = "icons"

        val groups = arrayOf(
                "appliance",
                "baby",
                "basic",
                "devices",
                "education",
                "finance",
                "food_1",
                "furniture",
                "holiday",
                "medicine",
                "navigation",
                "symbols",
                "transport",
                "weather",
                "work")
    }
}