package com.cactusteam.money.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.data.prediction.BudgetPrediction
import com.cactusteam.money.ui.ListItem
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.BudgetPlanActivity
import com.cactusteam.money.ui.activity.EditBudgetPlanActivity
import java.util.*

/**
 * @author vpotapenko
 */
class BudgetFragment : BaseMainFragment() {

    private val budgetPrediction = BudgetPrediction()

    private var mainCurrencyCode: String? = null

    private var listView: RecyclerView? = null
    private var currentPlansAmounts: CurrentPlansAmounts? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.BUDGET_PLAN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadPlans()
            }
        } else if (requestCode == UiConstants.EDIT_BUDGET_PLAN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadPlans()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode

        view.findViewById(R.id.create_plan_btn).setOnClickListener { showNewPlanActivity() }

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        loadPlans()
    }

    private fun loadPlans() {
        showProgress()
        val s = dataManager.budgetService
                .getBudgets(true)
                .subscribe(
                        { r ->
                            hideProgress()
                            plansLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun plansLoaded(plans: List<BudgetPlan>) {
        val current = ArrayList<BudgetPlan>()
        val finished = ArrayList<BudgetPlan>()
        for (plan in plans) {
            if (plan.isFinished) {
                finished.add(plan)
            } else {
                current.add(plan)
            }
        }
        Collections.sort(current, CURRENT_BUDGET_PLAN_COMPARATOR)

        val adapter = listView!!.adapter as ListAdapter
        adapter.items.clear()

        if (!current.isEmpty()) {
            adapter.items.add(ListItem(GROUP, getString(R.string.current_budget_plans)))

            currentPlansAmounts = CurrentPlansAmounts()
            for (plan in current) {
                adapter.items.add(ListItem(REGULAR, plan))
                currentPlansAmounts!!.limit += plan.limit
                currentPlansAmounts!!.amount += plan.expense
            }
            adapter.items.add(ListItem(FOOTER, currentPlansAmounts))
        }

        if (!finished.isEmpty()) {
            adapter.items.add(ListItem(GROUP, getString(R.string.finished_budget_plans)))
        }
        for (plan in finished) {
            adapter.items.add(ListItem(REGULAR, plan))
        }
        adapter.notifyDataSetChanged()

        hideProgress()
    }

    private fun showNewPlanActivity() {
        EditBudgetPlanActivity.actionStart(this, UiConstants.EDIT_BUDGET_PLAN_REQUEST_CODE)
    }

    private fun showPlanActivity(plan: BudgetPlan) {
        BudgetPlanActivity.actionStart(this, UiConstants.BUDGET_PLAN_REQUEST_CODE, plan.id!!, plan.name)
    }

    override fun dataChanged() {
        loadPlans()
    }

    private inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindFooter(currentPlansAmounts: CurrentPlansAmounts) {
            var amountStr = UiUtils.formatCurrency(currentPlansAmounts.limit, mainCurrencyCode)
            (itemView.findViewById(R.id.limit) as TextView).text = getString(R.string.limit_pattern, amountStr)

            amountStr = UiUtils.formatCurrency(currentPlansAmounts.amount, mainCurrencyCode)
            (itemView.findViewById(R.id.expense) as TextView).text = getString(R.string.expense_pattern, amountStr)

            val rest = currentPlansAmounts.limit - currentPlansAmounts.amount
            amountStr = UiUtils.formatCurrency(rest, mainCurrencyCode)
            (itemView.findViewById(R.id.rest) as TextView).text = getString(R.string.rest_pattern, amountStr)
        }

        fun bindItem(plan: BudgetPlan) {
            itemView.findViewById(R.id.list_item).setOnClickListener { showPlanActivity(plan) }

            (itemView.findViewById(R.id.name) as TextView).text = plan.name

            val periodStr = DateUtils.formatDateRange(activity, plan.start.time, plan.finish.time, DateUtils.FORMAT_SHOW_DATE)
            when (plan.type) {
                BudgetPlan.ONE_TIME_TYPE -> (itemView.findViewById(R.id.period) as TextView).text = getString(R.string.one_time_period_pattern, periodStr)
                BudgetPlan.PERIODICAL_TYPE -> (itemView.findViewById(R.id.period) as TextView).text = getString(R.string.periodical_period_pattern, periodStr)
            }

            val limitProgress = itemView.findViewById(R.id.limit_progress) as ProgressBar
            limitProgress.isIndeterminate = true
            limitProgress.visibility = View.VISIBLE

            val iconView = itemView.findViewById(R.id.icon) as ImageView
            iconView.setImageDrawable(null)

            itemView.findViewById(R.id.amount_container).visibility = View.GONE

            val warningView = itemView.findViewById(R.id.warning) as TextView
            warningView.visibility = View.GONE

            val amount = plan.expense
            budgetPrediction.calculate(plan.start, plan.finish, amount, plan.limit)

            itemView.findViewById(R.id.amount_container).visibility = View.VISIBLE

            val amountView = itemView.findViewById(R.id.amount) as TextView

            var amountStr = UiUtils.formatCurrency(amount, mainCurrencyCode)
            amountView.text = amountStr

            amountStr = UiUtils.formatCurrency(budgetPrediction.rest, mainCurrencyCode)
            (itemView.findViewById(R.id.rest) as TextView).text = amountStr

            limitProgress.isIndeterminate = false

            val max = Math.round(plan.limit).toInt()
            val progress = Math.round(amount).toInt()

            limitProgress.max = max
            limitProgress.progress = Math.min(max, progress)

            if (plan.isFinished) {
                iconView.setImageResource(if (max < progress) R.drawable.ic_plan_fail else R.drawable.ic_plan_done)

                if (budgetPrediction.rest < 0) {
                    warningView.setText(R.string.budget_is_over)
                    warningView.visibility = View.VISIBLE
                }
            } else {
                if (budgetPrediction.state == BudgetPrediction.EARLY_FINISH_STATE) {
                    warningView.setText(R.string.budget_will_be_finished_early)
                    warningView.visibility = View.VISIBLE
                } else if (budgetPrediction.rest <= 0) {
                    warningView.setText(R.string.budget_is_over)
                    warningView.visibility = View.VISIBLE
                }

                if (max <= progress) {
                    iconView.setImageResource(R.drawable.ic_plan_fail)
                } else {
                    val resId = if (budgetPrediction.state == BudgetPrediction.EARLY_FINISH_STATE) R.drawable.ic_plan_warn else R.drawable.ic_plan
                    iconView.setImageResource(resId)
                }
            }
        }

        fun bindGroup(groupName: String) {
            (itemView.findViewById(R.id.name) as TextView).text = groupName
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {

        val items = ArrayList<ListItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
            var layoutId = 0
            when (viewType) {
                REGULAR -> layoutId = R.layout.fragment_budget_item
                GROUP -> layoutId = R.layout.fragment_budget_subhead
                FOOTER -> layoutId = R.layout.fragment_budget_footer
            }
            val v = LayoutInflater.from(activity).inflate(layoutId, parent, false)
            return ListViewHolder(v)
        }

        override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
            val type = getItemViewType(position)
            if (type == REGULAR) {
                holder.bindItem(items[position].obj as BudgetPlan)
            } else if (type == GROUP) {
                holder.bindGroup(items[position].obj as String)
            } else if (type == FOOTER) {
                holder.bindFooter(items[position].obj as CurrentPlansAmounts)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].type
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }

    private class CurrentPlansAmounts {
        var limit: Double = 0.0
        var amount: Double = 0.0
    }

    companion object {

        private val REGULAR = 0
        private val GROUP = 1
        private val FOOTER = 2

        private val CURRENT_BUDGET_PLAN_COMPARATOR = Comparator<com.cactusteam.money.data.dao.BudgetPlan> { lhs, rhs -> lhs.name.compareTo(rhs.name) }

        fun build(): BudgetFragment {
            return BudgetFragment()
        }
    }
}
