package com.cactusteam.money.ui.widget.home

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.model.Totals
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.AccountActivity
import com.cactusteam.money.ui.activity.MainActivity
import com.cactusteam.money.ui.fragment.HomeFragment

/**
 * @author vpotapenko
 */
class AccountsHomeUnit(homeFragment: HomeFragment) : BaseHomeUnit(homeFragment) {

    private var accountsContainer: LinearLayout? = null
    private var accountsProgress: View? = null

    override fun getLayoutRes(): Int {
        return R.layout.fragment_home_accounts_unit
    }

    override fun initializeView() {
        accountsContainer = getView()!!.findViewById(R.id.accounts_container) as LinearLayout
        accountsProgress = getView()!!.findViewById(R.id.accounts_progress)

        getView()!!.findViewById(R.id.accounts_title).setOnClickListener { (homeFragment.activity as MainActivity).showSection(MainSection.ACCOUNTS) }
    }

    override fun update() {
        loadAccounts()
    }

    private fun loadAccounts() {
        showProgress()
        val s = homeFragment.dataManager.accountService
                .getAccounts(false, true, true)
                .subscribe(
                        { r ->
                            hideProgress()
                            accountsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            homeFragment.showError(e.message)
                        },
                        { hideProgress() }
                )
        homeFragment.compositeSubscription.add(s)
    }

    private fun showProgress() {
        accountsContainer!!.visibility = View.GONE
        accountsProgress!!.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        accountsContainer!!.visibility = View.VISIBLE
        accountsProgress!!.visibility = View.GONE
    }

    private fun accountsLoaded(accounts: List<Account>) {
        val totals = Totals()

        accountsContainer!!.removeAllViews()
        for (account in accounts) {
            createAccountView(account)
            totals.putAmount(account.currencyCode, account.balance ?: 0.0)
            totals.total += (account.balanceInMain ?: 0.0)
        }
        if (accounts.isEmpty()) {
            accountsContainer!!.addView(View.inflate(homeFragment.activity, R.layout.view_no_data, null))
        }

        val mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode
        totals.prepare(mainCurrencyCode)

        createTotalView(totals)
    }

    private fun createAccountView(account: Account) {
        val view = View.inflate(homeFragment.activity, R.layout.fragment_home_accounts_unit_item, null)

        (view.findViewById(R.id.name) as TextView).text = account.name

        val amountStr = UiUtils.formatCurrency(account.balance!!, account.currencyCode)
        (view.findViewById(R.id.balance) as TextView).text = amountStr

        view.findViewById(R.id.list_item).setOnClickListener { showAccountActivity(account) }
        accountsContainer!!.addView(view)
    }

    private fun createTotalView(totals: Totals) {
        val view = View.inflate(homeFragment.activity, R.layout.fragment_home_accounts_unit_total, null)

        val sb = StringBuilder()
        for (currencyCode in totals.currencyCodes) {
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(UiUtils.formatCurrency(totals.map[currencyCode]!!, currencyCode))
        }

        if (sb.isNotEmpty()) {
            (view.findViewById(R.id.totals) as TextView).text = sb.toString()
        } else {
            view.findViewById(R.id.totals).visibility = View.GONE
        }

        val amountStr = UiUtils.formatCurrency(totals.total, totals.mainCurrencyCode)
        (view.findViewById(R.id.total) as TextView).text = homeFragment.getString(R.string.total_pattern, amountStr)

        accountsContainer!!.addView(View.inflate(homeFragment.activity, R.layout.horizontal_divider, null))
        accountsContainer!!.addView(view)
    }

    private fun showAccountActivity(account: Account) {
        AccountActivity.actionStart(homeFragment, UiConstants.ACCOUNT_REQUEST_CODE, account.id!!, account.name, account.color)
    }

    override val shortName: String
        get() = UiConstants.ACCOUNTS_BLOCK
}
