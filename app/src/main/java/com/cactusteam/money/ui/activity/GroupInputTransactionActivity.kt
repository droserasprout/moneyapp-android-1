package com.cactusteam.money.ui.activity

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.text.format.DateFormat
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.data.model.BalanceChange
import com.cactusteam.money.data.model.BalanceChangeGroup
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiObjectRef
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.ChooseCategoryFragment
import org.apache.commons.lang3.time.DateUtils
import rx.Observable
import java.util.*

class GroupInputTransactionActivity : BaseDataActivity("GroupInputTransactionActivity") {

    // Extras
    private var initialAccountId: Long? = null
    private var initialCategoryId: Long? = null
    private var initialSubcategoryId: Long? = null
    private var initialType: Int? = null
    private var initialAmount: Double? = null
    private var initialComment: String? = null

    private val initialTags: MutableList<String> = mutableListOf()

    private val waitingItemsContainer = ItemsContainer()

    private val balanceChangeGroup = BalanceChangeGroup()

    private val expenseCategories: MutableList<Category> = mutableListOf()
    private val incomeCategories: MutableList<Category> = mutableListOf()
    private val accounts: MutableList<Account> = mutableListOf()

    private var sourceAccountIconView: ImageView? = null
    private var sourceAccountNameView: TextView? = null
    private var dateView: TextView? = null
    private var timeView: TextView? = null
    private var commentView: TextView? = null
    private var tagsContainer: LinearLayout? = null
    private var tagEdit: AutoCompleteTextView? = null

    private var itemsContainer: LinearLayout? = null
    private var noDataView: View? = null
    private var expenseView: TextView? = null
    private var incomeView: TextView? = null

    private var saveButton: Button? = null

    private var sourceAccount: Account? = null
    private var date = Calendar.getInstance()

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
        if (savedInstanceState == null) injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_input_transaction)

        initializeViewProgress()
        initializeToolbar()

        restoreState(savedInstanceState)
        initializeView()

        loadData()
    }

    private fun initializeView() {
        saveButton = findViewById(R.id.save_btn) as Button
        saveButton!!.setOnClickListener {
            addTagClicked()
            saveClicked()
        }

        dateView = findViewById(R.id.date) as TextView
        findViewById(R.id.date_container).setOnClickListener { dateClicked() }

        timeView = findViewById(R.id.time) as TextView
        findViewById(R.id.time_container).setOnClickListener { timeClicked() }
        updateDateViews()

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

        commentView = findViewById(R.id.comment) as TextView
        if (initialComment != null) {
            commentView!!.text = initialComment
            initialComment = null
        }

        findViewById(R.id.add_item_btn).setOnClickListener {
            addItemClicked()
        }

        sourceAccountNameView = findViewById(R.id.source_account_name) as TextView
        findViewById(R.id.source_account_container).setOnClickListener {
            SelectAccountActivity.actionStart(this, UiConstants.SOURCE_ACCOUNT_REQUEST_CODE)
        }
        sourceAccountIconView = findViewById(R.id.source_account_icon) as ImageView

        itemsContainer = findViewById(R.id.items_container) as LinearLayout
        noDataView = findViewById(R.id.no_data)
        expenseView = findViewById(R.id.expense) as TextView
        incomeView = findViewById(R.id.income) as TextView

        updateTotalChangeViews()
    }

    private fun addItemClicked() {
        val fragment = ChooseCategoryFragment.build(true) { p ->
            itemSelected(p)
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun itemSelected(item: Pair<Category, Subcategory?>) {
        val ref = waitingItemsContainer.putItem(item)
        CalculatorActivity.actionStart(this, UiConstants.CALCULATOR_REQUEST_CODE, 0.0, null, ref)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.CALCULATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val a = Math.abs(data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0))

                val ref = data.getStringExtra(UiConstants.EXTRA_ID)
                val item = waitingItemsContainer.extractItem(ref)
                if (item != null) {
                    createBalanceChange(item, a)
                }
            }
        } else if (requestCode == UiConstants.SOURCE_ACCOUNT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val id = data.getLongExtra(UiConstants.EXTRA_ID, -1)
                val account = accounts.find { it.id == id }
                if (account != null) updateSourceAccount(account)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun createBalanceChange(item: Pair<Category, Subcategory?>, amount: Double) {
        val balanceChange = BalanceChange(item.first.id, item.second?.id, item.first.type, amount)
        balanceChangeGroup.changes.add(balanceChange)

        createChangeView(item, amount, balanceChange)
        updateTotalChangeViews()
    }

    private fun createChangeView(item: Pair<Category, Subcategory?>, amount: Double, balanceChange: BalanceChange) {
        val view = View.inflate(this, R.layout.activity_group_input_transaction_change, null)
        view.findViewById(R.id.remove_btn).setOnClickListener {
            removeChangeClicked(balanceChange, item.first.name)
        }
        updateChangeView(view, item, amount)

        itemsContainer!!.addView(view)
        balanceChange.view = view

        updateTotalChangeViews()
        updateNoDataView()
    }

    private fun updateNoDataView() {
        noDataView!!.visibility = if (itemsContainer!!.childCount > 0) View.GONE else View.VISIBLE
    }

    private fun removeChangeClicked(balanceChange: BalanceChange, name: String) {
        AlertDialog.Builder(this)
                .setTitle(R.string.continue_question)
                .setMessage(getString(R.string.item_will_be_deleted, name))
                .setPositiveButton(android.R.string.yes) { dialog, which -> removeChange(balanceChange) }
                .setNegativeButton(android.R.string.no, null)
                .show()

    }

    private fun removeChange(balanceChange: BalanceChange) {
        if (balanceChangeGroup.changes.remove(balanceChange)) {
            itemsContainer!!.removeView(balanceChange.view)

            updateTotalChangeViews()
            updateNoDataView()
        }
    }

    @Suppress("DEPRECATION")
    private fun updateChangeView(view: View, item: Pair<Category, Subcategory?>, amount: Double) {
        val category = item.first
        val subcategory = item.second

        (view.findViewById(R.id.name) as TextView).text = category.name

        val account = this.sourceAccount
        if (account is Account) {
            val sourceAmountStr = UiUtils.formatCurrency(amount, account.currencyCode)
            val amountView = view.findViewById(R.id.amount) as TextView
            amountView.text = sourceAmountStr

            val color = if (category.type == Category.EXPENSE) resources.getColor(R.color.toolbar_expense_color) else resources.getColor(R.color.toolbar_income_color)
            amountView.setTextColor(color)
        }

        val subcategoryView = view.findViewById(R.id.subcategory_name) as TextView
        if (subcategory != null) {
            subcategoryView.visibility = View.VISIBLE
            subcategoryView.text = subcategory.name
        } else {
            subcategoryView.visibility = View.GONE
        }

        updateIconView(view, category)
    }

    private fun updateTotalChangeViews() {
        val account = this.sourceAccount
        if (account is Account) {
            val expense = balanceChangeGroup.expense
            if (expense > 0) {
                expenseView!!.visibility = View.VISIBLE
                val amountStr = UiUtils.formatCurrency(expense, account.currencyCode)
                expenseView!!.text = getString(R.string.expense_pattern, amountStr)
            } else {
                expenseView!!.visibility = View.GONE
            }

            val income = balanceChangeGroup.income
            if (income > 0) {
                incomeView!!.visibility = View.VISIBLE
                val amountStr = UiUtils.formatCurrency(income, account.currencyCode)
                incomeView!!.text = getString(R.string.income_pattern, amountStr)
            } else {
                incomeView!!.visibility = View.GONE
            }
        } else {
            expenseView!!.visibility = View.GONE
            incomeView!!.visibility = View.GONE
        }
    }

    private fun saveClicked() {
        if (balanceChangeGroup.changes.isEmpty()) {
            finish()
        } else {
            val b = dataManager.transactionService.newTransactionBuilder()
            val sourceAccount = this.sourceAccount!!
            b.putSourceAccountId(sourceAccount.id)

            val now = Calendar.getInstance()
            if (!DateUtils.isSameDay(date, now) && date.after(now)) {
                b.putStatus(Transaction.STATUS_PLANNING)
            }
            b.putDate(date.time)

            val commentText = commentView!!.text
            if (!commentText.isNullOrBlank())
                b.putComment(commentText.toString())

            for (tag in tagsFromView) {
                b.putTag(tag)
            }

            showBlockingProgress(getString(R.string.waiting))
            val observables: MutableList<Observable<Transaction>> = mutableListOf()
            for (ch in balanceChangeGroup.changes) {
                val c = b.clone()
                c.putCategoryId(ch.categoryId)
                c.putSubcategoryId(ch.subcategoryId)
                c.putType(if (ch.type == Category.INCOME) Transaction.INCOME else Transaction.EXPENSE)
                c.putAmount(ch.amount)

                observables.add(c.create())
            }

            Observable.merge(observables.asIterable())
                    .subscribe(
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
    }

    private fun transactionSaved() {
        Toast.makeText(this, R.string.transaction_was_saved, Toast.LENGTH_SHORT).show()

        setResult(Activity.RESULT_OK)
        finish()
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

    private fun createTag(newTag: CharSequence) {
        View.inflate(this, R.layout.activity_edit_transaction_tag, tagsContainer)
        val textView = tagsContainer!!.getChildAt(tagsContainer!!.childCount - 1) as TextView
        textView.text = newTag
        textView.setOnClickListener { tagsContainer!!.removeView(textView) }


        tagEdit!!.setText("")
    }

    private fun updateTagEdit(tags: List<Tag>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tags)
        tagEdit!!.setAdapter(adapter)

        while (!initialTags.isEmpty()) {
            createTag(initialTags.removeAt(0))
        }
    }

    private fun dateClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            date.set(year, monthOfYear, dayOfMonth)
            updateDateViews()
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun timeClicked() {
        val timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            date.set(Calendar.HOUR_OF_DAY, hourOfDay)
            date.set(Calendar.MINUTE, minute)
            updateDateViews()
        }, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), DateFormat.is24HourFormat(this))
        timePickerDialog.show()
    }

    private fun updateDateViews() {
        dateView!!.text = DateFormat.getDateFormat(this).format(date.time)
        timeView!!.text = DateFormat.getTimeFormat(this).format(date.time)

        val now = Calendar.getInstance()
        if (!DateUtils.isSameDay(date, now) && date.after(now)) {
            saveButton!!.setText(R.string.plan)
        } else {
            saveButton!!.setText(R.string.save)
        }
    }

    private fun updateChangesList() {
        for (change in balanceChangeGroup.changes) {
            val item = findItem(change)
            if (item != null && change.view != null) {
                updateChangeView(change.view!!, item, change.amount)
            }
        }
        updateTotalChangeViews()
    }

    private fun findItem(change: BalanceChange): Pair<Category, Subcategory?>? {
        val category = if (change.type == Category.EXPENSE) {
            expenseCategories.find { it.id == change.categoryId }
        } else {
            incomeCategories.find { it.id == change.categoryId }
        }
        return if (category != null) {
            val subcategory = category.subcategories.find { it.id == change.subcategoryId }
            Pair(category, subcategory)
        } else {
            null
        }
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(UiConstants.EXTRA_TIME)) {
                date.time = Date(savedInstanceState.getLong(UiConstants.EXTRA_TIME))
            }
            if (savedInstanceState.containsKey(UiConstants.EXTRA_CHANGES)) {
                val c = savedInstanceState.getString(UiConstants.EXTRA_CHANGES)
                balanceChangeGroup.extract(c)
            } else {
                balanceChangeGroup.changes.clear()
            }
            if (savedInstanceState.containsKey(UiConstants.EXTRA_ACCOUNT)) {
                initialAccountId = savedInstanceState.getLong(UiConstants.EXTRA_ACCOUNT)
            }
            if (savedInstanceState.containsKey(UiConstants.EXTRA_COMMENT)) {
                initialComment = savedInstanceState.getString(UiConstants.EXTRA_COMMENT)
            }
            if (savedInstanceState.containsKey(UiConstants.EXTRA_TAGS)) {
                val list = savedInstanceState.getStringArrayList(UiConstants.EXTRA_TAGS)
                initialTags.addAll(list)
            }
        }

        if (initialCategoryId != null && initialAmount != null && initialType != null) {
            balanceChangeGroup.changes.add(BalanceChange(initialCategoryId!!, initialSubcategoryId, initialType!!, initialAmount!!))

            initialCategoryId = null
            initialSubcategoryId = null
            initialType = null
            initialAmount = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(UiConstants.EXTRA_CHANGES, balanceChangeGroup.asString())
        outState?.putLong(UiConstants.EXTRA_TIME, date.time.time)

        val account = this.sourceAccount
        if (account is Account) {
            outState?.putLong(UiConstants.EXTRA_ACCOUNT, account.id)
        }

        val comment = commentView!!.text
        if (!comment.isNullOrBlank()) {
            outState?.putString(UiConstants.EXTRA_COMMENT, comment.toString())
        }

        val tags = tagsFromView
        if (!tags.isEmpty()) {
            outState?.putStringArrayList(UiConstants.EXTRA_TAGS, ArrayList(tags))
        }
        super.onSaveInstanceState(outState)
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadData() {
        showProgress()
        val o1 = dataManager.accountService.getAccounts()
        val o2 = dataManager.categoryService.getCategories(Category.EXPENSE)
        val o3 = dataManager.categoryService.getCategories(Category.INCOME)

        val s = Observable.zip(o1, o2, o3, { i1, i2, i3 ->
            listOf(i1, i2, i3)
        }).subscribe(
                { r ->
                    hideProgress()
                    accountsLoaded(r[0] as List<Account>)
                    expenseLoaded(r[1] as List<Category>)
                    incomeLoaded(r[2] as List<Category>)

                    populateChangesList()
                },
                { e ->
                    hideProgress()
                    showError(e.message)
                }
        )
        compositeSubscription.add(s)
    }

    private fun populateChangesList() {
        for (change in balanceChangeGroup.changes) {
            val category = findItem(change)
            if (category != null) {
                createChangeView(category, change.amount, change)
            }
        }

        updateTotalChangeViews()
        updateNoDataView()
    }

    private fun incomeLoaded(list: List<Category>) {
        incomeCategories.clear()
        incomeCategories.addAll(list)
    }

    private fun expenseLoaded(list: List<Category>) {
        expenseCategories.clear()
        expenseCategories.addAll(list)
    }

    private fun accountsLoaded(r: List<Account>) {
        accounts.clear()
        accounts.addAll(r)

        val accountId = if (initialAccountId != null) {
            val temp = initialAccountId
            initialAccountId = null

            temp
        } else {
            MoneyApp.instance.appPreferences.lastAccountId
        }

        val account = accounts.find { it.id == accountId }
        updateSourceAccount(account ?: accounts[0])
    }

    private fun getMockBitmap(): Bitmap? {
        if (mockBitmap == null) {
            mockBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_mock_category)
        }
        return mockBitmap
    }

    private fun requestCategoryIcon(iconKey: String, view: View, category: Category) {
        val icon = UiObjectRef()
        icons.put(iconKey, icon)

        val s = dataManager.systemService
                .getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.ref = r
                            updateIconView(view, category)
                        },
                        { e -> showError(e.message) }
                )
        compositeSubscription.add(s)
    }

    private fun updateIconView(view: View, category: Category) {
        val iconView = view.findViewById(R.id.icon) as ImageView
        val icon = category.icon
        if (icon != null) {
            val categoryIcon = icons[icon]
            if (categoryIcon == null) {
                requestCategoryIcon(icon, view, category)
            } else {
                val drawable = BitmapDrawable(resources, if (categoryIcon.ref != null) categoryIcon.getRefAs(Bitmap::class.java) else getMockBitmap())
                drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                iconView.setImageDrawable(drawable)
            }
        } else {
            val drawable = BitmapDrawable(resources, getMockBitmap())
            drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
            iconView.setImageDrawable(drawable)
        }
    }

    private fun injectExtras() {
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ACCOUNT)) {
                initialAccountId = extras.getLong(UiConstants.EXTRA_ACCOUNT)
            }
            if (extras.containsKey(UiConstants.EXTRA_CATEGORY)) {
                initialCategoryId = extras.getLong(UiConstants.EXTRA_CATEGORY)
            }
            if (extras.containsKey(UiConstants.EXTRA_SUBCATEGORY)) {
                initialSubcategoryId = extras.getLong(UiConstants.EXTRA_SUBCATEGORY)
            }
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                initialType = extras.getInt(UiConstants.EXTRA_TYPE)
            }
            if (extras.containsKey(UiConstants.EXTRA_AMOUNT)) {
                initialAmount = extras.getDouble(UiConstants.EXTRA_AMOUNT)
            }
        }
    }

    private fun updateSourceAccount(account: Account) {
        this.sourceAccount = account
        updateSourceAccountView()
        updateChangesList()
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

    private val tagsFromView: List<String>
        get() {
            val tags = (0..tagsContainer!!.childCount - 1)
                    .map { tagsContainer!!.getChildAt(it) }
                    .filterIsInstance<TextView>()
                    .map { it.text.toString() }
            return tags
        }

    companion object {

        fun actionStart(
                activity: Activity,
                requestCode: Int,
                initialAccountId: Long? = null,
                initialCategoryId: Long? = null,
                initialSubcategoryId: Long? = null,
                initialType: Int? = null,
                initialAmount: Double? = null
        ) {
            val intent = Intent(activity, GroupInputTransactionActivity::class.java)

            if (initialAccountId != null) intent.putExtra(UiConstants.EXTRA_ACCOUNT, initialAccountId)
            if (initialCategoryId != null) intent.putExtra(UiConstants.EXTRA_CATEGORY, initialCategoryId)
            if (initialSubcategoryId != null) intent.putExtra(UiConstants.EXTRA_SUBCATEGORY, initialSubcategoryId)
            if (initialType != null) intent.putExtra(UiConstants.EXTRA_TYPE, initialType)
            if (initialAmount != null) intent.putExtra(UiConstants.EXTRA_AMOUNT, initialAmount)

            activity.startActivityForResult(intent, requestCode)
        }
    }

    class ItemsContainer {

        private var count: Int = 0
        private val items: MutableMap<String, Pair<Category, Subcategory?>> = mutableMapOf()

        fun putItem(p: Pair<Category, Subcategory?>): String {
            count++

            val ref = count.toString()
            items[ref] = p

            return ref
        }

        fun extractItem(ref: String): Pair<Category, Subcategory?>? {
            return if (items.contains(ref)) {
                items.remove(ref)
            } else {
                null
            }
        }
    }
}
