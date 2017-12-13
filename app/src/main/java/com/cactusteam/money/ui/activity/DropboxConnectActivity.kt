package com.cactusteam.money.ui.activity

import android.app.AlertDialog
import android.app.Fragment
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.cactusteam.money.R
import com.cactusteam.money.sync.SyncManager
import com.cactusteam.money.ui.UiConstants
import com.dropbox.core.android.AuthActivity

/**
 * @author vpotapenko
 */
class DropboxConnectActivity : BaseActivity("DropboxConnectActivity") {

    private var successPanel: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dropbox_connect)

        initializeToolbar()

        successPanel = findViewById(R.id.success_panel)
        findViewById(R.id.ok_btn).setOnClickListener { finish() }

        authenticate()
    }

    private fun authenticate() {
        if (!AuthActivity.checkAppBeforeAuth(this, APP_KEY, true)) {
            finish()
            return
        }

        val apiType = "1"
        val webHost = "www.dropbox.com"
        val intent = AuthActivity.makeIntent(this, APP_KEY, webHost, apiType)
        startActivityForResult(intent, UiConstants.DROPBOX_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.DROPBOX_REQUEST_CODE) {
            handleAuth(AuthActivity.result)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleAuth(data: Intent?) {
        if (data == null) {
            finish()
        } else {
            val token = data.getStringExtra(AuthActivity.EXTRA_ACCESS_TOKEN)
            val secret = data.getStringExtra(AuthActivity.EXTRA_ACCESS_SECRET)
            val uid = data.getStringExtra(AuthActivity.EXTRA_UID)

            if (arrayOf(token, secret, uid).any(String::isNullOrBlank)) {
                finish()
            } else {
                checkRepository(secret)
            }
        }
    }

    private fun checkRepository(accessToken: String) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.syncService
                .checkDropboxRepository(accessToken)
                .subscribe(
                        { r ->
                            hideBlockingProgress()
                            if (r) {
                                repositoryExists(accessToken)
                            } else {
                                createRepository(accessToken)
                            }
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun repositoryExists(accessToken: String) {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.sync_repository_exists_warning).setCancelable(false).setPositiveButton(R.string.ok) { dialog, which -> joinToRepository(accessToken) }.setNegativeButton(R.string.cancel) { dialog, which -> finish() }.show()
    }

    private fun joinToRepository(accessToken: String) {
        showBlockingProgressWithUpdate(getString(R.string.waiting))
        val s = dataManager.syncService
                .joinToSyncRepository(SyncManager.DROPBOX_TYPE, accessToken)
                .subscribe(
                        { p ->
                            updateBlockingProgress(p.first, p.second)
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message, DialogInterface.OnClickListener { dialogInterface, i -> finish() })
                        },
                        {
                            hideBlockingProgress()
                            showSuccess()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun createRepository(accessToken: String) {
        showBlockingProgressWithUpdate(getString(R.string.waiting))
        val s = dataManager.syncService
                .createSyncRepository(SyncManager.DROPBOX_TYPE, accessToken)
                .subscribe(
                        { p ->
                            updateBlockingProgress(p.first, p.second)
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            showSuccess()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun showSuccess() {
        setResult(RESULT_OK)
        successPanel!!.visibility = View.VISIBLE
    }

    companion object {

        private val APP_KEY = "5humdwg5ll1o9o3"

        fun actionStart(fragment: Fragment, requestCode: Int) {
            val intent = Intent(fragment.activity, DropboxConnectActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
