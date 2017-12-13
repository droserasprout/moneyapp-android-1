package com.cactusteam.money.ui.activity

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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils

class SelectAccountActivity : BaseDataActivity("SelectAccountActivity") {

    private var listView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_account)

        initializeToolbar()
        initializeViewProgress()

        listView = findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(this)
        listView!!.adapter = ListAdapter()

        loadData()
    }

    private fun loadData() {
        showProgress()
        val s = dataManager.accountService
                .getAccounts(false, true)
                .subscribe(
                        { r ->
                            hideProgress()
                            accountsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun accountsLoaded(accounts: List<Account>) {
        val adapter = listView!!.adapter as ListAdapter
        adapter.items.clear()
        adapter.items.addAll(accounts)
        adapter.notifyDataSetChanged()
    }

    private fun accountSelected(account: Account) {
        val data = Intent()
        data.putExtra(UiConstants.EXTRA_ID, account.id)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    inner class AccountItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @Suppress("DEPRECATION")
        fun bindItem(account: Account) {
            val nameView = itemView.findViewById(R.id.name) as TextView
            nameView.text = account.name

            val balanceView = itemView.findViewById(R.id.balance) as TextView
            val balance = account.balance
            if (balance == null) {
                balanceView.visibility = View.GONE
            } else {
                balanceView.visibility = View.VISIBLE
                balanceView.text = UiUtils.formatCurrency(balance, account.currencyCode)

                val color = if (balance < 0) resources.getColor(R.color.toolbar_expense_color) else resources.getColor(R.color.toolbar_income_color)
                balanceView.setTextColor(color)
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

            itemView.findViewById(R.id.list_item).setOnClickListener {
                accountSelected(account)
            }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<AccountItemHolder>() {

        val items = mutableListOf<Account>()

        override fun onBindViewHolder(holder: AccountItemHolder?, position: Int) {
            holder?.bindItem(items[position])
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AccountItemHolder {
            val v = LayoutInflater.from(parent!!.context).inflate(R.layout.activity_select_account_item, parent, false)
            return AccountItemHolder(v)
        }

    }

    companion object {

        fun actionStart(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, SelectAccountActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}
