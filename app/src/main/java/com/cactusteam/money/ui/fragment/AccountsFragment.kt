package com.cactusteam.money.ui.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.ui.HtmlTagHandler
import com.cactusteam.money.ui.ListItem
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.AccountActivity
import com.cactusteam.money.ui.activity.BaseActivity
import com.cactusteam.money.ui.activity.EditAccountActivity
import com.cactusteam.money.ui.activity.SortingAccountsActivity
import java.util.*

/**
 * @author vpotapenko
 */
class AccountsFragment : BaseMainFragment() {

    private val tagHandler = HtmlTagHandler()

    private var listView: RecyclerView? = null

    private var includeDeleted = false
    private var currentAccounts: List<Account>? = null

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_accounts, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.show_deleted).isVisible = !includeDeleted
        menu.findItem(R.id.hide_deleted).isVisible = includeDeleted

        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.show_deleted) {
            showDeleted()
            return true
        } else if (itemId == R.id.hide_deleted) {
            hideDeleted()
            return true
        } else if (itemId == R.id.show_sorting) {
            showSortingActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSortingActivity() {
        SortingAccountsActivity.actionStart(this, UiConstants.SORTING_REQUEST_CODE)
    }

    private fun hideDeleted() {
        Toast.makeText(activity, R.string.deleted_accounts_was_hidden, Toast.LENGTH_SHORT).show()
        includeDeleted = false
        loadData()

        val supportActionBar = (activity as BaseActivity).supportActionBar
        supportActionBar?.invalidateOptionsMenu()
    }

    private fun showDeleted() {
        Toast.makeText(activity, R.string.deleted_accounts_was_shown, Toast.LENGTH_SHORT).show()
        includeDeleted = true
        loadData()

        val supportActionBar = (activity as BaseActivity).supportActionBar
        supportActionBar?.invalidateOptionsMenu()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        listView = view.findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        initializeProgress(view.findViewById(R.id.progress_bar), listView!!)

        loadData()
    }

    private fun loadData() {
        showProgress()
        val s = dataManager.accountService
                .getAccounts(includeDeleted, true)
                .subscribe(
                        { r ->
                            accountsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        { hideProgress() }
                )
        compositeSubscription.add(s)
    }

    private fun accountsLoaded(accounts: List<Account>) {
        currentAccounts = accounts
        val active = ArrayList<Account>()
        val inactive = ArrayList<Account>()

        for (account in currentAccounts!!) {
            val balance = account.balance
            if (balance != null && balance != 0.0) {
                active.add(account)
            } else {
                inactive.add(account)
            }
        }

        val adapter = listView!!.adapter as ListAdapter
        adapter.items.clear()

        if (!active.isEmpty()) {
            adapter.items.add(ListItem(GROUP, getString(R.string.active)))
            for (account in active) {
                adapter.items.add(ListItem(REGULAR, account))
            }
        }

        if (!inactive.isEmpty()) {
            adapter.items.add(ListItem(GROUP, getString(R.string.inactive)))
            for (account in inactive) {
                adapter.items.add(ListItem(REGULAR, account))
            }
        }
        adapter.items.add(ListItem(NEW_ACCOUNT, null))
        adapter.notifyDataSetChanged()
    }

    private fun showAccountActivity(account: Account) {
        showAccountActivity(account.id!!, account.name, account.color)
    }

    private fun showAccountActivity(id: Long, name: String, color: String) {
        AccountActivity.actionStart(this, UiConstants.ACCOUNT_REQUEST_CODE, id, name, color)
    }

    private fun showNewAccountActivity() {
        EditAccountActivity.actionStart(this, UiConstants.NEW_ACCOUNT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.ACCOUNT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.EDIT_ACCOUNT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.SORTING_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()
            }
        } else if (requestCode == UiConstants.NEW_ACCOUNT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadData()

                val id = data!!.getLongExtra(UiConstants.EXTRA_ID, 0)
                val name = data.getStringExtra(UiConstants.EXTRA_NAME)
                val color = data.getStringExtra(UiConstants.EXTRA_COLOR)
                showAccountActivity(id, name, color)
            }
        }
    }

    override fun dataChanged() {
        loadData()
    }

    private inner class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @Suppress("DEPRECATION")
        fun bind(account: Account) {
            val nameView = itemView.findViewById(R.id.name) as TextView
            if (account.deleted) {
                val s = String.format(UiConstants.DELETED_PATTERN, account.name)
                nameView.text = Html.fromHtml(s, null, tagHandler)
            } else {
                nameView.text = account.name
            }

            val balanceView = itemView.findViewById(R.id.balance) as TextView
            val balanceProgress = itemView.findViewById(R.id.balance_progress)
            val balance = account.balance
            if (balance == null) {
                balanceProgress.visibility = View.VISIBLE
                balanceView.visibility = View.GONE
            } else {
                balanceProgress.visibility = View.GONE
                balanceView.visibility = View.VISIBLE
                balanceView.text = UiUtils.formatCurrency(balance, account.currencyCode)
            }

            var color = Color.DKGRAY
            try {
                color = Color.parseColor(account.color)
            } catch (ignore: Exception) {
            }

            val drawable: Drawable?
            when (account.type) {
                Account.SAVINGS_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_savings))
                Account.BANK_ACCOUNT_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_bank_account))
                Account.CARD_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_card))
                Account.CASH_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
                else -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
            }

            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            (itemView.findViewById(R.id.account_icon) as ImageView).setImageDrawable(drawable)

            itemView.findViewById(R.id.account).setOnClickListener { showAccountActivity(account) }
        }

        fun bindNewAccount() {
            itemView.findViewById(R.id.create_account_btn).setOnClickListener { showNewAccountActivity() }
        }

        fun bindGroup(name: String) {
            (itemView.findViewById(R.id.name) as TextView).text = name
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<AccountViewHolder>() {

        val items = ArrayList<ListItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
            var layoutId = 0
            when (viewType) {
                REGULAR -> layoutId = R.layout.fragment_accounts_item
                NEW_ACCOUNT -> layoutId = R.layout.fragment_accounts_new
                GROUP -> layoutId = R.layout.fragment_accounts_subhead
            }
            val v = LayoutInflater.from(activity).inflate(layoutId, parent, false)
            return AccountViewHolder(v)
        }

        override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
            val type = getItemViewType(position)
            if (type == REGULAR) {
                holder.bind(items[position].obj as Account)
            } else if (type == NEW_ACCOUNT) {
                holder.bindNewAccount()
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

        private val REGULAR = 0
        private val NEW_ACCOUNT = 1
        private val GROUP = 2

        fun build(): AccountsFragment {
            return AccountsFragment()
        }
    }
}

