package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.filter.CategoryTransactionFilter
import com.cactusteam.money.data.model.CategoryPeriodData
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.util.*

/**
 * @author vpotapenko
 */
class CategoryReportActivity : BaseDataActivity("CategoryReportActivity") {

    private var mainCurrencyCode: String? = null

    private var categoryId: Long = 0
    private var categoryType: Int = 0
    private var categoryName: String? = null

    private var chart: BarChart? = null
    private var averageAmountView: TextView? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.SUBCATEGORY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)

                loadData()
            }
            chart!!.highlightValues(null)
        } else if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)

                loadData()
            }
            chart!!.highlightValues(null)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_report)

        mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode

        initializeToolbar()
        updateToolbarColor()
        title = categoryName

        initializeViewProgress()

        averageAmountView = findViewById(R.id.average_amount) as TextView

        chart = findViewById(R.id.chart) as BarChart
        chart!!.description = null
        chart!!.setDrawBarShadow(false)
        chart!!.setDrawValueAboveBar(true)
        chart!!.setDrawGridBackground(false)
        chart!!.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight) {
                showDetails(e.data as CategoryPeriodData)
            }

            override fun onNothingSelected() {
                // do nothing
            }
        })
        loadData()
    }

    private fun loadData() {
        showProgress()
        val s = dataManager.categoryService
                .getCategoryPeriodsData(categoryId, PERIOD_NUMBER, true)
                .subscribe(
                        { r ->
                            hideProgress()
                            showAmountsChart(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        })
        compositeSubscription.add(s)
    }

    @Suppress("DEPRECATION")
    private fun showAmountsChart(periods: List<CategoryPeriodData>) {
        val xValues = ArrayMap<Float, String>()
        val yValues = ArrayList<BarEntry>()

        Collections.reverse(periods)
        for (i in periods.indices) {
            val data = periods[i]

            if (i % 3 == 0) {
                val periodStr = DateUtils.formatDateRange(this, data.from!!.time, data.to!!.time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
                xValues.put(i.toFloat(), periodStr)
            }

            yValues.add(BarEntry(i.toFloat(), data.amount.toFloat(), data))
        }

        val set1 = BarDataSet(yValues, categoryName)
        set1.color = resources.getColor(if (categoryType == Category.EXPENSE) R.color.toolbar_expense_color else R.color.toolbar_income_color)

        val dataSets = ArrayList<IBarDataSet>()
        dataSets.add(set1)

        val data = BarData(dataSets)
        data.setValueTextSize(10f)

        val xAxis = chart!!.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IAxisValueFormatter { value, axis ->
            val s = xValues[value]
            s ?: ""
        }

        chart!!.axisLeft.setDrawLabels(false)
        chart!!.legend.isEnabled = false

        val rightAxis = chart!!.axisRight
        rightAxis.setDrawGridLines(false)
        rightAxis.setLabelCount(8, false)
        rightAxis.spaceTop = 15f

        var averageAmount = periods.sumByDouble { it.amount }

        averageAmount /= periods.size.toDouble()
        averageAmount = DataUtils.round(averageAmount, 2)
        val amountStr = UiUtils.formatCurrency(averageAmount, mainCurrencyCode)
        averageAmountView!!.text = getString(R.string.average_amount_pattern, amountStr)

        val l = LimitLine(DataUtils.toFloat(averageAmount), getString(R.string.average))
        val limitColor = resources.getColor(if (categoryType == Category.EXPENSE) R.color.toolbar_expense_color else R.color.toolbar_income_color)
        l.lineColor = UiUtils.darkenColor(limitColor)
        l.textSize = 10f
        rightAxis.addLimitLine(l)

        chart!!.animateY(700)
        chart!!.visibility = View.VISIBLE
        chart!!.data = data
    }

    private fun showDetails(data: CategoryPeriodData) {
        if (data.subcategoryAmounts.isEmpty()) {
            showTransactionsActivity(data)
        } else {
            showSubcategoriesReportActivity(data)
        }
    }

    private fun showSubcategoriesReportActivity(periodData: CategoryPeriodData) {
        SubcategoriesReportActivity.actionStart(this, UiConstants.SUBCATEGORIES_REPORT_REQUEST_CODE, categoryId, categoryName!!, periodData.from!!, periodData.to!!, categoryType)
    }

    private fun showTransactionsActivity(periodData: CategoryPeriodData) {
        val filter = CategoryTransactionFilter(categoryId)
        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                filter,
                getString(R.string.category_pattern, categoryName),
                periodData.from,
                periodData.to)
    }

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun updateToolbarColor() {
        val from = ColorDrawable(resources.getColor(R.color.color_primary))
        val to = ColorDrawable(resources.getColor(
                if (categoryType == Category.EXPENSE) R.color.toolbar_expense_color else R.color.toolbar_income_color))

        if (DataUtils.hasLollipop()) {
            window.statusBarColor = UiUtils.darkenColor(to.color)
        }

        val drawable = TransitionDrawable(arrayOf<Drawable>(from, to))
        //noinspection deprecation
        toolbar!!.setBackgroundDrawable(drawable)
        drawable.startTransition(UiConstants.TOOLBAR_TRANSITION_DURATION)
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                categoryId = extras.getLong(UiConstants.EXTRA_ID)
            }
            if (extras.containsKey(UiConstants.EXTRA_NAME)) {
                categoryName = extras.getString(UiConstants.EXTRA_NAME)
            }
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                categoryType = extras.getInt(UiConstants.EXTRA_TYPE)
            }
        }
    }

    companion object {

        private val PERIOD_NUMBER = 12

        fun actionStart(fragment: Fragment, requestCode: Int, categoryId: Long, categoryName: String?, type: Int) {
            val intent = Intent(fragment.activity, CategoryReportActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, categoryId)
            intent.putExtra(UiConstants.EXTRA_NAME, categoryName)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)

            fragment.startActivityForResult(intent, requestCode)
        }

        fun actionStart(activity: Activity, requestCode: Int, categoryId: Long, categoryName: String?, type: Int) {
            val intent = Intent(activity, CategoryReportActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, categoryId)
            intent.putExtra(UiConstants.EXTRA_NAME, categoryName)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)

            activity.startActivityForResult(intent, requestCode)
        }
    }
}
