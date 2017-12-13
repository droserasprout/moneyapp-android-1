package com.cactusteam.money.ui.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.fragment.ChooseFolderFragment
import java.io.File

/**
 * @author vpotapenko
 */
class DataManagementActivity : BaseActivity("DataManagementActivity") {

    private var backupPath: TextView? = null

    private var lastSavingsContainer: LinearLayout? = null
    private var autoBackupNumbers: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_management)

        initializeToolbar()

        backupPath = findViewById(R.id.backup_path) as TextView
        findViewById(R.id.backup_path_container).setOnClickListener { requestBackupClicked() }
        findViewById(R.id.save_btn).setOnClickListener { saveClicked() }
        findViewById(R.id.reset_data_btn).setOnClickListener { resetDataClicked() }
        findViewById(R.id.generate_data_btn).setOnClickListener { generateDataClicked() }
        findViewById(R.id.import_transactions_btn).setOnClickListener { importTransactionsClicked() }
        findViewById(R.id.export_transactions_btn).setOnClickListener { exportTransactionsClicked() }

        val appPreferences = MoneyApp.instance.appPreferences
        val autoBackupCheck = findViewById(R.id.auto_backup_check) as Switch
        autoBackupCheck.isChecked = appPreferences.isAutoBackup
        autoBackupCheck.setOnCheckedChangeListener { buttonView, isChecked -> appPreferences.isAutoBackup = isChecked }

        autoBackupNumbers = findViewById(R.id.auto_backup_numbers) as EditText
        updateAutoBackupNumbers()

        lastSavingsContainer = findViewById(R.id.last_savings_container) as LinearLayout

        backupPathUpdated()
    }

    private fun updateAutoBackupNumbers() {
        val appPreferences = MoneyApp.instance.appPreferences
        autoBackupNumbers!!.setText(appPreferences.backupMaxNumber.toString())
    }

    override fun onPause() {
        val appPreferences = MoneyApp.instance.appPreferences
        val text = autoBackupNumbers!!.text
        if (text.isNullOrBlank()) {
            appPreferences.backupMaxNumber = DataConstants.DEFAULT_BACKUP_NUMBERS
        } else {
            try {
                val max = Integer.parseInt(text.toString())
                appPreferences.backupMaxNumber = max
            } catch (e: Exception) {
                appPreferences.backupMaxNumber = DataConstants.DEFAULT_BACKUP_NUMBERS
            }

        }
        updateAutoBackupNumbers()
        super.onPause()
    }

    private fun importTransactionsClicked() {
        ImportTransactionsActivity.actionStart(this)
    }

    private fun exportTransactionsClicked() {
        ExportTransactionsActivity.actionStart(this)
    }

    private fun backupPathUpdated() {
        updateBackupPathView()
        loadLastBackupFiles()
    }

    private fun requestBackupClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    UiConstants.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE.toInt())
        } else {
            backupPathClicked()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            UiConstants.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE.toInt() -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    backupPathClicked()
                }
                return
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun generateDataClicked() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.transactionService
                .generateRandomTransactions()
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            Toast.makeText(this, R.string.transactions_were_generated, Toast.LENGTH_SHORT).show()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun resetDataClicked() {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.all_current_data_will_be_deleted).setPositiveButton(R.string.ok) { dialog, which -> resetData() }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun resetData() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.systemService
                .resetAllData()
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            Toast.makeText(this@DataManagementActivity, R.string.data_was_deleted, Toast.LENGTH_SHORT).show()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun deleteClicked(file: File) {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(getString(R.string.backup_will_be_deleted, file.name)).setPositiveButton(R.string.ok) { dialog, which -> deleteSavingsFile(file) }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun deleteSavingsFile(file: File) {
        val s = dataManager.fileService
                .deleteFile(file)
                .subscribe(
                        {},
                        { e ->
                            showError(e.message)
                        },
                        { loadLastBackupFiles() }
                )
        compositeSubscription.add(s)
    }

    private fun loadClicked(file: File?) {
        if (file == null) return

        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.all_current_data_will_be_replaced).setPositiveButton(R.string.ok) { dialog, which -> restoreData(file) }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun restoreData(file: File) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.backupService
                .restoreFromBackup(file)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            Toast.makeText(this@DataManagementActivity, R.string.data_was_restored, Toast.LENGTH_SHORT).show()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun saveClicked() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.backupService
                .createBackup(newBackupFile)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()

                            Toast.makeText(this@DataManagementActivity, R.string.backup_was_saved, Toast.LENGTH_SHORT).show()
                            loadLastBackupFiles()
                        }
                )
        compositeSubscription.add(s)
    }

    private val newBackupFile: File
        get() {
            val dataDir = DataUtils.backupFolder
            var version = 0
            var file = File(dataDir, DataUtils.getBackupFileName(version))
            while (file.exists()) {
                version++
                file = File(dataDir, DataUtils.getBackupFileName(version))
            }
            return file
        }

    private fun loadLastBackupFiles() {
        lastSavingsContainer!!.visibility = View.GONE
        val s = dataManager.backupService
                .getLastBackups()
                .subscribe(
                        { r ->
                            lastBackupsLoaded(r)
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun lastBackupsLoaded(files: List<File>) {
        updateLastSavings(files)
    }

    private fun updateLastSavings(files: List<File>) {
        lastSavingsContainer!!.visibility = View.VISIBLE

        lastSavingsContainer!!.removeAllViews()
        for (file in files) {
            addSavingView(file)
        }

        if (files.isEmpty()) {
            lastSavingsContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
            lastSavingsContainer!!.addView(View.inflate(this, R.layout.horizontal_divider, null))
        }
    }

    private fun addSavingView(file: File) {
        val view = View.inflate(this, R.layout.activity_data_management_saving, null)

        val fileName = file.name
        (view.findViewById(R.id.name) as TextView).text = fileName
        val descriptionId = if (fileName.endsWith(DataConstants.AUTO_BACKUP_FILENAME_SUFFIX)) R.string.auto_backup else R.string.manual_backup
        (view.findViewById(R.id.description) as TextView).setText(descriptionId)

        view.findViewById(R.id.restore_saving).setOnClickListener { loadClicked(file) }
        view.findViewById(R.id.delete_saving).setOnClickListener { deleteClicked(file) }

        lastSavingsContainer!!.addView(view)
    }

    private fun updateBackupPathView() {
        this.backupPath!!.text = DataUtils.backupFolder.path
    }

    private fun backupPathClicked() {
        val fragment = ChooseFolderFragment.build(DataUtils.backupFolder)
        fragment.listener = { file ->
            MoneyApp.instance.appPreferences.backupPath = file.path
            backupPathUpdated()
        }
        fragment.show(fragmentManager, "dialog")
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, DataManagementActivity::class.java)
            context.startActivity(intent)
        }
    }
}
