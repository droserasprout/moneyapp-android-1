package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.AccountTransactionFilter
import com.cactusteam.money.data.model.AccountPeriodData
import com.cactusteam.money.ui.HtmlTagHandler
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.format.DateTimeFormatter
import rx.Observable

/**
 * @author vpotapenko
 */
class AccountActivity : BaseDataActivity("AccountActivity") {

    // Extras
    private var accountId: Long = 0
    private var accountName: String? = null
    private var accountColor: String? = null

    private var totalView: TextView? = null

    private var currentPeriod: TextView? = null
    private var currentStartBalance: TextView? = null
    private var currentExpense: TextView? = null
    private var currentIncome: TextView? = null
    private var currentTransfer: TextView? = null
    private var balanceProgress: ProgressBar? = null

    private var previousPeriod: TextView? = null
    private var previousStartBalance: TextView? = null
    private var previousExpense: TextView? = null
    private var previousIncome: TextView? = null
    private var previousTransfer: TextView? = null
    private var previousBalanceProgress: ProgressBar? = null

    private var transactionsContainer: LinearLayout? = null

    private var currentTotal: Double = 0.toDouble()
    private var deleted: Boolean = false

    private var dateTimeFormatter: DateTimeFormatter? = null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.edit) {
            showEditAccountActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showEditAccountActivity() {
        EditAccountActivity.actionStart(this, UiConstants.EDIT_ACCOUNT_REQUEST_CODE, accountId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_ACCOUNT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                if (data != null && data.getBooleanExtra(UiConstants.EXTRA_DELETED, false)) {
                    finish()
                } else {
                    if (data != null) {
                        val newColor = data.getStringExtra(UiConstants.EXTRA_COLOR)
                        if (newColor != null) updateToolbarColor(newColor)
                    }

                    loadData()
                }
            }
        } else if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                loadData()
            }
        } else if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                loadData()
            }
        } else if (requestCode == UiConstants.CALCULATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val amount = data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0)
                changeBalance(amount)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_account, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        initializeToolbar()
        updateToolbarColor(accountColor!!)
        updateTitle()

        initializeViewProgress()

        val appPreferences = MoneyApp.instance.appPreferences
        dateTimeFormatter = DateTimeFormatter.create(appPreferences.transactionFormatDateMode, this)

        totalView = findViewById(R.id.total) as TextView

        currentPeriod = findViewById(R.id.current_period) as TextView
        currentStartBalance = findViewById(R.id.current_start_balance) as TextView
        currentExpense = findViewById(R.id.current_expense) as TextView
        currentIncome = findViewById(R.id.current_income) as TextView
        currentTransfer = findViewById(R.id.current_transfer) as TextView
        balanceProgress = findViewById(R.id.balance_progress) as ProgressBar

        previousPeriod = findViewById(R.id.previous_period) as TextView
        previousStartBalance = findViewById(R.id.previous_start_balance) as TextView
        previousExpense = findViewById(R.id.previous_expense) as TextView
        previousIncome = findViewById(R.id.previous_income) as TextView
        previousTransfer = findViewById(R.id.previous_transfer) as TextView
        previousBalanceProgress = findViewById(R.id.previous_balance_progress) as ProgressBar

        transactionsContainer = findViewById(R.id.transactions_container) as LinearLayout
        findViewById(R.id.all_transactions).setOnClickListener { allTransactionsClicked() }

        findViewById(R.id.create_transaction_btn).setOnClickListener { showNewTransactionActivity() }
        findViewById(R.id.change_balance_btn).setOnClickListener { changeBalanceClicked() }

        loadData()
    }

    private fun changeBalanceClicked() {
        CalculatorActivity.actionStart(this, UiConstants.CALCULATOR_REQUEST_CODE, currentTotal)
    }

    private fun changeBalance(newAmount: Double) {
        showProgress()
        val s = dataManager.accountService
                .changeAccountBalance(accountId, newAmount)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        {
                            setResult(RESULT_OK)
                            loadData()
                            hideProgress()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun allTransactionsClicked() {
        val filter = AccountTransactionFilter(accountId)

        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                filter,
                getString(R.string.account_pattern, accountName),
                null,
                null)
    }

    private fun showNewTransactionActivity() {
        NewTransactionActivity.ActionBuilder().account(accountId).start(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE)
    }

    private fun loadData() {
        showProgress()
        val o1 = dataManager.accountService.getAccountPeriodData(accountId, false)
        val o2 = dataManager.accountService.getAccount(accountId)
        val o3 = dataManager.transactionService
                .newListTransactionsBuilder()
                .putAccountId(accountId)
                .putMax(UiConstants.MAX_SHORT_TRANSACTIONS).list()

        Observable.zip(o1, o2, o3, { i1, i2, i3 ->
            listOf(i1, i2, i3)
        }).subscribe(
                { r ->
                    dataLoaded(r)
                },
                { e ->
                    hideProgress()
                    showError(e.message)
                },
                { hideProgress() }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun dataLoaded(result: List<Any>) {
        val data = result[0] as List<AccountPeriodData>
        updateCurrentViews(data[0])
        updatePreviousViews(data[1])

        val account = result[1] as Account
        deleted = account.deleted
        updateTitle()

        val transactions = result[2] as List<Transaction>
        transactionsLoaded(transactions)
    }

    private fun transactionsLoaded(transactions: List<Transaction>) {
        transactionsContainer!!.removeAllViews()
        for (transaction in transactions) {
            addTransactionView(transaction)
        }
        if (transactions.isEmpty()) {
            transactionsContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
        }
    }

    @Suppress("DEPRECATION")
    private fun addTransactionView(transaction: Transaction) {
        val view = View.inflate(this, R.layout.activity_account_transaction, null)

        view.findViewById(R.id.transaction).setOnClickListener { showTransactionActivity(transaction.id!!) }

        val commentView = view.findViewById(R.id.comment) as TextView
        if (!transaction.comment.isNullOrBlank()) {
            commentView.text = transaction.comment
            commentView.visibility = View.VISIBLE
        } else {
            commentView.visibility = View.GONE
        }

        (view.findViewById(R.id.date) as TextView).text = dateTimeFormatter!!.format(transaction.date)

        val tagsContainer = view.findViewById(R.id.tags_container) as LinearLayout
        tagsContainer.removeAllViews()
        for (tag in transaction.tags) {
            View.inflate(this, R.layout.fragment_transactions_tag, tagsContainer)
            val textView = tagsContainer.getChildAt(tagsContainer.childCount - 1) as TextView
            textView.text = tag.tag.name
        }

        val accountView = view.findViewById(R.id.source_account) as TextView
        accountView.text = transaction.sourceAccount.name

        val amountTextView = view.findViewById(R.id.amount) as TextView
        if (transaction.type == Transaction.EXPENSE) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expense_transaction, 0)
            (view.findViewById(R.id.dest_name) as TextView).text = transaction.categoryDisplayName

            val amountStr = UiUtils.formatCurrency(-transaction.amount, transaction.sourceAccount.currencyCode)
            amountTextView.text = amountStr
            amountTextView.setTextColor(resources.getColor(R.color.toolbar_expense_color))
        } else if (transaction.type == Transaction.INCOME) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_income_transaction, 0)
            (view.findViewById(R.id.dest_name) as TextView).text = transaction.categoryDisplayName

            val amountStr = UiUtils.formatCurrency(transaction.amount, transaction.sourceAccount.currencyCode)
            amountTextView.text = amountStr
            amountTextView.setTextColor(resources.getColor(R.color.toolbar_income_color))
        } else {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_transfer_transaction, 0)

            (view.findViewById(R.id.dest_name) as TextView).text = transaction.destAccount.name

            val amountStr: String
            if (accountId == transaction.sourceAccountId) {
                amountStr = UiUtils.formatCurrency(-transaction.amount, transaction.sourceAccount.currencyCode)
            } else {
                amountStr = UiUtils.formatCurrency(transaction.destAmount!!, transaction.destAccount.currencyCode)
            }
            amountTextView.text = amountStr
            amountTextView.setTextColor(Color.BLACK)
        }

        transactionsContainer!!.addView(view)
    }

    private fun showTransactionActivity(transactionId: Long) {
        EditTransactionActivity.actionStart(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
    }

    private fun updatePreviousViews(data: AccountPeriodData) {
        val periodStr = DateUtils.formatDateRange(this, data.from.time, data.to.time, DateUtils.FORMAT_SHOW_DATE)
        previousPeriod!!.text = periodStr

        previousStartBalance!!.text = UiUtils.formatCurrency(data.initial, data.currencyCode)
        previousIncome!!.text = UiUtils.formatCurrency(data.income, data.currencyCode)
        previousExpense!!.text = UiUtils.formatCurrency(data.expense, data.currencyCode)
        previousTransfer!!.text = UiUtils.formatCurrency(data.transfer, data.currencyCode)

        if (data.expense != 0.0 || data.income != 0.0) {
            previousBalanceProgress!!.visibility = View.VISIBLE
            previousBalanceProgress!!.max = 100

            val progress = data.expense / (data.income + data.expense) * 100
            previousBalanceProgress!!.progress = Math.round(progress).toInt()
        } else {
            previousBalanceProgress!!.visibility = View.GONE
        }
    }

    private fun updateCurrentViews(data: AccountPeriodData) {
        currentTotal = data.total
        totalView!!.text = UiUtils.formatCurrency(currentTotal, data.currencyCode)

        val periodStr = DateUtils.formatDateRange(this, data.from.time, data.to.time, DateUtils.FORMAT_SHOW_DATE)
        currentPeriod!!.text = periodStr

        currentStartBalance!!.text = UiUtils.formatCurrency(data.initial, data.currencyCode)
        currentIncome!!.text = UiUtils.formatCurrency(data.income, data.currencyCode)
        currentExpense!!.text = UiUtils.formatCurrency(data.expense, data.currencyCode)
        currentTransfer!!.text = UiUtils.formatCurrency(data.transfer, data.currencyCode)

        if (data.expense != 0.0 || data.income != 0.0) {
            balanceProgress!!.visibility = View.VISIBLE
            balanceProgress!!.max = 100

            val progress = data.expense / (data.income + data.expense) * 100
            balanceProgress!!.progress = Math.round(progress).toInt()
        } else {
            balanceProgress!!.visibility = View.GONE
        }
    }

    @Suppress("DEPRECATION")
    private fun updateTitle() {
        if (deleted) {
            val s = String.format(UiConstants.DELETED_PATTERN, accountName)
            title = Html.fromHtml(s, null, HtmlTagHandler())
        } else {
            title = accountName
        }
    }

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun updateToolbarColor(accountColor: String) {
        var color = resources.getColor(R.color.color_primary)
        try {
            color = Color.parseColor(accountColor)
        } catch (ignore: Exception) {
        }

        toolbar!!.setBackgroundColor(color)
        if (DataUtils.hasLollipop()) {
            window.statusBarColor = UiUtils.darkenColor(color)
        }
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                accountId = extras.getLong(UiConstants.EXTRA_ID)
            }
            if (extras.containsKey(UiConstants.EXTRA_NAME)) {
                accountName = extras.getString(UiConstants.EXTRA_NAME)
            }
            if (extras.containsKey(UiConstants.EXTRA_COLOR)) {
                accountColor = extras.getString(UiConstants.EXTRA_COLOR)
            }
        }
    }

    companion object {

        fun actionStart(fragment: Fragment, requestCode: Int, accountId: Long, accountName: String, color: String) {
            val intent = Intent(fragment.activity, AccountActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, accountId)
            intent.putExtra(UiConstants.EXTRA_NAME, accountName)
            intent.putExtra(UiConstants.EXTRA_COLOR, color)

            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
