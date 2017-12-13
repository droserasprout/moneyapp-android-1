package com.cactusteam.money.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.filter.AndTransactionFilters
import com.cactusteam.money.data.filter.TypeTransactionFilter
import com.cactusteam.money.data.filter.WithoutTagTransactionFilter
import com.cactusteam.money.data.model.TagInfo
import com.cactusteam.money.data.service.TagService
import com.cactusteam.money.ui.ListItem
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.FilteredTransactionsActivity
import com.cactusteam.money.ui.activity.TagActivity
import com.cactusteam.money.ui.activity.TagsReportActivity
import java.util.*

/**
 * @author vpotapenko
 */
class TagsFragment : BaseMainFragment() {

    private var typeSpinner: Spinner? = null
    private var listView: RecyclerView? = null

    private var currencyCode: String? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.TAGS_REPORT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.TAG_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE) {
            loadData()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_tags, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.report) {
            showReportActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showReportActivity() {
        TagsReportActivity.actionStart(this, UiConstants.TAGS_REPORT_REQUEST_CODE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        currencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode

        typeSpinner = view.findViewById(R.id.type) as Spinner
        val adapter = ArrayAdapter(activity,
                R.layout.fragment_tags_item_type,
                android.R.id.text1,
                arrayOf(getString(R.string.expense_label), getString(R.string.income_label)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        typeSpinner!!.adapter = adapter
        typeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        initializeProgress(view.findViewById(R.id.progress_bar), listView!!)
        loadData()

    }

    private fun showWithoutTagTransactions() {
        val filters = AndTransactionFilters()
        filters.addFilter(WithoutTagTransactionFilter.instance)
        filters.addFilter(TypeTransactionFilter(if (typeSpinner!!.selectedItemPosition == 0) Transaction.EXPENSE else Transaction.INCOME))

        val current = MoneyApp.instance.period.current
        FilteredTransactionsActivity.actionStart(this, UiConstants.FILTERED_TRANSACTIONS_REQUEST_CODE, filters, null, current.first, current.second)
    }

    private fun showTagActivity(tagName: String) {
        TagActivity.actionStart(this, UiConstants.TAG_REQUEST_CODE, tagName)
    }

    private fun loadData() {
        showProgress()
        val s = dataManager.tagService
                .getTagAmounts(if (typeSpinner!!.selectedItemPosition == 0) Transaction.EXPENSE else Transaction.INCOME)
                .subscribe(
                        { r ->
                            hideProgress()
                            tagsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun tagsLoaded(tags: List<TagInfo>) {
        val adapter = listView!!.adapter as ListAdapter

        var withoutTag: TagInfo? = null
        val active = ArrayList<TagInfo>()
        val inactive = ArrayList<TagInfo>()

        for (tag in tags) {
            if (tag.tagId == TagService.WITHOUT_TAGS_ID) {
                withoutTag = tag
            } else if (tag.amount > 0) {
                active.add(tag)
            } else {
                inactive.add(tag)
            }
        }
        active.sortBy { it.tagName }
        inactive.sortBy { it.tagName }

        adapter.tags.clear()

        if (!active.isEmpty() || withoutTag != null) {
            val current = MoneyApp.instance.period.current
            val currentStr = DateUtils.formatDateRange(activity, current.first.time, current.second.time, DateUtils.FORMAT_SHOW_DATE)
            val title = getString(R.string.active_items, currentStr)
            adapter.tags.add(ListItem(HEADER_TYPE, title))
        }
        for (tag in active) {
            adapter.tags.add(ListItem(TAG_TYPE, tag))
        }
        if (withoutTag != null) adapter.tags.add(ListItem(WITHOUT_TAG_TYPE, withoutTag))

        if (!inactive.isEmpty()) {
            val title = getString(R.string.inactive)
            adapter.tags.add(ListItem(HEADER_TYPE, title))
        }
        for (tag in inactive) {
            adapter.tags.add(ListItem(TAG_TYPE, tag))
        }
        adapter.notifyDataSetChanged()
    }

    override fun dataChanged() {
        loadData()
    }

    private inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindTag(listItem: ListItem) {
            val tag = listItem.obj as TagInfo?

            val nameView = itemView.findViewById(R.id.name) as TextView
            nameView.text = tag!!.tagName

            val balanceView = itemView.findViewById(R.id.balance) as TextView
            if (tag.amount > 0) {
                val amountStr = UiUtils.formatCurrency(tag.amount, currencyCode)
                balanceView.text = amountStr
            } else {
                balanceView.text = "-"
            }

            itemView.findViewById(R.id.list_item).setOnClickListener { if (tag.tagName != null) showTagActivity(tag.tagName) }
        }

        fun bindWithoutTag(listItem: ListItem) {
            val tag = listItem.obj as TagInfo?

            val nameView = itemView.findViewById(R.id.name) as TextView
            nameView.text = getString(R.string.without_tags)

            val balanceView = itemView.findViewById(R.id.balance) as TextView
            if (tag!!.amount > 0) {
                val amountStr = UiUtils.formatCurrency(tag.amount, currencyCode)
                balanceView.text = amountStr
            } else {
                balanceView.text = "-"
            }

            itemView.findViewById(R.id.list_item).setOnClickListener { showWithoutTagTransactions() }
        }

        fun bindHeader(listItem: ListItem) {
            val name = listItem.obj as String?
            (itemView.findViewById(R.id.name) as TextView).text = name
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<TagViewHolder>() {

        val tags = ArrayList<ListItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
            var layoutId = 0
            when (viewType) {
                TAG_TYPE -> layoutId = R.layout.fragment_tags_item
                WITHOUT_TAG_TYPE -> layoutId = R.layout.fragment_tags_without_tag
                HEADER_TYPE -> layoutId = R.layout.fragment_tags_header
            }
            val v = LayoutInflater.from(activity).inflate(layoutId, parent, false)
            return TagViewHolder(v)
        }

        override fun getItemViewType(position: Int): Int {
            return tags[position].type
        }

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            val listItem = tags[position]
            if (listItem.type == WITHOUT_TAG_TYPE) {
                holder.bindWithoutTag(listItem)
            } else if (listItem.type == HEADER_TYPE) {
                holder.bindHeader(listItem)
            } else {
                holder.bindTag(listItem)
            }
        }

        override fun getItemCount(): Int {
            return tags.size
        }
    }

    companion object {

        private val TAG_TYPE = 0
        private val WITHOUT_TAG_TYPE = 1
        private val HEADER_TYPE = 2

        fun build(): TagsFragment {
            return TagsFragment()
        }
    }
}
