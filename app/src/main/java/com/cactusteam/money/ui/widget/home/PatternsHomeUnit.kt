package com.cactusteam.money.ui.widget.home

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.dao.TransactionPattern
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.EditTransactionPatternActivity
import com.cactusteam.money.ui.activity.NewTransactionPatternActivity
import com.cactusteam.money.ui.fragment.HomeFragment

/**
 * @author vpotapenko
 */
class PatternsHomeUnit(homeFragment: HomeFragment) : BaseHomeUnit(homeFragment) {

    private var patternsContainer: LinearLayout? = null
    private var patternsProgress: View? = null

    override fun getLayoutRes(): Int {
        return R.layout.fragment_home_patterns_unit
    }

    override fun initializeView() {
        patternsContainer = getView()!!.findViewById(R.id.patterns_container) as LinearLayout
        patternsProgress = getView()!!.findViewById(R.id.patterns_progress)

        getView()!!.findViewById(R.id.create_pattern_btn).setOnClickListener { createPatternClicked() }
    }

    override fun update() {
        loadPatterns()
    }

    private fun loadPatterns() {
        patternsContainer!!.visibility = View.GONE
        patternsProgress!!.visibility = View.VISIBLE

        val s = homeFragment.dataManager.patternService
                .getPatterns()
                .subscribe(
                        { r ->
                            patternsLoaded(r)
                        },
                        { e ->
                            patternsProgress!!.visibility = View.GONE
                            homeFragment.showError(e.message)
                        }
                )
        homeFragment.compositeSubscription.add(s)
    }

    private fun patternsLoaded(patterns: List<TransactionPattern>) {
        patternsContainer!!.removeAllViews()
        for (pattern in patterns) {
            createPatternView(pattern)
        }
        if (patterns.isEmpty()) {
            patternsContainer!!.addView(View.inflate(homeFragment.activity, R.layout.view_no_data, null))
        }
        patternsContainer!!.visibility = View.VISIBLE
        patternsProgress!!.visibility = View.GONE
    }

    private fun createPatternView(pattern: TransactionPattern) {
        val view = View.inflate(homeFragment.activity, R.layout.fragment_home_pattern, null)

        val amountStr = UiUtils.formatCurrency(pattern.amount, pattern.sourceAccount.currencyCode)
        (view.findViewById(R.id.amount) as TextView).text = amountStr
        (view.findViewById(R.id.name) as TextView).text = pattern.name
        (view.findViewById(R.id.account) as TextView).text = pattern.sourceAccount.name
        when (pattern.type) {
            Transaction.EXPENSE -> {
                val category: Category? = pattern.category
                (view.findViewById(R.id.dest) as TextView).text = if (category != null) category.name else ""
                (view.findViewById(R.id.type_marker) as ImageView).setImageResource(R.drawable.ic_expense_transaction)
            }
            Transaction.INCOME -> {
                val category = pattern.category
                (view.findViewById(R.id.dest) as TextView).text = if (category != null) category.name else ""
                (view.findViewById(R.id.type_marker) as ImageView).setImageResource(R.drawable.ic_income_transaction)
            }
            Transaction.TRANSFER -> {
                (view.findViewById(R.id.dest) as TextView).text = pattern.destAccount.name
                (view.findViewById(R.id.type_marker) as ImageView).setImageResource(R.drawable.ic_transfer_transaction)
            }
        }


        view.findViewById(R.id.create).setOnClickListener { homeFragment.createTransactionFromPatternClicked(pattern) }

        view.findViewById(R.id.edit).setOnClickListener { editPatternClicked(pattern) }

        patternsContainer!!.addView(view)
    }

    private fun editPatternClicked(pattern: TransactionPattern) {
        EditTransactionPatternActivity.actionStart(homeFragment, UiConstants.PATTERN_REQUEST_CODE, pattern.id!!)
    }

    private fun createPatternClicked() {
        NewTransactionPatternActivity.actionStart(homeFragment, UiConstants.PATTERN_REQUEST_CODE)
    }

    override val shortName: String
        get() = UiConstants.PATTERNS_BLOCK
}
