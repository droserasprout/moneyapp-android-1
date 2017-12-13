package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.Fragment
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils

/**
 * @author vpotapenko
 */
class EditTransactionPatternActivity : BaseTransactionPatternActivity("EditTransactionPatternActivity") {

    private var patternId: Long = 0
    private var pattern: TransactionPattern? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_edit_transaction_pattern, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.delete) {
            deleteClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteClicked() {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.pattern_will_be_deleted).setPositiveButton(android.R.string.yes) { dialog, which -> deletePattern() }.setNegativeButton(android.R.string.no, null).show()
    }

    private fun deletePattern() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.patternService
                .deletePattern(patternId)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()

                            Toast.makeText(this, R.string.pattern_was_deleted, Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                )
        compositeSubscription.add(s)
    }

    override fun saveClicked() {
        clearError()

        if (amount < 0) amount = 0.0

        val name = nameView!!.text
        if (name.isNullOrBlank()) {
            nameErrorView!!.setText(R.string.pattern_name_is_required)
            nameErrorView!!.visibility = View.VISIBLE
            return
        }

        val b = dataManager.patternService
                .newPatternBuilder()
                .putId(patternId)

        val sourceAccount = this.sourceAccount!!

        b.putName(name.toString())
                .putSourceAccountId(sourceAccount.id!!)
                .putAmount(amount)

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

        showBlockingProgress(getString(R.string.waiting))
        val s = b.update()
                .subscribe(
                        { r ->
                            hideBlockingProgress()
                            Toast.makeText(this, R.string.pattern_was_saved, Toast.LENGTH_SHORT).show()

                            setResult(RESULT_OK)
                            finish()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    override fun selectCategoryClicked() {
        SelectCategoryActivity.actionStart(this, UiConstants.CATEGORY_REQUEST_CODE, if (isExpense) Category.EXPENSE else Category.INCOME)
    }

    override fun clearError() {
        super.clearError()
        nameErrorView!!.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)

        initializeToolbar()
        loadPattern()
    }

    private fun loadPattern() {
        showProgress()
        val s = dataManager.patternService
                .getPattern(patternId)
                .subscribe(
                        { r ->
                            hideProgress()
                            pattern = r

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
        amount = pattern!!.amount
        for (tag in pattern!!.tags) {
            createTag(tag.tag.name)
        }
        prepareForPatternType()
        fillView()
    }

    private fun fillView() {
        if (isExpense) {
            updateAccounts(false)
            updateCategory(pattern!!.category, pattern!!.subcategory)
        } else if (isIncome) {
            updateAccounts(false)
            updateCategory(pattern!!.category, pattern!!.subcategory)
        } else {
            updateAccounts(true)

            val sourceAmount = pattern!!.amount
            val destAmount = pattern!!.destAmount!!
            if (sourceAmount > destAmount) {
                rate = CurrencyRate()
                rate!!.sourceCurrencyCode = pattern!!.destAccount.currencyCode
                rate!!.destCurrencyCode = pattern!!.sourceAccount.currencyCode
                rate!!.rate = sourceAmount / destAmount
            } else if (destAmount > sourceAmount) {
                rate = CurrencyRate()
                rate!!.sourceCurrencyCode = pattern!!.sourceAccount.currencyCode
                rate!!.destCurrencyCode = pattern!!.destAccount.currencyCode
                rate!!.rate = destAmount / sourceAmount
            }
        }
        updateRateContainer()
        updateAmountView()

        if (pattern!!.comment != null) {
            commentView!!.text = pattern!!.comment
        }

        nameView!!.text = pattern!!.name
        if (pattern!!.comment != null) {
            commentView!!.text = pattern!!.comment
        }
    }

    private fun updateAccounts(transfer: Boolean) {
        updateSourceAccount(pattern!!.sourceAccount)
        if (transfer) {
            updateDestAccount(pattern!!.destAccount)
        }
    }

    override val currentTransactionType: Int
        get() = if (pattern != null) pattern!!.type else Transaction.EXPENSE

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun prepareForPatternType() {
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

    private val isExpense: Boolean
        get() = pattern!!.type == Transaction.EXPENSE

    private val isIncome: Boolean
        get() = pattern!!.type == Transaction.INCOME

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                patternId = extras.getLong(UiConstants.EXTRA_ID)
            }
        }
    }

    companion object {

        fun actionStart(fragment: Fragment, requestCode: Int, patternId: Long) {
            val intent = Intent(fragment.activity, EditTransactionPatternActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, patternId)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
