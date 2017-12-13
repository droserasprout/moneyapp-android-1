package com.cactusteam.money.ui.activity

import android.app.AlertDialog
import android.app.Fragment
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.ui.UiConstants
import com.google.android.gms.analytics.HitBuilders
import rx.subscriptions.CompositeSubscription

/**
 * @author vpotapenko
 */
abstract class BaseActivity(val tag: String) : AppCompatActivity() {

    private var forceCheckPassword: Boolean = false

    private var progressDialog: ProgressDialog? = null
    protected var toolbar: Toolbar? = null

    val compositeSubscription: CompositeSubscription get() = _compositeSubscription!!

    val dataManager: DataManager
        get() = MoneyApp.instance.dataManager

    protected val application: MoneyApp
        get() = MoneyApp.instance

    private var _compositeSubscription: CompositeSubscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _compositeSubscription = CompositeSubscription()
    }

    protected open fun initializeToolbar() {
        toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        toolbar?.setNavigationIcon(R.drawable.ic_navigation_arrow_back)
        toolbar?.setNavigationOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()

        forceCheckPassword = false
        if (!isLoggedIn) showPasswordActivity()
    }

    override fun onPause() {
        if (!forceCheckPassword) updateLoginTime()

        super.onPause()
    }

    protected open fun updateLoginTime() {
        MoneyApp.instance.appPreferences.lastLoginTime = System.currentTimeMillis()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.PASSWORD_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showPasswordActivity() {
        forceCheckPassword = true
        PasswordActivity.actionStart(this, UiConstants.PASSWORD_REQUEST_CODE)
    }

    protected open val isLoggedIn: Boolean
        get() {
            val appPreferences = MoneyApp.instance.appPreferences
            if (appPreferences.password == null) return true

            val lastLoginTime = appPreferences.lastLoginTime
            return lastLoginTime != null && System.currentTimeMillis() - lastLoginTime < UiConstants.LOGIN_PAUSE
        }

    override fun onDestroy() {
        _compositeSubscription?.unsubscribe()
        _compositeSubscription = null

        super.onDestroy()
    }

    protected fun showFragment(resId: Int, fragment: Fragment, tag: String) {
        val fragmentManager = fragmentManager
        val ft = fragmentManager.beginTransaction()
        ft.setCustomAnimations(R.animator.fragment_enter, R.animator.fragment_exit)
        ft.replace(resId, fragment, tag)
        ft.commit()
    }

    fun showBlockingProgress(message: String) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog.show(this, null, message, true, false)
        }
    }

    fun hideBlockingProgress() {
        if (progressDialog != null) {
            progressDialog!!.cancel()
            progressDialog = null
        }
    }

    fun showBlockingProgressWithUpdate(message: String) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
            progressDialog!!.setMessage(message)
            progressDialog!!.isIndeterminate = true
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }
    }

    fun updateBlockingProgress(progress: Int, max: Int) {
        if (progressDialog != null) {
            progressDialog!!.isIndeterminate = false
            progressDialog!!.max = max
            progressDialog!!.progress = progress
        }
    }

    fun showError(errorDescription: String?, listener: DialogInterface.OnClickListener? = null) {
        sendEvent("error", "$tag :", errorDescription)

        AlertDialog.Builder(this).setTitle(getString(R.string.error)).setMessage(errorDescription ?: "").setPositiveButton(R.string.ok, listener).show()
    }

    protected fun sendEvent(category: String, action: String, label: String?) {
        val t = MoneyApp.instance.tracker

        t.setScreenName(javaClass.name)
        t.send(HitBuilders.EventBuilder().setCategory(category).setAction(action)
                .setLabel(label ?: "").build())
    }

    fun showFileSaved(filePath: String?, dismissListener: DialogInterface.OnDismissListener?) {
        val dialog = AlertDialog.Builder(this).setTitle(getString(R.string.app_name)).setMessage(getString(R.string.data_was_saved_to, filePath ?: "")).setPositiveButton(R.string.ok, null).create()
        if (dismissListener != null) dialog.setOnDismissListener(dismissListener)

        dialog.show()
    }
}
