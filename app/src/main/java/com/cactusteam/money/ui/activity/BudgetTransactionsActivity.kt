package com.cactusteam.money.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.TransactionsFragment

/**
 * @author vpotapenko
 */
class BudgetTransactionsActivity : BaseActivity("BudgetTransactionsActivity") {

    private var budgetId: Long = 0

    private var filterTitle: TextView? = null
    private var filterTitleProgress: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_transactions)

        initializeToolbar()

        filterTitle = findViewById(R.id.filter_title) as TextView
        filterTitleProgress = findViewById(R.id.title_progress)

        loadBudget()
    }

    private fun loadBudget() {
        filterTitle!!.visibility = View.GONE
        filterTitleProgress!!.visibility = View.VISIBLE

        val s = dataManager.budgetService
                .getBudget(budgetId)
                .subscribe(
                        { r ->
                            val s = UiUtils.formatDataObject(this, DataConstants.BUDGET_TYPE, r)
                            filterTitle!!.text = s
                            filterTitle!!.visibility = View.VISIBLE
                            filterTitleProgress!!.visibility = View.GONE

                            budgetLoaded(r)
                        },
                        { e ->
                            filterTitle!!.visibility = View.VISIBLE
                            filterTitleProgress!!.visibility = View.GONE

                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun budgetLoaded(budgetPlan: BudgetPlan) {
        val fragment = TransactionsFragment.build()
        fragment.setFilter(budgetPlan.createFilter())
        fragment.setFromDate(budgetPlan.start)
        fragment.setToDate(budgetPlan.finish)

        showTransaction(fragment)
    }

    private fun showTransaction(fragment: TransactionsFragment) {
        val fragmentManager = fragmentManager
        val ft = fragmentManager.beginTransaction()
        ft.replace(R.id.content_frame, fragment, "transactions")
        ft.commit()
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                budgetId = extras.getLong(UiConstants.EXTRA_ID)
            }
        }
    }

    companion object {

        fun actionStart(activity: Activity, requestCode: Int, budgetId: Long) {
            val intent = Intent(activity, BudgetTransactionsActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, budgetId)

            activity.startActivityForResult(intent, requestCode)
        }
    }
}
