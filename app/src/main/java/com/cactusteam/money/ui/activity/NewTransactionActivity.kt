package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.CurrencyRate
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.dao.TransactionPattern
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.ChoosePatternFragment
import org.apache.commons.lang3.time.DateUtils
import java.util.*

/**
 * @author vpotapenko
 */
class NewTransactionActivity(tag: String = "NewTransactionActivity") : BaseTransactionActivity(tag) {

    private var initialType = Transaction.EXPENSE

    private var initialAccount: Long = -1
    private var initialCategory: Long = -1
    private var initialSubcategory: Long = -1
    private var initialDestAccount: Long = -1

    private var currentToolbarColor: ColorDrawable? = null

    private var firstStart: Boolean = true

    private var typeSpinner: Spinner? = null

    class ActionBuilder {

        private var type: Int? = null
        private var accountId: Long? = null
        private var categoryId: Long? = null
        private var subcategoryId: Long? = null

        fun account(accountId: Long): ActionBuilder {
            this.accountId = accountId
            return this
        }

        fun category(categoryId: Long): ActionBuilder {
            this.categoryId = categoryId
            return this
        }

        fun subcategory(subcategoryId: Long): ActionBuilder {
            this.subcategoryId = subcategoryId
            return this
        }

        fun type(type: Int): ActionBuilder {
            this.type = type
            return this
        }

        fun createIntent(context: Context): Intent {
            val intent = Intent(context, NewTransactionActivity::class.java)
            if (accountId != null) {
                intent.putExtra(UiConstants.EXTRA_ACCOUNT, accountId!!)
            }
            if (categoryId != null) {
                intent.putExtra(UiConstants.EXTRA_CATEGORY, categoryId!!)
            }
            if (subcategoryId != null) {
                intent.putExtra(UiConstants.EXTRA_SUBCATEGORY, subcategoryId!!)
            }
            if (type != null) {
                intent.putExtra(UiConstants.EXTRA_TYPE, type!!)
            }
            return intent
        }

        fun start(activity: Activity, requestCode: Int) {
            val intent = createIntent(activity)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    override fun selectCategoryClicked() {
        SelectCategoryActivity.actionStart(this, UiConstants.CATEGORY_REQUEST_CODE, if (typeSpinner!!.selectedItemPosition == 0) Category.EXPENSE else Category.INCOME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        initializeToolbar()

        loadData()
    }

    override fun onResume() {
        super.onResume()

        val pref = MoneyApp.instance.appPreferences
        if (firstStart && isLoggedIn && pref.isOpenAmountTransaction) {
            showAmountDialog()
            firstStart = false
        }
    }

    override fun dataLoaded() {
        updateTransactionViews()
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

        if (initialCategory >= 0) {
            val c = categories.find { it.id == initialCategory }
            if (c != null) {
                val s = c.subcategories.find { it.id == initialSubcategory }
                updateCategory(c, s)
            } else {
                updateCategory(if (categories.isNotEmpty()) categories[0] else null, null)
            }
        } else {
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
    }

    private fun updateAccounts() {
        if (initialAccount >= 0) {
            val account = accounts.find { it.id == initialAccount }
            updateSourceAccount(account ?: accounts[0])
        } else {
            val lastAccountId = MoneyApp.instance.appPreferences.lastAccountId
            if (lastAccountId >= 0) {
                val account = accounts.find { it.id == lastAccountId }
                updateSourceAccount(account ?: accounts[0])
            } else {
                updateSourceAccount(accounts[0])
            }
        }

        if (initialDestAccount >= 0) {
            val account = accounts.find { it.id == initialDestAccount }
            updateDestAccount(account ?: accounts[0])
        } else {
            val account = accounts.find { it.id != this.sourceAccount?.id }
            updateDestAccount(account ?: accounts[0])
        }
    }

    override val currentTransactionType: Int
        get() = if (typeSpinner!!.selectedItemPosition == 1) Transaction.INCOME else Transaction.EXPENSE

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_new_transaction, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.show_patterns) {
            showPatterns()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPatterns() {
        val fragment = ChoosePatternFragment.build { pattern -> fillFromPattern(pattern) }
        fragment.show(fragmentManager, "dialog")
    }

    private fun fillFromPattern(pattern: TransactionPattern) {
        initialAccount = pattern.sourceAccountId
        initialCategory = if (pattern.categoryId == null) -1 else pattern.categoryId
        initialSubcategory = if (pattern.subcategoryId == null) -1 else pattern.subcategoryId

        if (pattern.type == Transaction.TRANSFER) {
            amount = pattern.amount

            initialDestAccount = pattern.destAccountId!!

            val sourceAmount = pattern.amount
            val destAmount = pattern.destAmount!!
            if (sourceAmount > destAmount) {
                rate = CurrencyRate()
                rate!!.sourceCurrencyCode = pattern.destAccount.currencyCode
                rate!!.destCurrencyCode = pattern.sourceAccount.currencyCode
                rate!!.rate = sourceAmount / destAmount
            } else if (destAmount > sourceAmount) {
                rate = CurrencyRate()
                rate!!.sourceCurrencyCode = pattern.sourceAccount.currencyCode
                rate!!.destCurrencyCode = pattern.destAccount.currencyCode
                rate!!.rate = destAmount / sourceAmount
            }
        } else {
            if (amount == 0.0) amount = pattern.amount

            rate = null
            initialDestAccount = -1
        }

        tagsContainer!!.removeAllViews()
        for (tag in pattern.tags) {
            createTag(tag.tag.name)
        }

        if (pattern.comment != null) {
            commentView!!.text = pattern.comment
        }

        if (typeSpinner!!.selectedItemPosition == pattern.type) {
            updateTransactionViews()
        } else {
            typeSpinner!!.setSelection(pattern.type)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.GROUP_INPUT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                typeSpinner!!.setSelection(0)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
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

        val sourceAccount = this.sourceAccount!!
        b.putSourceAccountId(sourceAccount.id)

        val now = Calendar.getInstance()
        if (!DateUtils.isSameDay(date, now) && date.after(now)) {
            b.putStatus(Transaction.STATUS_PLANNING)
        }
        b.putDate(date.time)
        b.putAmount(amount)

        val commentText = commentView!!.text
        if (!commentText.isNullOrBlank())
            b.putComment(commentText.toString())

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
                b.putRate(rate)
            } else {
                b.putDestAmount(amount)
            }
        }

        showBlockingProgress(getString(R.string.waiting))
        b.create().subscribe(
                { r ->
                    hideBlockingProgress()
                    transactionSaved()
                },
                { e ->
                    hideBlockingProgress()
                    showError(e.message)
                }
        )
    }

    private fun transactionSaved() {
        Toast.makeText(this, R.string.transaction_was_saved, Toast.LENGTH_SHORT).show()

        setResult(Activity.RESULT_OK)
        finish()
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
                        getString(R.string.transfer_label),
                        getString(R.string.group_input_label)
                )
        )
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)

        val view = View.inflate(supportActionBar!!.themedContext, R.layout.view_toolbar_spinner, null)
        typeSpinner = view.findViewById(R.id.spinner) as Spinner

        typeSpinner!!.adapter = adapter
        typeSpinner!!.setSelection(initialType)
        typeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 3) {
                    openGroupInputActivity()
                } else {
                    onChangeTransactionType()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }
        toolbar!!.addView(view)
    }

    private fun openGroupInputActivity() {
        if (amount > 0 && categoryContainer!!.visibility == View.VISIBLE) {
            val sourceAccount = this.sourceAccount!!
            GroupInputTransactionActivity.actionStart(
                    this,
                    UiConstants.GROUP_INPUT_REQUEST_CODE,
                    sourceAccount.id,
                    category?.id,
                    subcategory?.id,
                    category?.type,
                    amount
            )
        } else {
            GroupInputTransactionActivity.actionStart(this, UiConstants.GROUP_INPUT_REQUEST_CODE)
        }
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

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ACCOUNT)) {
                initialAccount = extras.getLong(UiConstants.EXTRA_ACCOUNT)
            }
            if (extras.containsKey(UiConstants.EXTRA_CATEGORY)) {
                initialCategory = extras.getLong(UiConstants.EXTRA_CATEGORY)
            }
            if (extras.containsKey(UiConstants.EXTRA_SUBCATEGORY)) {
                initialSubcategory = extras.getLong(UiConstants.EXTRA_SUBCATEGORY)
            }
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                initialType = extras.getInt(UiConstants.EXTRA_TYPE)
            }
        }
    }
}
