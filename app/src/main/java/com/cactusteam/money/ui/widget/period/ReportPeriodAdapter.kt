package com.cactusteam.money.ui.widget.period

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.cactusteam.money.R

/**
 * @author vpotapenko
 */
class ReportPeriodAdapter(private val context: Context) : BaseAdapter() {

    private val periods: List<IReportPeriod> = listOf(
            ThisPeriodReportPeriod(context),
            TodayReportPeriod(context),
            ThisWeekReportPeriod(context),
            ThisMonthReportPeriod(context),
            LastPeriodReportPeriod(context),
            LastWeekReportPeriod(context),
            LastMonthReportPeriod(context),
            Last30DayReportPeriod(context),
            ThisYearReportPeriod(context),
            CustomReportPeriod(context)
    )

    override fun getCount(): Int {
        return periods.size
    }

    override fun getItem(position: Int): IReportPeriod {
        return periods[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: View.inflate(context, R.layout.view_report_period_type, null)

        val reportPeriod = getItem(position)
        (view.findViewById(R.id.title) as TextView).text = reportPeriod.getTitle()

        val descriptionView = view.findViewById(R.id.description) as TextView
        if (reportPeriod.isCustom()) {
            descriptionView.visibility = View.GONE
        } else {
            descriptionView.visibility = View.VISIBLE
            descriptionView.text = reportPeriod.getPeriod()
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: View.inflate(context, android.R.layout.simple_list_item_1, null)

        val reportPeriod = getItem(position)
        (view.findViewById(android.R.id.text1) as TextView).text = reportPeriod.getTitle()

        return view
    }
}
