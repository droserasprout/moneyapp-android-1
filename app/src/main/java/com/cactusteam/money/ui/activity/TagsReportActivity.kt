package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.DatePickerDialog
import android.app.Fragment
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.filter.*
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.ExpenseTagsReportFragment
import com.cactusteam.money.ui.fragment.FilterFragment
import com.cactusteam.money.ui.fragment.IncomeTagsReportFragment
import com.cactusteam.money.ui.grouping.TransactionsGrouper
import com.cactusteam.money.ui.widget.period.IReportPeriod
import com.cactusteam.money.ui.widget.period.ReportPeriodAdapter
import java.util.*

/**
 * @author vpotapenko
 */
class TagsReportActivity : BaseDataActivity("TagsReportActivity") {

    private var initialFrom: Long? = null
    private var initialTo: Long? = null
    private var initialType: Int? = null

    private var currentToolbarColor: ColorDrawable? = null
    private var type = Category.EXPENSE

    private var fromView: TextView? = null
    private var toView: TextView? = null

    private var filterDescription: TextView? = null
    private var filterContainer: View? = null

    var filter: ITransactionFilter? = null
        private set
    private var lastFilter: FilterFragment.FilterInformation? = null

    private val from = Calendar.getInstance()
    private val to = Calendar.getInstance()

    private var expenseFragment: ExpenseTagsReportFragment? = null
    private var incomeFragment: IncomeTagsReportFragment? = null

    private var tablet: Boolean = false
    private var typeSpinner: Spinner? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            setResult(RESULT_OK)
            loadData()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_tags_report, menu)
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

    private fun loadData() {
        if (incomeFragment != null && !incomeFragment!!.isKilled) {
            incomeFragment!!.loadData()
        } else {
            incomeFragment = null
        }

        if (expenseFragment != null && !expenseFragment!!.isKilled) {
            expenseFragment!!.loadData()
        } else {
            expenseFragment = null
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags_report)

        var fragment = fragmentManager.findFragmentById(R.id.expense)
        expenseFragment = if (fragment != null) fragment as ExpenseTagsReportFragment else null
        fragment = fragmentManager.findFragmentById(R.id.income)
        incomeFragment = if (fragment != null) fragment as IncomeTagsReportFragment else null

        tablet = expenseFragment != null

        updateInitialState(savedInstanceState)

        if (initialType != null) type = initialType!!

        initializeToolbar()
        if (tablet) {
            setTitle(R.string.tags_report_title)
        } else {
            supportActionBar!!.setDisplayShowTitleEnabled(false)

            initializeSpinner()

            currentToolbarColor = ColorDrawable(resources.getColor(R.color.color_primary))
            updateToolbarColor()
        }

        fromView = findViewById(R.id.from_date) as TextView
        findViewById(R.id.from_date_container).setOnClickListener { fromDateClicked() }

        toView = findViewById(R.id.to_date) as TextView
        findViewById(R.id.to_date_container).setOnClickListener { toDateClicked() }

        filterDescription = findViewById(R.id.filter_description) as TextView
        filterContainer = findViewById(R.id.filter_container)
        filterContainer!!.setOnClickListener {
            filter = null
            filterContainer!!.visibility = View.GONE

            loadData()
        }

        initializePeriods()

        updateCurrentFragment()
    }

    private fun initializePeriods() {
        val customDateContainer = findViewById(R.id.custom_period_container)

        val periodTypeSpinner = findViewById(R.id.period_type) as Spinner

        val adapter = ReportPeriodAdapter(this)
        periodTypeSpinner.adapter = adapter
        periodTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (parent == null) return

                val reportPeriod = parent.getItemAtPosition(position) as IReportPeriod
                if (reportPeriod.isCustom()) {
                    customDateContainer.visibility = View.VISIBLE
                    updateFromDateView()
                    updateToDateView()
                } else {
                    customDateContainer.visibility = View.GONE
                    from!!.timeInMillis = reportPeriod.getStartDate()!!.time
                    to!!.timeInMillis = reportPeriod.getEndDate()!!.time
                }

                loadData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        val current = application.period.current
        if (initialTo != null || initialFrom != null) {
            from!!.timeInMillis = if (initialFrom == null) current.first.time else initialFrom!!
            to!!.timeInMillis = if (initialTo == null) current.second.time else initialTo!!

            periodTypeSpinner.setSelection(adapter.count - 1)
            customDateContainer.visibility = View.VISIBLE
        } else {
            from!!.timeInMillis = current.first.time
            to!!.timeInMillis = current.second.time

            customDateContainer.visibility = View.GONE
            periodTypeSpinner.setSelection(0)
        }
    }

    private fun updateInitialState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        var l = savedInstanceState.getLong(FROM, 0)
        if (l != 0L) initialFrom = l

        l = savedInstanceState.getLong(TO, 0)
        if (l != 0L) initialTo = l

        val t = savedInstanceState.getInt(TYPE, -1)
        if (t != -1) initialType = t
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (from != null) outState.putLong(FROM, from.timeInMillis)
        if (to != null) outState.putLong(TO, to.timeInMillis)
        if (typeSpinner != null) outState.putInt(TYPE, typeSpinner!!.selectedItemPosition)

        super.onSaveInstanceState(outState)
    }

    private fun updateCurrentFragment() {
        if (!tablet) {
            val fragmentManager = fragmentManager
            if (type == Category.EXPENSE) {
                val fragment = fragmentManager.findFragmentByTag(EXPENSE_TAG)
                expenseFragment = if (fragment != null) fragment as ExpenseTagsReportFragment else ExpenseTagsReportFragment()

                showFragment(expenseFragment!!, EXPENSE_TAG)
            } else {
                val fragment = fragmentManager.findFragmentByTag(INCOME_TAG)
                incomeFragment = if (fragment != null) fragment as IncomeTagsReportFragment else IncomeTagsReportFragment()

                showFragment(incomeFragment!!, INCOME_TAG)
            }
        } else {
            loadData()
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val fragmentManager = fragmentManager
        if (fragmentManager.findFragmentByTag(tag) == null) {
            showFragment(R.id.content_frame, fragment, tag)
        }
    }

    fun showTransactionsActivity(group: TransactionsGrouper.Group, type: Int?) {
        var transactionFilter: ITransactionFilter
        var description: String?
        if (group.id == -1L) { // without tag
            val filters = AndTransactionFilters()
            filters.addFilter(WithoutTagTransactionFilter.instance)
            filters.addFilter(TypeTransactionFilter(type!!))
            transactionFilter = filters
            description = null
        } else if (group.id == -2L) { // all
            transactionFilter = TypeTransactionFilter(type!!)
            description = null
        } else { // tag specified
            transactionFilter = TagTransactionFilter(group.id)
            description = "<strong>" + getString(R.string.tag_pattern, "</strong>" + group.title)
        }

        if (filter is TransactionExtendedFilter) {
            val extendedFilter = (filter as TransactionExtendedFilter).subtractWithAdditional(transactionFilter,
                    description ?: "")
            transactionFilter = extendedFilter
            description = extendedFilter.displayName
        }

        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                transactionFilter,
                description,
                from!!.time,
                to!!.time)
    }

    private fun toDateClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            to!!.set(year, monthOfYear, dayOfMonth)
            to.set(Calendar.HOUR_OF_DAY, 23)
            to.set(Calendar.MINUTE, 59)
            to.set(Calendar.SECOND, 59)
            to.set(Calendar.MILLISECOND, 999)

            updateToDateView()
            loadData()
        }, to!!.get(Calendar.YEAR), to.get(Calendar.MONTH), to.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateToDateView() {
        toView!!.text = DateFormat.getDateFormat(this).format(to!!.time)
    }

    private fun fromDateClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            from!!.set(year, monthOfYear, dayOfMonth)
            from.set(Calendar.HOUR_OF_DAY, 0)
            from.clear(Calendar.MINUTE)
            from.clear(Calendar.SECOND)
            from.clear(Calendar.MILLISECOND)

            updateFromDateView()
            loadData()
        }, from!!.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateFromDateView() {
        fromView!!.text = DateFormat.getDateFormat(this).format(from!!.time)
    }

    private fun typeChanged(newType: Int) {
        type = newType
        updateToolbarColor()

        updateCurrentFragment()
    }

    private fun initializeSpinner() {
        @SuppressWarnings("ConstantConditions") val adapter = ArrayAdapter(supportActionBar!!.themedContext,
                R.layout.activity_edit_transaction_type_view,
                android.R.id.text1,
                arrayOf(getString(R.string.expense_label), getString(R.string.income_label)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)

        val view = View.inflate(supportActionBar!!.themedContext, R.layout.view_toolbar_spinner, null)
        typeSpinner = view.findViewById(R.id.spinner) as Spinner
        typeSpinner!!.adapter = adapter
        typeSpinner!!.setSelection(type)
        typeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                typeChanged(if (position == 0) Category.EXPENSE else Category.INCOME)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }
        toolbar!!.addView(view)
    }

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun updateToolbarColor() {
        val nextColor: ColorDrawable
        if (type == Category.EXPENSE) {
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_expense_color))
        } else {
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_income_color))
        }

        if (DataUtils.hasLollipop()) {
            window.statusBarColor = UiUtils.darkenColor(nextColor.color)
        }

        val drawable = TransitionDrawable(arrayOf(currentToolbarColor, nextColor))
        currentToolbarColor = nextColor

        toolbar!!.setBackgroundDrawable(drawable)
        drawable.startTransition(UiConstants.TOOLBAR_TRANSITION_DURATION)
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_START)) {
                initialFrom = extras.getLong(UiConstants.EXTRA_START)
            }
            if (extras.containsKey(UiConstants.EXTRA_FINISH)) {
                initialTo = extras.getLong(UiConstants.EXTRA_FINISH)
            }
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                initialType = extras.getInt(UiConstants.EXTRA_TYPE)
            }
        }
    }

    val fromTime: Date
        get() = from!!.time

    val toTime: Date
        get() = to!!.time

    companion object {

        private val EXPENSE_TAG = "expense"
        private val INCOME_TAG = "income"

        private val FROM = "from"
        private val TO = "to"
        private val TYPE = "type"

        fun actionStart(fragment: Fragment, requestCode: Int) {
            val intent = Intent(fragment.activity, TagsReportActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
