package com.cactusteam.money.ui.activity

import android.app.AlertDialog
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.TagNameTransactionFilter
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.format.DateTimeFormatter
import com.cactusteam.money.ui.fragment.EditTagFragment
import java.util.*

/**
 * @author vpotapenko
 */
class TagActivity : BaseDataActivity("TagActivity") {

    private var tagName: String? = null

    private var amountsContainer: View? = null
    private var expenseView: TextView? = null
    private var incomeView: TextView? = null
    private var amountProgress: View? = null

    private var transactionsContainer: LinearLayout? = null
    private var transactionsProgress: View? = null

    private var dateTimeFormatter: DateTimeFormatter? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                loadData()
            }
        } else if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK)

                loadData()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_tag, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.edit) {
            editClicked()
            return true
        } else if (itemId == R.id.delete) {
            deleteClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteClicked() {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.tag_will_be_deleted).setPositiveButton(android.R.string.yes) { dialog, which -> deleteTag() }.setNegativeButton(android.R.string.no, null).show()
    }

    private fun deleteTag() {
        showProgress()
        val s = dataManager.tagService
                .deleteTag(tagName!!)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        {
                            hideProgress()
                            tagDeleted()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun tagDeleted() {
        Toast.makeText(this, R.string.tag_was_deleted, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun editClicked() {
        EditTagFragment.build(tagName!!) { newName ->
            Toast.makeText(this, R.string.tag_was_saved, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)

            tagName = newName
            title = tagName
        }.show(fragmentManager, "dialog")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)

        initializeToolbar()
        initializeViewProgress()

        title = tagName

        val appPreferences = MoneyApp.instance.appPreferences
        dateTimeFormatter = DateTimeFormatter.create(appPreferences.transactionFormatDateMode, this)

        amountsContainer = findViewById(R.id.amounts_container)
        amountProgress = findViewById(R.id.amount_progress)
        expenseView = findViewById(R.id.expense) as TextView
        incomeView = findViewById(R.id.income) as TextView

        transactionsProgress = findViewById(R.id.transactions_progress)
        transactionsContainer = findViewById(R.id.transactions_container) as LinearLayout

        findViewById(R.id.all_transactions).setOnClickListener { allTransactionsClicked() }

        loadData()
    }

    private fun allTransactionsClicked() {
        val filter = TagNameTransactionFilter(tagName!!)
        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE,
                filter,
                getString(R.string.tag_pattern, tagName),
                null,
                null)
    }

    private fun loadData() {
        amountsContainer!!.visibility = View.GONE
        amountProgress!!.visibility = View.VISIBLE

        transactionsProgress!!.visibility = View.VISIBLE
        transactionsContainer!!.visibility = View.GONE

        val current = application.period.current
        val s = dataManager.transactionService
                .newListTransactionsBuilder()
                .putFrom(current.first)
                .putTo(current.second)
                .putConvertToMain(true)
                .putTransactionFilter(TagNameTransactionFilter(tagName!!))
                .list()
                .subscribe(
                        { r ->
                            dataLoaded(r)
                            amountProgress!!.visibility = View.GONE
                            amountsContainer!!.visibility = View.VISIBLE
                            transactionsProgress!!.visibility = View.GONE
                            transactionsContainer!!.visibility = View.VISIBLE
                        },
                        { e ->
                            showError(e.message)
                            amountProgress!!.visibility = View.GONE
                        }
                )
        compositeSubscription.add(s)
    }

    private fun dataLoaded(transactions: List<Transaction>) {
        showAmounts(transactions)

        val list = LinkedList<Transaction>()
        var i = transactions.size - 1
        while (i >= 0 && list.size < UiConstants.MAX_SHORT_TRANSACTIONS) {
            val t = transactions[i]
            list.add(0, t)
            i--
        }
        transactionsLoaded(list)
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
        val view = View.inflate(this, R.layout.activity_tag_transaction, null)

        view.findViewById(R.id.transaction).setOnClickListener { showTransactionActivity(transaction.id!!) }

        val accountView = view.findViewById(R.id.source_account) as TextView
        accountView.text = transaction.sourceAccount.name

        val amountStr = UiUtils.formatCurrency(transaction.amount, transaction.sourceAccount.currencyCode)
        val amountTextView = view.findViewById(R.id.amount) as TextView
        amountTextView.text = amountStr
        if (transaction.type == Transaction.EXPENSE) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expense_transaction, 0)
            amountTextView.setTextColor(resources.getColor(R.color.toolbar_expense_color))
        } else if (transaction.type == Transaction.INCOME) {
            accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_income_transaction, 0)
            amountTextView.setTextColor(resources.getColor(R.color.toolbar_income_color))
        }

        val commentView = view.findViewById(R.id.comment) as TextView
        if (transaction.comment != null) {
            commentView.text = transaction.comment
        } else {
            commentView.visibility = View.GONE
        }

        (view.findViewById(R.id.date) as TextView).text = dateTimeFormatter!!.format(transaction.date)
        (view.findViewById(R.id.dest_name) as TextView).text = transaction.categoryDisplayName

        transactionsContainer!!.addView(view)
    }

    private fun showTransactionActivity(transactionId: Long) {
        EditTransactionActivity.actionStart(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
    }

    private fun showAmounts(transactions: List<Transaction>) {
        var expense = 0.0
        var income = 0.0
        for (t in transactions) {
            if (t.type == Transaction.EXPENSE) {
                expense += t.amountInMainCurrency
            } else if (t.type == Transaction.INCOME) {
                income += t.amountInMainCurrency
            }
        }

        val mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode
        if (expense > 0) {
            expenseView!!.text = UiUtils.formatCurrency(expense, mainCurrencyCode)
        } else {
            expenseView!!.text = "-"
        }

        if (income > 0) {
            incomeView!!.text = UiUtils.formatCurrency(income, mainCurrencyCode)
        } else {
            incomeView!!.text = "-"
        }
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_NAME)) {
                tagName = extras.getString(UiConstants.EXTRA_NAME)
            }
        }
    }

    companion object {

        fun actionStart(fragment: Fragment, requestCode: Int, tagName: String) {
            val intent = Intent(fragment.activity, TagActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_NAME, tagName)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
