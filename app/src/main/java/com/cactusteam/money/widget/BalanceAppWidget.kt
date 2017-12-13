package com.cactusteam.money.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler

import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.IBalanceListener
import com.cactusteam.money.ui.UiConstants

class BalanceAppWidget : AppWidgetProvider(), IBalanceListener {

    private var handler: Handler? = null
    private var context: Context? = null

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (handler == null) {
            MoneyApp.instance.dataManager.addBalanceListener(this)
            handler = Handler()
            this.context = context
        }

        updateWidget()
    }

    override fun onDisabled(context: Context) {
        MoneyApp.instance.dataManager.removeBalanceListener(this)
        super.onDisabled(context)
    }

    override fun balanceChanged() {
        postUpdateAppWidget()
    }

    private fun postUpdateAppWidget() {
        handler!!.post { updateWidget() }
    }

    private fun updateWidget() {
        context!!.startService(Intent(context, BalanceWidgetUpdateService::class.java))
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.hasExtra(UiConstants.EXTRA_ID) ?: false) {
            val ids = intent!!.extras.getIntArray(UiConstants.EXTRA_ID)
            onUpdate(context, AppWidgetManager.getInstance(context), ids)
        }
    }
}

