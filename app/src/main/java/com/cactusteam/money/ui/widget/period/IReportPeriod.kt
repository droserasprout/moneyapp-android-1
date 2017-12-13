package com.cactusteam.money.ui.widget.period

import java.util.Date

/**
 * @author vpotapenko
 */
interface IReportPeriod {

    fun getTitle(): String

    fun getPeriod(): String?

    fun getStartDate(): Date?

    fun getEndDate(): Date?

    fun isCustom(): Boolean
}
