package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.AndTransactionFilters
import com.cactusteam.money.data.filter.CategoryTransactionFilter
import com.cactusteam.money.data.filter.ITransactionFilter
import com.cactusteam.money.data.filter.SubcategoryTransactionFilter
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.grouping.CategoriesReportTransactionsGrouper
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
class SubcategoriesReportActivity : BaseDataActivity("SubcategoriesReportActivity") {

    private var categoryId: Long? = null
    private var categoryName: String? = null
    private var type: Int? = null

    private var initialFrom: Long? = null
    private var initialTo: Long? = null

    private var fromView: TextView? = null
    private var toView: TextView? = null

    private var amountsChart: PieChart? = null
    private var categoriesContainer: LinearLayout? = null

    private var colors: ArrayList<Int>? = null
    private var mainCurrencyCode: String? = null

    private var chartContainer: View? = null
    private var noDataView: View? = null

    private val from = Calendar.getInstance()
    private val to = Calendar.getInstance()

    private var grouper: CategoriesReportTransactionsGrouper? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            setResult(Activity.RESULT_OK)
            loadData()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subcategories_report)

        initializeToolbar()
        updateToolbarColor()

        title = categoryName

        fromView = findViewById(R.id.from_date) as TextView
        findViewById(R.id.from_date_container).setOnClickListener { fromDateClicked() }

        toView = findViewById(R.id.to_date) as TextView
        findViewById(R.id.to_date_container).setOnClickListener { toDateClicked() }

        val current = application.period.current
        from.timeInMillis = if (initialFrom == null) current.first.time else initialFrom!!
        updateFromDateView()

        to.timeInMillis = if (initialTo == null) current.second.time else initialTo!!
        updateToDateView()

        initializeViewProgress()

        chartContainer = findViewById(R.id.chart_container)
        noDataView = findViewById(R.id.no_data)

        amountsChart = findViewById(R.id.amounts_chart) as PieChart
        amountsChart!!.description = null
        amountsChart!!.setUsePercentValues(true)
        amountsChart!!.setEntryLabelColor(Color.BLACK)
        amountsChart!!.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

            override fun onValueSelected(e: Entry, h: Highlight) {
                showDetails(e.data as TransactionsGrouper.Group)
            }

            override fun onNothingSelected() {
                // do nothing
            }
        })

        categoriesContainer = findViewById(R.id.categories_container) as LinearLayout
        grouper = CategoriesReportTransactionsGrouper(type!!)
        mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode

        initializeColors()

        loadData()
    }

    @Suppress("DEPRECATION")
    private fun initializeColors() {
        colors = ArrayList<Int>()
        for (color in UiConstants.UI_COLORS) {
            colors!!.add(resources.getColor(color))
        }

        if (type === Category.INCOME) {
            Collections.reverse(colors!!)
        }
    }

    private fun showDetails(group: TransactionsGrouper.Group) {
        if (group.id < 0) {
            showTransactions(true)
        } else {
            val filter = SubcategoryTransactionFilter(group.id)
            FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                    filter,
                    getString(R.string.subcategory_pattern, group.title),
                    from.time,
                    to.time)
        }
    }

    private fun showTransactions(withoutSubcategories: Boolean) {
        val filter: ITransactionFilter
        val description: String
        if (withoutSubcategories) {
            val filters = AndTransactionFilters()
            filters.addFilter(CategoryTransactionFilter(categoryId!!))
            filters.addFilter(SubcategoryTransactionFilter(-1))
            filter = filters
            description = getString(R.string.category_pattern, categoryName) + " (" + getString(R.string.without_subcategory) + ")"
        } else {
            filter = CategoryTransactionFilter(categoryId!!)
            description = getString(R.string.category_pattern, categoryName)
        }

        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                filter,
                description,
                from.time,
                to.time)
    }

    private fun loadData() {
        showProgress()
        val s = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(from.time)
                .putTo(to.time)
                .putCategoryId(categoryId!!)
                .putConvertToMain(true)
                .list()
                .subscribe(
                        { r ->
                            hideProgress()
                            transactionsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun transactionsLoaded(transactions: List<Transaction>) {
        var groups: List<TransactionsGrouper.Group> = grouper!!.group(transactions)
        if (groups.isEmpty() || groups[0].subGroups.isEmpty()) {
            chartContainer!!.visibility = View.GONE
            noDataView!!.visibility = View.VISIBLE
        } else {
            groups = groups[0].subGroups

            chartContainer!!.visibility = View.VISIBLE
            noDataView!!.visibility = View.GONE

            val categoryTotal = calculateTotal(transactions)
            val subcategoriesTotal = groups.indices
                    .map { groups[it] }
                    .sumByDouble { getGroupAmount(it) }

            if (categoryTotal > subcategoriesTotal) {
                val group = TransactionsGrouper.Group(-1, getString(R.string.without_subcategory))
                if (type === Category.EXPENSE) {
                    group.expense = categoryTotal - subcategoriesTotal
                } else {
                    group.income = categoryTotal - subcategoriesTotal
                }
                groups.add(group)
            }

            prepareChart(groups)

            val chartTotal = amountsChart!!.data.yValueSum
            categoriesContainer!!.removeAllViews()
            for (i in groups.indices) {
                val group = groups[i]
                createGroupView(group, if (i < colors!!.size) colors!![i] else null, chartTotal)
            }
            if (!groups.isEmpty()) {
                val group = TransactionsGrouper.Group(-1, getString(R.string.total))
                if (type === Category.EXPENSE) {
                    group.expense = categoryTotal
                } else {
                    group.income = categoryTotal
                }

                createTotalView(group)
            }
        }
    }

    private fun calculateTotal(transactions: List<Transaction>): Double {
        return transactions.sumByDouble { it.amountInMainCurrency }
    }

    private fun createTotalView(group: TransactionsGrouper.Group) {
        val view = View.inflate(this, R.layout.activity_subcategories_report_total, null)

        view.findViewById(R.id.list_item).setOnClickListener { showTransactions(false) }

        (view.findViewById(R.id.name) as TextView).text = group.title

        val amountStr = UiUtils.formatCurrency(getGroupAmount(group), mainCurrencyCode)
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        categoriesContainer!!.addView(view)
    }

    private fun createGroupView(group: TransactionsGrouper.Group, color: Int?, chartTotal: Float) {
        val view = View.inflate(this, R.layout.activity_subcategories_report_item, null)

        view.findViewById(R.id.list_item).setOnClickListener { showDetails(group) }

        (view.findViewById(R.id.name) as TextView).text = group.title

        val groupAmount = getGroupAmount(group)
        var amountStr = UiUtils.formatCurrency(groupAmount, mainCurrencyCode)
        if (color != null) {
            val percent = groupAmount.toFloat() / chartTotal * 100f
            amountStr = amountStr + " (" + percentFormat.format(percent.toDouble()) + " %)"
        }
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        val marker = view.findViewById(R.id.type_marker)
        if (color == null) {
            marker.visibility = View.INVISIBLE
        } else {
            marker.visibility = View.VISIBLE
            marker.setBackgroundColor(color)
        }

        categoriesContainer!!.addView(view)
    }

    private fun getGroupAmount(group: TransactionsGrouper.Group): Double {
        return if (type === Category.EXPENSE) group.expense else group.income
    }

    private fun prepareChart(groups: List<TransactionsGrouper.Group>) {
        val yVals = ArrayList<PieEntry>()
        for (i in 0..Math.min(groups.size, UiConstants.UI_COLORS.size) - 1) {
            val group = groups[i]
            val entry = PieEntry(getGroupAmount(group).toFloat(), group.title)
            entry.data = group
            yVals.add(entry)
        }

        val dataSet = PieDataSet(yVals, getString(R.string.categories_title))
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        dataSet.colors = colors

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

    private fun toDateClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            to.set(year, monthOfYear, dayOfMonth)
            to.set(Calendar.HOUR_OF_DAY, 23)
            to.set(Calendar.MINUTE, 59)
            to.set(Calendar.SECOND, 59)
            to.set(Calendar.MILLISECOND, 999)

            updateToDateView()
            loadData()
        }, to.get(Calendar.YEAR), to.get(Calendar.MONTH), to.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateToDateView() {
        toView!!.text = DateFormat.getDateFormat(this).format(to.time)
    }

    private fun fromDateClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            from.set(year, monthOfYear, dayOfMonth)
            from.set(Calendar.HOUR_OF_DAY, 0)
            from.clear(Calendar.MINUTE)
            from.clear(Calendar.SECOND)
            from.clear(Calendar.MILLISECOND)

            updateFromDateView()
            loadData()
        }, from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateFromDateView() {
        fromView!!.text = DateFormat.getDateFormat(this).format(from.time)
    }

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun updateToolbarColor() {
        val nextColor: ColorDrawable
        if (type === Category.EXPENSE) {
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_expense_color))
        } else {
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_income_color))
        }
        val currentColor = ColorDrawable(resources.getColor(R.color.color_primary))

        if (DataUtils.hasLollipop()) {
            window.statusBarColor = UiUtils.darkenColor(nextColor.color)
        }

        val drawable = TransitionDrawable(arrayOf<Drawable>(currentColor, nextColor))
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
            if (extras.containsKey(UiConstants.EXTRA_START)) {
                initialFrom = extras.getLong(UiConstants.EXTRA_START)
            }
            if (extras.containsKey(UiConstants.EXTRA_FINISH)) {
                initialTo = extras.getLong(UiConstants.EXTRA_FINISH)
            }
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                type = extras.getInt(UiConstants.EXTRA_TYPE)
            }
        }
    }

    companion object {

        private val percentFormat = DecimalFormat("###,###,##0.0")

        fun actionStart(activity: Activity, requestCode: Int, categoryId: Long, categoryName: String, from: Date, to: Date, type: Int) {
            val intent = Intent(activity, SubcategoriesReportActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, categoryId)
            intent.putExtra(UiConstants.EXTRA_NAME, categoryName)
            intent.putExtra(UiConstants.EXTRA_START, from.time)
            intent.putExtra(UiConstants.EXTRA_FINISH, to.time)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}
