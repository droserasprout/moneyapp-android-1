package com.cactusteam.money.ui.widget.home

import android.text.format.DateUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.data.prediction.BudgetPrediction
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.BudgetPlanActivity
import com.cactusteam.money.ui.activity.MainActivity
import com.cactusteam.money.ui.fragment.HomeFragment

/**
 * @author vpotapenko
 */
class BudgetHomeUnit(homeFragment: HomeFragment) : BaseHomeUnit(homeFragment) {

    private val budgetPrediction: BudgetPrediction

    private var budgetContainer: View? = null
    private var budgetItemsContainer: LinearLayout? = null

    init {
        budgetPrediction = BudgetPrediction()
    }

    override fun getLayoutRes(): Int {
        return R.layout.fragment_home_budget_unit
    }

    override fun initializeView() {
        budgetContainer = getView()!!.findViewById(R.id.budget_container)
        budgetItemsContainer = getView()!!.findViewById(R.id.budget_items_container) as LinearLayout
        getView()!!.findViewById(R.id.budget_title).setOnClickListener { (homeFragment.activity as MainActivity).showSection(MainSection.BUDGET) }
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
                        },
                        { e ->
                            homeFragment.showError(e.message)
                        }
                )
        homeFragment.compositeSubscription.add(s)
    }

    private fun budgetLoaded(plans: List<BudgetPlan>) {
        if (!plans.isEmpty()) {
            budgetItemsContainer!!.removeAllViews()
            for (plan in plans) {
                createBudgetItem(plan)
            }
            budgetContainer!!.visibility = View.VISIBLE
        }
    }

    private fun createBudgetItem(plan: BudgetPlan) {
        val view = View.inflate(homeFragment.activity, R.layout.fragment_home_budget_item, null)
        view.findViewById(R.id.budget_item).setOnClickListener { showPlanActivity(plan) }
        (view.findViewById(R.id.name) as TextView).text = plan.name

        val periodStr = DateUtils.formatDateRange(homeFragment.activity, plan.start.time, plan.finish.time, DateUtils.FORMAT_SHOW_DATE)
        (view.findViewById(R.id.period) as TextView).text = periodStr

        val amountView = view.findViewById(R.id.amount) as TextView

        var amountStr = UiUtils.formatCurrency(plan.expense, mainCurrencyCode)
        amountView.text = amountStr

        val limitProgress = view.findViewById(R.id.limit_progress) as ProgressBar
        limitProgress.isIndeterminate = false

        val max = Math.round(plan.limit).toInt()
        val progress = Math.round(plan.expense).toInt()

        limitProgress.max = max
        limitProgress.progress = Math.min(max, progress)

        budgetPrediction.calculate(plan.start, plan.finish, plan.expense, plan.limit)
        amountStr = UiUtils.formatCurrency(budgetPrediction.rest, mainCurrencyCode)
        (view.findViewById(R.id.rest) as TextView).text = amountStr

        val warningView = view.findViewById(R.id.warning) as TextView
        if (budgetPrediction.state == BudgetPrediction.EARLY_FINISH_STATE) {
            warningView.setText(R.string.budget_will_be_finished_early)
            warningView.visibility = View.VISIBLE
        } else if (budgetPrediction.rest <= 0) {
            warningView.setText(R.string.budget_is_over)
            warningView.visibility = View.VISIBLE
        } else {
            warningView.visibility = View.GONE
        }

        budgetItemsContainer!!.addView(view)
    }

    private fun showPlanActivity(plan: BudgetPlan) {
        BudgetPlanActivity.actionStart(homeFragment, UiConstants.BUDGET_PLAN_REQUEST_CODE, plan.id!!, plan.name)
    }

    override val shortName: String
        get() = UiConstants.BUDGET_BLOCK
}
