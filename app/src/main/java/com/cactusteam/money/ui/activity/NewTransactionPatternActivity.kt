package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.Fragment
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils

/**
 * @author vpotapenko
 */
class NewTransactionPatternActivity : BaseTransactionPatternActivity("NewTransactionPatternActivity") {

    private var initialType = Transaction.EXPENSE

    private var typeSpinner: Spinner? = null
    private var currentToolbarColor: ColorDrawable? = null

    override fun selectCategoryClicked() {
        SelectCategoryActivity.actionStart(this, UiConstants.CATEGORY_REQUEST_CODE, if (typeSpinner!!.selectedItemPosition == 0) Category.EXPENSE else Category.INCOME)
    }

    override val currentTransactionType: Int
        get() = if (typeSpinner!!.selectedItemPosition == 1) Transaction.INCOME else Transaction.EXPENSE

    override fun dataLoaded() {
        updateTransactionViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeToolbar()

        loadData()
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
                .putName(name.toString())

        val sourceAccount = this.sourceAccount!!
        b.putSourceAccountId(sourceAccount.id)
                .putAmount(amount)

        val commentText = commentView!!.text
        if (!commentText.isNullOrBlank()) b.putComment(commentText.toString())

        for (tag in tagsFromView) {
            b.putTag(tag)
        }

        val position = typeSpinner!!.selectedItemPosition
        if (position == 0) {
            b.putType(Transaction.EXPENSE)
            b.putCategoryId(category!!.id).putSubcategoryId(subcategory?.id)
        } else if (position == 1) {
            b.putType(Transaction.INCOME)
            b.putCategoryId(category!!.id).putSubcategoryId(subcategory?.id)
        } else {
            b.putType(Transaction.TRANSFER)

            val destAccount = this.destAccount!!
            if (destAccount.id == sourceAccount.id) {
                destAccountErrorView!!.setText(R.string.accounts_must_be_different)
                destAccountErrorView!!.visibility = View.VISIBLE
                return
            }
            b.putDestAccountId(destAccount.id)

            if (rate != null) {
                b.putDestAmount(rate!!.convertTo(amount, destAccount.currencyCode))
                        .putRate(rate)
            } else {
                b.putDestAmount(amount)
            }
        }

        showBlockingProgress(getString(R.string.waiting))
        val s = b.create().subscribe(
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

    override fun clearError() {
        super.clearError()
        nameErrorView!!.visibility = View.GONE
    }

    @Suppress("DEPRECATION")
    override fun initializeToolbar() {
        super.initializeToolbar()

        supportActionBar!!.setDisplayShowTitleEnabled(false)

        initializeSpinner()

        currentToolbarColor = ColorDrawable(resources.getColor(R.color.color_primary))
        updateToolbarColor()
    }

    private fun initializeSpinner() {
        val adapter = ArrayAdapter(
                supportActionBar!!.themedContext,
                R.layout.activity_edit_transaction_type_view,
                android.R.id.text1,
                arrayOf(
                        getString(R.string.expense_label),
                        getString(R.string.income_label),
                        getString(R.string.transfer_label)
                )
        )
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)

        val view = View.inflate(supportActionBar!!.themedContext, R.layout.view_toolbar_spinner, null)
        typeSpinner = view.findViewById(R.id.spinner) as Spinner

        typeSpinner!!.adapter = adapter
        typeSpinner!!.setSelection(initialType)
        typeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onChangeTransactionType()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }
        toolbar!!.addView(view)
    }

    private fun updateTransactionViews() {
        when (typeSpinner!!.selectedItemPosition) {
            0 -> {
                categoryContainer!!.visibility = View.VISIBLE
                destAccountContainer!!.visibility = View.GONE

                updateCategory(Category.EXPENSE)
                updateAccounts()

                rate = null
            }
            1 -> {
                categoryContainer!!.visibility = View.VISIBLE
                destAccountContainer!!.visibility = View.GONE

                updateCategory(Category.INCOME)
                updateAccounts()

                rate = null
            }
            2 -> {
                categoryContainer!!.visibility = View.GONE
                destAccountContainer!!.visibility = View.VISIBLE

                updateAccounts()
            }
        }

        updateAmountView()
        updateRateContainer()
        updateToolbarColor()
    }

    private fun updateCategory(type: Int) {
        val categories = if (type == Category.EXPENSE) expenseCategories else incomeCategories

        val lastCategoryId = MoneyApp.instance.appPreferences.lastCategoryId
        if (lastCategoryId >= 0) {
            val c = categories.find { it.id == lastCategoryId }
            if (c != null) {
                updateCategory(c, null)
            } else {
                updateCategory(if (categories.isNotEmpty()) categories[0] else null, null)
            }
        } else {
            updateCategory(if (categories.isNotEmpty()) categories[0] else null, null)
        }
    }

    private fun updateAccounts() {
        val lastAccountId = MoneyApp.instance.appPreferences.lastAccountId
        if (lastAccountId >= 0) {
            val account = accounts.find { it.id == lastAccountId }
            updateSourceAccount(account ?: accounts[0])
        } else {
            updateSourceAccount(accounts[0])
        }


        val account = accounts.find { it.id != this.sourceAccount?.id }
        updateDestAccount(account ?: accounts[0])
    }

    private fun onChangeTransactionType() {
        updateTransactionViews()
    }

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun updateToolbarColor() {
        val nextColor: ColorDrawable

        val position = typeSpinner!!.selectedItemPosition
        if (position == 0) {
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_expense_color))
        } else if (position == 1) {
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_income_color))
        } else {
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_transfer_color))
        }

        if (DataUtils.hasLollipop()) {
            window.statusBarColor = UiUtils.darkenColor(nextColor.color)
        }

        val drawable = TransitionDrawable(arrayOf(currentToolbarColor, nextColor))
        currentToolbarColor = nextColor

        toolbar!!.setBackgroundDrawable(drawable)
        drawable.startTransition(UiConstants.TOOLBAR_TRANSITION_DURATION)
    }

    companion object {

        fun actionStart(fragment: Fragment, requestCode: Int) {
            val intent = Intent(fragment.activity, NewTransactionPatternActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
