package com.cactusteam.money.data

import android.os.Build
import android.os.Environment
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.ui.UiConstants
import org.apache.commons.io.FileUtils
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author vpotapenko
 */
object DataUtils {

    fun round(value: Double, places: Int): Double {
        if (places < 0) throw IllegalArgumentException()

        var bd = BigDecimal(value)
        bd = bd.setScale(places, BigDecimal.ROUND_HALF_UP)
        return bd.toDouble()
    }

    fun toFloat(d: Double): Float {
        return BigDecimal(d).toFloat()
    }

    val exportFolder: File
        get() {
            val extDir = Environment.getExternalStorageDirectory()
            val moneyAppFolder = File(extDir, UiConstants.MONEY_APP_FOLDER_NAME)
            if (!moneyAppFolder.exists()) {
                FileUtils.forceMkdir(moneyAppFolder)
            }

            return moneyAppFolder
        }

    val initialBackupFolder: File
        get() {
            val extDir = MoneyApp.instance.getExternalFilesDir(null)

            val moneyAppFolder = File(extDir, UiConstants.MONEY_APP_FOLDER_NAME)
            val backupFolder = File(moneyAppFolder, "backup")
            if (!backupFolder.exists()) {
                FileUtils.forceMkdir(backupFolder)
            }

            return backupFolder
        }

    val backupFolder: File
        get() {
            val backupPath = MoneyApp.instance.appPreferences.backupPath
            val backupFolder = File(backupPath)
            if (!backupFolder.exists()) {
                backupFolder.mkdirs()
            }

            return backupFolder
        }

    fun getBackupFileName(version: Int): String {
        val dateFormat = SimpleDateFormat("yyyy_MM_dd")
        val versionStr = if (version == 0) "" else String.format("(%d)", version)
        return DataConstants.BACKUP_FILENAME_PREFIX + dateFormat.format(Date()) +
                versionStr + DataConstants.BACKUP_FILENAME_SUFFIX
    }

    fun getAutoBackupFileName(version: Int): String {
        val dateFormat = SimpleDateFormat("yyyy_MM_dd")
        val versionStr = if (version == 0) "" else String.format("(%d)", version)
        return DataConstants.BACKUP_FILENAME_PREFIX + dateFormat.format(Date()) +
                versionStr + DataConstants.AUTO_BACKUP_FILENAME_SUFFIX
    }

    fun hasICS(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
    }

    fun hasLollipop(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }
}
