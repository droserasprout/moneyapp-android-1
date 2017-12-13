package com.cactusteam.money.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Pair
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.ITransactionFilter
import com.cactusteam.money.data.period.Period
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.*
import com.cactusteam.money.ui.grouping.*
import java.util.*

/**
 * @author vpotapenko
 */
class TransactionsFragment : BaseMainFragment() {

    private val periodsStack = mutableListOf<Pair<Date, Date>>()
    private val icons = mutableMapOf<String, Icon>()
    private val groups = mutableListOf<TransactionsGrouper.Group>()

    private var syncSupported: Boolean = false
    private var mainCurrencyCode: String? = null
    private var period: Period? = null
    private var transactionsGrouper: TransactionsGrouper? = null

    private var periodView: TextView? = null
    private var nextPeriodButton: View? = null

    private var listView: RecyclerView? = null
    private var noDataView: View? = null

    private var filter: ITransactionFilter? = null
    private var fromDate: Date? = null
    private var toDate: Date? = null

    private var timeFormat: java.text.DateFormat? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadTransactions()
            }
        } else if (requestCode == UiConstants.IMPORT_TRANSACTIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadTransactions()
            }
        } else if (requestCode == UiConstants.CATEGORIES_REPORT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadTransactions()
            }
        }
    }

    override fun onDestroyView() {
        for ((key, value) in icons) {
            value.bitmap?.recycle()
        }
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.group_by) {
            showGroupByDialog()
            return true
        } else if (itemId == R.id.export_transactions) {
            showExportTransactionsActivity()
            return true
        } else if (itemId == R.id.import_transactions) {
            showImportTransactionsActivity()
            return true
        } else if (itemId == R.id.fold_all) {
            foldAllClicked()
            return true
        } else if (itemId == R.id.unfold_all) {
            unfoldAllClicked()
            return true
        } else if (itemId == R.id.search) {
            searchClicked()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    private fun searchClicked() {
        SearchTransactionsActivity.actionStart(activity)
    }

    private fun unfoldAllClicked() {
        groups.forEach { it.opened = true }
        updateAdapterItems()
    }

    private fun foldAllClicked() {
        groups.forEach { it.opened = false }
        updateAdapterItems()
    }

    private fun showExportTransactionsActivity() {
        ExportTransactionsActivity.actionStart(activity)
    }

    private fun showImportTransactionsActivity() {
        ImportTransactionsActivity.actionStart(this, UiConstants.IMPORT_TRANSACTIONS_REQUEST_CODE)
    }

    private fun showGroupByDialog() {
        val items = arrayOf<CharSequence>(getString(R.string.grouping_date), getString(R.string.grouping_account), getString(R.string.grouping_category), getString(R.string.grouping_tag), getString(R.string.grouping_transaction_type), getString(R.string.grouping_without_group))
        AlertDialog.Builder(activity)
                .setTitle(R.string.group_by)
                .setItems(items) { dialog, which ->
                    when (which) {
                        0 -> changeGrouper(DateTransactionsGrouper(activity))
                        1 -> changeGrouper(AccountTransactionsGrouper())
                        2 -> changeGrouper(CategoryTransactionsGrouper(activity))
                        3 -> changeGrouper(TagTransactionsGrouper(activity))
                        4 -> changeGrouper(TransactionTypeTransactionsGrouper(activity))
                        5 -> changeGrouper(OneGroupTransactionsGrouper(activity))
                    }
                }.show()
    }

    private fun changeGrouper(grouper: TransactionsGrouper) {
        transactionsGrouper = grouper
        loadTransactions()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_transactions, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        val moneyApp = MoneyApp.instance
        mainCurrencyCode = moneyApp.appPreferences.mainCurrencyCode

        syncSupported = moneyApp.syncManager.isSyncConnected

        period = moneyApp.period
        val current = period!!.current
        periodsStack.add(Pair(current.first, current.second))

        transactionsGrouper = DateTransactionsGrouper(activity)

        timeFormat = DateFormat.getTimeFormat(activity)

        nextPeriodButton = view.findViewById(R.id.next_period_btn)
        nextPeriodButton!!.setOnClickListener { nextPeriodClicked() }

        view.findViewById(R.id.previous_period_btn).setOnClickListener { previousPeriodClicked() }

        periodView = view.findViewById(R.id.period) as TextView
        updatePeriodView()

        if (fromDate != null) {
            view.findViewById(R.id.header).visibility = View.GONE
        }

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        noDataView = view.findViewById(R.id.no_data)

        view.findViewById(R.id.period_container).setOnClickListener { showDetails() }

        loadTransactions()
    }

    private fun showDetails() {
        val current = topStackPeriod
        CategoriesReportActivity.actionStart(this, UiConstants.CATEGORIES_REPORT_REQUEST_CODE, current.first, current.second, Category.EXPENSE)
    }

    private fun previousPeriodClicked() {
        val lastPeriod = topStackPeriod
        val previous = period!!.getPrevious(lastPeriod.first, lastPeriod.second)
        periodsStack.add(Pair(previous.first, previous.second))
        updatePeriodView()

        loadTransactions()
    }

    private fun nextPeriodClicked() {
        if (!isCurrentPeriod) {
            periodsStack.removeAt(periodsStack.size - 1)
            updatePeriodView()

            loadTransactions()
        }
    }

    private fun loadTransactions() {
        showProgress()
        noDataView!!.visibility = View.GONE
        val b = dataManager.transactionService
                .newListTransactionsBuilder()
                .putConvertToMain(true)
        if (fromDate == null) {
            val current = topStackPeriod
            b.putFrom(current.first)
            b.putTo(if (isCurrentPeriod) Date() else current.second)
        } else {
            b.putFrom(fromDate!!).putTo(toDate!!)
        }
        val s = b.list()
                .subscribe(
                        { r ->
                            transactionsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun transactionsLoaded(result: List<Transaction>) {
        val transactions = filterTransactions(result)

        groups.clear()
        val list = transactionsGrouper!!.group(transactions)
        var current: TransactionsGrouper.Group? = null
        for (item in list) {
            when (item.type) {
                TransactionsGrouper.GROUP -> {
                    current = item.obj as TransactionsGrouper.Group
                    groups.add(current)
                }
                TransactionsGrouper.TRANSACTION -> {
                    current?.items?.add(item)
                }
            }
        }
        if (groups.isNotEmpty()) groups[0].opened = true

        if (filter == null && isCurrentPeriod) {
            loadPlanningTransaction()
        } else {
            hideProgress()
            updateAdapterItems()
        }
    }

    private fun showEmptyViewIfNoTransactions() {
        val adapter = listView!!.adapter as ListAdapter
        if (adapter.items.isEmpty()) {
            noDataView!!.visibility = View.VISIBLE
        }
    }

    private fun filterTransactions(transactions: List<Transaction>): List<Transaction> {
        return if (filter == null) transactions else transactions.filter { filter!!.allow(it) }
    }

    private fun loadPlanningTransaction() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, 1)
        val s = dataManager.transactionService
                .newListTransactionsBuilder()
                .putNotStatus(Transaction.STATUS_COMPLETED)
                .list()
                .subscribe(
                        { r ->
                            planningTransactionsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun planningTransactionsLoaded(transactions: List<Transaction>) {
        if (!transactions.isEmpty()) {
            val group = TransactionsGrouper.Group(-1, getString(R.string.planning_transactions))
            for (i in transactions.indices.reversed()) {
                group.items.add(0, TransactionsGrouper.Item(TransactionsGrouper.TRANSACTION, transactions[i]))
            }
            groups.add(0, group)
        }

        updateAdapterItems()
        hideProgress()
    }

    private fun updateAdapterItems() {
        val adapter = listView!!.adapter as ListAdapter

        adapter.items.clear()
        groups.forEach {
            adapter.items.add(TransactionsGrouper.Item(TransactionsGrouper.GROUP, it))
            if (it.opened) {
                adapter.items.addAll(it.items)
            }
        }
        adapter.notifyDataSetChanged()
        showEmptyViewIfNoTransactions()
    }

    private fun updatePeriodView() {
        val current = topStackPeriod
        val s = DateUtils.formatDateRange(activity, current.first.time, current.second.time, DateUtils.FORMAT_SHOW_DATE)
        periodView!!.text = s

        nextPeriodButton!!.visibility = if (isCurrentPeriod) View.INVISIBLE else View.VISIBLE
    }

    private val isCurrentPeriod: Boolean
        get() = periodsStack.size == 1

    private val topStackPeriod: Pair<Date, Date>
        get() = periodsStack[periodsStack.size - 1]

    private fun showTransactionActivity(transactionId: Long) {
        EditTransactionActivity.actionStart(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
    }

    private fun requestCategoryIcon(iconKey: String) {
        val icon = Icon()
        icons.put(iconKey, icon)
        val s = dataManager.systemService
                .getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.bitmap = r
                            listView?.adapter?.notifyDataSetChanged()
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    fun setFilter(filter: ITransactionFilter?) {
        this.filter = filter
    }

    fun setFromDate(fromDate: Date) {
        this.fromDate = fromDate
    }

    fun setToDate(toDate: Date) {
        this.toDate = toDate
    }

    override fun dataChanged() {
        loadTransactions()
    }

    private fun groupClicked(group: TransactionsGrouper.Group) {
        group.opened = !group.opened
        updateAdapterItems()
    }

    private inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @Suppress("DEPRECATION")
        fun bindTransaction(transaction: Transaction) {
            itemView.findViewById(R.id.transaction).setOnClickListener { showTransactionActivity(transaction.id!!) }

            val commentView = itemView.findViewById(R.id.comment) as TextView
            if (transaction.comment != null) {
                commentView.text = transaction.comment
                commentView.visibility = View.VISIBLE
            } else {
                commentView.visibility = View.GONE
            }

            val dateView = itemView.findViewById(R.id.date) as TextView
            dateView.text = timeFormat!!.format(transaction.date)
            if (syncSupported) {
                val synced = transaction.synced
                if (synced != null && synced) {
                    dateView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_sync_updated, 0)
                } else {
                    dateView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_sync_dirty, 0)
                }
            } else {
                dateView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }

            val tagsContainer = itemView.findViewById(R.id.tags_container) as LinearLayout
            tagsContainer.removeAllViews()
            for (tag in transaction.tags) {
                View.inflate(activity, R.layout.fragment_transactions_tag, tagsContainer)
                val textView = tagsContainer.getChildAt(tagsContainer.childCount - 1) as TextView
                textView.text = tag.tag.name
            }

            val accountView = itemView.findViewById(R.id.source_account) as TextView
            accountView.text = transaction.sourceAccount.name

            val category = transaction.category
            val amountTextView = itemView.findViewById(R.id.amount) as TextView
            if (transaction.type == Transaction.EXPENSE) {
                accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expense_transaction, 0)
                (itemView.findViewById(R.id.dest_name) as TextView).text = transaction.categoryDisplayName

                val amountStr = UiUtils.formatCurrency(-transaction.amount, transaction.sourceAccount.currencyCode)
                amountTextView.text = amountStr
                amountTextView.setTextColor(resources.getColor(R.color.toolbar_expense_color))

                updateIconView(category)
            } else if (transaction.type == Transaction.INCOME) {
                accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_income_transaction, 0)
                (itemView.findViewById(R.id.dest_name) as TextView).text = transaction.categoryDisplayName

                val amountStr = UiUtils.formatCurrency(transaction.amount, transaction.sourceAccount.currencyCode)
                amountTextView.text = amountStr
                amountTextView.setTextColor(resources.getColor(R.color.toolbar_income_color))

                updateIconView(category)
            } else {
                accountView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_transfer_transaction, 0)

                (itemView.findViewById(R.id.dest_name) as TextView).text = transaction.destAccount.name

                val sourceCurrencyCode = transaction.sourceAccount.currencyCode
                val destCurrencyCode = transaction.destAccount.currencyCode

                var amountStr = UiUtils.formatCurrency(transaction.amount, sourceCurrencyCode)
                if (sourceCurrencyCode != destCurrencyCode) {
                    val destAmount = UiUtils.formatCurrency(transaction.destAmount!!, destCurrencyCode)
                    amountStr = "$amountStr ($destAmount)"
                }

                amountTextView.text = amountStr
                amountTextView.setTextColor(Color.BLACK)

                var color = Color.DKGRAY
                try {
                    color = Color.parseColor(transaction.sourceAccount.color)
                } catch (ignore: Exception) {
                }

                val drawable: Drawable
                when (transaction.sourceAccount.type) {
                    Account.SAVINGS_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_savings))
                    Account.BANK_ACCOUNT_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_bank_account))
                    Account.CARD_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_card))
                    else -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
                }
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                (itemView.findViewById(R.id.icon) as ImageView).setImageDrawable(drawable)
            }
        }

        private fun updateIconView(category: Category?) {
            val iconView = itemView.findViewById(R.id.icon) as ImageView
            iconView.setImageResource(R.drawable.ic_mock_icon)
            if (category == null) return

            val icon = category.icon
            if (icon != null) {
                val categoryIcon = icons[icon]
                if (categoryIcon == null) {
                    requestCategoryIcon(icon)
                } else {
                    if (categoryIcon.bitmap != null) {
                        val drawable = BitmapDrawable(resources, categoryIcon.bitmap)
                        drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                        iconView.setImageDrawable(drawable)
                    }
                }
            }
        }

        fun bindGroup(group: TransactionsGrouper.Group) {
            (itemView.findViewById(R.id.name) as TextView).text = group.title

            val expenseView = itemView.findViewById(R.id.expense) as TextView
            if (group.expense != 0.0) {
                expenseView.visibility = View.VISIBLE
                val amountStr = UiUtils.formatCurrency(group.expense, mainCurrencyCode)
                expenseView.text = getString(R.string.expense_pattern, amountStr)
            } else {
                expenseView.visibility = View.GONE
            }

            val incomeView = itemView.findViewById(R.id.income) as TextView
            if (group.income != 0.0) {
                incomeView.visibility = View.VISIBLE
                val amountStr = UiUtils.formatCurrency(group.income, mainCurrencyCode)
                incomeView.text = getString(R.string.income_pattern, amountStr)
            } else {
                incomeView.visibility = View.GONE
            }

            itemView.findViewById(R.id.list_item).setOnClickListener { groupClicked(group) }

            val textView = itemView.findViewById(R.id.transactions_count) as TextView
            if (group.opened) {
                textView.visibility = View.GONE
            } else {
                textView.text = "(${group.items.size})"
                textView.visibility = View.VISIBLE
            }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {

        val items = mutableListOf<TransactionsGrouper.Item>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
            var layoutId = 0
            when (viewType) {
                TransactionsGrouper.TRANSACTION -> layoutId = R.layout.fragment_transactions_item
                TransactionsGrouper.GROUP -> layoutId = R.layout.fragment_transactions_group
            }
            val v = LayoutInflater.from(activity).inflate(layoutId, parent, false)
            return ListViewHolder(v)
        }

        override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
            val type = getItemViewType(position)
            if (type == TransactionsGrouper.TRANSACTION) {
                holder.bindTransaction(items[position].obj as Transaction)
            } else if (type == TransactionsGrouper.GROUP) {
                holder.bindGroup(items[position].obj as TransactionsGrouper.Group)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].type
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }

    private class Icon {

        var bitmap: Bitmap? = null
    }

    companion object {

        fun build(): TransactionsFragment {
            return TransactionsFragment()
        }
    }
}
