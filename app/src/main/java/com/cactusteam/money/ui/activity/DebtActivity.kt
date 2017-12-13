package com.cactusteam.money.ui.activity

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Fragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Debt
import com.cactusteam.money.data.dao.DebtNote
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.format.DateTimeFormatter
import com.cactusteam.money.ui.fragment.EditDebtNoteFragment
import rx.Observable
import java.util.*

/**
 * @author vpotapenko
 */
class DebtActivity : BaseDataActivity("DebtActivity") {

    private var debtId: Long = 0
    private var debtName: String? = null

    private var tillDateView: TextView? = null
    private var startDateView: TextView? = null
    private var amountView: TextView? = null
    private var amountLabelView: TextView? = null
    private var phoneView: TextView? = null

    private var currentDebtHeader: View? = null
    private var finishedDebtHeader: View? = null

    private var finishedDebtDescription: TextView? = null

    private var closeDebtButton: View? = null

    private var transactionsContainer: LinearLayout? = null
    private var startDateContainer: View? = null

    private var amount: Double = 0.toDouble()
    private var phone: String? = null
    private var tillDate: Date? = null

    private var dateTimeFormatter: DateTimeFormatter? = null

    private var notesContainer: LinearLayout? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_DEBT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)

                if (data != null && data.getBooleanExtra(UiConstants.EXTRA_DELETED, false)) {
                    finish()
                } else {
                    if (data != null) {
                        val newName = data.getStringExtra(UiConstants.EXTRA_NAME)
                        if (newName != null) title = newName
                    }
                    loadDebt()
                }
            }
        } else if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)

                loadDebt()
            }
        } else if (requestCode == UiConstants.INCREASE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val a = Math.abs(data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0))
                increaseDebt(a)
            }
        } else if (requestCode == UiConstants.DECREASE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val a = Math.abs(data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0))
                decreaseDebt(a)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_debt, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.edit) {
            showEditDebtActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showEditDebtActivity() {
        EditDebtActivity.actionStart(this, UiConstants.EDIT_DEBT_REQUEST_CODE, debtId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debt)

        initializeToolbar()
        title = debtName

        initializeViewProgress()

        val appPreferences = MoneyApp.instance.appPreferences
        dateTimeFormatter = DateTimeFormatter.create(appPreferences.transactionFormatDateMode, this)

        tillDateView = findViewById(R.id.till_date) as TextView
        amountView = findViewById(R.id.amount) as TextView
        amountLabelView = findViewById(R.id.amount_label) as TextView
        phoneView = findViewById(R.id.phone) as TextView

        findViewById(R.id.till_container).setOnClickListener { tillClicked() }

        findViewById(R.id.phone_container).setOnClickListener { phoneClicked() }

        startDateView = findViewById(R.id.start_date) as TextView
        startDateContainer = findViewById(R.id.start_container)

        currentDebtHeader = findViewById(R.id.current_debt_header)
        finishedDebtHeader = findViewById(R.id.finished_debt_header)

        finishedDebtDescription = findViewById(R.id.finished_debt_description) as TextView?

        closeDebtButton = findViewById(R.id.close_btn)
        closeDebtButton?.setOnClickListener { closeClicked() }

        findViewById(R.id.decrease_debt_btn).setOnClickListener { decreaseDebtClicked() }
        findViewById(R.id.increase_debt_btn).setOnClickListener { increaseDebtClicked() }

        transactionsContainer = findViewById(R.id.transactions_container) as LinearLayout

        notesContainer = findViewById(R.id.notes_container) as LinearLayout
        findViewById(R.id.create_note_btn).setOnClickListener { createNoteClicked() }

        loadDebt()
    }

    private fun createNoteClicked() {
        val fragment = EditDebtNoteFragment.build(debtId) {
            loadDebt()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun tillClicked() {
        val cal = Calendar.getInstance()
        if (tillDate != null) cal.time = tillDate
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(year, monthOfYear, dayOfMonth, 0, 0, 0)

            updateDebtTime(cal.time)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateDebtTime(time: Date) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.debtService
                .updateDebtTime(debtId, time)
                .subscribe(
                        { r ->
                            setResult(Activity.RESULT_OK)
                            loadDebt()
                            hideBlockingProgress()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun increaseDebtClicked() {
        CalculatorActivity.actionStart(this, UiConstants.INCREASE_REQUEST_CODE, 0.0, title.toString())
    }

    private fun decreaseDebtClicked() {
        CalculatorActivity.actionStart(this, UiConstants.DECREASE_REQUEST_CODE, 0.0, title.toString())
    }

    private fun increaseDebt(v: Double) {
        if (v != 0.0) addDebtAmount(Math.copySign(v, -amount))
    }

    private fun decreaseDebt(v: Double) {
        if (v != 0.0) addDebtAmount(Math.copySign(v, amount))
    }

    private fun addDebtAmount(v: Double) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.debtService
                .changeDebtAmount(debtId, v)
                .subscribe(
                        { r ->
                            hideBlockingProgress()

                            setResult(Activity.RESULT_OK)
                            loadDebt()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun closeClicked() {
        AlertDialog.Builder(this)
                .setTitle(R.string.continue_question)
                .setMessage(R.string.debt_will_be_closed)
                .setPositiveButton(android.R.string.yes) { dialog, which -> closeDebt() }
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    fun closeDebt() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.debtService
                .closeDebt(debtId)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            setResult(RESULT_OK)
                            finish()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun phoneClicked() {
        if (phone.isNullOrBlank()) return

        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
        startActivity(intent)
    }

    private fun loadDebt() {
        showProgress()
        val o1 = dataManager.debtService.getDebt(debtId)
        val o2 = dataManager.transactionService
                .newListTransactionsBuilder()
                .putRef(String.format(Debt.DEBT_REF_PATTERN, debtId))
                .list()
        val s = Observable.zip(o1, o2, { i1, i2 ->
            Pair(i1, i2)
        }).subscribe(
                { r ->
                    debtLoaded(r.first)
                    transactionsLoaded(r.second)
                    hideProgress()
                },
                { e ->
                    hideProgress()
                    showError(e.message)
                }
        )
        compositeSubscription.add(s)
    }

    private fun debtLoaded(debt: Debt) {
        title = debt.name

        tillDate = debt.till
        val dateFormat = DateFormat.getDateFormat(this)
        tillDateView?.text = dateFormat.format(debt.till)

        val startDate = debt.start
        if (startDate != null) {
            startDateView?.text = dateFormat.format(startDate)
            startDateContainer?.visibility = View.VISIBLE
        } else {
            startDateContainer?.visibility = View.GONE
        }

        phoneView!!.text = debt.phone ?: "-"
        phone = debt.phone

        if (debt.finished) {
            currentDebtHeader?.visibility = View.GONE
            closeDebtButton?.visibility = View.GONE
            finishedDebtHeader?.visibility = View.VISIBLE

            amount = debt.amount
            val amountStr = UiUtils.formatCurrency(Math.abs(amount), debt.currencyCode)

            finishedDebtDescription?.text = getString(R.string.debt_is_finished_description, amountStr)
        } else {
            currentDebtHeader?.visibility = View.VISIBLE
            closeDebtButton?.visibility = View.VISIBLE
            finishedDebtHeader?.visibility = View.GONE

            amount = debt.amount
            amountView!!.text = UiUtils.formatCurrency(Math.abs(amount), debt.currencyCode)

            if (amount > 0) {
                amountLabelView!!.setText(R.string.lending_amount)
            } else if (amount < 0) {
                amountLabelView!!.setText(R.string.borrowing_amount)
            } else {
                amountLabelView!!.setText(R.string.amount_label)
            }
        }

        updateNotes(debt)
    }

    private fun updateNotes(debt: Debt) {
        notesContainer?.removeAllViews()
        debt.notes
                .sortedByDescending { it.date.time }
                .forEach { createNoteView(it) }

        if (debt.notes.isEmpty()) {
            notesContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
        }
    }

    private fun createNoteView(note: DebtNote) {
        val view = View.inflate(this, R.layout.activity_debt_note, null)

        (view.findViewById(R.id.note) as TextView).text = note.text
        (view.findViewById(R.id.date) as TextView).text = dateTimeFormatter?.format(note.date) ?: ""
        view.findViewById(R.id.list_item).setOnClickListener {
            openNote(note)
        }
        view.findViewById(R.id.clear_btn).setOnClickListener {
            deleteNoteClicked(note)
        }
        notesContainer!!.addView(view)
    }

    private fun deleteNoteClicked(note: DebtNote) {
        AlertDialog.Builder(this)
                .setTitle(R.string.continue_question)
                .setMessage(R.string.note_will_be_deleted)
                .setPositiveButton(R.string.ok) { dialog, which -> deleteNote(note) }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun deleteNote(note: DebtNote) {
        showProgress()
        val s = dataManager.debtService
                .deleteDebtNote(note.id)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        {
                            hideProgress()
                            loadDebt()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun openNote(note: DebtNote) {
        val fragment = EditDebtNoteFragment.build(debtId, note) {
            loadDebt()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun showTransactionActivity(transactionId: Long) {
        EditTransactionActivity.actionStart(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
    }

    private fun transactionsLoaded(transactions: List<Transaction>) {
        transactionsContainer!!.removeAllViews()
        for (transaction in transactions) {
            createTransactionView(transaction)
        }
        if (transactions.isEmpty()) {
            transactionsContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
        }
    }

    @Suppress("DEPRECATION")
    private fun createTransactionView(transaction: Transaction) {
        val view = View.inflate(this, R.layout.activity_debt_transaction, null)

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
        }

        transactionsContainer!!.addView(view)
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                debtId = extras.getLong(UiConstants.EXTRA_ID)
            }
            if (extras.containsKey(UiConstants.EXTRA_NAME)) {
                debtName = extras.getString(UiConstants.EXTRA_NAME)
            }
        }
    }

    companion object {

        fun actionStart(fragment: Fragment, requestCode: Int, debtId: Long, debtName: String) {
            val intent = Intent(fragment.activity, DebtActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, debtId)
            intent.putExtra(UiConstants.EXTRA_NAME, debtName)

            fragment.startActivityForResult(intent, requestCode)
        }

        fun actionStart(activity: Activity, requestCode: Int, debtId: Long) {
            val intent = Intent(activity, DebtActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, debtId)

            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, requestCode: Int, debtId: Long) {
            val intent = Intent(fragment.activity, DebtActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, debtId)

            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
