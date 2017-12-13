package com.cactusteam.money.widget

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.model.Totals
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.NewTransactionActivity
import com.cactusteam.money.ui.activity.SplashActivity
import rx.subscriptions.CompositeSubscription

/**
 * @author vpotapenko
 */
class BalanceWidgetUpdateService : Service() {

    var compositeSubscription: CompositeSubscription? = null

    private val dataManager: DataManager
        get() {
            return MoneyApp.instance.dataManager
        }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        compositeSubscription = CompositeSubscription()

        loadData()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun loadData() {
        val s = dataManager.accountService
                .getTotals()
                .subscribe(
                        { r -> showData(r) },
                        Throwable::printStackTrace
                )
        compositeSubscription!!.add(s)
    }

    private fun showData(totals: Totals) {
        val views = RemoteViews(packageName, R.layout.widget_balance)

        val amountStr = UiUtils.formatCurrency(totals.total, totals.mainCurrencyCode)
        views.setTextViewText(R.id.total, amountStr)

        val expenseIntent = NewTransactionActivity.ActionBuilder().type(Transaction.EXPENSE).createIntent(this)
        expenseIntent.action = "NewTransaction"
        views.setOnClickPendingIntent(R.id.expense_btn, PendingIntent.getActivity(this, 0, expenseIntent, 0))

        val mainIntent = SplashActivity.createIntent(this)
        mainIntent.action = "Main"
        views.setOnClickPendingIntent(R.id.open_btn, PendingIntent.getActivity(this, 0, mainIntent, 0))

        val mainAccountsIntent = SplashActivity.createIntent(this, MainSection.ACCOUNTS)
        mainIntent.action = MainSection.ACCOUNTS.name
        views.setOnClickPendingIntent(R.id.balance_container, PendingIntent.getActivity(this, 0, mainAccountsIntent, 0))

        val widget = ComponentName(this, BalanceAppWidget::class.java)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetManager.updateAppWidget(widget, views)
    }

    override fun onDestroy() {
        compositeSubscription?.unsubscribe()
        compositeSubscription = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
