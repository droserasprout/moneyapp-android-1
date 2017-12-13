package com.cactusteam.money.ui.widget.home

import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.EditTransactionActivity
import com.cactusteam.money.ui.activity.MainActivity
import com.cactusteam.money.ui.format.DateTimeFormatter
import com.cactusteam.money.ui.fragment.HomeFragment
import java.util.*

/**
 * @author vpotapenko
 */
class TransactionsHomeUnit(homeFragment: HomeFragment) : BaseHomeUnit(homeFragment) {

    private val dateTimeFormatter: DateTimeFormatter

    private var transactionsContainer: LinearLayout? = null
    private var transactionsProgress: View? = null

    init {

        val appPreferences = MoneyApp.instance.appPreferences
        dateTimeFormatter = DateTimeFormatter.create(appPreferences.transactionFormatDateMode, homeFragment.activity)
    }

    override fun getLayoutRes(): Int {
        return R.layout.fragment_home_transactions_unit
    }

    override fun initializeView() {
        transactionsContainer = getView()!!.findViewById(R.id.transactions_container) as LinearLayout
        transactionsProgress = getView()!!.findViewById(R.id.transactions_progress)

        getView()!!.findViewById(R.id.all_transactions).setOnClickListener { (homeFragment.activity as MainActivity).showSection(MainSection.TRANSACTIONS) }
    }

    override fun update() {
        loadTransactions()
    }

    private fun loadTransactions() {
        transactionsContainer!!.visibility = View.GONE
        transactionsProgress!!.visibility = View.VISIBLE

        val s = homeFragment.dataManager.transactionService
                .newListTransactionsBuilder()
                .putTo(Date())
                .putMax(UiConstants.MAX_SHORT_TRANSACTIONS)
                .list()
                .subscribe(
                        { r ->
                            transactionsLoaded(r)
                        },
                        { e ->
                            transactionsProgress!!.visibility = View.GONE
                            homeFragment.showError(e.message)
                        }
                )
        homeFragment.compositeSubscription.add(s)
    }

    private fun transactionsLoaded(transactions: List<Transaction>) {
        transactionsContainer!!.removeAllViews()
        for (transaction in transactions) {
            createTransactionView(transaction)
        }
        if (transactions.isEmpty()) {
            transactionsContainer!!.addView(View.inflate(homeFragment.activity, R.layout.view_no_data, null))
        }
        transactionsContainer!!.visibility = View.VISIBLE
        transactionsProgress!!.visibility = View.GONE
    }

    @Suppress("DEPRECATION")
    private fun createTransactionView(transaction: Transaction) {
        val view = View.inflate(homeFragment.activity, R.layout.fragment_home_transaction, null)

        view.findViewById(R.id.transaction).setOnClickListener { showTransactionActivity(transaction.id!!) }

        val commentView = view.findViewById(R.id.comment) as TextView
        if (!transaction.comment.isNullOrBlank()) {
            commentView.text = transaction.comment
            commentView.visibility = View.VISIBLE
        } else {
            commentView.visibility = View.GONE
        }

        (view.findViewById(R.id.date) as TextView).text = dateTimeFormatter.format(transaction.date)

        val tagsContainer = view.findViewById(R.id.tags_container) as LinearLayout
        tagsContainer.removeAllViews()
        for (tag in transaction.tags) {
            View.inflate(homeFragment.activity, R.layout.fragment_transactions_tag, tagsContainer)
            val textView = tagsContainer.getChildAt(tagsContainer.childCount - 1) as TextView
            textView.text = tag.tag.name
        }

        val accountView = view.findViewById(R.id.source_account) as TextView
        val sourceAccount = transaction.sourceAccount
        accountView.text = sourceAccount.name

        val amountTextView = view.findViewById(R.id.amount) as TextView
        if (transaction.type == Transaction.EXPENSE) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expense_transaction, 0)
            (view.findViewById(R.id.dest_name) as TextView).text = transaction.categoryDisplayName

            val amountStr = UiUtils.formatCurrency(-transaction.amount, sourceAccount.currencyCode)
            amountTextView.text = amountStr
            amountTextView.setTextColor(homeFragment.resources.getColor(R.color.toolbar_expense_color))
        } else if (transaction.type == Transaction.INCOME) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_income_transaction, 0)
            (view.findViewById(R.id.dest_name) as TextView).text = transaction.categoryDisplayName

            val amountStr = UiUtils.formatCurrency(transaction.amount, sourceAccount.currencyCode)
            amountTextView.text = amountStr
            amountTextView.setTextColor(homeFragment.resources.getColor(R.color.toolbar_income_color))
        } else {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_transfer_transaction, 0)

            (view.findViewById(R.id.dest_name) as TextView).text = transaction.destAccount.name

            val sourceCurrencyCode = sourceAccount.currencyCode
            val destCurrencyCode = transaction.destAccount.currencyCode

            var amountStr = UiUtils.formatCurrency(transaction.amount, sourceCurrencyCode)
            if (sourceCurrencyCode != destCurrencyCode) {
                val destAmount = UiUtils.formatCurrency(transaction.destAmount!!, destCurrencyCode)
                amountStr = "$amountStr ($destAmount)"
            }
            amountTextView.text = amountStr
            amountTextView.setTextColor(Color.BLACK)
        }

        transactionsContainer!!.addView(view)
    }

    private fun showTransactionActivity(transactionId: Long) {
        EditTransactionActivity.actionStart(homeFragment, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
    }

    override val shortName: String
        get() = UiConstants.TRANSACTIONS_BLOCK
}
