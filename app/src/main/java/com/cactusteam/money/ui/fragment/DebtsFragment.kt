package com.cactusteam.money.ui.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.util.ArrayMap
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Debt
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.ui.ListItem
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiObjectRef
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.DebtActivity
import com.cactusteam.money.ui.activity.EditDebtActivity
import java.util.*

/**
 * @author vpotapenko
 */
class DebtsFragment : BaseMainFragment() {

    private val icons = ArrayMap<Long, UiObjectRef>()

    private var listView: RecyclerView? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.DEBT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadDebts()
            }
        } else if (requestCode == UiConstants.EDIT_DEBT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadDebts()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_debts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeProgress(view.findViewById(R.id.progress_bar), view.findViewById(R.id.content))

        view.findViewById(R.id.borrow_btn).setOnClickListener { showNewDebtActivity(Transaction.INCOME) }

        view.findViewById(R.id.lend_btn).setOnClickListener { showNewDebtActivity(Transaction.EXPENSE) }

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        loadDebts()
    }

    override fun onDestroyView() {
        for ((key, value) in icons) {
            if (value.ref != null) value.getRefAs(Bitmap::class.java).recycle()
        }
        super.onDestroyView()
    }

    private fun loadDebts() {
        showProgress()
        val s = dataManager.debtService.getDebts()
                .subscribe(
                        { r ->
                            debtsLoaded(r)
                            hideProgress()
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun debtsLoaded(debts: List<Debt>) {
        val finished = ArrayList<Debt>()
        val lending = ArrayList<Debt>()
        val borrowing = ArrayList<Debt>()

        val now = Date()
        for (debt in debts) {
            if (debt.finished) {
                finished.add(debt)
                debt.isUrgent = false
            } else {
                debt.isUrgent = debt.amount != 0.0 && now.after(debt.till)
                if (debt.amount > 0) {
                    lending.add(debt)
                } else {
                    borrowing.add(debt)
                }
            }
        }

        val adapter = listView!!.adapter as ListAdapter
        adapter.items.clear()
        if (!lending.isEmpty()) {
            adapter.items.add(ListItem(GROUP, getString(R.string.lending_amount)))

            for (debt in lending) {
                adapter.items.add(ListItem(REGULAR, debt))
            }
        }
        if (!borrowing.isEmpty()) {
            adapter.items.add(ListItem(GROUP, getString(R.string.borrowing_amount)))

            for (debt in borrowing) {
                adapter.items.add(ListItem(REGULAR, debt))
            }
        }
        if (!finished.isEmpty()) {
            adapter.items.add(ListItem(GROUP, getString(R.string.finished_debts)))

            for (debt in finished) {
                adapter.items.add(ListItem(REGULAR, debt))
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showNewDebtActivity(type: Int) {
        EditDebtActivity.actionStart(this, UiConstants.EDIT_DEBT_REQUEST_CODE, type)
    }

    private fun showDebtActivity(debt: Debt) {
        DebtActivity.actionStart(this, UiConstants.DEBT_REQUEST_CODE, debt.id!!, debt.name)
    }

    private fun requestContactIcon(contactId: Long) {
        val icon = UiObjectRef()
        icons.put(contactId, icon)

        val s = dataManager.systemService.getContactImage(contactId)
                .subscribe(
                        { r ->
                            icon.ref = r
                            listView!!.adapter.notifyDataSetChanged()
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    override fun dataChanged() {
        loadDebts()
    }

    private inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @Suppress("DEPRECATION")
        fun bindItem(debt: Debt) {
            itemView.findViewById(R.id.list_item).setOnClickListener { showDebtActivity(debt) }

            val nameView = itemView.findViewById(R.id.name) as TextView
            nameView.text = debt.name
            if (debt.isUrgent) {
                nameView.setTextColor(resources.getColor(R.color.toolbar_expense_color))
            } else {
                nameView.setTextColor(resources.getColor(android.R.color.black))
            }

            (itemView.findViewById(R.id.till_date) as TextView).text = DateUtils.getRelativeTimeSpanString(debt.till.time)

            val amountStr = UiUtils.formatCurrency(debt.amount, debt.currencyCode)
            (itemView.findViewById(R.id.amount) as TextView).text = amountStr

            val iconView = itemView.findViewById(R.id.icon) as ImageView
            iconView.setImageResource(R.drawable.ic_contact)

            if (debt.contactId != null) {
                val contactId = debt.contactId!!
                val contactIcon = icons[contactId]
                if (contactIcon == null) {
                    requestContactIcon(contactId)
                } else {
                    if (contactIcon.ref != null) {
                        val src = contactIcon.getRefAs(Bitmap::class.java)
                        val drawable = RoundedBitmapDrawableFactory.create(resources, src)
                        drawable.cornerRadius = Math.min(drawable.minimumWidth, drawable.minimumHeight) / 2.0f

                        iconView.setImageDrawable(drawable)
                    }
                }
            }
        }

        fun bindGroup(groupName: String) {
            (itemView.findViewById(R.id.name) as TextView).text = groupName
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {

        val items = ArrayList<ListItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
            var layoutId = 0
            when (viewType) {
                REGULAR -> layoutId = R.layout.fragment_debt_item
                GROUP -> layoutId = R.layout.fragment_debt_subhead
            }
            val v = LayoutInflater.from(activity).inflate(layoutId, parent, false)
            return ListViewHolder(v)
        }

        override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
            val type = getItemViewType(position)
            if (type == REGULAR) {
                holder.bindItem(items[position].obj as Debt)
            } else if (type == GROUP) {
                holder.bindGroup(items[position].obj as String)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].type
        }

        override fun getItemCount(): Int {
            return items.size
        }
    }

    companion object {

        val REGULAR = 0
        val GROUP = 1

        fun build(): DebtsFragment {
            return DebtsFragment()
        }
    }
}
