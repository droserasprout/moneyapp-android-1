package com.cactusteam.money.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.ui.UiConstants

/**
 * @author vpotapenko
 */
class SortingSettingsActivity : BaseActivity("SortingSettingsActivity") {

    private var sortAccountTypeView: TextView? = null
    private var sortExpenseTypeView: TextView? = null
    private var sortIncomeTypeView: TextView? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.SORTING_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                updateView()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sorting_settings)

        initializeToolbar()

        findViewById(R.id.sort_account_type_container).setOnClickListener { sortAccountTypeClicked() }

        findViewById(R.id.sort_expense_type_container).setOnClickListener { sortExpenseTypeClicked() }

        findViewById(R.id.sort_income_type_container).setOnClickListener { sortIncomeTypeClicked() }

        sortAccountTypeView = findViewById(R.id.sort_account_type) as TextView
        sortExpenseTypeView = findViewById(R.id.sort_expense_type) as TextView
        sortIncomeTypeView = findViewById(R.id.sort_income_type) as TextView

        updateView()
    }

    private fun updateView() {
        val appPreferences = MoneyApp.instance.appPreferences
        when (appPreferences.accountSortType) {
            Account.TYPE_NAME_SORT -> sortAccountTypeView!!.text = getString(R.string.sorting_type_and_name)
            Account.NAME_SORT -> sortAccountTypeView!!.text = getString(R.string.sorting_name)
            Account.CUSTOM_SORT -> sortAccountTypeView!!.text = getString(R.string.sorting_custom)
        }

        when (appPreferences.expenseSortType) {
            Category.NAME_SORT -> sortExpenseTypeView!!.text = getString(R.string.sorting_name)
            Category.FREQUENCY_SORT -> sortExpenseTypeView!!.text = getString(R.string.sorting_frequency)
            Category.CUSTOM_SORT -> sortExpenseTypeView!!.text = getString(R.string.sorting_custom)
        }

        when (appPreferences.incomeSortType) {
            Category.NAME_SORT -> sortIncomeTypeView!!.text = getString(R.string.sorting_name)
            Category.FREQUENCY_SORT -> sortIncomeTypeView!!.text = getString(R.string.sorting_frequency)
            Category.CUSTOM_SORT -> sortIncomeTypeView!!.text = getString(R.string.sorting_custom)
        }
    }

    private fun sortAccountTypeClicked() {
        SortingAccountsActivity.actionStart(this, UiConstants.SORTING_REQUEST_CODE)
    }

    private fun sortIncomeTypeClicked() {
        SortingCategoriesActivity.actionStart(this, Category.INCOME, UiConstants.SORTING_REQUEST_CODE)
    }

    private fun sortExpenseTypeClicked() {
        SortingCategoriesActivity.actionStart(this, Category.EXPENSE, UiConstants.SORTING_REQUEST_CODE)
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, SortingSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
