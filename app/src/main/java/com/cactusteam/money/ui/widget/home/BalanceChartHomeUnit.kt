package com.cactusteam.money.ui.widget.home

import android.text.format.DateUtils
import android.view.View
import com.cactusteam.money.R
import com.cactusteam.money.data.filter.AllowAllTransactionFilter
import com.cactusteam.money.data.model.BalanceData
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.activity.BalanceReportActivity
import com.cactusteam.money.ui.fragment.HomeFragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import java.util.*

/**
 * @author vpotapenko
 */
class BalanceChartHomeUnit(homeFragment: HomeFragment) : BaseHomeUnit(homeFragment) {

    private var chartProgress: View? = null
    private var lineChart: LineChart? = null

    override fun getLayoutRes(): Int {
        return R.layout.fragment_home_balance_chart_unit
    }

    override fun initializeView() {
        getView()!!.findViewById(R.id.title).setOnClickListener { showBalanceReport() }

        chartProgress = getView()!!.findViewById(R.id.chart_progress)

        lineChart = getView()!!.findViewById(R.id.chart) as LineChart
        lineChart!!.description = null
        lineChart!!.setNoDataText(homeFragment.getString(R.string.no_data))
        lineChart!!.setDrawGridBackground(false)

        lineChart!!.setTouchEnabled(false)
        lineChart!!.isDragEnabled = false
        lineChart!!.setScaleEnabled(true)
        lineChart!!.setPinchZoom(true)
        lineChart!!.isDoubleTapToZoomEnabled = false
        lineChart!!.isHighlightPerDragEnabled = false
        lineChart!!.isHighlightPerTapEnabled = true
        lineChart!!.isDragDecelerationEnabled = false

        val xAxis = lineChart!!.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)

        val leftAxis = lineChart!!.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.setDrawLabels(false)
    }

    private fun showBalanceReport() {
        BalanceReportActivity.actionStart(homeFragment.activity)
    }

    override fun update() {
        loadData()
    }

    private fun loadData() {
        showProgress()
        val s = homeFragment.dataManager.reportService
                .getBalanceData(AllowAllTransactionFilter.instance, false)
                .subscribe(
                        { r -> showData(r) },
                        { e ->
                            hideProgress()
                            homeFragment.showError(e.message)
                        },
                        { hideProgress() }
                )
        homeFragment.compositeSubscription.add(s)
    }

    private fun showProgress() {
        lineChart!!.visibility = View.GONE
        chartProgress!!.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        lineChart!!.visibility = View.VISIBLE
        chartProgress!!.visibility = View.GONE
    }

    private fun showData(dataList: List<BalanceData>) {
        lineChart!!.data = buildDataset(dataList)
        lineChart!!.legend.textSize = 16f

        lineChart!!.animateX(2500, Easing.EasingOption.EaseInOutQuart)

        val l = lineChart!!.legend
        l.form = Legend.LegendForm.LINE
    }

    @Suppress("DEPRECATION")
    private fun buildDataset(dataList: List<BalanceData>): LineData {
        val xVals: MutableMap<Float, String> = mutableMapOf()

        val profitEntries = ArrayList<Entry>()
        val expenseEntries = ArrayList<Entry>()
        val incomeEntries = ArrayList<Entry>()

        for (i in dataList.indices) {
            val data = dataList[i]
            profitEntries.add(Entry(i.toFloat(), data.profit.toFloat(), data))
            expenseEntries.add(Entry(i.toFloat(), data.expense.toFloat(), data))
            incomeEntries.add(Entry(i.toFloat(), data.income.toFloat(), data))

            if (i % 3 == 0) {
                val periodStr = DateUtils.formatDateRange(homeFragment.activity, data.from.time, data.to.time, DateUtils.FORMAT_SHOW_DATE)
                xVals[i.toFloat()] = periodStr
            }
        }

        lineChart!!.xAxis.valueFormatter = IAxisValueFormatter { value, axis ->
            val s = xVals[value]
            s ?: ""
        }

        val profitDataSet = LineDataSet(profitEntries, homeFragment.getString(R.string.profit))
        profitDataSet.color = homeFragment.resources.getColor(R.color.profit_chart_line)
        profitDataSet.setCircleColor(homeFragment.resources.getColor(R.color.profit_chart_line))
        profitDataSet.setCircleColorHole(homeFragment.resources.getColor(R.color.profit_chart_line))
        profitDataSet.circleRadius = 3f
        profitDataSet.setDrawValues(false)
        profitDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val expenseDataSet = LineDataSet(expenseEntries, homeFragment.getString(R.string.expense_label))
        expenseDataSet.color = homeFragment.resources.getColor(R.color.expense_chart_line)
        expenseDataSet.setCircleColor(homeFragment.resources.getColor(R.color.expense_chart_line))
        expenseDataSet.setCircleColorHole(homeFragment.resources.getColor(R.color.expense_chart_line))
        expenseDataSet.circleRadius = 3f
        expenseDataSet.setDrawValues(false)
        expenseDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val incomeDataSet = LineDataSet(incomeEntries, homeFragment.getString(R.string.income_label))
        incomeDataSet.color = homeFragment.resources.getColor(R.color.income_chart_line)
        incomeDataSet.setCircleColor(homeFragment.resources.getColor(R.color.income_chart_line))
        incomeDataSet.setCircleColorHole(homeFragment.resources.getColor(R.color.income_chart_line))
        incomeDataSet.circleRadius = 3f
        incomeDataSet.setDrawValues(false)
        incomeDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        return LineData(expenseDataSet, incomeDataSet, profitDataSet)
    }

    override val shortName: String
        get() = UiConstants.BALANCE_CHART_BLOCK
}
