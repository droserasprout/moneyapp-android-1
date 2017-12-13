package com.cactusteam.money.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.AllowAllTransactionFilter
import com.cactusteam.money.data.filter.CategoryNameTransactionFilter
import com.cactusteam.money.data.filter.ITransactionFilter
import com.cactusteam.money.data.model.BalanceData
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.FilterFragment
import com.cactusteam.money.ui.grouping.CategoriesReportTransactionsGrouper
import com.cactusteam.money.ui.grouping.TransactionsGrouper
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.util.*

/**
 * @author vpotapenko
 */
class BalanceReportActivity : BaseDataActivity("BalanceReportActivity") {

    private val expenseGrouper = CategoriesReportTransactionsGrouper(Transaction.EXPENSE)
    private val incomeGrouper = CategoriesReportTransactionsGrouper(Transaction.INCOME)

    private var dateFormat: java.text.DateFormat? = null

    private var lineChart: LineChart? = null

    private var descriptionContainer: LinearLayout? = null
    private var nothingSelectedView: View? = null

    private var periodView: TextView? = null
    private var expenseView: TextView? = null
    private var incomeView: TextView? = null
    private var profitView: TextView? = null
    private var balanceView: TextView? = null
    private var balanceTitleView: TextView? = null

    private var filterDescription: TextView? = null
    private var filterContainer: View? = null

    private var categoriesProgress: View? = null
    private var categoriesContainer: LinearLayout? = null

    private var currentBalanceData: BalanceData? = null

    private var filter: ITransactionFilter? = null
    private var lastFilter: FilterFragment.FilterInformation? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            setResult(RESULT_OK)
            loadData()
        } else if (requestCode == UiConstants.CATEGORIES_REPORT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)
                loadData()
            }
        } else if (requestCode == UiConstants.SUBCATEGORIES_REPORT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)
                loadData()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_balance_report, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.filter) {
            showFilter()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION")
    private fun showFilter() {
        FilterFragment.build({ f, filterInfo ->
            lastFilter = filterInfo
            if (f != null) {
                filter = f
                filterDescription!!.text = Html.fromHtml(f.displayName)
                filterContainer!!.visibility = View.VISIBLE

                loadData()
            }
        }, lastFilter).show(fragmentManager, "dialog")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_report)

        initializeToolbar()
        initializeViewProgress()

        dateFormat = DateFormat.getDateFormat(this)

        descriptionContainer = findViewById(R.id.description_container) as LinearLayout
        nothingSelectedView = findViewById(R.id.nothing_selected_description)

        periodView = findViewById(R.id.period) as TextView

        expenseView = findViewById(R.id.expense) as TextView
        findViewById(R.id.expense_container).setOnClickListener { expenseClicked() }
        incomeView = findViewById(R.id.income) as TextView
        findViewById(R.id.income_container).setOnClickListener { incomeClicked() }
        profitView = findViewById(R.id.profit) as TextView

        balanceView = findViewById(R.id.balance) as TextView
        balanceTitleView = findViewById(R.id.balance_title) as TextView

        categoriesProgress = findViewById(R.id.categories_progress_bar)
        categoriesContainer = findViewById(R.id.categories_container) as LinearLayout

        lineChart = findViewById(R.id.chart) as LineChart

        lineChart!!.description = null
        lineChart!!.setNoDataText(getString(R.string.no_data))
        lineChart!!.setDrawGridBackground(false)

        lineChart!!.setTouchEnabled(true)
        lineChart!!.isDragEnabled = true
        lineChart!!.setScaleEnabled(true)
        lineChart!!.setPinchZoom(true)
        lineChart!!.isDoubleTapToZoomEnabled = false
        lineChart!!.isHighlightPerDragEnabled = false
        lineChart!!.isHighlightPerTapEnabled = true
        lineChart!!.isDragDecelerationEnabled = false

        lineChart!!.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onNothingSelected() {
                nothingChartSelected()
            }

            override fun onValueSelected(e: Entry?, h: Highlight?) {
                chartValueSelected(e!!.data as BalanceData)
            }
        })

        val xAxis = lineChart!!.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)

        val leftAxis = lineChart!!.axisLeft
        leftAxis.setDrawGridLines(true)

        filterDescription = findViewById(R.id.filter_description) as TextView
        filterContainer = findViewById(R.id.filter_container)
        filterContainer!!.setOnClickListener {
            filter = null
            filterContainer!!.visibility = View.GONE

            loadData()
        }

        loadData()
    }

    @Suppress("DEPRECATION")
    private fun incomeClicked() {
        if (currentBalanceData == null) return

        val text = filterDescription!!.text
        CategoriesReportActivity.actionStart(this,
                UiConstants.CATEGORIES_REPORT_REQUEST_CODE,
                currentBalanceData!!.from,
                currentBalanceData!!.to,
                Category.INCOME,
                filter,
                if (text is Spanned) Html.toHtml(text) else text.toString())
    }

    @Suppress("DEPRECATION")
    private fun expenseClicked() {
        if (currentBalanceData == null) return

        val text = filterDescription!!.text
        CategoriesReportActivity.actionStart(this,
                UiConstants.CATEGORIES_REPORT_REQUEST_CODE,
                currentBalanceData!!.from,
                currentBalanceData!!.to,
                Category.EXPENSE,
                filter,
                if (text is Spanned) Html.toHtml(text) else text.toString())
    }

    private fun chartValueSelected(data: BalanceData) {
        this.currentBalanceData = data

        nothingSelectedView!!.visibility = View.GONE
        descriptionContainer!!.visibility = View.VISIBLE

        val periodStr = DateUtils.formatDateRange(this, data.from.time, data.to.time, DateUtils.FORMAT_SHOW_DATE)
        periodView!!.text = periodStr

        val mainCurrency = MoneyApp.instance.appPreferences.mainCurrencyCode
        expenseView!!.text = UiUtils.formatCurrency(data.expense, mainCurrency)
        incomeView!!.text = UiUtils.formatCurrency(data.income, mainCurrency)
        profitView!!.text = UiUtils.formatCurrency(data.profit, mainCurrency)
        balanceView!!.text = UiUtils.formatCurrency(data.balance, mainCurrency)
        balanceTitleView!!.text = getString(R.string.balance) + " (" + dateFormat!!.format(data.to) + ")"

        updateCategoriesData(data)
    }

    private fun updateCategoriesData(data: BalanceData) {
        categoriesContainer!!.visibility = View.GONE
        categoriesProgress!!.visibility = View.VISIBLE

        val b = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(data.from)
                .putTo(data.to)
                .putConvertToMain(true)

        val f = filter
        if (f != null) b.putTransactionFilter(f)

        val s = b.list().subscribe(
                { r -> handleTransactions(r) },
                { e ->
                    showError(e.message)
                    categoriesProgress!!.visibility = View.GONE
                },
                {
                    categoriesContainer!!.visibility = View.VISIBLE
                    categoriesProgress!!.visibility = View.GONE
                }
        )
        compositeSubscription.add(s)
    }

    private fun handleTransactions(transactions: List<Transaction>) {
        val expenseGroups = expenseGrouper.group(transactions)
        val incomeGroups = incomeGrouper.group(transactions)

        val mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode
        categoriesContainer!!.removeAllViews()
        if (!expenseGroups.isEmpty()) {
            val view = View.inflate(this, R.layout.activity_balance_categories_title, null)
            (view.findViewById(R.id.title) as TextView).setText(R.string.expense_label)
            categoriesContainer!!.addView(view)

            for (group in expenseGroups) {
                createGroupView(group, mainCurrencyCode, Category.EXPENSE)
            }
        }

        if (!incomeGroups.isEmpty()) {
            if (categoriesContainer!!.childCount > 0) {
                categoriesContainer!!.addView(View.inflate(this, R.layout.horizontal_divider, null))
            }

            val view = View.inflate(this, R.layout.activity_balance_categories_title, null)
            (view.findViewById(R.id.title) as TextView).setText(R.string.income_label)
            categoriesContainer!!.addView(view)

            for (group in incomeGroups) {
                createGroupView(group, mainCurrencyCode, Category.INCOME)
            }
        }

        if (categoriesContainer!!.childCount == 0) {
            categoriesContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
        }
    }

    private fun createGroupView(group: TransactionsGrouper.Group, mainCurrencyCode: String, type: Int) {
        val view: View
        view = View.inflate(this, R.layout.activity_balance_categories_item, null)
        view.findViewById(R.id.list_item).setOnClickListener {
            if (group.subGroups.isEmpty()) {
                showTransactionsActivity(group)
            } else {
                showSubcategoriesActivity(group, type)
            }
        }
        (view.findViewById(R.id.name) as TextView).text = group.title

        var amountStr = UiUtils.formatCurrency(if (type == Category.INCOME) group.income else group.expense, mainCurrencyCode)
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        val container = view.findViewById(R.id.subcategories_container) as LinearLayout
        if (!group.subGroups.isEmpty()) {
            container.visibility = View.VISIBLE

            for (sub in group.subGroups) {
                val subView = View.inflate(this, R.layout.activity_balance_categories_subitem, null)
                (subView.findViewById(R.id.name) as TextView).text = sub.title

                amountStr = UiUtils.formatCurrency(if (type == Category.INCOME) sub.income else sub.expense, mainCurrencyCode)
                (subView.findViewById(R.id.amount) as TextView).text = amountStr

                container.addView(subView)
            }
        } else {
            container.visibility = View.GONE
        }

        categoriesContainer!!.addView(view)
    }

    private fun showSubcategoriesActivity(group: TransactionsGrouper.Group, type: Int) {
        SubcategoriesReportActivity.actionStart(this, UiConstants.SUBCATEGORIES_REPORT_REQUEST_CODE,
                group.id, group.title, currentBalanceData!!.from,
                currentBalanceData!!.to, type)
    }

    fun showTransactionsActivity(group: TransactionsGrouper.Group) {
        if (currentBalanceData == null) return

        val filter = CategoryNameTransactionFilter(group.title, null)
        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                filter,
                getString(R.string.category_pattern, group.title),
                currentBalanceData!!.from,
                currentBalanceData!!.to)
    }

    private fun nothingChartSelected() {
        currentBalanceData = null

        nothingSelectedView!!.visibility = View.VISIBLE
        descriptionContainer!!.visibility = View.GONE
    }

    private fun loadData() {
        showProgress()
        val s = dataManager.reportService
                .getBalanceData(if (filter == null) AllowAllTransactionFilter.instance else filter!!, true)
                .subscribe(
                        { r -> showData(r) },
                        { e ->
                            showError(e.message)
                        },
                        { hideProgress() }
                )
        compositeSubscription.add(s)
    }

    private fun showData(dataList: List<BalanceData>) {
        lineChart!!.highlightValues(null)
        nothingChartSelected()

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
        val balanceEntries = ArrayList<Entry>()

        for (i in dataList.indices) {
            val data = dataList[i]
            profitEntries.add(Entry(i.toFloat(), data.profit.toFloat(), data))
            expenseEntries.add(Entry(i.toFloat(), data.expense.toFloat(), data))
            incomeEntries.add(Entry(i.toFloat(), data.income.toFloat(), data))
            balanceEntries.add(Entry(i.toFloat(), data.balance.toFloat(), data))

            if (i % 3 == 0) {
                val periodStr = DateUtils.formatDateRange(this, data.from.time, data.to.time, DateUtils.FORMAT_SHOW_DATE)
                xVals[i.toFloat()] = periodStr
            }
        }

        lineChart!!.xAxis.valueFormatter = IAxisValueFormatter { value, axis ->
            val s = xVals[value]
            s ?: ""
        }

        val profitDataSet = LineDataSet(profitEntries, getString(R.string.profit))
        profitDataSet.color = resources.getColor(R.color.profit_chart_line)
        profitDataSet.setCircleColor(resources.getColor(R.color.profit_chart_line))
        profitDataSet.setCircleColorHole(resources.getColor(R.color.profit_chart_line))
        profitDataSet.circleRadius = 3f
        profitDataSet.setDrawValues(false)
        profitDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val expenseDataSet = LineDataSet(expenseEntries, getString(R.string.expense_label))
        expenseDataSet.color = resources.getColor(R.color.expense_chart_line)
        expenseDataSet.setCircleColor(resources.getColor(R.color.expense_chart_line))
        expenseDataSet.setCircleColorHole(resources.getColor(R.color.expense_chart_line))
        expenseDataSet.circleRadius = 3f
        expenseDataSet.setDrawValues(false)
        expenseDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val incomeDataSet = LineDataSet(incomeEntries, getString(R.string.income_label))
        incomeDataSet.color = resources.getColor(R.color.income_chart_line)
        incomeDataSet.setCircleColor(resources.getColor(R.color.income_chart_line))
        incomeDataSet.setCircleColorHole(resources.getColor(R.color.income_chart_line))
        incomeDataSet.circleRadius = 3f
        incomeDataSet.setDrawValues(false)
        incomeDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val balanceDataSet = LineDataSet(balanceEntries, getString(R.string.balance))
        balanceDataSet.color = resources.getColor(R.color.balance_chart_line)
        balanceDataSet.setCircleColor(resources.getColor(R.color.balance_chart_line))
        balanceDataSet.setCircleColorHole(resources.getColor(R.color.balance_chart_line))
        balanceDataSet.circleRadius = 3f
        balanceDataSet.setDrawValues(false)
        balanceDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        return LineData(expenseDataSet, incomeDataSet, profitDataSet, balanceDataSet)
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, BalanceReportActivity::class.java)
            context.startActivity(intent)
        }
    }
}
