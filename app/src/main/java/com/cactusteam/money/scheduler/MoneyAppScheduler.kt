package com.cactusteam.money.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.Transaction
import java.util.*


/**
 * @author vpotapenko
 */
class MoneyAppScheduler(val context: Context) {

    val dataManager: DataManager
        get() = MoneyApp.instance.dataManager

    var alarmIntent: PendingIntent? = null

    val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    fun updateAlarm() {
        cancelAlarm()

        val time = findPlanningTransactionTime()
        if (time != null) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, 3) // for checking
            scheduleAlarm(calendar.timeInMillis)
        }
    }

    private fun findPlanningTransactionTime(): Date? {
        val now = java.util.Date()
        val list = dataManager.transactionService
                .newListTransactionsBuilder()
                .putStatus(Transaction.STATUS_PLANNING)
                .listInternal()
                .filter { it.date.after(now) }
                .sortedBy { it.date }
        return if (list.isNotEmpty()) {
            list[0].date
        } else {
            null
        }
    }

    private fun scheduleAlarm(timeInMillis: Long) {
        val intent = Intent(context, MoneyAppAlarmReceiver::class.java)
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, alarmIntent)

        val receiver = ComponentName(context, MoneyAppBootReceiver::class.java)
        val pm = context.packageManager

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)
    }

    private fun cancelAlarm() {
        if (alarmIntent != null) {
            alarmManager.cancel(alarmIntent)

            val receiver = ComponentName(context, MoneyAppBootReceiver::class.java)
            val pm = context.packageManager

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)

            alarmIntent = null
        }
    }
}