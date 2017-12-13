package com.cactusteam.money.ui.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.report.CategoriesReportData
import com.cactusteam.money.data.report.CategoriesReportItem
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiObjectRef
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.CategoriesReportActivity
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
abstract class BaseCategoriesReportFragment(private val type: Int) : BaseFragment() {

    var isKilled: Boolean = false

    private var amountsChart: PieChart? = null
    private var categoriesContainer: LinearLayout? = null

    private var colors: ArrayList<Int>? = null
    private var mainCurrencyCode: String? = null

    private var chartContainer: View? = null
    private var noDataView: View? = null

    private val icons = ArrayMap<String, UiObjectRef>()
    private var mockBitmap: Bitmap? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_categories_report, container, false)
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
                val item = e.data as CategoriesReportItem
                if (item.group != null) showDetails(item.group)
            }

            override fun onNothingSelected() {
                // do nothing
            }
        })

        categoriesContainer = view.findViewById(R.id.categories_container) as LinearLayout
        mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode

        initializeColors()

        loadData()
    }

    override fun onDestroyView() {
        for ((key, value) in icons) {
            if (value.ref != null) value.getRefAs(Bitmap::class.java).recycle()
        }
        icons.clear()

        if (mockBitmap != null) {
            mockBitmap!!.recycle()
            mockBitmap = null
        }
        isKilled = true
        super.onDestroyView()
    }

    private fun getMockBitmap(): Bitmap? {
        if (mockBitmap == null) {
            mockBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_mock_category)
        }
        return mockBitmap
    }

    private fun requestCategoryIcon(iconKey: String, imageView: ImageView, color: Int?) {
        val icon = UiObjectRef()
        icons.put(iconKey, icon)

        val s = dataManager.systemService
                .getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.ref = r
                            updateIconView(imageView, r, color)
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun updateIconView(imageView: ImageView, bitmap: Bitmap?, color: Int?) {
        val drawable = BitmapDrawable(resources, bitmap ?: getMockBitmap())
        drawable.setColorFilter(color ?: Color.GRAY, PorterDuff.Mode.SRC_ATOP)
        imageView.setImageDrawable(drawable)

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

    val reportActivity: CategoriesReportActivity
        get() = activity as CategoriesReportActivity

    private fun showDetails(group: TransactionsGrouper.Group) {
        if (group.id < 0 || group.subGroups.isEmpty()) {
            reportActivity.showTransactionsActivity(group, type)
        } else {
            reportActivity.showSubcategoriesReportActivity(group.id, group.title, type)
        }
    }

    fun loadData() {
        showProgress()
        val reportActivity = reportActivity
        val s = dataManager.reportService
                .getCategoriesReportData(
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
                            baseActivity.showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun dataLoaded(reportData: CategoriesReportData) {
        if (reportData.allItems.isEmpty()) {
            chartContainer!!.visibility = View.GONE
            noDataView!!.visibility = View.VISIBLE
        } else {
            chartContainer!!.visibility = View.VISIBLE
            noDataView!!.visibility = View.GONE

            prepareChart(reportData)
            categoriesContainer!!.removeAllViews()
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
        val view = View.inflate(activity, R.layout.activity_categories_report_total, null)

        view.findViewById(R.id.list_item).setOnClickListener { showDetails(group) }

        (view.findViewById(R.id.name) as TextView).text = group.title

        val amountStr = UiUtils.formatCurrency(getGroupAmount(group), mainCurrencyCode)
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        categoriesContainer!!.addView(view)
    }

    private fun createGroupView(item: CategoriesReportItem) {
        val view = View.inflate(activity, R.layout.activity_categories_report_item, null)

        view.findViewById(R.id.list_item).setOnClickListener { showDetails(item.group!!) }

        (view.findViewById(R.id.name) as TextView).text = item.group!!.title

        var amountStr = UiUtils.formatCurrency(item.amount, mainCurrencyCode)
        if (item.percent != null) {
            amountStr = amountStr + " (" + percentFormat.format(item.percent) + " %)"
        }
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        val marker = view.findViewById(R.id.type_marker) as ImageView
        if (item.group.icon == null) {
            updateIconView(marker, null, item.color)
        } else {
            val ref = icons[item.group.icon]
            if (ref == null) {
                requestCategoryIcon(item.group.icon!!, marker, item.color)
            } else if (ref.ref == null) {
                updateIconView(marker, null, item.color)
            } else {
                updateIconView(marker, ref.getRefAs(Bitmap::class.java), item.color)
            }
        }

        val container = view.findViewById(R.id.subcategories_container) as LinearLayout
        if (!item.group.subGroups.isEmpty()) {
            container.visibility = View.VISIBLE

            for (sub in item.group.subGroups) {
                val subView = View.inflate(activity, R.layout.activity_categories_report_subitem, null)
                (subView.findViewById(R.id.name) as TextView).text = sub.title

                amountStr = UiUtils.formatCurrency(getGroupAmount(sub), mainCurrencyCode)
                (subView.findViewById(R.id.amount) as TextView).text = amountStr

                container.addView(subView)
            }
        } else {
            container.visibility = View.GONE
        }

        categoriesContainer!!.addView(view)
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
