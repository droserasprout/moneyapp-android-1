package com.cactusteam.money.ui.activity

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.format.DateFormat
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.ui.UiConstants
import java.io.File
import java.util.*

/**
 * @author vpotapenko
 */
class ExportTransactionsActivity : BaseActivity("ExportTransactionsActivity") {

    private var fromView: TextView? = null
    private var toView: TextView? = null

    private var expenseCheck: CheckBox? = null
    private var incomeCheck: CheckBox? = null
    private var transferCheck: CheckBox? = null

    private var formatSpinner: Spinner? = null

    private val from = Calendar.getInstance()
    private val to = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_transactions)

        initializeToolbar()

        fromView = findViewById(R.id.from_date) as TextView
        findViewById(R.id.from_date_container).setOnClickListener { fromDateClicked() }

        toView = findViewById(R.id.to_date) as TextView
        findViewById(R.id.to_date_container).setOnClickListener { toDateClicked() }

        val current = application.period.current
        from.time = current.first
        updateFromDateView()

        to.time = current.second
        updateToDateView()

        expenseCheck = findViewById(R.id.expense_check) as CheckBox
        incomeCheck = findViewById(R.id.income_check) as CheckBox
        transferCheck = findViewById(R.id.transfer_check) as CheckBox

        val adapter = ArrayAdapter(this, R.layout.activity_export_transactions_format_item, resources.getStringArray(R.array.export_formats))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        formatSpinner = findViewById(R.id.format) as Spinner
        formatSpinner!!.adapter = adapter

        findViewById(R.id.save_btn).setOnClickListener { saveClicked() }
        findViewById(R.id.send_btn).setOnClickListener { sendClicked() }

    }

    private fun sendClicked() {
        if (!hasDataForExport()) {
            Toast.makeText(this, R.string.no_data_for_export, Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    UiConstants.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_ON_SEND.toInt())
        } else {
            send()
        }
    }

    private fun send() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.systemService.exportTransactions(from.time,
                to.time,
                formatSpinner!!.selectedItemPosition,
                expenseCheck!!.isChecked,
                incomeCheck!!.isChecked,
                transferCheck!!.isChecked)
                .subscribe(
                        { r ->
                            hideBlockingProgress()
                            sendFile(r)
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun sendFile(filePath: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "plain/text"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.transactions_title))
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(filePath)))

        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_transactions)))
    }

    private fun saveClicked() {
        if (!hasDataForExport()) {
            Toast.makeText(this, R.string.no_data_for_export, Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    UiConstants.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_ON_SAVE.toInt())
        } else {
            save()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode.toByte()) {
            UiConstants.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_ON_SAVE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    save()
                }
                return
            }
            UiConstants.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_ON_SEND -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    send()
                }
                return
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun save() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.systemService.exportTransactions(from.time,
                to.time,
                formatSpinner!!.selectedItemPosition,
                expenseCheck!!.isChecked,
                incomeCheck!!.isChecked,
                transferCheck!!.isChecked)
                .subscribe(
                        { r ->
                            hideBlockingProgress()
                            showFileSaved(r, DialogInterface.OnDismissListener { finish() })
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun hasDataForExport(): Boolean {
        return expenseCheck!!.isChecked ||
                incomeCheck!!.isChecked ||
                transferCheck!!.isChecked
    }

    private fun fromDateClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            from.set(year, monthOfYear, dayOfMonth)
            from.set(Calendar.HOUR_OF_DAY, 0)
            from.clear(Calendar.MINUTE)
            from.clear(Calendar.SECOND)
            from.clear(Calendar.MILLISECOND)

            updateFromDateView()
        }, from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateFromDateView() {
        fromView!!.text = DateFormat.getDateFormat(this).format(from.time)
    }

    private fun toDateClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            to.set(year, monthOfYear, dayOfMonth)
            to.set(Calendar.HOUR_OF_DAY, 23)
            to.set(Calendar.MINUTE, 59)
            to.set(Calendar.SECOND, 59)
            to.set(Calendar.MILLISECOND, 999)

            updateToDateView()
        }, to.get(Calendar.YEAR), to.get(Calendar.MONTH), to.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateToDateView() {
        toView!!.text = DateFormat.getDateFormat(this).format(to.time)
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, ExportTransactionsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
