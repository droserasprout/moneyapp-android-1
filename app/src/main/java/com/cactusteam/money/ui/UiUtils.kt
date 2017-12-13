package com.cactusteam.money.ui

import android.content.Context
import android.graphics.Color
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.data.period.Period
import java.text.DateFormatSymbols
import java.util.*

/**
 * @author vpotapenko
 */
object UiUtils {

    fun darkenColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f
        return Color.HSVToColor(hsv)
    }

    fun formatDataObject(context: Context, type: Int, obj: Any?): String {
        if (type == DataConstants.CATEGORY_TYPE) {
            val category = obj as Category
            return context.getString(R.string.category_pattern, category.name)
        } else if (type == DataConstants.SUBCATEGORY_TYPE) {
            val subcategory = obj as Subcategory
            return context.getString(R.string.subcategory_pattern, subcategory.name)
        } else if (type == DataConstants.TAG_TYPE) {
            val tag = obj as Tag
            return context.getString(R.string.tag_pattern, tag.name)
        } else if (type == DataConstants.ACCOUNT_TYPE) {
            val account = obj as Account
            return context.getString(R.string.account_pattern, account.name)
        } else if (type == DataConstants.BUDGET_TYPE) {
            val plan = obj as BudgetPlan
            return context.getString(R.string.budget_pattern, plan.name)
        } else if (type == DataConstants.CATEGORY_NAME_TYPE) {
            return context.getString(R.string.category_pattern, obj.toString())
        } else {
            return ""
        }
    }

    fun formatCurrency(amount: Double, currencyCode: String?): String {
        val currencyFormatter = MoneyApp.instance.currencyManager.currencyFormatter
        return if (currencyCode != null) currencyFormatter.formatCurrency(amount, currencyCode) else amount.toString()
    }

    fun formatPeriod(period: Period, context: Context): String {
        when (period.type) {
            Period.MONTH_TYPE -> return context.getString(R.string.period_month_pattern, period.startFrom.toString())
            Period.WEEK_TYPE -> return context.getString(R.string.period_week_pattern, getWeekDayName(period.startFrom))
            else -> return ""
        }
    }

    private fun getWeekDayName(day: Int): String {
        val symbols = DateFormatSymbols()
        val weekdays = symbols.weekdays
        try {
            return weekdays[day]
        } catch (e: Exception) {
            return ""
        }

    }

    val weekDays: List<WeekDay>
        get() {
            val result = ArrayList<WeekDay>()

            val symbols = DateFormatSymbols()
            val weekdays = symbols.weekdays

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            do {
                val index = calendar.get(Calendar.DAY_OF_WEEK)
                result.add(WeekDay(index, weekdays[index]))
                calendar.add(Calendar.DATE, 1)

            } while (calendar.get(Calendar.DAY_OF_WEEK) != calendar.firstDayOfWeek)

            return result
        }
}
