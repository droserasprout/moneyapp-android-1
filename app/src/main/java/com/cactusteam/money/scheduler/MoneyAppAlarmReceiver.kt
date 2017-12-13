package com.cactusteam.money.scheduler

import android.content.Context
import android.content.Intent
import android.support.v4.content.WakefulBroadcastReceiver


/**
 * @author vpotapenko
 */
class MoneyAppAlarmReceiver : WakefulBroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val service = Intent(context, MoneyAppAlarmService::class.java)
        startWakefulService(context, service)
    }
}