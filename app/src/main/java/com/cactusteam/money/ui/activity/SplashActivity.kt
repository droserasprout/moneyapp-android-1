package com.cactusteam.money.ui.activity

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.widget.BalanceAppWidget

/**
 * @author vpotapenko
 */
class SplashActivity : BaseUnauthorizedActivity("SplashActivity") {

    private var initialSection: MainSection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val appPreferences = MoneyApp.instance.appPreferences
        if (appPreferences.isFirstStart) {
            startInitializeWizard()
        } else {
            val s = dataManager.systemService
                    .prepareMoneyAppContext()
                    .subscribe(
                            {},
                            { e ->
                                showError(e.message)
                            },
                            { startMainActivity() }
                    )
            compositeSubscription.add(s)
        }
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                initialSection = MainSection.find(extras.getString(UiConstants.EXTRA_TYPE))
            }
        }

        if (initialSection == null) {
            // try to find through action
            val action = intent.action
            val mainSection = MainSection.find(action)
            if (mainSection != null) initialSection = mainSection
        }
    }

    private fun startMainActivity() {
        updateAppWidget()

        MainActivity.actionStart(this, initialSection)
        finish()
    }

    private fun updateAppWidget() {
        val widgetManager = AppWidgetManager.getInstance(this)
        val ids = widgetManager.getAppWidgetIds(ComponentName(this, BalanceAppWidget::class.java))
        if (ids.isNotEmpty()) {
            val updateIntent = Intent()
            updateIntent.putExtra(UiConstants.EXTRA_ID, ids)
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            sendBroadcast(updateIntent)
        }
    }

    private fun startInitializeWizard() {
        FirstStartActivity.actionStart(this)
        finish()
    }

    companion object {

        fun createIntent(context: Context, section: MainSection? = null): Intent {
            val intent = Intent(context, SplashActivity::class.java)
            if (section != null) intent.putExtra(UiConstants.EXTRA_TYPE, section.name)

            return intent
        }
    }
}
