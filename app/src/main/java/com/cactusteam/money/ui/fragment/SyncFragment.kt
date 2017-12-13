package com.cactusteam.money.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.*
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.sync.ISyncListener
import com.cactusteam.money.sync.SyncManager
import com.cactusteam.money.sync.SyncService
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.activity.DropboxConnectActivity
import com.cactusteam.money.ui.activity.MainActivity
import java.util.*

/**
 * @author vpotapenko
 */
class SyncFragment : BaseMainFragment(), ISyncListener {

    private var syncLayout: View? = null
    private var withoutSyncLayout: View? = null

    private var typeText: TextView? = null
    private var syncLog: TextView? = null

    private var syncPeriodSpinner: Spinner? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.CONNECT_SYNC_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val mainActivity = mainActivity
                mainActivity.invalidateOptionsMenu()
                updateViewState()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_sync, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val syncManager = MoneyApp.instance.syncManager
        menu.findItem(R.id.disconnect_sync).isVisible = syncManager.isSyncConnected

        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.disconnect_sync) {
            disconnectClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun disconnectClicked() {
        AlertDialog.Builder(activity)
                .setTitle(R.string.continue_question)
                .setMessage(R.string.disconnect_sync_warning)
                .setPositiveButton(R.string.ok) { dialog, which -> disconnect() }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun disconnect() {
        mainActivity.showBlockingProgress(getString(R.string.sync_title))
        val s = dataManager.syncService
                .disconnectSync()
                .subscribe(
                        {},
                        { e ->
                            mainActivity.hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            updateViewState()

                            mainActivity.hideBlockingProgress()
                            mainActivity.invalidateOptionsMenu()
                        }
                )
        compositeSubscription.add(s)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sync, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        view.findViewById(R.id.connect_btn).setOnClickListener { connectClicked() }

        view.findViewById(R.id.sync_btn).setOnClickListener { syncClicked() }

        syncLayout = view.findViewById(R.id.sync_layout)
        withoutSyncLayout = view.findViewById(R.id.without_sync_layout)

        typeText = view.findViewById(R.id.sync_type) as TextView
        syncLog = view.findViewById(R.id.sync_log) as TextView

        val preferences = MoneyApp.instance.appPreferences
        val autoSyncMode = !preferences.isManualSyncMode

        val adapter = ArrayAdapter(activity,
                R.layout.fragment_sync_period_item,
                android.R.id.text1,
                Arrays.asList(
                        getString(R.string.one_per_day),
                        getString(R.string.one_per_hour),
                        getString(R.string.one_per_ten_minutes)
                )
        )
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        syncPeriodSpinner = view.findViewById(R.id.sync_period) as Spinner
        syncPeriodSpinner!!.adapter = adapter
        syncPeriodSpinner!!.setSelection(preferences.syncPeriod)
        syncPeriodSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                preferences.syncPeriod = position
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }
        syncPeriodSpinner!!.visibility = if (autoSyncMode) View.VISIBLE else View.GONE

        val syncModeDescription = view.findViewById(R.id.sync_mode_description) as TextView
        syncModeDescription.setText(if (autoSyncMode) R.string.auto_mode else R.string.manual_mode)

        val syncManualMode = view.findViewById(R.id.manual_sync) as Switch
        syncManualMode.isChecked = autoSyncMode

        syncManualMode.setOnCheckedChangeListener { buttonView, isChecked ->
            preferences.isManualSyncMode = !isChecked
            syncModeDescription.setText(if (isChecked) R.string.auto_mode else R.string.manual_mode)
            syncPeriodSpinner!!.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        updateViewState()
    }

    private fun updateViewState() {
        val syncManager = MoneyApp.instance.syncManager
        if (syncManager.isSyncConnected) {
            syncLayout!!.visibility = View.VISIBLE
            withoutSyncLayout!!.visibility = View.GONE

            if (syncManager.syncType == SyncManager.DROPBOX_TYPE) {
                typeText!!.setText(R.string.connected_to_dropbox)
            }
            updateSyncLog()

            mainActivity.startSyncService()
        } else {
            syncLayout!!.visibility = View.GONE
            withoutSyncLayout!!.visibility = View.VISIBLE

            SyncService.actionStop(activity)
        }
    }

    @Suppress("DEPRECATION")
    private fun updateSyncLog() {
        syncLog!!.text = ""
        val s = dataManager.syncService
                .loadSyncLog()
                .subscribe(
                        { r ->
                            syncLog!!.text = if (r.isEmpty()) getString(R.string.no_data) else Html.fromHtml(r)
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun syncClicked() {
        val syncManager = MoneyApp.instance.syncManager
        val eventsController = syncManager.eventsController
        eventsController.addListener(this)

        if (eventsController.isSyncing) {
            // shows because already started
            mainActivity.showBlockingProgressWithUpdate(getString(R.string.sync_title))
        } else {
            SyncService.actionSyncImmediately(activity)
        }
    }

    private fun connectClicked() {
        DropboxConnectActivity.actionStart(this, UiConstants.CONNECT_SYNC_REQUEST_CODE)
    }

    override fun dataChanged() {
        // do nothing
    }

    override fun syncStarted() {
        mainActivity.showBlockingProgressWithUpdate(getString(R.string.sync_title))
    }

    override fun syncFinished() {
        if (isDetached) return

        mainActivity.hideBlockingProgress()
        updateSyncLog()
        syncLayout!!.post {
            val eventsController = MoneyApp.instance.syncManager.eventsController
            eventsController.removeListener(this@SyncFragment)
        }
    }

    override fun onProgressUpdated(progress: Int, max: Int) {
        if (!isDetached) mainActivity.updateBlockingProgress(progress, max)
    }

    companion object {

        fun build(): SyncFragment {
            return SyncFragment()
        }
    }
}
