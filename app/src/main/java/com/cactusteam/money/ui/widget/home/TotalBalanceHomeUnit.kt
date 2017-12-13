package com.cactusteam.money.ui.widget.home

import android.util.Pair
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.model.Totals
import com.cactusteam.money.data.period.Period
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.HomeFragment
import java.text.DecimalFormat
import java.util.*

/**
 * @author vpotapenko
 */
class TotalBalanceHomeUnit(homeFragment: HomeFragment) : BaseHomeUnit(homeFragment) {

    private var balancesContainer: LinearLayout? = null
    private var balancesProgress: View? = null

    private var balanceProgressContainer: View? = null
    private var currentBalanceProgress: ProgressBar? = null
    private var currentBalanceLabel: TextView? = null
    private var currentExpense: TextView? = null
    private var currentExpensePercent: TextView? = null
    private var currentIncome: TextView? = null
    private var currentIncomePercent: TextView? = null

    override fun getLayoutRes(): Int {
        return R.layout.fragment_home_total_unit
    }

    override fun initializeView() {
        balancesContainer = getView()!!.findViewById(R.id.balances_container) as LinearLayout
        balancesProgress = getView()!!.findViewById(R.id.balances_progress)

        balanceProgressContainer = getView()!!.findViewById(R.id.balance_progress_container)
        currentBalanceProgress = getView()!!.findViewById(R.id.current_balance_progress) as ProgressBar
        currentExpense = getView()!!.findViewById(R.id.current_expense) as TextView
        currentExpensePercent = getView()!!.findViewById(R.id.current_expense_percent) as TextView
        currentIncome = getView()!!.findViewById(R.id.current_income) as TextView
        currentIncomePercent = getView()!!.findViewById(R.id.current_income_precent) as TextView

        currentBalanceLabel = getView()!!.findViewById(R.id.current_balance_label) as TextView
    }

    override fun update() {
        loadBalance()
    }

    private fun loadBalance() {
        showProgress()
        val s = homeFragment.dataManager.accountService
                .getTotals()
                .subscribe(
                        { r -> balancesLoaded(r) },
                        { e ->
                            hideProgress()
                            homeFragment.showError(e.message)
                        },
                        { hideProgress() }
                )
        homeFragment.compositeSubscription.add(s)
    }

    private fun showProgress() {
        balancesContainer!!.visibility = View.GONE
        balancesProgress!!.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        balancesContainer!!.visibility = View.VISIBLE
        balancesProgress!!.visibility = View.GONE
    }

    private fun balancesLoaded(totals: Totals) {
        balancesContainer!!.removeAllViews()
        for (currencyCode in totals.currencyCodes) {
            createBalanceView(currencyCode, totals.map[currencyCode]!!)
        }
        if (totals.currencyCodes.size > 1) {
            createTotalView(totals.mainCurrencyCode, totals.total)
        }

        balancesContainer!!.visibility = View.VISIBLE
        balancesProgress!!.visibility = View.GONE

        loadBalanceProgress()
    }

    private fun loadBalanceProgress() {
        val appPreferences = MoneyApp.instance.appPreferences
        val mainBalanceType = appPreferences.mainBalanceType
        val currentPeriod: Pair<Date, Date>
        when (mainBalanceType) {
            0 -> {
                currentBalanceLabel!!.setText(R.string.current_period_balance)
                currentPeriod = appPreferences.period.current
            }
            1 -> {
                currentBalanceLabel!!.setText(R.string.current_month_balance)
                currentPeriod = Period.getThisMonthPeriod()
            }
            2 -> {
                currentBalanceLabel!!.setText(R.string.last_30_days_balance)
                currentPeriod = Period.getLast30DaysPeriod()
            }
            else -> {
                currentBalanceLabel!!.setText(R.string.current_period_balance)
                currentPeriod = appPreferences.period.current
            }
        }

        balanceProgressContainer!!.visibility = View.GONE
        val s = homeFragment.dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(currentPeriod.first)
                .putTo(currentPeriod.second)
                .putConvertToMain(true)
                .list()
                .subscribe(
                        { r -> showBalanceProgressData(r) },
                        { e ->
                            homeFragment.showError(e.message)
                        }
                )
        homeFragment.compositeSubscription.add(s)
    }

    private fun showBalanceProgressData(transactions: List<Transaction>) {
        var expense = 0.0
        var income = 0.0

        transactions
                .filterNot { it.sourceAccount.skipInBalance }
                .forEach {
                    if (it.type == Transaction.EXPENSE) {
                        expense += it.amountInMainCurrency
                    } else if (it.type == Transaction.INCOME) {
                        income += it.amountInMainCurrency
                    }
                }

        if (expense > 0 || income > 0) {
            currentBalanceProgress!!.max = 100

            val progress = expense / (income + expense) * 100
            currentBalanceProgress!!.progress = Math.round(progress).toInt()

            currentExpense!!.text = UiUtils.formatCurrency(expense, mainCurrencyCode)
            currentIncome!!.text = UiUtils.formatCurrency(income, mainCurrencyCode)

            val total = expense + income
            currentExpensePercent!!.text = formatBalancePercent(expense, total)
            currentIncomePercent!!.text = formatBalancePercent(income, total)

            balanceProgressContainer!!.visibility = View.VISIBLE
        }
    }

    private fun formatBalancePercent(amount: Double, total: Double): String {
        val percent = amount / total * 100.0
        return percentFormat.format(percent) + " %"
    }

    private fun createBalanceView(currencyCode: String, total: Double) {
        val view = View.inflate(homeFragment.activity, R.layout.fragment_home_balance, null)

        val amountStr = UiUtils.formatCurrency(total, currencyCode)
        (view.findViewById(R.id.total) as TextView).text = amountStr

        balancesContainer!!.addView(view)
    }

    private fun createTotalView(currencyCode: String, total: Double) {
        val view = View.inflate(homeFragment.activity, R.layout.fragment_home_total, null)

        val amountStr = UiUtils.formatCurrency(total, currencyCode)
        (view.findViewById(R.id.total) as TextView).text = homeFragment.getString(R.string.total_pattern, amountStr)

        balancesContainer!!.addView(view)
    }

    override val shortName: String
        get() = UiConstants.TOTAL_BALANCE_BLOCK

    companion object {

        private val percentFormat = DecimalFormat("###,###,##0.0")
    }
}
