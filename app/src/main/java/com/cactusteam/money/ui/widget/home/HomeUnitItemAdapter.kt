package com.cactusteam.money.ui.widget.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.ui.UiConstants
import com.woxthebox.draglistview.DragItemAdapter

/**
 * @author vpotapenko
 */
class HomeUnitItemAdapter(list: List<HomeUnitInfo>) : DragItemAdapter<HomeUnitInfo, HomeUnitItemAdapter.ViewHolder>() {

    private val grabHandleId: Int

    init {
        this.grabHandleId = R.id.drag_area
        setHasStableIds(true)
        itemList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_home_settings_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.bind(itemList[position])
    }

    override fun getItemId(position: Int): Long {
        return mItemList[position].id.toLong()
    }

    inner class ViewHolder(itemView: View) : DragItemAdapter.ViewHolder(itemView, grabHandleId, false) {

        internal fun bind(unitInfo: HomeUnitInfo) {
            (itemView.findViewById(R.id.icon) as ImageView).setImageResource(unitInfo.iconResId)
            (itemView.findViewById(R.id.name) as TextView).text = unitInfo.name

            val checkBox = itemView.findViewById(R.id.show) as CheckBox
            checkBox.isChecked = unitInfo.show
            checkBox.setOnCheckedChangeListener { buttonView, isChecked -> showChanged(unitInfo, isChecked) }
        }
    }

    private fun showChanged(unit: HomeUnitInfo, show: Boolean) {
        unit.show = show

        val appPreferences = MoneyApp.instance.appPreferences

        when (unit.shortName) {
            UiConstants.TOTAL_BALANCE_BLOCK -> appPreferences.isMainShowTotalBalance = show
            UiConstants.ACCOUNTS_BLOCK -> appPreferences.isMainShowAccounts = show
            UiConstants.PATTERNS_BLOCK -> appPreferences.isMainShowPatterns = show
            UiConstants.BUDGET_BLOCK -> appPreferences.isMainShowBudget = show
            UiConstants.TRANSACTIONS_BLOCK -> appPreferences.isMainShowTransactions = show
            UiConstants.BALANCE_CHART_BLOCK -> appPreferences.isMainShowBalanceChart = show
            UiConstants.EXPENSE_CHART_BLOCK -> appPreferences.isMainShowExpenseChart = show
            UiConstants.INCOME_CHART_BLOCK -> appPreferences.isMainShowIncomeChart = show
            UiConstants.BUDGET_SUMMARY_BLOCK -> appPreferences.isMainShowBudgetSummary = show
        }
    }
}
