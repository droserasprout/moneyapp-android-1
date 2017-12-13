package com.cactusteam.money.scheduler

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.WakefulBroadcastReceiver
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.ui.activity.SplashActivity


/**
 * @author vpotapenko
 */
class MoneyAppAlarmService : IntentService("AlarmService") {

    val dataManager: DataManager
        get() = MoneyApp.instance.dataManager

    override fun onHandleIntent(intent: Intent?) {
        if (dataManager.systemService.handleLatePlanningTransactions()) {
            sendNotification(getString(R.string.some_data_need_your_attention))
        }

        if (intent != null) WakefulBroadcastReceiver.completeWakefulIntent(intent)
    }

    private fun sendNotification(msg: String) {
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = PendingIntent.getActivity(this, 0, SplashActivity.createIntent(this), 0)

        val builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_scheduler_message)
                .setContentTitle(getString(R.string.app_name))
                .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                .setAutoCancel(true)
                .setContentText(msg)

        builder.setContentIntent(contentIntent)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        val NOTIFICATION_ID = 10101;
    }
}