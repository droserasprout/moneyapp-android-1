package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.io.ImportResult
import com.cactusteam.money.ui.activity.ImportTransactionsActivity
import com.cactusteam.money.ui.widget.ImportAccountView
import com.cactusteam.money.ui.widget.ImportCategoryView
import rx.Observable

/**
 * @author vpotapenko
 */
class ImportTransactionsSecondFragment : BaseFragment() {

    private var accountsLayout: LinearLayout? = null
    private var accountsProgress: View? = null

    private var expenseCategoriesLayout: LinearLayout? = null
    private var expenseProgress: View? = null

    private var incomeCategoriesLayout: LinearLayout? = null
    private var incomeProgress: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_transactions_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById(R.id.next_btn).setOnClickListener { nextClicked() }
        view.findViewById(R.id.previous_btn).setOnClickListener { previousClicked() }


        accountsLayout = view.findViewById(R.id.accounts) as LinearLayout
        accountsProgress = view.findViewById(R.id.accounts_progress)

        expenseCategoriesLayout = view.findViewById(R.id.expense_categories) as LinearLayout
        expenseProgress = view.findViewById(R.id.expense_progress)

        incomeCategoriesLayout = view.findViewById(R.id.income_categories) as LinearLayout
        incomeProgress = view.findViewById(R.id.income_progress)

        loadData()

        initializeHeaderView(view)
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadData() {
        showProgress()

        val o1 = dataManager.accountService.getAccounts()
        val o2 = dataManager.categoryService.getCategories(Category.EXPENSE)
        val o3 = dataManager.categoryService.getCategories(Category.INCOME)
        val s = Observable.zip(o1, o2, o3, { i1, i2, i3 ->
            listOf(i1, i2, i3)
        }).subscribe(
                { r ->
                    hideProgress()

                    updateAccountsView(r[0] as List<Account>)
                    updateExpenseViews(r[1] as List<Category>)
                    updateIncomeViews(r[2] as List<Category>)
                },
                { e ->
                    hideProgress()
                    showError(e.message)
                }
        )
        compositeSubscription.add(s)
    }

    override fun showProgress() {
        accountsProgress!!.visibility = View.VISIBLE
        accountsLayout!!.visibility = View.GONE

        expenseCategoriesLayout!!.visibility = View.GONE
        expenseProgress!!.visibility = View.VISIBLE

        incomeCategoriesLayout!!.visibility = View.GONE
        incomeProgress!!.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        accountsProgress!!.visibility = View.GONE
        accountsLayout!!.visibility = View.VISIBLE

        expenseCategoriesLayout!!.visibility = View.VISIBLE
        expenseProgress!!.visibility = View.GONE

        incomeCategoriesLayout!!.visibility = View.VISIBLE
        incomeProgress!!.visibility = View.GONE
    }

    private fun updateIncomeViews(categories: List<Category>) {
        updateCategoriesViews(incomeCategoriesLayout!!, categories, Category.INCOME)
    }

    private fun updateExpenseViews(categories: List<Category>) {
        updateCategoriesViews(expenseCategoriesLayout!!, categories, Category.EXPENSE)
    }

    private fun updateCategoriesViews(categoriesLayout: LinearLayout, categories: List<Category>, type: Int) {
        categoriesLayout.removeAllViews()
        importActivity.schema!!.getAllCategories(type)
                .map { ImportCategoryView(activity, it, categories) }
                .forEach { categoriesLayout.addView(it) }
    }

    private fun updateAccountsView(accounts: List<Account>) {
        accountsLayout!!.removeAllViews()
        importActivity.schema!!.allAccounts
                .map { ImportAccountView(activity, it, accounts) }
                .forEach { accountsLayout!!.addView(it) }
    }

    private fun initializeHeaderView(view: View) {
        val pathView = view.findViewById(R.id.path) as TextView
        pathView.text = importActivity.sourceFile!!.path

        val schema = importActivity.schema

        val linesCountView = view.findViewById(R.id.lines_count) as TextView
        linesCountView.text = schema!!.linesCount.toString()

        val basLinesCountView = view.findViewById(R.id.bad_lines_count) as TextView
        basLinesCountView.text = schema.badLinesCount.toString()
    }

    private fun previousClicked() {
        importActivity.previousStep()
    }

    private fun nextClicked() {
        applyAccounts()
        applyCategories()

        importActivity.showBlockingProgressWithUpdate(getString(R.string.waiting))
        val s = dataManager.systemService
                .importTransactions(importActivity.sourceFile!!, importActivity.schema!!)
                .subscribe(
                        { p ->
                            when (p) {
                                is Pair<*, *> -> {
                                    importActivity.updateBlockingProgress(p.first as Int, p.second as Int)
                                }
                                is ImportResult -> {
                                    importActivity.importResult = p
                                }
                            }
                        },
                        { e ->
                            importActivity.hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            importActivity.hideBlockingProgress()
                            importActivity.nextStep()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun applyAccounts() {
        (0..accountsLayout!!.childCount - 1)
                .map { accountsLayout!!.getChildAt(it) }
                .filterIsInstance<ImportAccountView>()
                .forEach { it.apply() }
    }

    private fun applyCategories() {
        (0..expenseCategoriesLayout!!.childCount - 1)
                .map { expenseCategoriesLayout!!.getChildAt(it) }
                .filterIsInstance<ImportCategoryView>()
                .forEach { it.apply() }
        (0..incomeCategoriesLayout!!.childCount - 1)
                .map { incomeCategoriesLayout!!.getChildAt(it) }
                .filterIsInstance<ImportCategoryView>()
                .forEach { it.apply() }
    }


    private val importActivity: ImportTransactionsActivity
        get() = activity as ImportTransactionsActivity

    companion object {

        fun build(): ImportTransactionsSecondFragment {
            return ImportTransactionsSecondFragment()
        }
    }
}