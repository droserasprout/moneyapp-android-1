package com.cactusteam.money.ui.format

import android.text.format.DateUtils

import java.util.Date

/**
 * @author vpotapenko
 */
class RelativeDateTimeFormatter : DateTimeFormatter() {

    override fun format(date: Date): CharSequence {
        return DateUtils.getRelativeTimeSpanString(date.time)
    }
}
