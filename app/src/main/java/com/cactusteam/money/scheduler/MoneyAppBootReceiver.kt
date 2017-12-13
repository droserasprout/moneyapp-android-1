package com.cactusteam.money.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cactusteam.money.app.MoneyApp

/**
 * @author vpotapenko
 */
class MoneyAppBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.BOOT_COMPLETED") {
            MoneyApp.instance.scheduler.updateAlarm()
        }
    }
}