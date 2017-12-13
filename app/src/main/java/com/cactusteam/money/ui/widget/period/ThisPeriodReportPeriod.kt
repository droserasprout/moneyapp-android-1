package com.cactusteam.money.ui.widget.period

import android.content.Context
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp

/**
 * @author vpotapenko
 */
class ThisPeriodReportPeriod(context: Context) : BaseReportPeriod(context) {

    override fun calculatePeriod() {
        val current = MoneyApp.instance.appPreferences.period.current
        _startDate = current.first
        _endDate = current.second
    }

    override fun getTitle(): String {
        return context.getString(R.string.this_period)
    }
}
