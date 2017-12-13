package com.cactusteam.money.ui.widget.period

import android.content.Context
import com.cactusteam.money.R
import java.util.*

/**
 * @author vpotapenko
 */
class CustomReportPeriod(private val context: Context) : IReportPeriod {

    override fun getTitle(): String {
        return context.getString(R.string.custom_period)
    }

    override fun getPeriod(): String? {
        return null
    }

    override fun getStartDate(): Date? {
        return null
    }

    override fun getEndDate(): Date? {
        return null
    }

    override fun isCustom(): Boolean {
        return true
    }
}
