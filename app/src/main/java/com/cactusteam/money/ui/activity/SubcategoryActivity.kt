package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.Fragment
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Subcategory
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.SubcategoryTransactionFilter
import com.cactusteam.money.data.model.CategoryPeriodData
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.format.DateTimeFormatter
import com.cactusteam.money.ui.fragment.EditSubcategoryFragment
import com.github.mikephil.charting.charts.BarChart
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
class SubcategoryActivity : BaseDataActivity("SubcategoryActivity") {

    private var subcategoryId: Long = 0
    private var categoryId: Long = 0
    private var subcategoryName: String? = null
    private var mainCurrencyCode: String? = null
    private var categoryType: Int = 0

    private var deleted: Boolean = false

    private var amountView: TextView? = null
    private var currentPeriod: TextView? = null

    private var amountProgress: View? = null
    private var transactionsProgress: View? = null

    private var transactionsContainer: LinearLayout? = null

    private var amountsChart: BarChart? = null

    private var dateTimeFormatter: DateTimeFormatter? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)

                loadData()
            }
        } else if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)

                loadData()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_subcategory, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.delete).isVisible = !deleted
        menu.findItem(R.id.restore).isVisible = deleted
        menu.findItem(R.id.to_category).isVisible = !deleted

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.edit) {
            editSubcategoryClicked()
            return true
        } else if (itemId == R.id.delete) {
            deleteSubcategoryClicked()
            return true
        } else if (itemId == R.id.restore) {
            restoreSubcategory()
            return true
        } else if (itemId == R.id.to_category) {
            toCategoryClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toCategoryClicked() {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.subcategory_will_be_transformed_to_category).setPositiveButton(R.string.ok) { dialog, which -> convertToCategory() }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun convertToCategory() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.categoryService
                .convertToCategory(subcategoryId)
                .subscribe(
                        { r ->
                            hideBlockingProgress()

                            Toast.makeText(this@SubcategoryActivity, R.string.subcategory_was_transformed_to_category, Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun restoreSubcategory() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.categoryService
                .restoreSubcategory(subcategoryId)
                .subscribe(
                        { r ->
                            hideBlockingProgress()
                            setResult(Activity.RESULT_OK)
                            finish()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun deleteSubcategoryClicked() {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.subcategory_will_be_deleted).setPositiveButton(android.R.string.yes) { dialog, which -> deleteSubcategory() }.setNegativeButton(android.R.string.no, null).show()
    }

    private fun deleteSubcategory() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.categoryService
                .deleteSubcategory(subcategoryId)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            Toast.makeText(this@SubcategoryActivity, R.string.subcategory_was_deleted, Toast.LENGTH_SHORT).show()

                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun editSubcategoryClicked() {
        val fragment = EditSubcategoryFragment.buildEdit(subcategoryId, subcategoryName)
        fragment.listener = { name ->
            Toast.makeText(this, R.string.subcategory_was_saved, Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)

            subcategoryName = name
            title = subcategoryName
        }
        fragment.show(fragmentManager, "dialog")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        val appPreferences = MoneyApp.instance.appPreferences
        mainCurrencyCode = appPreferences.mainCurrencyCode
        dateTimeFormatter = DateTimeFormatter.create(appPreferences.transactionFormatDateMode, this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subcategory)

        initializeToolbar()
        title = subcategoryName
        updateToolbarColor()

        initializeViewProgress()

        amountView = findViewById(R.id.amount) as TextView
        amountProgress = findViewById(R.id.amount_progress)
        amountsChart = findViewById(R.id.amounts_chart) as BarChart
        amountsChart!!.description = null
        amountsChart!!.setDrawBarShadow(false)
        amountsChart!!.setDrawValueAboveBar(true)
        amountsChart!!.setDrawGridBackground(false)
        amountsChart!!.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

            override fun onValueSelected(e: Entry, h: Highlight) {
                showTransactionsActivity(e.data as CategoryPeriodData)
            }

            override fun onNothingSelected() {
                // do nothing
            }
        })

        currentPeriod = findViewById(R.id.current_period) as TextView

        transactionsContainer = findViewById(R.id.transactions_container) as LinearLayout
        transactionsProgress = findViewById(R.id.transactions_progress)

        findViewById(R.id.all_transactions).setOnClickListener { allTransactionsClicked() }

        findViewById(R.id.create_transaction_btn).setOnClickListener { showNewTransactionActivity() }

        loadData()
    }

    override fun showProgress() {
        amountView!!.visibility = View.GONE
        amountsChart!!.visibility = View.GONE
        amountProgress!!.visibility = View.VISIBLE

        transactionsContainer!!.visibility = View.GONE
        transactionsProgress!!.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        amountView!!.visibility = View.VISIBLE
        amountProgress!!.visibility = View.GONE

        transactionsContainer!!.visibility = View.VISIBLE
        transactionsProgress!!.visibility = View.GONE
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadData() {
        showProgress()

        val o1 = dataManager.categoryService.getSubcategory(subcategoryId)
        val o2 = dataManager.categoryService.getCategoryPeriodsData(categoryId)
        val o3 = dataManager.transactionService.newListTransactionsBuilder()
                .putMax(UiConstants.MAX_SHORT_TRANSACTIONS)
                .putCategoryId(categoryId)
                .putSubcategoryId(subcategoryId)
                .list()

        val s = Observable.zip(o1, o2, o3, { i1, i2, i3 ->
            listOf(i1, i2, i3)
        }).subscribe(
                { r ->
                    hideProgress()

                    subcategoryLoaded(r[0] as Subcategory)
                    amountsLoaded(r[1] as List<CategoryPeriodData>)
                    transactionsLoaded(r[2] as List<Transaction>)
                },
                { e ->
                    hideProgress()
                    showError(e.message)
                }
        )
        compositeSubscription.add(s)
    }

    private fun subcategoryLoaded(subcategory: Subcategory) {
        deleted = subcategory.deleted
        supportActionBar?.invalidateOptionsMenu()
    }

    private fun showTransactionsActivity(periodData: CategoryPeriodData) {
        val filter = SubcategoryTransactionFilter(subcategoryId)
        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                filter,
                getString(R.string.subcategory_pattern, subcategoryName),
                periodData.from,
                periodData.to)
    }

    private fun transactionsLoaded(transactions: List<Transaction>) {
        transactionsContainer!!.removeAllViews()
        for (transaction in transactions) {
            addTransactionView(transaction)
        }
        if (transactions.isEmpty()) {
            transactionsContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
        }

        transactionsContainer!!.visibility = View.VISIBLE
        transactionsProgress!!.visibility = View.GONE
    }

    private fun addTransactionView(transaction: Transaction) {
        val view = View.inflate(this, R.layout.activity_subcategory_transaction, null)

        view.findViewById(R.id.transaction).setOnClickListener { showTransactionActivity(transaction.id!!) }

        val accountView = view.findViewById(R.id.source_account) as TextView
        accountView.text = transaction.sourceAccount.name
        if (transaction.type == Transaction.EXPENSE) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expense_transaction, 0)
        } else if (transaction.type == Transaction.INCOME) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_income_transaction, 0)
        }

        val commentView = view.findViewById(R.id.comment) as TextView
        if (transaction.comment != null) {
            commentView.text = transaction.comment
        } else {
            commentView.visibility = View.GONE
        }

        (view.findViewById(R.id.date) as TextView).text = dateTimeFormatter!!.format(transaction.date)
        (view.findViewById(R.id.dest_name) as TextView).text = transaction.categoryDisplayName

        val amountStr = UiUtils.formatCurrency(transaction.amount, transaction.sourceAccount.currencyCode)
        (view.findViewById(R.id.amount) as TextView).text = amountStr

        transactionsContainer!!.addView(view)
    }

    private fun showTransactionActivity(transactionId: Long) {
        EditTransactionActivity.actionStart(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
    }

    private fun amountsLoaded(periods: List<CategoryPeriodData>) {
        val currentPeriodData = periods[0]

        val periodStr = DateUtils.formatDateRange(this, currentPeriodData.from!!.time, currentPeriodData.to!!.time, DateUtils.FORMAT_SHOW_DATE)
        currentPeriod!!.text = periodStr
        amountView!!.visibility = View.VISIBLE

        var amount: Double? = currentPeriodData.subcategoryAmounts[subcategoryId]
        if (amount == null) amount = 0.0

        amountView!!.text = UiUtils.formatCurrency(amount, mainCurrencyCode)
        amountProgress!!.visibility = View.GONE

        if (periods.size > 1) {
            showAmountsChart(periods)
        }
    }

    @Suppress("DEPRECATION")
    private fun showAmountsChart(periods: List<CategoryPeriodData>) {
        val xValues = ArrayMap<Float, String>()
        val yValues = ArrayList<BarEntry>()

        Collections.reverse(periods)
        for (i in periods.indices) {
            val data = periods[i]

            val periodStr = DateUtils.formatDateRange(this, data.from!!.time, data.to!!.time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
            xValues.put(i.toFloat(), periodStr)

            var amount: Double? = data.subcategoryAmounts[subcategoryId]
            if (amount == null) amount = 0.0

            yValues.add(BarEntry(i.toFloat(), amount.toFloat(), data))
        }

        val set1 = BarDataSet(yValues, subcategoryName)
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

        amountsChart!!.animateY(700)
        amountsChart!!.visibility = View.VISIBLE
        amountsChart!!.data = data
    }

    private fun showNewTransactionActivity() {
        NewTransactionActivity.ActionBuilder().type(if (categoryType == Category.EXPENSE) Transaction.EXPENSE else Transaction.INCOME).category(categoryId).subcategory(subcategoryId).start(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE)
    }

    private fun allTransactionsClicked() {
        val filter = SubcategoryTransactionFilter(subcategoryId)
        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                filter,
                getString(R.string.subcategory_pattern, subcategoryName),
                null,
                null)
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

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                subcategoryId = extras.getLong(UiConstants.EXTRA_ID)
            }
            if (extras.containsKey(UiConstants.EXTRA_NAME)) {
                subcategoryName = extras.getString(UiConstants.EXTRA_NAME)
            }
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                categoryType = extras.getInt(UiConstants.EXTRA_TYPE)
            }
            if (extras.containsKey(UiConstants.EXTRA_PARENT)) {
                categoryId = extras.getLong(UiConstants.EXTRA_PARENT)
            }
        }
    }

    companion object {

        fun actionStart(activity: Activity, requestCode: Int, categoryId: Long, subcategoryId: Long, subcategoryName: String, type: Int) {
            val intent = Intent(activity, SubcategoryActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, subcategoryId)
            intent.putExtra(UiConstants.EXTRA_NAME, subcategoryName)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)
            intent.putExtra(UiConstants.EXTRA_PARENT, categoryId)

            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, requestCode: Int, categoryId: Long, subcategoryId: Long, subcategoryName: String, type: Int) {
            val intent = Intent(fragment.activity, SubcategoryActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, subcategoryId)
            intent.putExtra(UiConstants.EXTRA_NAME, subcategoryName)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)
            intent.putExtra(UiConstants.EXTRA_PARENT, categoryId)

            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
