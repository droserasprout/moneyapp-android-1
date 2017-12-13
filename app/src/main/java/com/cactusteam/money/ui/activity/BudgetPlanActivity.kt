package com.cactusteam.money.ui.activity

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.data.dao.BudgetPlanDependency
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.prediction.BudgetPrediction
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.format.DateTimeFormatter
import rx.Observable
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author vpotapenko
 */
class BudgetPlanActivity : BaseDataActivity("BudgetPlanActivity") {

    private var planId: Long = 0
    private var planName: String? = null

    private var mainCurrencyCode: String? = null
    private var amount: Double? = null
    private var plan: BudgetPlan? = null

    private var iconView: ImageView? = null
    private var amountProgress: View? = null
    private var transactionsProgress: View? = null
    private var limitProgress: ProgressBar? = null

    private var dependenciesContainer: LinearLayout? = null
    private var transactionsContainer: LinearLayout? = null
    private var predictionContainer: LinearLayout? = null

    private var periodView: TextView? = null
    private var amountView: TextView? = null
    private var limitView: TextView? = null
    private var limitSpeedView: TextView? = null
    private var restView: TextView? = null
    private var speedView: TextView? = null
    private var estimationView: TextView? = null
    private var warningView: TextView? = null

    private var dateTimeFormatter: DateTimeFormatter? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_BUDGET_PLAN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)

                if (data != null) {
                    if (data.getBooleanExtra(UiConstants.EXTRA_DELETED, false) || data.getBooleanExtra(UiConstants.EXTRA_COPY, false)) {
                        finish()
                        return
                    } else {
                        val newName = data.getStringExtra(UiConstants.EXTRA_NAME)
                        if (newName != null) title = newName
                    }
                }

                loadPlan()
            }
        } else if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)

                loadData()
            }
        } else if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)

                loadData()
            }
        } else if (requestCode == UiConstants.CALCULATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val a = Math.abs(data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0))
                updateLimit(a)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_budget_plan, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.edit) {
            showEditBudgetPlanActivity()
            return true
        } else if (itemId == R.id.copy) {
            copyBudgetPlanActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun copyBudgetPlanActivity() {
        EditBudgetPlanActivity.actionStart(this, UiConstants.EDIT_BUDGET_PLAN_REQUEST_CODE, planId, true)
    }

    private fun showEditBudgetPlanActivity() {
        EditBudgetPlanActivity.actionStart(this, UiConstants.EDIT_BUDGET_PLAN_REQUEST_CODE, planId, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_plan)

        initializeToolbar()
        title = planName

        initializeViewProgress()

        val appPreferences = MoneyApp.instance.appPreferences
        mainCurrencyCode = appPreferences.mainCurrencyCode
        dateTimeFormatter = DateTimeFormatter.create(appPreferences.transactionFormatDateMode, this)

        iconView = findViewById(R.id.icon) as ImageView
        amountProgress = findViewById(R.id.amount_progress)
        transactionsProgress = findViewById(R.id.transactions_progress)
        limitProgress = findViewById(R.id.limit_progress) as ProgressBar

        transactionsContainer = findViewById(R.id.transactions_container) as LinearLayout
        dependenciesContainer = findViewById(R.id.dependencies_container) as LinearLayout
        predictionContainer = findViewById(R.id.prediction_container) as LinearLayout

        periodView = findViewById(R.id.period) as TextView
        amountView = findViewById(R.id.amount) as TextView
        limitView = findViewById(R.id.limit) as TextView
        limitSpeedView = findViewById(R.id.limit_speed) as TextView
        restView = findViewById(R.id.rest) as TextView
        speedView = findViewById(R.id.speed) as TextView
        estimationView = findViewById(R.id.estimation) as TextView
        warningView = findViewById(R.id.warning) as TextView

        findViewById(R.id.all_transactions).setOnClickListener { allTransactionsClicked() }

        findViewById(R.id.limit_container).setOnClickListener { limitClicked() }

        loadPlan()
    }

    private fun limitClicked() {
        CalculatorActivity.actionStart(this, UiConstants.CALCULATOR_REQUEST_CODE, plan?.limit ?: 0.0, getString(R.string.limit_label))
    }

    private fun updateLimit(limit: Double) {
        if (plan == null || limit <= 0) return

        showProgress()
        val s = dataManager.budgetService
                .updateBudgetLimit(planId, limit)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        {
                            hideProgress()
                            Toast.makeText(this, R.string.budget_plan_was_saved, Toast.LENGTH_SHORT).show()

                            setResult(Activity.RESULT_OK)
                            loadPlan()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun allTransactionsClicked() {
        BudgetTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE, planId)
    }

    private fun loadPlan() {
        showProgress()

        val s = dataManager.budgetService.getBudget(planId)
                .subscribe(
                        { r ->
                            hideProgress()
                            planLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun planLoaded(plan: BudgetPlan) {
        this.plan = plan

        title = plan.name

        val amountStr = UiUtils.formatCurrency(plan.limit, mainCurrencyCode)
        limitView!!.text = amountStr
        updateLimitSpeed()

        updatePeriodView(plan)

        dependenciesContainer!!.removeAllViews()
        for (dependency in plan.dependencies) {
            createDependencyView(dependency)
        }

        updateCurrentAmountViews()

        loadData()
    }

    private fun loadData() {
        amount = null
        updateCurrentAmountViews()

        transactionsProgress!!.visibility = View.VISIBLE
        transactionsContainer!!.removeAllViews()

        val o1 = dataManager.budgetService.getBudgetAmount(planId)

        val now = Date()
        val finish = plan!!.finish
        val o2 = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(plan!!.start)
                .putTo(if (now.before(finish)) now else finish)
                .list()

        val s = Observable.zip(o1, o2, { i1, i2 ->
            Pair(i1, i2)
        }).subscribe(
                { r ->
                    amount = r.first
                    updateCurrentAmountViews()

                    transactionsProgress!!.visibility = View.GONE
                    transactionsLoaded(r.second)
                },
                { e ->
                    transactionsProgress!!.visibility = View.GONE
                    showError(e.message)
                }
        )
        compositeSubscription.add(s)
    }

    private fun updatePeriodView(plan: BudgetPlan) {
        val periodStr = DateUtils.formatDateRange(this, plan.start.time, plan.finish.time, DateUtils.FORMAT_SHOW_DATE)

        when (plan.type) {
            BudgetPlan.ONE_TIME_TYPE -> periodView!!.text = getString(R.string.one_time_period_pattern, periodStr)
            BudgetPlan.PERIODICAL_TYPE -> periodView!!.text = getString(R.string.periodical_period_pattern, periodStr)
        }
    }

    private fun updateLimitSpeed() {
        val days = TimeUnit.MILLISECONDS.toDays(plan!!.finish.time - plan!!.start.time).toInt()
        if (days <= 0) {
            limitSpeedView!!.visibility = View.GONE
        } else {
            val speed = plan!!.limit / days.toDouble()
            val s = UiUtils.formatCurrency(speed, mainCurrencyCode)
            limitSpeedView!!.text = getString(R.string.speed_pattern, s)
            limitSpeedView!!.visibility = View.VISIBLE
        }
    }

    private fun transactionsLoaded(transactions: List<Transaction>) {
        val filter = plan!!.createFilter()
        var count = UiConstants.MAX_SHORT_TRANSACTIONS
        for (transaction in transactions) {
            if (filter.allow(transaction)) {
                createTransactionView(transaction)
                count--

                if (count == 0) return
            }
        }
        if (transactionsContainer!!.childCount == 0) {
            transactionsContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
        }
    }

    private fun createTransactionView(transaction: Transaction) {
        val view = View.inflate(this, R.layout.activity_budget_plan_transaction, null)

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

    private fun showTransactionActivity(transactionId: Long) {
        EditTransactionActivity.actionStart(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
    }

    private fun createDependencyView(dependency: BudgetPlanDependency) {
        val obj = plan!!.dependencyObjects[dependency]

        val view = View.inflate(this, R.layout.activity_budget_plan_dependency, null)
        val s = UiUtils.formatDataObject(this, dependency.refType, obj)

        (view.findViewById(R.id.name) as TextView).text = s

        dependenciesContainer!!.addView(view)
    }

    private fun updateCurrentAmountViews() {
        if (amount == null) {
            amountProgress!!.visibility = View.VISIBLE
            amountView!!.visibility = View.GONE
            restView!!.visibility = View.GONE
            predictionContainer!!.visibility = View.GONE
            limitProgress!!.isIndeterminate = true
        } else {
            amountProgress!!.visibility = View.GONE
            amountView!!.visibility = View.VISIBLE
            restView!!.visibility = View.VISIBLE
            limitProgress!!.isIndeterminate = false

            val amountStr = UiUtils.formatCurrency(amount!!, mainCurrencyCode)
            amountView!!.text = amountStr

            val prediction = BudgetPrediction()
            prediction.calculate(plan!!.start, plan!!.finish, amount!!, plan!!.limit)

            restView!!.text = UiUtils.formatCurrency(prediction.rest, mainCurrencyCode)

            val max = Math.round(plan!!.limit).toInt()
            val progress = Math.round(amount!!).toInt()

            limitProgress!!.max = max
            limitProgress!!.progress = Math.min(max, progress)

            updatePrediction(prediction, max, progress)
        }
    }

    private fun updatePrediction(prediction: BudgetPrediction, max: Int, progress: Int) {
        if (plan!!.isFinished) {
            predictionContainer!!.visibility = View.GONE

            iconView!!.setImageResource(if (max < progress) R.drawable.ic_plan_fail else R.drawable.ic_plan_done)

            if (prediction.rest < 0) {
                warningView!!.setText(R.string.budget_is_over)
                warningView!!.visibility = View.VISIBLE
            } else {
                warningView!!.visibility = View.GONE
            }
        } else if (progress == 0) {
            predictionContainer!!.visibility = View.GONE
            iconView!!.setImageResource(R.drawable.ic_plan)
        } else {
            predictionContainer!!.visibility = View.VISIBLE

            val speedStr = UiUtils.formatCurrency(prediction.speed, mainCurrencyCode)
            speedView!!.text = getString(R.string.speed_pattern, speedStr)

            val willBeFinishedAt = prediction.willBeFinishedAt
            if (willBeFinishedAt != null) {
                val date = DateFormat.getDateFormat(this).format(willBeFinishedAt)
                estimationView!!.text = getString(R.string.estimation_pattern, date)
                estimationView!!.visibility = View.VISIBLE
            } else {
                estimationView!!.visibility = View.GONE
            }

            if (prediction.state == BudgetPrediction.EARLY_FINISH_STATE) {
                warningView!!.setText(R.string.budget_will_be_finished_early)
                warningView!!.visibility = View.VISIBLE
            } else if (prediction.rest <= 0) {
                warningView!!.setText(R.string.budget_is_over)
                warningView!!.visibility = View.VISIBLE
            } else {
                warningView!!.visibility = View.GONE
            }

            if (max <= progress) {
                iconView!!.setImageResource(R.drawable.ic_plan_fail)
            } else {
                val resId = if (prediction.state == BudgetPrediction.EARLY_FINISH_STATE) R.drawable.ic_plan_warn else R.drawable.ic_plan
                iconView!!.setImageResource(resId)
            }
        }
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                planId = extras.getLong(UiConstants.EXTRA_ID)
            }
            if (extras.containsKey(UiConstants.EXTRA_NAME)) {
                planName = extras.getString(UiConstants.EXTRA_NAME)
            }
        }
    }

    companion object {

        fun actionStart(fragment: Fragment, requestCode: Int, planId: Long, planName: String) {
            val intent = Intent(fragment.activity, BudgetPlanActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, planId)
            intent.putExtra(UiConstants.EXTRA_NAME, planName)

            fragment.startActivityForResult(intent, requestCode)
        }

        fun actionStart(activity: Activity, requestCode: Int, planId: Long) {
            val intent = Intent(activity, BudgetPlanActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, planId)

            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, requestCode: Int, planId: Long) {
            val intent = Intent(fragment.activity, BudgetPlanActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, planId)

            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
