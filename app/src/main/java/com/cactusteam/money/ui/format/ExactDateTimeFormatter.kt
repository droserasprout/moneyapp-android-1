package com.cactusteam.money.ui.format

import android.content.Context
import android.text.format.DateFormat

import java.util.Date

/**
 * @author vpotapenko
 */
class ExactDateTimeFormatter(context: Context) : DateTimeFormatter() {

    private val dateFormat: java.text.DateFormat
    private val timeFormat: java.text.DateFormat

    private var dateAtFirst: Boolean = false

    init {
        dateFormat = DateFormat.getDateFormat(context)
        timeFormat = DateFormat.getTimeFormat(context)
    }

    fun setDateAtFirst(dateAtFirst: Boolean) {
        this.dateAtFirst = dateAtFirst
    }

    override fun format(date: Date): CharSequence {
        return if (dateAtFirst)
            String.format(DATE_FORMAT, dateFormat.format(date), timeFormat.format(date))
        else
            String.format(DATE_FORMAT, timeFormat.format(date), dateFormat.format(date))
    }

    companion object {

        private val DATE_FORMAT = "%s, %s"
    }
}
