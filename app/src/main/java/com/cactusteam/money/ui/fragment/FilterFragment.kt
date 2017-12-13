package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.*
import rx.Observable
import java.util.*

/**
 * @author vpotapenko
 */
class FilterFragment : BaseDialogFragment() {

    private var listener: ((filter: TransactionExtendedFilter?, filterInfo: FilterInformation) -> Unit)? = null
    private var lastFilterInfo: FilterInformation? = null

    private var accountsLayout: LinearLayout? = null
    private var expenseLayout: LinearLayout? = null
    private var incomeLayout: LinearLayout? = null

    private var allAccountsCheck: CheckBox? = null
    private var allExpenseCategoriesCheck: CheckBox? = null
    private var allIncomeCategoriesCheck: CheckBox? = null

    private val accounts = ArrayList<Pair<Account, CheckBox>>()
    private val expenseCategories = ArrayList<Pair<Category, CheckBox>>()
    private val incomeCategories = ArrayList<Pair<Category, CheckBox>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(R.string.filter)

        allAccountsCheck = view.findViewById(R.id.all_accounts) as CheckBox
        allAccountsCheck!!.setOnCheckedChangeListener { buttonView, isChecked -> accountsLayout!!.visibility = if (isChecked) View.GONE else View.VISIBLE }
        allExpenseCategoriesCheck = view.findViewById(R.id.all_expense_categories) as CheckBox
        allExpenseCategoriesCheck!!.setOnCheckedChangeListener { buttonView, isChecked -> expenseLayout!!.visibility = if (isChecked) View.GONE else View.VISIBLE }
        allIncomeCategoriesCheck = view.findViewById(R.id.all_income_categories) as CheckBox
        allIncomeCategoriesCheck!!.setOnCheckedChangeListener { buttonView, isChecked -> incomeLayout!!.visibility = if (isChecked) View.GONE else View.VISIBLE }

        accountsLayout = view.findViewById(R.id.accounts) as LinearLayout
        expenseLayout = view.findViewById(R.id.expense) as LinearLayout
        incomeLayout = view.findViewById(R.id.income) as LinearLayout

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        view.findViewById(R.id.cancel_btn).setOnClickListener { dismiss() }

        view.findViewById(R.id.ok_btn).setOnClickListener { okClicked() }

        loadData()
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
                    accountsLoaded(r[0] as List<Account>)
                    expenseLoaded(r[1] as List<Category>)
                    incomeLoaded(r[2] as List<Category>)

                    handleLastFilterInfo()
                    hideProgress()
                },
                { e ->
                    hideProgress()
                    showError(e.message)
                }
        )
        compositeSubscription.add(s)
    }

    private fun accountsLoaded(accounts: List<Account>) {
        for (account in accounts) {
            val view = View.inflate(activity, R.layout.fragment_filter_item, null)
            val itemCheck = view.findViewById(R.id.item_check) as CheckBox
            itemCheck.text = account.name

            this.accounts.add(Pair(account, itemCheck))
            accountsLayout!!.addView(view)
        }
    }

    private fun expenseLoaded(categories: List<Category>) {
        for (category in categories) {
            val view = View.inflate(activity, R.layout.fragment_filter_item, null)
            val itemCheck = view.findViewById(R.id.item_check) as CheckBox
            itemCheck.text = category.name

            this.expenseCategories.add(Pair(category, itemCheck))
            expenseLayout!!.addView(view)
        }
    }

    private fun incomeLoaded(categories: List<Category>) {
        for (category in categories) {
            val view = View.inflate(activity, R.layout.fragment_filter_item, null)
            val itemCheck = view.findViewById(R.id.item_check) as CheckBox
            itemCheck.text = category.name

            this.incomeCategories.add(Pair(category, itemCheck))
            incomeLayout!!.addView(view)
        }
    }

    private fun handleLastFilterInfo() {
        if (lastFilterInfo != null) {

            if (lastFilterInfo!!.allAccountsCheck) {
                allAccountsCheck!!.isChecked = true
            } else {
                allAccountsCheck!!.isChecked = false
                for (pair in accounts) {
                    pair.second.isChecked = lastFilterInfo!!.checkedAccounts.contains(pair.first.id)
                }
            }

            if (lastFilterInfo!!.allExpenseCategoriesCheck) {
                allExpenseCategoriesCheck!!.isChecked = true
            } else {
                allExpenseCategoriesCheck!!.isChecked = false
                for (pair in expenseCategories) {
                    pair.second.isChecked = lastFilterInfo!!.checkedExpenseCategories.contains(pair.first.id)
                }
            }

            if (lastFilterInfo!!.allIncomeCategoriesCheck) {
                allIncomeCategoriesCheck!!.isChecked = true
            } else {
                allIncomeCategoriesCheck!!.isChecked = false
                for (pair in incomeCategories) {
                    pair.second.isChecked = lastFilterInfo!!.checkedIncomeCategories.contains(pair.first.id)
                }
            }
        }
    }

    private fun okClicked() {
        val filter = TransactionExtendedFilter()
        filter.setAccountFilter(createAccountsFilter())
        filter.setAccountName(createAccountDescription())

        val categoryFilter = OrTransactionFilters()
        categoryFilter.addFilter(createExpenseFilter())
        categoryFilter.addFilter(createIncomeFilter())
        filter.setCategoryFilter(categoryFilter)
        filter.setCategoryName(createCategoryDescription())

        val info = createFilterInfo()

        listener!!(filter, info)

        dismiss()
    }

    private fun createFilterInfo(): FilterInformation {
        val filterInformation = FilterInformation()

        if (allAccountsCheck!!.isChecked) {
            filterInformation.allAccountsCheck = true
        } else {
            filterInformation.allAccountsCheck = false
            accounts
                    .filter { it.second.isChecked }
                    .forEach { filterInformation.checkedAccounts.add(it.first.id) }
        }
        if (allExpenseCategoriesCheck!!.isChecked) {
            filterInformation.allExpenseCategoriesCheck = true
        } else {
            filterInformation.allExpenseCategoriesCheck = false
            expenseCategories
                    .filter { it.second.isChecked }
                    .forEach { filterInformation.checkedExpenseCategories.add(it.first.id) }
        }

        if (allIncomeCategoriesCheck!!.isChecked) {
            filterInformation.allIncomeCategoriesCheck = true
        } else {
            filterInformation.allIncomeCategoriesCheck = false
            incomeCategories
                    .filter { it.second.isChecked }
                    .forEach { filterInformation.checkedIncomeCategories.add(it.first.id) }
        }

        return filterInformation
    }

    private fun createCategoryDescription(): String {
        val sb = StringBuilder()
        sb.append(createExpenseDescription())

        val incomeDescription = createIncomeDescription()
        if (!incomeDescription.isNullOrBlank()) {
            if (sb.isNotEmpty()) sb.append("<br/>")
            sb.append(incomeDescription)
        }

        return sb.toString()
    }

    private fun createIncomeDescription(): String {
        val sb = StringBuilder()
        if (!allIncomeCategoriesCheck!!.isChecked) {
            for (pair in incomeCategories) {
                if (pair.second.isChecked) {
                    if (sb.isNotEmpty()) sb.append(", ")
                    sb.append(pair.first.name)
                }
            }
            if (sb.isEmpty()) sb.append(getString(R.string.no_data))
        }
        return if (sb.isNotEmpty()) "<strong>" + getString(R.string.income_label) + "</strong>: " + sb.toString() else ""
    }

    private fun createExpenseDescription(): String {
        val sb = StringBuilder()
        if (!allExpenseCategoriesCheck!!.isChecked) {
            for (pair in expenseCategories) {
                if (pair.second.isChecked) {
                    if (sb.isNotEmpty()) sb.append(", ")
                    sb.append(pair.first.name)
                }
            }
            if (sb.isEmpty()) sb.append(getString(R.string.no_data))
        }
        return if (sb.isNotEmpty()) "<strong>" + getString(R.string.expense_label) + "</strong>: " + sb.toString() else ""
    }

    private fun createAccountDescription(): String {
        val sb = StringBuilder()
        if (!allAccountsCheck!!.isChecked) {
            for (pair in accounts) {
                if (pair.second.isChecked) {
                    if (sb.isNotEmpty()) sb.append(", ")
                    sb.append(pair.first.name)
                }
            }
            if (sb.isEmpty()) sb.append(getString(R.string.no_data))
        }
        return if (sb.isNotEmpty()) "<strong>" + getString(R.string.accounts_title) + "</strong>: " + sb.toString() else ""
    }

    private fun createExpenseFilter(): ITransactionFilter {
        if (allExpenseCategoriesCheck!!.isChecked) {
            return TypeTransactionFilter(Transaction.EXPENSE)
        } else {
            val filter = OrTransactionFilters()
            expenseCategories
                    .filter { it.second.isChecked }
                    .forEach { filter.addFilter(CategoryTransactionFilter(it.first.id!!)) }
            return filter
        }
    }

    private fun createIncomeFilter(): ITransactionFilter {
        if (allIncomeCategoriesCheck!!.isChecked) {
            return TypeTransactionFilter(Transaction.INCOME)
        } else {
            val filter = OrTransactionFilters()
            incomeCategories
                    .filter { it.second.isChecked }
                    .forEach { filter.addFilter(CategoryTransactionFilter(it.first.id!!)) }
            return filter
        }
    }

    private fun createAccountsFilter(): ITransactionFilter {
        if (allAccountsCheck!!.isChecked) {
            return AllowAllTransactionFilter.instance
        } else {
            val filter = OrTransactionFilters()
            accounts
                    .filter { it.second.isChecked }
                    .forEach { filter.addFilter(AccountTransactionFilter(it.first.id!!)) }

            return filter
        }
    }

    class FilterInformation {

        var allAccountsCheck: Boolean = false
        var allExpenseCategoriesCheck: Boolean = false
        var allIncomeCategoriesCheck: Boolean = false

        val checkedAccounts: MutableList<Long> = ArrayList()
        val checkedExpenseCategories: MutableList<Long> = ArrayList()
        val checkedIncomeCategories: MutableList<Long> = ArrayList()
    }

    companion object {

        fun build(listener: ((filter: TransactionExtendedFilter?, filterInfo: FilterInformation) -> Unit)?, lastFilterInfo: FilterInformation?): FilterFragment {
            val fragment = FilterFragment()
            fragment.listener = listener
            fragment.lastFilterInfo = lastFilterInfo
            return fragment
        }
    }
}
