package com.cactusteam.money.ui.widget.period

import android.content.Context
import com.cactusteam.money.R
import com.cactusteam.money.data.period.Period

/**
 * @author vpotapenko
 */
class LastWeekReportPeriod(context: Context) : BaseReportPeriod(context) {

    override fun calculatePeriod() {
        val period = Period.getLastWeekPeriod()
        _startDate = period.first
        _endDate = period.second
    }

    override fun getTitle(): String {
        return context.getString(R.string.last_week)
    }
}
