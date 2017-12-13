package com.cactusteam.money.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.widget.home.HomeUnitInfo
import com.cactusteam.money.ui.widget.home.HomeUnitItemAdapter
import com.cactusteam.money.ui.widget.home.UnitContainer
import com.woxthebox.draglistview.DragListView

/**
 * @author vpotapenko
 */
class HomeSettingsActivity : BaseActivity("HomeSettingsActivity") {

    private var listView: DragListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_settings)

        initializeToolbar()

        listView = findViewById(R.id.list) as DragListView
        listView!!.setDragListListener(object : DragListView.DragListListenerAdapter() {
            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                updateOrderUnits()
            }
        })

        listView!!.setLayoutManager(LinearLayoutManager(this))
        listView!!.setCanDragHorizontally(false)

        val units = createUnits()
        listView!!.setAdapter(HomeUnitItemAdapter(units), true)
    }

    private fun createUnits(): List<HomeUnitInfo> {
        val appPreferences = MoneyApp.instance.appPreferences
        val mainBlockOrder = appPreferences.mainBlockOrder

        val unitContainer = UnitContainer<HomeUnitInfo>()
        unitContainer.add(HomeUnitInfo(0, R.drawable.ic_balance, UiConstants.TOTAL_BALANCE_BLOCK, getString(R.string.balance)))
        unitContainer.add(HomeUnitInfo(1, R.drawable.ic_accounts, UiConstants.ACCOUNTS_BLOCK, getString(R.string.accounts_title)))
        unitContainer.add(HomeUnitInfo(2, R.drawable.ic_patterns, UiConstants.PATTERNS_BLOCK, getString(R.string.patterns)))
        unitContainer.add(HomeUnitInfo(3, R.drawable.ic_budget_normal, UiConstants.BUDGET_BLOCK, getString(R.string.budget_details)))
        unitContainer.add(HomeUnitInfo(4, R.drawable.ic_transactions_normal, UiConstants.TRANSACTIONS_BLOCK, getString(R.string.transactions_title)))
        unitContainer.add(HomeUnitInfo(5, R.drawable.ic_balance_chart, UiConstants.BALANCE_CHART_BLOCK, getString(R.string.balance_chart)))
        unitContainer.add(HomeUnitInfo(6, R.drawable.ic_categories_chart, UiConstants.EXPENSE_CHART_BLOCK, getString(R.string.expense_label)))
        unitContainer.add(HomeUnitInfo(7, R.drawable.ic_categories_chart, UiConstants.INCOME_CHART_BLOCK, getString(R.string.income_label)))
        unitContainer.add(HomeUnitInfo(8, R.drawable.ic_budget_normal, UiConstants.BUDGET_SUMMARY_BLOCK, getString(R.string.budget_title)))

        val preparedUnits = unitContainer.prepareUnitsByOrder(mainBlockOrder)

        for (unit in preparedUnits) {
            when (unit.shortName) {
                UiConstants.TOTAL_BALANCE_BLOCK -> unit.show = appPreferences.isMainShowTotalBalance
                UiConstants.ACCOUNTS_BLOCK -> unit.show = appPreferences.isMainShowAccounts
                UiConstants.PATTERNS_BLOCK -> unit.show = appPreferences.isMainShowPatterns
                UiConstants.BUDGET_BLOCK -> unit.show = appPreferences.isMainShowBudget
                UiConstants.TRANSACTIONS_BLOCK -> unit.show = appPreferences.isMainShowTransactions
                UiConstants.BALANCE_CHART_BLOCK -> unit.show = appPreferences.isMainShowBalanceChart
                UiConstants.EXPENSE_CHART_BLOCK -> unit.show = appPreferences.isMainShowExpenseChart
                UiConstants.INCOME_CHART_BLOCK -> unit.show = appPreferences.isMainShowIncomeChart
                UiConstants.BUDGET_SUMMARY_BLOCK -> unit.show = appPreferences.isMainShowBudgetSummary
            }
        }

        return preparedUnits
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateOrderUnits() {
        val itemList: List<HomeUnitInfo> = listView!!.adapter.itemList as List<HomeUnitInfo>
        val sb = StringBuilder()
        for (unit in itemList) {
            if (sb.isNotEmpty()) sb.append(',')
            sb.append(unit.shortName)
        }

        val appPreferences = MoneyApp.instance.appPreferences
        appPreferences.mainBlockOrder = sb.toString()
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, HomeSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
