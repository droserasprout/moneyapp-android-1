package com.cactusteam.money.ui.widget.home

import android.text.TextUtils
import android.text.format.DateUtils
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.data.prediction.BudgetPrediction
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.MainActivity
import com.cactusteam.money.ui.fragment.HomeFragment

/**
 * @author vpotapenko
 */
class BudgetSummaryHomeUnit(homeFragment: HomeFragment) : BaseHomeUnit(homeFragment) {

    private val budgetPrediction: BudgetPrediction

    private var budgetContainer: View? = null

    private var periodView: TextView? = null
    private var limitProgressView: ProgressBar? = null
    private var amountView: TextView? = null
    private var restView: TextView? = null
    private var limitView: TextView? = null
    private var speedView: TextView? = null
    private var warningView: TextView? = null
    private var warningBudgetsView: TextView? = null

    private var dataContainer: View? = null
    private var noDataContainer: View? = null

    init {
        budgetPrediction = BudgetPrediction()
    }

    override fun getLayoutRes(): Int {
        return R.layout.fragment_home_budget_summary_unit
    }

    override fun initializeView() {
        budgetContainer = getView()!!.findViewById(R.id.budget_container)
        getView()!!.findViewById(R.id.budget_title).setOnClickListener { (homeFragment.activity as MainActivity).showSection(MainSection.BUDGET) }
        getView()!!.findViewById(R.id.create_btn).setOnClickListener { (homeFragment.activity as MainActivity).showSection(MainSection.BUDGET) }
        periodView = getView()!!.findViewById(R.id.period) as TextView
        limitProgressView = getView()!!.findViewById(R.id.limit_progress) as ProgressBar
        amountView = getView()!!.findViewById(R.id.amount) as TextView
        restView = getView()!!.findViewById(R.id.rest) as TextView
        limitView = getView()!!.findViewById(R.id.limit) as TextView
        speedView = getView()!!.findViewById(R.id.speed) as TextView
        warningView = getView()!!.findViewById(R.id.warning) as TextView
        warningBudgetsView = getView()!!.findViewById(R.id.warning_budgets) as TextView

        dataContainer = getView()!!.findViewById(R.id.data_container)
        noDataContainer = getView()!!.findViewById(R.id.no_data_container)
    }

    override fun update() {
        loadBudgets()
    }

    private fun loadBudgets() {
        budgetContainer!!.visibility = View.GONE
        val s = homeFragment.dataManager.budgetService
                .getCurrentBudgets()
                .subscribe(
                        { r ->
                            budgetLoaded(r)
                            budgetContainer!!.visibility = View.VISIBLE
                        },
                        { e ->
                            homeFragment.showError(e.message)
                        }
                )
        homeFragment.compositeSubscription.add(s)
    }

    private fun budgetLoaded(plans: List<BudgetPlan>) {
        if (!plans.isEmpty()) {
            dataContainer!!.visibility = View.VISIBLE
            noDataContainer!!.visibility = View.GONE

            var amount = 0.0
            var limit = 0.0
            var warningBudgets = ""
            for (plan in plans) {
                amount += plan.expense
                limit += plan.limit

                budgetPrediction.calculate(plan.start, plan.finish, plan.expense, plan.limit)
                if (budgetPrediction.rest <= 0) {
                    if (!TextUtils.isEmpty(warningBudgets)) warningBudgets += "\n"

                    warningBudgets += homeFragment.getString(R.string.budget_is_over_pattern, plan.name)
                }
            }

            val currentPeriod = MoneyApp.instance.appPreferences.period.fullCurrent
            val periodStr = DateUtils.formatDateRange(homeFragment.activity, currentPeriod.first.time,
                    currentPeriod.second.time, DateUtils.FORMAT_SHOW_DATE)
            periodView!!.text = periodStr

            var amountStr = UiUtils.formatCurrency(amount, mainCurrencyCode)
            amountView!!.text = amountStr

            limitProgressView!!.isIndeterminate = false

            val max = Math.round(limit).toInt()
            val progress = Math.round(amount).toInt()

            limitProgressView!!.max = max
            limitProgressView!!.progress = Math.min(max, progress)

            budgetPrediction.calculate(currentPeriod.first, currentPeriod.second, amount, limit)
            amountStr = UiUtils.formatCurrency(budgetPrediction.rest, mainCurrencyCode)
            restView!!.text = amountStr

            val speedStr = UiUtils.formatCurrency(budgetPrediction.speed, mainCurrencyCode)
            speedView!!.text = homeFragment.getString(R.string.speed_pattern, speedStr)

            amountStr = UiUtils.formatCurrency(limit, mainCurrencyCode)
            limitView!!.text = amountStr

            if (budgetPrediction.state == BudgetPrediction.EARLY_FINISH_STATE) {
                warningView!!.setText(R.string.budget_will_be_finished_early)
                warningView!!.visibility = View.VISIBLE
            } else if (budgetPrediction.rest <= 0) {
                warningView!!.setText(R.string.budget_is_over)
                warningView!!.visibility = View.VISIBLE
            } else {
                warningView!!.visibility = View.GONE
            }

            if (!TextUtils.isEmpty(warningBudgets)) {
                warningBudgetsView!!.visibility = View.VISIBLE
                warningBudgetsView!!.text = warningBudgets
            } else {
                warningBudgetsView!!.visibility = View.GONE
            }
        } else {
            dataContainer!!.visibility = View.GONE
            noDataContainer!!.visibility = View.VISIBLE
        }
    }

    override val shortName: String
        get() = UiConstants.BUDGET_SUMMARY_BLOCK
}
