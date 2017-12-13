package com.cactusteam.money.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.TransactionPattern
import com.cactusteam.money.sync.ISyncListener
import com.cactusteam.money.sync.SyncService
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.activity.CalculatorActivity
import com.cactusteam.money.ui.activity.MainActivity
import com.cactusteam.money.ui.widget.home.*
import java.util.*

class HomeFragment : BaseMainFragment(), ISyncListener {

    private val units = ArrayList<IHomeUnit>()
    private var unitsContainer: LinearLayout? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.PATTERN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.BUDGET_PLAN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.ACCOUNT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.DEBT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.CATEGORIES_REPORT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.CALCULATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val amount = Math.abs(data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0))
                val referenceId = data.getStringExtra(UiConstants.EXTRA_ID)
                if (amount > 0 && referenceId != null) {
                    try {
                        val patternId = referenceId.toLong()
                        createTransactionFromPattern(patternId, amount)
                    } catch (e: NumberFormatException) {
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_home, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val syncManager = MoneyApp.instance.syncManager
        menu.findItem(R.id.sync).isVisible = syncManager.isSyncConnected

        super.onPrepareOptionsMenu(menu)
    }

    override fun onDestroyView() {
        val eventsController = MoneyApp.instance.syncManager.eventsController
        eventsController.removeListener(this)

        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sync) {
            syncClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun syncClicked() {
        val syncManager = MoneyApp.instance.syncManager
        val eventsController = syncManager.eventsController
        eventsController.addListener(this)

        if (eventsController.isSyncing) {
            // shows because already started
            mainActivity.showBlockingProgressWithUpdate(getString(R.string.sync_title))
        } else {
            SyncService.actionSyncImmediately(activity)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        unitsContainer = view.findViewById(R.id.units_container) as LinearLayout

        createUnits()
        loadData()
    }

    private fun createUnits() {
        val unitContainer = UnitContainer<IHomeUnit>()
        unitContainer.add(TotalBalanceHomeUnit(this))
        unitContainer.add(PatternsHomeUnit(this))
        unitContainer.add(BudgetHomeUnit(this))
        unitContainer.add(TransactionsHomeUnit(this))
        unitContainer.add(AccountsHomeUnit(this))
        unitContainer.add(BalanceChartHomeUnit(this))
        unitContainer.add(ExpenseChartHomeUnit(this))
        unitContainer.add(IncomeChartHomeUnit(this))
        unitContainer.add(BudgetSummaryHomeUnit(this))

        val appPreferences = MoneyApp.instance.appPreferences
        val blockOrder = appPreferences.mainBlockOrder

        val preparedUnits = unitContainer.prepareUnitsByOrder(blockOrder)
        val iterator = preparedUnits.iterator()
        while (iterator.hasNext()) {
            val unit = iterator.next()
            when (unit.shortName) {
                UiConstants.TOTAL_BALANCE_BLOCK -> if (!appPreferences.isMainShowTotalBalance) iterator.remove()
                UiConstants.ACCOUNTS_BLOCK -> if (!appPreferences.isMainShowAccounts) iterator.remove()
                UiConstants.PATTERNS_BLOCK -> if (!appPreferences.isMainShowPatterns) iterator.remove()
                UiConstants.BUDGET_BLOCK -> if (!appPreferences.isMainShowBudget) iterator.remove()
                UiConstants.TRANSACTIONS_BLOCK -> if (!appPreferences.isMainShowTransactions) iterator.remove()
                UiConstants.BALANCE_CHART_BLOCK -> if (!appPreferences.isMainShowBalanceChart) iterator.remove()
                UiConstants.EXPENSE_CHART_BLOCK -> if (!appPreferences.isMainShowExpenseChart) iterator.remove()
                UiConstants.INCOME_CHART_BLOCK -> if (!appPreferences.isMainShowIncomeChart) iterator.remove()
                UiConstants.BUDGET_SUMMARY_BLOCK -> if (!appPreferences.isMainShowBudgetSummary) iterator.remove()
            }
        }

        units.clear()
        units.add(NotesHomeUnit(this))
        units.addAll(preparedUnits)

        unitsContainer!!.removeAllViews()
        for (unit in units) {
            unit.initialize()

            if (unitsContainer!!.childCount > 0) {
                View.inflate(activity, R.layout.horizontal_divider, unitsContainer)
            }
            unitsContainer!!.addView(unit.getView())
        }
    }

    private fun loadData() {
        for (unit in units) {
            unit.update()
        }
    }

    fun createTransactionFromPatternClicked(pattern: TransactionPattern) {
        if (pattern.amount <= 0) {
            CalculatorActivity.actionStart(this, UiConstants.CALCULATOR_REQUEST_CODE, 0.0, null, pattern.id.toString())
        } else {
            createTransactionFromPattern(pattern.id, pattern.amount)
        }
    }

    private fun createTransactionFromPattern(patternId: Long, amount: Double) {
        baseActivity.showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.transactionService
                .createTransactionFromPattern(patternId, amount)
                .subscribe(
                        { r ->
                            baseActivity.hideBlockingProgress()
                            Toast.makeText(activity, R.string.transaction_was_saved, Toast.LENGTH_SHORT).show()

                            loadData()
                        },
                        { e ->
                            baseActivity.hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    override fun dataChanged() {
        loadData()
    }

    override fun syncStarted() {
        mainActivity.showBlockingProgressWithUpdate(getString(R.string.sync_title))
    }

    override fun syncFinished() {
        if (!isDetached) {
            mainActivity.hideBlockingProgress()
            unitsContainer!!.post {
                val eventsController = MoneyApp.instance.syncManager.eventsController
                eventsController.removeListener(this@HomeFragment)
            }
        }
    }

    override fun onProgressUpdated(progress: Int, max: Int) {
        if (!isDetached) mainActivity.updateBlockingProgress(progress, max)
    }

    companion object {

        fun build(): HomeFragment {
            return HomeFragment()
        }
    }
}
