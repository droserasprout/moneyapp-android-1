package com.cactusteam.money.ui.format

import android.content.Context

import java.util.Date

/**
 * @author vpotapenko
 */
abstract class DateTimeFormatter {

    abstract fun format(date: Date): CharSequence

    companion object {

        val RELATIVE = 0
        val EXACT = 1
        val DETAILED = 2
        val EXACT2 = 3
        val DETAILED2 = 4

        fun create(mode: Int, context: Context): DateTimeFormatter {
            when (mode) {
                RELATIVE -> return RelativeDateTimeFormatter()
                EXACT -> return ExactDateTimeFormatter(context)
                EXACT2 -> {
                    val exactDateTimeFormatter = ExactDateTimeFormatter(context)
                    exactDateTimeFormatter.setDateAtFirst(true)
                    return exactDateTimeFormatter
                }
                DETAILED -> return DetailedDateTimeFormatter(context)
                DETAILED2 -> {
                    val detailedDateTimeFormatter = DetailedDateTimeFormatter(context)
                    detailedDateTimeFormatter.setDateAtFirst(true)
                    return detailedDateTimeFormatter
                }
                else -> throw RuntimeException("Must not be here")
            }
        }
    }
}
