package com.cactusteam.money.ui.widget.period

import android.content.Context
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp

/**
 * @author vpotapenko
 */
class LastPeriodReportPeriod(context: Context) : BaseReportPeriod(context) {

    override fun calculatePeriod() {
        val period = MoneyApp.instance.appPreferences.period
        var p = period.current
        p = period.getPrevious(p)

        _startDate = p.first
        _endDate = p.second
    }

    override fun getTitle(): String {
        return context.getString(R.string.last_period)
    }
}
