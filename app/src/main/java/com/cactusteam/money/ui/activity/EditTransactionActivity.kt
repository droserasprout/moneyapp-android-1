package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.Fragment
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.CurrencyRate
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import org.apache.commons.lang3.time.DateUtils
import java.util.*

/**
 * @author vpotapenko
 */
class EditTransactionActivity : BaseTransactionActivity("EditTransactionActivity") {
    private var transactionId: Long = 0

    private var transaction: Transaction? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_edit_transaction, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.delete) {
            deleteTransaction()
            return true
        } else if (itemId == R.id.copy) {
            copyTransaction()
            return true
        } else if (itemId == R.id.save_as_pattern) {
            saveAsPatternClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveAsPatternClicked() {
        val view = View.inflate(this, R.layout.view_new_name, null)
        val nameEdit = view.findViewById(R.id.name) as EditText
        AlertDialog.Builder(this).setTitle(R.string.create_pattern).setView(view).setPositiveButton(R.string.ok) { dialog, which ->
            val text = nameEdit.text
            if (!TextUtils.isEmpty(text)) saveAsPattern(text.toString())
        }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun saveAsPattern(name: String) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.patternService
                .createPatternFromTransaction(transactionId, name)
                .subscribe(
                        { r ->
                            hideBlockingProgress()
                            patternSaved()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun patternSaved() {
        setResult(Activity.RESULT_OK)
        Toast.makeText(this, R.string.pattern_was_saved, Toast.LENGTH_SHORT).show()
    }

    private fun copyTransaction() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.transactionService
                .copyTransaction(transactionId)
                .subscribe(
                        { r ->
                            hideBlockingProgress()
                            transactionCopied(r)
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun transactionCopied(transaction: Transaction) {
        Toast.makeText(this, R.string.transaction_was_copied, Toast.LENGTH_SHORT).show()

        EditTransactionActivity.actionStart(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transaction.id!!)
    }

    private fun deleteTransaction() {
        AlertDialog.Builder(this)
                .setTitle(R.string.continue_question)
                .setMessage(R.string.transaction_will_be_deleted)
                .setPositiveButton(android.R.string.yes) { dialog, which -> removeTransaction() }
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    private fun removeTransaction() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.transactionService
                .deleteTransaction(transactionId)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            transactionDeleted()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun transactionDeleted() {
        Toast.makeText(this, R.string.transaction_was_deleted, Toast.LENGTH_SHORT).show()

        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun selectCategoryClicked() {
        SelectCategoryActivity.actionStart(this, UiConstants.CATEGORY_REQUEST_CODE, if (isExpense) Category.EXPENSE else Category.INCOME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)

        initializeToolbar()
        loadTransaction()
    }

    private fun loadTransaction() {
        showProgress()
        val s = dataManager.transactionService
                .getTransaction(transactionId)
                .subscribe(
                        { r ->
                            transaction = r
                            loadData()
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    override fun dataLoaded() {
        amount = transaction!!.amount
        date.time = transaction!!.date

        for (tag in transaction!!.tags) {
            createTag(tag.tag.name)
        }
        prepareForTransactionType()
        fillView()
    }

    private fun fillView() {
        if (isExpense) {
            updateAccounts(false)
            updateCategory(transaction!!.category, transaction!!.subcategory)
        } else if (isIncome) {
            updateAccounts(false)
            updateCategory(transaction!!.category, transaction!!.subcategory)
        } else {
            updateAccounts(true)

            val sourceAmount = transaction!!.amount
            val destAmount = transaction!!.destAmount!!
            if (sourceAmount > destAmount) {
                rate = CurrencyRate()
                rate!!.sourceCurrencyCode = transaction!!.destAccount.currencyCode
                rate!!.destCurrencyCode = transaction!!.sourceAccount.currencyCode
                rate!!.rate = sourceAmount / destAmount
            } else if (destAmount > sourceAmount) {
                rate = CurrencyRate()
                rate!!.sourceCurrencyCode = transaction!!.sourceAccount.currencyCode
                rate!!.destCurrencyCode = transaction!!.destAccount.currencyCode
                rate!!.rate = destAmount / sourceAmount
            }
        }
        updateRateContainer()
        updateAmountView()
        updateDateViews()

        if (transaction!!.comment != null) {
            commentView!!.text = transaction!!.comment
        }
    }

    private fun updateAccounts(transfer: Boolean) {
        updateSourceAccount(transaction!!.sourceAccount)
        if (transfer) {
            updateDestAccount(transaction!!.destAccount)
        }
    }

    override val currentTransactionType: Int
        get() = if (transaction != null) transaction!!.type else Transaction.EXPENSE

    private val isExpense: Boolean
        get() = transaction!!.type == Transaction.EXPENSE

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun prepareForTransactionType() {
        val nextColor: ColorDrawable
        if (isExpense) {
            setTitle(R.string.expense_label)
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_expense_color))

            categoryContainer!!.visibility = View.VISIBLE
            destAccountContainer!!.visibility = View.GONE
        } else if (isIncome) {
            setTitle(R.string.income_label)
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_income_color))

            categoryContainer!!.visibility = View.VISIBLE
            destAccountContainer!!.visibility = View.GONE
        } else {
            setTitle(R.string.transfer_label)
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_transfer_color))

            categoryContainer!!.visibility = View.GONE
            destAccountContainer!!.visibility = View.VISIBLE
        }

        if (DataUtils.hasLollipop()) {
            window.statusBarColor = UiUtils.darkenColor(nextColor.color)
        }

        val drawable = TransitionDrawable(arrayOf<Drawable>(ColorDrawable(resources.getColor(R.color.color_primary)), nextColor))

        //noinspection deprecation
        toolbar!!.setBackgroundDrawable(drawable)
        drawable.startTransition(UiConstants.TOOLBAR_TRANSITION_DURATION)
    }

    private val isIncome: Boolean
        get() = transaction!!.type == Transaction.INCOME

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                transactionId = extras.getLong(UiConstants.EXTRA_ID)
            }
        }
    }

    override fun saveClicked() {
        clearError()

        if (amount <= 0) {
            amountErrorView!!.setText(R.string.amount_must_be_more_than_zero)
            amountErrorView!!.visibility = View.VISIBLE
            return
        }

        val b = dataManager.transactionService.newTransactionBuilder()
                .putId(transactionId)
        val sourceAccount = this.sourceAccount!!
        b.putSourceAccountId(sourceAccount.id)
                .putDate(date.time)
                .putAmount(amount)
                .putRef(transaction!!.ref)

        val commentText = commentView!!.text
        if (!commentText.isNullOrBlank()) b.putComment(commentText.toString())

        for (tag in tagsFromView) {
            b.putTag(tag)
        }

        if (isExpense) {
            b.putCategoryId(category!!.id).putSubcategoryId(subcategory?.id)
        } else if (isIncome) {
            b.putCategoryId(category!!.id).putSubcategoryId(subcategory?.id)
        } else {
            val destAccount = this.destAccount!!
            if (destAccount.id == sourceAccount.id) {
                destAccountErrorView!!.setText(R.string.accounts_must_be_different)
                destAccountErrorView!!.visibility = View.VISIBLE
                return
            }
            b.putDestAccountId(destAccount.id)

            if (rate != null) {
                b.putDestAmount(rate!!.convertTo(amount, destAccount.currencyCode))
            } else {
                b.putDestAmount(amount)
            }
        }

        val now = Calendar.getInstance()
        if (!DateUtils.isSameDay(date, now) && date.after(now)) {
            b.putStatus(Transaction.STATUS_PLANNING)
        } else {
            b.putStatus(Transaction.STATUS_COMPLETED)
        }

        showBlockingProgress(getString(R.string.waiting))
        val s = b.update().subscribe(
                { r ->
                    hideBlockingProgress()
                    transactionSaved()
                },
                { e ->
                    hideBlockingProgress()
                    showError(e.message)
                }
        )
        compositeSubscription.add(s)
    }

    private fun transactionSaved() {
        Toast.makeText(this, R.string.transaction_was_saved, Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {

        fun actionStart(activity: Activity, requestCode: Int, transactionId: Long) {
            val intent = Intent(activity, EditTransactionActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, transactionId)

            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, requestCode: Int, transactionId: Long) {
            val intent = Intent(fragment.activity, EditTransactionActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, transactionId)

            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
