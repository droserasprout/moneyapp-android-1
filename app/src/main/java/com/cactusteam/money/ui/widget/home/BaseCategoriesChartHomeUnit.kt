package com.cactusteam.money.ui.widget.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.report.CategoriesReportData
import com.cactusteam.money.data.report.CategoriesReportItem
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.CategoriesReportActivity
import com.cactusteam.money.ui.fragment.HomeFragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.util.*

/**
 * @author vpotapenko
 */
abstract class BaseCategoriesChartHomeUnit(homeFragment: HomeFragment, private val type: Int) : BaseHomeUnit(homeFragment) {

    private var colors: ArrayList<Int>? = null

    private var chartProgress: View? = null
    private var chartContainer: View? = null

    private var noDataView: View? = null
    private var chartView: PieChart? = null
    private var itemsContainer: LinearLayout? = null

    override fun initializeView() {
        initializeColors()

        getView()!!.findViewById(R.id.title).setOnClickListener { showCategoriesReport() }

        chartProgress = getView()!!.findViewById(R.id.chart_progress)
        chartContainer = getView()!!.findViewById(R.id.chart_container)

        noDataView = getView()!!.findViewById(R.id.no_data)

        chartView = getView()!!.findViewById(R.id.chart) as PieChart
        chartView!!.description = null
        chartView!!.setUsePercentValues(true)
        chartView!!.setEntryLabelColor(Color.BLACK)

        itemsContainer = getView()!!.findViewById(R.id.items_container) as LinearLayout
    }

    @Suppress("DEPRECATION")
    private fun initializeColors() {
        colors = ArrayList<Int>()
        for (color in UiConstants.UI_COLORS) {

            colors!!.add(homeFragment.resources.getColor(color))
        }

        if (type == Category.INCOME) {
            Collections.reverse(colors!!)
        }
    }

    private fun showCategoriesReport() {
        val appPreferences = MoneyApp.instance.appPreferences
        val currentPeriod = appPreferences.period.current

        CategoriesReportActivity.actionStart(homeFragment, UiConstants.CATEGORIES_REPORT_REQUEST_CODE, currentPeriod.first, currentPeriod.second, type)
    }

    override fun update() {
        loadData()
    }

    private fun loadData() {
        chartContainer!!.visibility = View.GONE
        noDataView!!.visibility = View.GONE
        chartProgress!!.visibility = View.VISIBLE

        val appPreferences = MoneyApp.instance.appPreferences
        val currentPeriod = appPreferences.period.current

        val s = homeFragment.dataManager.reportService
                .getCategoriesReportData(currentPeriod.first, currentPeriod.second, null, type)
                .subscribe(
                        { r ->
                            chartProgress!!.visibility = View.GONE
                            dataLoaded(r)
                        },
                        { e ->
                            chartProgress!!.visibility = View.GONE
                            homeFragment.showError(e.message)
                        }
                )
        homeFragment.compositeSubscription.add(s)
    }

    private fun dataLoaded(reportData: CategoriesReportData) {
        if (reportData.allItems.isEmpty()) {
            chartContainer!!.visibility = View.GONE
            noDataView!!.visibility = View.VISIBLE
        } else {
            chartContainer!!.visibility = View.VISIBLE
            noDataView!!.visibility = View.GONE

            prepareChart(reportData)
            itemsContainer!!.removeAllViews()
            for (item in reportData.allItems) {
                createGroupView(item)
            }
            setChartTotal(reportData.total)

            chartContainer!!.visibility = View.VISIBLE
        }
    }

    @Suppress("DEPRECATION")
    private fun setChartTotal(amount: Double) {
        val amountStr = UiUtils.formatCurrency(amount, mainCurrencyCode)

        chartView!!.centerText = Html.fromHtml("<strong>" + homeFragment.getString(R.string.total) + "</strong><br/>" + amountStr)
    }

    private fun createGroupView(item: CategoriesReportItem) {
        val view = View.inflate(homeFragment.activity, R.layout.fragment_home_categories_chart_unit_item, null)

        (view.findViewById(R.id.name) as TextView).text = item.group!!.title

        val amountStr = UiUtils.formatCurrency(item.amount, mainCurrencyCode)
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        val marker = view.findViewById(R.id.type_marker) as ImageView
        if (item.color == null) {
            marker.visibility = View.INVISIBLE
        } else {
            marker.visibility = View.VISIBLE
            marker.setImageDrawable(ColorDrawable(item.color!!))
        }

        itemsContainer!!.addView(view)
    }

    @Suppress("DEPRECATION")
    private fun prepareChart(reportData: CategoriesReportData) {
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
                label = homeFragment.getString(R.string.chart_other)

                item.color = homeFragment.resources.getColor(R.color.chart_other)
            }
            chartColors.add(item.color!!)

            val entry = PieEntry(item.amount.toFloat(), label)
            entry.data = item
            yVals.add(entry)
        }

        val dataSet = PieDataSet(yVals, homeFragment.getString(R.string.categories_title))
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        dataSet.colors = chartColors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.BLACK)
        chartView!!.data = data

        chartView!!.legend.isEnabled = false
    }
}
