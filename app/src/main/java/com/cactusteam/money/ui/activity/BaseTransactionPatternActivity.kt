package com.cactusteam.money.ui.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.text.Html
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.ui.HtmlTagHandler
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiObjectRef
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.ChooseRateFragment
import rx.Observable

/**
 * @author vpotapenko
 */
abstract class BaseTransactionPatternActivity(tag: String) : BaseDataActivity(tag) {

    private val tagHandler = HtmlTagHandler()

    protected val initialTags: MutableList<String> = mutableListOf()

    protected val expenseCategories: MutableList<Category> = mutableListOf()
    protected val incomeCategories: MutableList<Category> = mutableListOf()
    protected val accounts: MutableList<Account> = mutableListOf()

    protected var sourceAccountIconView: ImageView? = null
    protected var sourceAccountNameView: TextView? = null
    protected var destAccountIconView: ImageView? = null
    protected var destAccountNameView: TextView? = null
    protected var categoryIconView: ImageView? = null
    protected var categoryNameView: TextView? = null
    protected var subcategoryNameView: TextView? = null
    protected var destAccountErrorView: TextView? = null

    protected var amountView: TextView? = null
    protected var amountErrorView: TextView? = null
    protected var commentView: TextView? = null
    protected var rateView: TextView? = null

    protected var categoryContainer: View? = null
    protected var subcategoryContainer: View? = null
    protected var destAccountContainer: View? = null
    protected var rateContainer: View? = null
    protected var rateProgress: View? = null

    protected var nameView: TextView? = null
    protected var nameErrorView: TextView? = null

    protected var tagsContainer: LinearLayout? = null
    protected var tagEdit: AutoCompleteTextView? = null

    protected var saveButton: Button? = null

    protected var sourceAccount: Account? = null
    protected var destAccount: Account? = null
    protected var category: Category? = null
    protected var subcategory: Subcategory? = null

    protected var amount: Double = 0.toDouble()
    protected var rate: CurrencyRate? = null

    private val icons = ArrayMap<String, UiObjectRef>()
    private var mockBitmap: Bitmap? = null

    override fun onDestroy() {
        for ((key, value) in icons) {
            if (value.ref != null) value.getRefAs(Bitmap::class.java).recycle()
        }
        if (mockBitmap != null && !mockBitmap!!.isRecycled) {
            mockBitmap!!.recycle()
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transaction_pattern)

        initializeViewProgress()
        initializeView()
    }

    protected fun initializeView() {
        sourceAccountNameView = findViewById(R.id.source_account_name) as TextView
        findViewById(R.id.source_account_container).setOnClickListener {
            SelectAccountActivity.actionStart(this, UiConstants.SOURCE_ACCOUNT_REQUEST_CODE)
        }
        sourceAccountIconView = findViewById(R.id.source_account_icon) as ImageView

        destAccountContainer = findViewById(R.id.dest_account_container)
        destAccountContainer!!.setOnClickListener {
            SelectAccountActivity.actionStart(this, UiConstants.DEST_ACCOUNT_REQUEST_CODE)
        }
        destAccountNameView = findViewById(R.id.dest_account_name) as TextView
        destAccountIconView = findViewById(R.id.dest_account_icon) as ImageView

        tagsContainer = findViewById(R.id.tags_container) as LinearLayout

        tagEdit = findViewById(R.id.tag_edit) as AutoCompleteTextView
        tagEdit!!.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                addTagClicked()
                return@OnEditorActionListener true
            }
            false
        })
        tagEdit!!.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, id ->
            addTagClicked()
        }
        val s = dataManager.tagService
                .getTags()
                .subscribe(
                        { r -> updateTagEdit(r) },
                        { e -> showError(e.message) }
                )
        compositeSubscription.add(s)

        rateView = findViewById(R.id.rate) as TextView
        rateProgress = findViewById(R.id.rate_progress_bar)
        rateContainer = findViewById(R.id.rate_container)
        rateContainer!!.setOnClickListener { rateClicked() }

        categoryContainer = findViewById(R.id.category_container)
        categoryContainer!!.setOnClickListener {
            selectCategoryClicked()
        }
        categoryIconView = findViewById(R.id.category_icon) as ImageView?
        categoryNameView = findViewById(R.id.category_name) as TextView?
        subcategoryContainer = findViewById(R.id.subcategory_container)
        subcategoryNameView = findViewById(R.id.subcategory_name) as TextView?

        nameView = findViewById(R.id.name) as TextView
        nameErrorView = findViewById(R.id.name_error) as TextView

        saveButton = findViewById(R.id.save_btn) as Button
        saveButton!!.setOnClickListener {
            addTagClicked()
            saveClicked()
        }

        commentView = findViewById(R.id.comment) as TextView
        amountErrorView = findViewById(R.id.amount_error) as TextView
        destAccountErrorView = findViewById(R.id.dest_account_error) as TextView
        amountView = findViewById(R.id.amount) as TextView
        findViewById(R.id.amount_container).setOnClickListener { showAmountDialog() }
    }

    abstract fun selectCategoryClicked()

    protected fun updateSourceAccount(account: Account) {
        this.sourceAccount = account
        updateSourceAccountView()

        updateAmountView()
        updateRateContainer()
    }

    protected fun updateDestAccount(account: Account) {
        this.destAccount = account
        updateDestAccountView()

        clearError()
        updateAmountView()
        updateRateContainer()
    }

    private fun updateSourceAccountView() {
        sourceAccountNameView!!.text = sourceAccount?.name ?: ""

        var color = Color.DKGRAY
        try {
            color = Color.parseColor(sourceAccount?.color)
        } catch (ignore: Exception) {
        }

        val drawable: Drawable?
        when (sourceAccount?.type) {
            Account.SAVINGS_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_savings))
            Account.BANK_ACCOUNT_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_bank_account))
            Account.CARD_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_card))
            Account.CASH_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
            else -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
        }

        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        sourceAccountIconView!!.setImageDrawable(drawable)
    }

    private fun updateDestAccountView() {
        destAccountNameView!!.text = destAccount?.name ?: ""

        var color = Color.DKGRAY
        try {
            color = Color.parseColor(destAccount?.color)
        } catch (ignore: Exception) {
        }

        val drawable: Drawable?
        when (destAccount?.type) {
            Account.SAVINGS_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_savings))
            Account.BANK_ACCOUNT_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_bank_account))
            Account.CARD_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_card))
            Account.CASH_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
            else -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
        }

        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        destAccountIconView!!.setImageDrawable(drawable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.CALCULATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                amount = Math.abs(data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0))
                updateAmountView()
            }
        } else if (requestCode == UiConstants.SOURCE_ACCOUNT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val id = data.getLongExtra(UiConstants.EXTRA_ID, -1)
                val account = accounts.find { it.id == id }
                if (account != null) updateSourceAccount(account)
            }
        } else if (requestCode == UiConstants.DEST_ACCOUNT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val id = data.getLongExtra(UiConstants.EXTRA_ID, -1)
                val account = accounts.find { it.id == id }
                if (account != null) updateDestAccount(account)
            }
        } else if (requestCode == UiConstants.CATEGORY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.getBooleanExtra(UiConstants.EXTRA_CHANGES, false)) {
                    val o1 = dataManager.categoryService.getCategories(Category.EXPENSE)
                    val o2 = dataManager.categoryService.getCategories(Category.INCOME)
                    showProgress()
                    val s = Observable.zip(o1, o2, { i1, i2 -> Pair(i1, i2) })
                            .subscribe(
                                    { r ->
                                        hideProgress()
                                        expenseCategories.clear()
                                        expenseCategories.addAll(r.first)

                                        incomeCategories.clear()
                                        incomeCategories.addAll(r.second)

                                        updateCategory(data)
                                    },
                                    { e ->
                                        hideProgress()
                                        showError(e.message)
                                    }
                            )
                    compositeSubscription.add(s)
                } else {
                    updateCategory(data)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun updateCategory(data: Intent) {
        val id = data.getLongExtra(UiConstants.EXTRA_CATEGORY, -1)
        val type = data.getIntExtra(UiConstants.EXTRA_TYPE, -1)
        val c = if (type == Category.EXPENSE) {
            expenseCategories.find { it.id == id }
        } else {
            incomeCategories.find { it.id == id }
        }

        val subcategoryId = data.getLongExtra(UiConstants.EXTRA_SUBCATEGORY, -1)
        val s = if (subcategoryId >= 0 && c != null) {
            c.subcategories.find { it.id == subcategoryId }
        } else null

        updateCategory(c, s)
    }

    protected abstract val currentTransactionType: Int

    @Suppress("UNCHECKED_CAST")
    protected fun loadData() {
        showProgress()
        val o1 = dataManager.accountService.getAccounts()
        val o2 = dataManager.categoryService.getCategories(Category.EXPENSE)
        val o3 = dataManager.categoryService.getCategories(Category.INCOME)

        val s = Observable.zip(o1, o2, o3, { i1, i2, i3 ->
            listOf(i1, i2, i3)
        }).subscribe(
                { r ->
                    hideProgress()
                    accounts.clear()
                    accounts.addAll(r[0] as List<Account>)

                    expenseCategories.clear()
                    expenseCategories.addAll(r[1] as List<Category>)

                    incomeCategories.clear()
                    incomeCategories.addAll(r[2] as List<Category>)

                    dataLoaded()
                },
                { e ->
                    hideProgress()
                    showError(e.message)
                }
        )
        compositeSubscription.add(s)
    }

    protected abstract fun dataLoaded()

    private fun updateTagEdit(tags: List<Tag>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tags)
        tagEdit!!.setAdapter(adapter)

        while (!initialTags.isEmpty()) {
            createTag(initialTags.removeAt(0))
        }
    }

    private fun addTagClicked() {
        val newTagText = tagEdit!!.text
        if (newTagText.isNullOrBlank()) return

        val newTag = newTagText.toString().trim { it <= ' ' }
        (0..tagsContainer!!.childCount - 1)
                .map { tagsContainer!!.getChildAt(it) }
                .filterIsInstance<TextView>()
                .map { it.text }
                .filter { it == newTag }
                .forEach { return }

        createTag(newTag)
    }

    protected fun createTag(newTag: CharSequence) {
        View.inflate(this, R.layout.activity_edit_transaction_tag, tagsContainer)
        val textView = tagsContainer!!.getChildAt(tagsContainer!!.childCount - 1) as TextView
        textView.text = newTag
        textView.setOnClickListener { tagsContainer!!.removeView(textView) }


        tagEdit!!.setText("")
    }

    protected val tagsFromView: List<String>
        get() {
            val tags = (0..tagsContainer!!.childCount - 1)
                    .map { tagsContainer!!.getChildAt(it) }
                    .filterIsInstance<TextView>()
                    .map { it.text }
                    .map { it.toString() }
            return tags
        }

    private fun rateClicked() {
        val fragment = ChooseRateFragment.build(rate!!.sourceCurrencyCode, rate!!.destCurrencyCode, rate!!.rate)
        fragment.onChooseRateListener = { sourceCode, destCode, r ->
            rate = CurrencyRate()
            rate!!.sourceCurrencyCode = sourceCode
            rate!!.destCurrencyCode = destCode
            rate!!.rate = r

            updateRateView()
            updateAmountView()
        }
        fragment.show(fragmentManager, "dialog")
    }

    protected abstract fun saveClicked()

    protected fun updateRateContainer() {
        if (destAccountContainer!!.visibility == View.GONE) {
            rateContainer!!.visibility = View.GONE
        } else {
            val sourceAccount = this.sourceAccount
            val destAccount = this.destAccount

            if (sourceAccount !is Account || destAccount !is Account) {
                rateContainer!!.visibility = View.GONE
            } else {
                val sourceCurrencyCode = sourceAccount.currencyCode
                val destCurrencyCode = destAccount.currencyCode
                if (sourceCurrencyCode == destCurrencyCode) {
                    rateContainer!!.visibility = View.GONE
                    rate = null
                    updateAmountView()
                } else {
                    rateContainer!!.visibility = View.VISIBLE
                    loadRate(sourceCurrencyCode, destCurrencyCode)
                }
            }
        }
    }

    private fun loadRate(source: String, dest: String) {
        if (rate != null && rate!!.same(source, dest)) {
            updateRateView()
            updateAmountView()
        } else {
            fillRateAsStub(source, dest)

            rateProgress!!.visibility = View.VISIBLE
            rateView!!.visibility = View.GONE

            val s = dataManager.currencyService.getRate(source, dest)
                    .subscribe(
                            { r ->
                                rateProgress!!.visibility = View.GONE
                                rateView!!.visibility = View.VISIBLE

                                if (r == null) {
                                    fillRateAsStub(source, dest)
                                } else {
                                    rate = r
                                }

                                updateRateView()
                                updateAmountView()
                            },
                            { e ->
                                rateProgress!!.visibility = View.GONE
                                showError(e.message)
                            }
                    )
            compositeSubscription.add(s)
        }
    }

    private fun fillRateAsStub(source: String, dest: String) {
        rate = CurrencyRate()
        rate!!.rate = 1.0
        rate!!.sourceCurrencyCode = source
        rate!!.destCurrencyCode = dest
    }

    private fun updateRateView() {
        val source = UiUtils.formatCurrency(1.0, rate!!.sourceCurrencyCode)
        val dest = UiUtils.formatCurrency(rate!!.rate, rate!!.destCurrencyCode)

        rateView!!.text = getString(R.string.rate_pattern, source, dest)
    }

    protected open fun clearError() {
        amountErrorView!!.visibility = View.GONE
        destAccountErrorView!!.visibility = View.GONE
    }

    protected fun showAmountDialog() {
        CalculatorActivity.actionStart(this, UiConstants.CALCULATOR_REQUEST_CODE, amount)
    }

    protected fun updateCategory(c: Category?, s: Subcategory?) {
        this.category = c
        this.subcategory = s

        updateCategoryView()
    }

    @Suppress("DEPRECATION")
    private fun updateCategoryView() {
        if (category != null) {
            var name: CharSequence = category!!.name
            if (category!!.deleted) {
                val s = String.format(UiConstants.DELETED_PATTERN, name)
                name = Html.fromHtml(s, null, tagHandler)
            }
            categoryNameView!!.text = name

            val icon = category!!.icon
            if (icon != null) {
                val categoryIcon = icons[icon]
                if (categoryIcon == null) {
                    requestCategoryIcon(icon)
                } else {
                    val drawable = BitmapDrawable(resources, if (categoryIcon.ref != null) categoryIcon.getRefAs(Bitmap::class.java) else getMockBitmap())
                    drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                    categoryIconView!!.setImageDrawable(drawable)
                }
            } else {
                val drawable = BitmapDrawable(resources, getMockBitmap())
                drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                categoryIconView!!.setImageDrawable(drawable)
            }
        }
        if (subcategory != null) {
            subcategoryContainer!!.visibility = View.VISIBLE
            subcategoryNameView!!.text = subcategory!!.name
        } else {
            subcategoryContainer!!.visibility = View.GONE
        }
    }

    protected fun updateAmountView() {
        val account = this.sourceAccount
        if (account is Account) {
            val sourceAmountStr = UiUtils.formatCurrency(amount, account.currencyCode)

            if (rate != null) {
                val destAccount = this.destAccount!!
                val destAmount = rate!!.convertTo(amount, destAccount.currencyCode)
                val destAmountStr = UiUtils.formatCurrency(destAmount, destAccount.currencyCode)

                amountView!!.text = getString(R.string.rate_pattern, sourceAmountStr, destAmountStr)
            } else {
                amountView!!.text = sourceAmountStr
            }

            clearError()
        }
    }

    private fun requestCategoryIcon(iconKey: String) {
        val icon = UiObjectRef()
        icons.put(iconKey, icon)

        val s = dataManager.systemService
                .getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.ref = r
                            updateCategoryView()
                        },
                        { e -> showError(e.message) }
                )
        compositeSubscription.add(s)
    }


    private fun getMockBitmap(): Bitmap? {
        if (mockBitmap == null) {
            mockBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_mock_category)
        }
        return mockBitmap
    }
}
