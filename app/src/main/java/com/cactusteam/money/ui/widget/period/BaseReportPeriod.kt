package com.cactusteam.money.ui.widget.period

import android.content.Context
import android.text.format.DateFormat
import java.util.*

/**
 * @author vpotapenko
 */
abstract class BaseReportPeriod(protected val context: Context) : IReportPeriod {

    protected var _startDate: Date? = null
    protected var _endDate: Date? = null

    private val dateFormat: java.text.DateFormat

    init {
        dateFormat = DateFormat.getDateFormat(context)
    }

    protected abstract fun calculatePeriod()

    override fun getPeriod(): String? {
        checkPeriod()

        return String.format(DATE_PATTERN, dateFormat.format(_startDate), dateFormat.format(_endDate))
    }

    private fun checkPeriod() {
        if (_startDate == null || _endDate == null)
            calculatePeriod()
    }

    override fun getStartDate(): Date? {
        checkPeriod()
        return _startDate
    }

    override fun getEndDate(): Date? {
        checkPeriod()
        return _endDate
    }

    override fun isCustom(): Boolean {
        return false
    }

    companion object {

        private val DATE_PATTERN = "%s - %s"
    }
}
