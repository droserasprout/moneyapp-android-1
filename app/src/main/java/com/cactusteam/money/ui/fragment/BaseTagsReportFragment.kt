package com.cactusteam.money.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.report.TagsReportData
import com.cactusteam.money.data.report.TagsReportItem
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.TagsReportActivity
import com.cactusteam.money.ui.grouping.TransactionsGrouper
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.DecimalFormat
import java.util.*

/**
 * @author vpotapenko
 */
abstract class BaseTagsReportFragment(private val type: Int) : BaseFragment() {

    var isKilled: Boolean = false

    private var amountsChart: PieChart? = null
    private var tagsContainer: LinearLayout? = null

    private var colors: ArrayList<Int>? = null
    private var mainCurrencyCode: String? = null

    private var chartContainer: View? = null
    private var noDataView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tags_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isKilled = false

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        chartContainer = view.findViewById(R.id.chart_container)
        noDataView = view.findViewById(R.id.no_data)

        amountsChart = view.findViewById(R.id.amounts_chart) as PieChart
        amountsChart!!.description = null
        amountsChart!!.setUsePercentValues(true)
        amountsChart!!.setEntryLabelColor(Color.BLACK)
        amountsChart!!.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

            override fun onValueSelected(e: Entry, h: Highlight) {
                val item = e.data as TagsReportItem
                if (item.group != null) showDetails(item.group)
            }

            override fun onNothingSelected() {
                // do nothing
            }
        })

        tagsContainer = view.findViewById(R.id.tags_container) as LinearLayout
        mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode

        initializeColors()

        loadData()
    }

    override fun onDestroyView() {
        isKilled = true
        super.onDestroyView()
    }

    @Suppress("DEPRECATION")
    private fun initializeColors() {
        colors = ArrayList<Int>()
        for (color in UiConstants.UI_COLORS) {
            colors!!.add(resources.getColor(color))
        }

        if (type == Category.INCOME) {
            Collections.reverse(colors!!)
        }
    }

    val reportActivity: TagsReportActivity
        get() = activity as TagsReportActivity

    private fun showDetails(group: TransactionsGrouper.Group) {
        reportActivity.showTransactionsActivity(group, type)
    }

    fun loadData() {
        showProgress()
        val reportActivity = reportActivity
        val s = dataManager.reportService
                .getTagsReportData(
                        reportActivity.fromTime,
                        reportActivity.toTime,
                        reportActivity.filter,
                        type
                )
                .subscribe(
                        { r ->
                            hideProgress()
                            dataLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun dataLoaded(reportData: TagsReportData) {
        if (reportData.allItems.isEmpty()) {
            chartContainer!!.visibility = View.GONE
            noDataView!!.visibility = View.VISIBLE
        } else {
            chartContainer!!.visibility = View.VISIBLE
            noDataView!!.visibility = View.GONE

            prepareChart(reportData)
            tagsContainer!!.removeAllViews()
            for (item in reportData.allItems) {
                createGroupView(item)
            }

            val group = TransactionsGrouper.Group(-1, getString(R.string.total))
            if (type == Category.EXPENSE) {
                group.expense = reportData.total
            } else {
                group.income = reportData.total
            }
            createTotalView(group)
            setChartTotal(reportData.total)
        }
    }

    @Suppress("DEPRECATION")
    private fun setChartTotal(amount: Double) {
        val amountStr = UiUtils.formatCurrency(amount, mainCurrencyCode)
        amountsChart!!.centerText = Html.fromHtml("<strong>" + getString(R.string.total) + "</strong><br/>" + amountStr)
    }

    private fun createTotalView(group: TransactionsGrouper.Group) {
        val view = View.inflate(activity, R.layout.activity_tags_report_total, null)

        view.findViewById(R.id.list_item).setOnClickListener { showDetails(group) }

        (view.findViewById(R.id.name) as TextView).text = group.title

        val amountStr = UiUtils.formatCurrency(getGroupAmount(group), mainCurrencyCode)
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        tagsContainer!!.addView(view)
    }

    private fun createGroupView(item: TagsReportItem) {
        val view = View.inflate(activity, R.layout.activity_tags_report_item, null)

        view.findViewById(R.id.list_item).setOnClickListener { showDetails(item.group) }

        (view.findViewById(R.id.name) as TextView).text = item.group.title

        var amountStr = UiUtils.formatCurrency(item.amount, mainCurrencyCode)
        if (item.percent != null) {
            amountStr = amountStr + " (" + percentFormat.format(item.percent) + " %)"
        }
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        val marker = view.findViewById(R.id.type_marker)
        if (item.color == null) {
            marker.visibility = View.INVISIBLE
        } else {
            marker.visibility = View.VISIBLE
            marker.setBackgroundColor(item.color!!)
        }
        tagsContainer!!.addView(view)
    }

    @Suppress("DEPRECATION")
    private fun prepareChart(reportData: TagsReportData) {
        val yVals = ArrayList<PieEntry>()
        val chartColors = ArrayList<Int>()

        for (i in reportData.chartItems.indices) {
            val item = reportData.chartItems[i]
            val label: String
            if (item.group != null) {
                label = item.group.title
                item.color = colors!![chartColors.size]
            } else {
                // other group
                label = getString(R.string.chart_other)
                item.color = resources.getColor(R.color.chart_other)
            }
            chartColors.add(item.color!!)

            val entry = PieEntry(item.amount.toFloat(), label)
            entry.data = item
            yVals.add(entry)
        }

        val dataSet = PieDataSet(yVals, getString(R.string.categories_title))
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        dataSet.colors = chartColors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.BLACK)
        amountsChart!!.data = data

        // undo all highlights
        amountsChart!!.highlightValues(null)
        amountsChart!!.legend.isEnabled = false

        amountsChart!!.invalidate()
    }

    private fun getGroupAmount(group: TransactionsGrouper.Group): Double {
        return if (type == Category.EXPENSE) group.expense else group.income
    }

    companion object {

        private val percentFormat = DecimalFormat("###,###,##0.0")
    }
}
