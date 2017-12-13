package com.cactusteam.money.ui.activity

import android.content.Context
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
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.model.TransactionSearch
import com.cactusteam.money.ui.ListItem
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiObjectRef
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.format.DateTimeFormatter
import rx.Subscription

class SearchTransactionsActivity : BaseDataActivity("SearchTransactionsActivity") {

    var listView: RecyclerView? = null
    var noDataView: View? = null

    var currentSubscription: Subscription? = null
    var currentSearch: TransactionSearch? = null

    private var dateTimeFormatter: DateTimeFormatter? = null
    private var syncSupported: Boolean = false

    private val icons = mutableMapOf<String, UiObjectRef>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_transactions)

        initializeToolbar()
        initializeViewProgress()

        val moneyApp = MoneyApp.instance
        dateTimeFormatter = DateTimeFormatter.create(moneyApp.appPreferences.transactionFormatDateMode, this)

        syncSupported = moneyApp.syncManager.isSyncConnected

        listView = findViewById(R.id.list) as RecyclerView?
        listView?.layoutManager = LinearLayoutManager(this)
        listView?.adapter = ListAdapter()

        noDataView = findViewById(R.id.no_data)

        val queryEdit = findViewById(R.id.edit_query) as EditText
        queryEdit.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                performQueryClicked(v.text)
                true
            } else {
                false
            }
        }
    }

    private fun requestCategoryIcon(iconKey: String) {
        val icon = UiObjectRef()
        icons.put(iconKey, icon)

        val s = dataManager.systemService.getIconImage(iconKey)
                .subscribe(
                        { r ->
                            icon.ref = r
                            listView?.adapter?.notifyDataSetChanged()
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun performQueryClicked(text: CharSequence?) {
        cancelCurrentSubscription()
        if (text.isNullOrBlank()) {
            showNoData()
        } else {
            performQuery(text.toString())
        }
    }

    private fun performQuery(query: String) {
        showProgress()
        currentSearch = TransactionSearch(query)
        currentSubscription = dataManager.transactionService
                .search(currentSearch!!)
                .subscribe(
                        { r ->
                            hideProgress()
                            transactionsLoaded(r.transactions, r.hasMore)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
    }

    private fun loadAdditional() {
        showProgress()
        currentSubscription = dataManager.transactionService
                .search(currentSearch!!)
                .subscribe(
                        { r ->
                            hideProgress()
                            additionalLoaded(r.transactions, r.hasMore)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
    }

    private fun transactionsLoaded(transactions: MutableList<Transaction>, hasMore: Boolean) {
        val adapter = listView?.adapter as ListAdapter
        adapter.items.clear()

        if (transactions.isNotEmpty()) {
            transactions.mapTo(adapter.items) { ListItem(ITEM_TRANSACTION, it) }
            if (hasMore) {
                adapter.items.add(ListItem(ITEM_MORE, null))
            }
            adapter.notifyDataSetChanged()
        } else {
            adapter.notifyDataSetChanged()
            showNoData()
        }
    }

    private fun additionalLoaded(transactions: MutableList<Transaction>, hasMore: Boolean) {
        val adapter = listView?.adapter as ListAdapter

        if (adapter.items.isNotEmpty() && adapter.items.last().type == ITEM_MORE) {
            adapter.items.removeAt(adapter.items.lastIndex)
        }
        if (transactions.isNotEmpty()) {
            transactions.mapTo(adapter.items) { ListItem(ITEM_TRANSACTION, it) }
            if (hasMore) {
                adapter.items.add(ListItem(ITEM_MORE, null))
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showNoData() {
        noDataView?.visibility = View.VISIBLE
    }

    override fun showProgress() {
        noDataView?.visibility = View.GONE
        super.showProgress()
    }

    override fun onDestroy() {
        cancelCurrentSubscription()
        for ((key, value) in icons) {
            if (value.ref != null) {
                val bitmap = value.getRefAs(Bitmap::class.java)
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
        super.onDestroy()
    }

    private fun cancelCurrentSubscription() {
        if (currentSubscription != null) {
            currentSubscription?.unsubscribe()
            currentSubscription = null
        }
    }

    private fun moreClicked() {
        if (currentSearch != null) {
            loadAdditional()
        }
    }

    private fun showTransactionActivity(transactionId: Long) {
        EditTransactionActivity.actionStart(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE, transactionId)
    }

    private inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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
            dateView.text = dateTimeFormatter!!.format(transaction.date)
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
                View.inflate(itemView.context, R.layout.fragment_transactions_tag, tagsContainer)
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
                    if (categoryIcon.ref != null) {
                        val drawable = BitmapDrawable(resources, categoryIcon.getRefAs(Bitmap::class.java))
                        drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
                        iconView.setImageDrawable(drawable)
                    }
                }
            }
        }

        fun bindMoreItem() {
            itemView.findViewById(R.id.load_more_btn).setOnClickListener { moreClicked() }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {

        val items = mutableListOf<ListItem>()

        override fun onBindViewHolder(holder: ListViewHolder?, position: Int) {
            val item = items[position]
            when (getItemViewType(position)) {
                ITEM_TRANSACTION -> holder?.bindTransaction(item.obj as Transaction)
                ITEM_MORE -> holder?.bindMoreItem()
            }
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].type
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ListViewHolder {
            var layoutId = 0
            when (viewType) {
                ITEM_TRANSACTION -> layoutId = R.layout.activity_search_transactions_item
                ITEM_MORE -> layoutId = R.layout.activity_search_transactions_more_item
            }
            val v = LayoutInflater.from(parent?.context).inflate(layoutId, parent, false)
            return ListViewHolder(v)
        }

    }

    companion object {

        val ITEM_TRANSACTION = 0
        val ITEM_MORE = 1

        fun actionStart(context: Context) {
            val intent = Intent(context, SearchTransactionsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
