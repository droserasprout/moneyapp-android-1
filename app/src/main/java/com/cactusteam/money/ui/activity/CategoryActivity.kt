package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.Fragment
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Subcategory
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.CategoryTransactionFilter
import com.cactusteam.money.data.model.CategoryPeriodData
import com.cactusteam.money.ui.HtmlTagHandler
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.format.DateTimeFormatter
import com.cactusteam.money.ui.fragment.EditSubcategoryFragment
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
import rx.Observable
import java.util.*

/**
 * @author vpotapenko
 */
class CategoryActivity : BaseDataActivity("CategoryActivity") {

    private val tagHandler = HtmlTagHandler()

    private var categoryId: Long = 0
    private var categoryType: Int = 0
    private var categoryName: String? = null

    private var mainCurrencyCode: String? = null

    private var amountView: TextView? = null
    private var currentPeriod: TextView? = null

    private var amountProgress: View? = null
    private var transactionsProgress: View? = null
    private var subcategoriesProgress: View? = null

    private var transactionsContainer: LinearLayout? = null
    private var subcategoriesContainer: LinearLayout? = null

    private var amountsChart: BarChart? = null
    private var chartContainer: LinearLayout? = null
    private var averageAmountView: TextView? = null

    private var currentPeriodData: CategoryPeriodData? = null
    private var deleted: Boolean = false

    private var dateTimeFormatter: DateTimeFormatter? = null

    private var includeDeleted: Boolean = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_CATEGORY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                if (data != null && data.getBooleanExtra(UiConstants.EXTRA_DELETED, false)) {
                    finish()
                } else {
                    if (data != null) {
                        val newName = data.getStringExtra(UiConstants.EXTRA_NAME)
                        if (newName != null) categoryName = newName
                    }

                    loadData()
                }
            }
        } else if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                loadData()
            }
        } else if (requestCode == UiConstants.SUBCATEGORY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                loadData()
            }
            amountsChart!!.highlightValues(null)
        } else if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                loadData()
            }
            amountsChart!!.highlightValues(null)
        } else if (requestCode == UiConstants.CATEGORY_REPORT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                loadData()
            }
            amountsChart!!.highlightValues(null)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_category, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.show_deleted).isVisible = !includeDeleted
        menu.findItem(R.id.hide_deleted).isVisible = includeDeleted

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.edit) {
            showEditCategoryActivity()
            return true
        } else if (itemId == R.id.show_deleted) {
            showDeleted()
            return true
        } else if (itemId == R.id.hide_deleted) {
            hideDeleted()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hideDeleted() {
        Toast.makeText(this, R.string.deleted_subcategories_was_hidden, Toast.LENGTH_SHORT).show()
        includeDeleted = false
        loadData()

        val supportActionBar = supportActionBar
        supportActionBar?.invalidateOptionsMenu()
    }

    private fun showDeleted() {
        Toast.makeText(this, R.string.deleted_subcategories_was_shown, Toast.LENGTH_SHORT).show()
        includeDeleted = true
        loadData()

        val supportActionBar = supportActionBar
        supportActionBar?.invalidateOptionsMenu()
    }

    private fun showEditCategoryActivity() {
        EditCategoryActivity.actionStart(this, UiConstants.EDIT_CATEGORY_REQUEST_CODE, categoryId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()
        mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        initializeToolbar()
        updateTitle()

        initializeViewProgress()

        updateToolbarColor()

        val appPreferences = MoneyApp.instance.appPreferences
        dateTimeFormatter = DateTimeFormatter.create(appPreferences.transactionFormatDateMode, this)

        amountView = findViewById(R.id.amount) as TextView
        amountProgress = findViewById(R.id.amount_progress)
        amountsChart = findViewById(R.id.amounts_chart) as BarChart
        amountsChart!!.description.isEnabled = false
        amountsChart!!.setDrawBarShadow(false)
        amountsChart!!.setDrawValueAboveBar(true)
        amountsChart!!.setDrawGridBackground(false)
        amountsChart!!.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

            override fun onValueSelected(e: Entry?, h: Highlight?) {
                showDetails(e!!.data as CategoryPeriodData)
            }

            override fun onNothingSelected() {
                // do nothing
            }
        })
        chartContainer = findViewById(R.id.chart_container) as LinearLayout
        findViewById(R.id.more_chart_btn).setOnClickListener { showCategoryReportActivity() }
        averageAmountView = findViewById(R.id.average_amount) as TextView

        currentPeriod = findViewById(R.id.current_period) as TextView

        transactionsContainer = findViewById(R.id.transactions_container) as LinearLayout
        transactionsProgress = findViewById(R.id.transactions_progress)

        subcategoriesContainer = findViewById(R.id.subcategories_container) as LinearLayout
        subcategoriesContainer!!.visibility = View.GONE
        subcategoriesProgress = findViewById(R.id.subcategories_progress)
        subcategoriesProgress!!.visibility = View.VISIBLE

        findViewById(R.id.all_transactions).setOnClickListener { allTransactionsClicked() }

        findViewById(R.id.create_transaction_btn).setOnClickListener { showNewTransactionActivity() }

        findViewById(R.id.create_subcategory_btn).setOnClickListener { createSubcategoryClicked() }

        loadData()
    }

    private fun showCategoryReportActivity() {
        CategoryReportActivity.actionStart(this, UiConstants.CATEGORY_REPORT_REQUEST_CODE, categoryId, categoryName, categoryType)
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

    override fun showProgress() {
        amountView!!.visibility = View.GONE
        chartContainer!!.visibility = View.GONE
        amountProgress!!.visibility = View.VISIBLE

        subcategoriesContainer!!.visibility = View.GONE
        subcategoriesProgress!!.visibility = View.VISIBLE

        transactionsContainer!!.visibility = View.GONE
        transactionsProgress!!.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        amountView!!.visibility = View.VISIBLE
        amountProgress!!.visibility = View.GONE

        subcategoriesContainer!!.visibility = View.VISIBLE
        subcategoriesProgress!!.visibility = View.GONE

        transactionsContainer!!.visibility = View.VISIBLE
        transactionsProgress!!.visibility = View.GONE
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadData() {
        showProgress()

        val o1 = dataManager.categoryService.getCategory(categoryId)
        val o2 = dataManager.categoryService.getCategoryPeriodsData(categoryId)
        val o3 = dataManager.transactionService.newListTransactionsBuilder()
                .putMax(UiConstants.MAX_SHORT_TRANSACTIONS)
                .putCategoryId(categoryId)
                .list()

        val s = Observable.zip(o1, o2, o3, { i1, i2, i3 ->
            listOf(i1, i2, i3)
        }).subscribe(
                { r ->
                    hideProgress()

                    periodDataLoaded(r[1] as List<CategoryPeriodData>) // period should be first because amounts use in category
                    categoryLoaded(r[0] as Category)
                    transactionsLoaded(r[2] as List<Transaction>)
                },
                { e ->
                    hideProgress()
                    showError(e.message)
                }
        )
        compositeSubscription.add(s)
    }

    private fun createSubcategoryClicked() {
        val fragment = EditSubcategoryFragment.buildNew(categoryId)
        fragment.listener = {
            Toast.makeText(this@CategoryActivity, R.string.subcategory_was_saved, Toast.LENGTH_SHORT).show()
            loadData()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun showNewTransactionActivity() {
        NewTransactionActivity.ActionBuilder().type(if (categoryType == Category.EXPENSE) Transaction.EXPENSE else Transaction.INCOME).category(categoryId).start(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE)
    }

    private fun allTransactionsClicked() {
        val filter = CategoryTransactionFilter(categoryId)
        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                filter,
                getString(R.string.category_pattern, categoryName),
                null,
                null)
    }

    private fun periodDataLoaded(periods: List<CategoryPeriodData>) {
        currentPeriodData = periods[0]

        val periodStr = DateUtils.formatDateRange(this, currentPeriodData!!.from!!.time, currentPeriodData!!.to!!.time, DateUtils.FORMAT_SHOW_DATE)
        currentPeriod!!.text = periodStr
        amountView!!.text = UiUtils.formatCurrency(currentPeriodData!!.amount, mainCurrencyCode)

        if (periods.size > 1) {
            showAmountsChart(periods)
        }
    }

    private fun categoryLoaded(category: Category) {
        deleted = category.deleted
        updateTitle()

        subcategoriesContainer!!.removeAllViews()
        val subcategories = category.subcategories
        var maxAmount = 0.0
        if (currentPeriodData != null) {
            maxAmount = currentPeriodData!!.maxSubcategoryAmount
        }
        subcategories
                .filter { !it.deleted || includeDeleted }
                .forEach { addSubcategoryView(it, maxAmount) }
        if (subcategories.isEmpty()) {
            subcategoriesContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
        }
    }

    @Suppress("DEPRECATION")
    private fun addSubcategoryView(subcategory: Subcategory, maxAmount: Double) {
        val view = View.inflate(this, R.layout.activity_category_subcategory, null)

        view.findViewById(R.id.subcategory).setOnClickListener { showSubcategoryActivity(subcategory.id!!, subcategory.name) }

        val nameView = view.findViewById(R.id.name) as TextView
        if (subcategory.deleted) {
            val s = String.format(UiConstants.DELETED_PATTERN, subcategory.name)
            nameView.text = Html.fromHtml(s, null, tagHandler)
        } else {
            nameView.text = subcategory.name
        }

        var amount: Double? = null
        if (currentPeriodData != null) {
            amount = currentPeriodData!!.subcategoryAmounts[subcategory.id]
        }
        val amountStr = if (amount == null) "-" else UiUtils.formatCurrency(amount, mainCurrencyCode)
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        val amountProgress = view.findViewById(R.id.amount_progress) as ProgressBar
        if (maxAmount > 0 && amount != null) {
            amountProgress.visibility = View.VISIBLE

            amountProgress.max = 100

            val progress = DataUtils.round(amount / maxAmount * 100, 0).toInt()
            amountProgress.progress = progress
        } else {
            amountProgress.visibility = View.GONE
        }

        subcategoriesContainer!!.addView(view)
    }

    private fun showSubcategoryActivity(subcategoryId: Long, subcategoryName: String) {
        SubcategoryActivity.actionStart(this, UiConstants.SUBCATEGORY_REQUEST_CODE, categoryId, subcategoryId, subcategoryName, categoryType)
    }

    @Suppress("DEPRECATION")
    private fun showAmountsChart(periods: List<CategoryPeriodData>) {
        val yValues = ArrayList<BarEntry>()
        val xValues: MutableMap<Float, String> = mutableMapOf()

        Collections.reverse(periods)
        for (i in periods.indices) {
            val data = periods[i]
            val label = DateUtils.formatDateRange(this, data.from!!.time, data.to!!.time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
            xValues[i.toFloat()] = label

            yValues.add(BarEntry(i.toFloat(), data.amount.toFloat(), data))
        }

        val set1 = BarDataSet(yValues, categoryName)
        set1.color = resources.getColor(if (categoryType == Category.EXPENSE) R.color.toolbar_expense_color else R.color.toolbar_income_color)

        val dataSets = ArrayList<IBarDataSet>()
        dataSets.add(set1)

        val data = BarData(dataSets)
        data.setValueTextSize(10f)

        val xAxis = amountsChart!!.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IAxisValueFormatter { value, axis ->
            val s = xValues[value]
            s ?: ""
        }

        amountsChart!!.axisLeft.setDrawLabels(false)
        amountsChart!!.legend.isEnabled = false

        val rightAxis = amountsChart!!.axisRight
        rightAxis.setDrawGridLines(false)
        rightAxis.setLabelCount(8, false)
        rightAxis.spaceTop = 15f

        var averageAmount = periods.sumByDouble { it.amount }
        averageAmount /= periods.size.toDouble()
        averageAmount = DataUtils.round(averageAmount, 2)
        val amountStr = UiUtils.formatCurrency(averageAmount, mainCurrencyCode)
        averageAmountView!!.text = getString(R.string.average_amount_pattern, amountStr)

        rightAxis.removeAllLimitLines()
        val l = LimitLine(DataUtils.toFloat(averageAmount), getString(R.string.average))
        val limitColor = resources.getColor(if (categoryType == Category.EXPENSE) R.color.toolbar_expense_color else R.color.toolbar_income_color)
        l.lineColor = UiUtils.darkenColor(limitColor)
        l.textSize = 10f
        rightAxis.addLimitLine(l)

        amountsChart!!.animateY(1000)
        chartContainer!!.visibility = View.VISIBLE
        amountsChart!!.data = data
    }

    private fun transactionsLoaded(transactions: List<Transaction>) {
        transactionsContainer!!.removeAllViews()
        for (transaction in transactions) {
            addTransactionView(transaction)
        }
        if (transactions.isEmpty()) {
            transactionsContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
        }
    }

    private fun showTransactionActivity(transactionId: Long) {
        EditTransactionActivity.actionStart(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
    }

    private fun addTransactionView(transaction: Transaction) {
        val view = View.inflate(this, R.layout.activity_category_transaction, null)

        view.findViewById(R.id.transaction).setOnClickListener { showTransactionActivity(transaction.id!!) }

        val accountView = view.findViewById(R.id.source_account) as TextView
        accountView.text = transaction.sourceAccount.name
        if (transaction.type == Transaction.EXPENSE) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expense_transaction, 0)
        } else if (transaction.type == Transaction.INCOME) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_income_transaction, 0)
        }

        val tagsContainer = view.findViewById(R.id.tags_container) as LinearLayout
        tagsContainer.removeAllViews()
        for (tag in transaction.tags) {
            View.inflate(this, R.layout.fragment_transactions_tag, tagsContainer)
            val textView = tagsContainer.getChildAt(tagsContainer.childCount - 1) as TextView
            textView.text = tag.tag.name
        }

        val commentView = view.findViewById(R.id.comment) as TextView
        if (!transaction.comment.isNullOrBlank()) {
            commentView.text = transaction.comment
            commentView.visibility = View.VISIBLE
        } else {
            commentView.visibility = View.GONE
        }

        (view.findViewById(R.id.date) as TextView).text = dateTimeFormatter!!.format(transaction.date)
        (view.findViewById(R.id.dest_name) as TextView).text = transaction.categoryDisplayName

        val amountStr = UiUtils.formatCurrency(transaction.amount, transaction.sourceAccount.currencyCode)
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        transactionsContainer!!.addView(view)
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
        toolbar!!.setBackgroundDrawable(drawable)
        drawable.startTransition(UiConstants.TOOLBAR_TRANSITION_DURATION)
    }

    @Suppress("DEPRECATION")
    private fun updateTitle() {
        if (deleted) {
            val s = String.format(UiConstants.DELETED_PATTERN, categoryName)
            title = Html.fromHtml(s, null, HtmlTagHandler())
        } else {
            title = categoryName
        }
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

        fun actionStart(fragment: Fragment, requestCode: Int, categoryId: Long, categoryName: String, type: Int) {
            val intent = Intent(fragment.activity, CategoryActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, categoryId)
            intent.putExtra(UiConstants.EXTRA_NAME, categoryName)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)

            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
