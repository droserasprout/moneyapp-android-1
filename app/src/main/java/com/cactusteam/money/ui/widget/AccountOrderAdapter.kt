package com.cactusteam.money.ui.widget

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Account
import com.woxthebox.draglistview.DragItemAdapter

/**
 * @author vpotapenko
 */
class AccountOrderAdapter(private val context: Context) : DragItemAdapter<Account, AccountOrderAdapter.ViewHolder>() {

    private val grabHandleId = R.id.drag_area

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_sorting_accounts_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.bind(itemList[position])
    }

    override fun getItemId(position: Int): Long {
        return mItemList[position].id!!
    }

    inner class ViewHolder(itemView: View) : DragItemAdapter.ViewHolder(itemView, grabHandleId, false) {

        internal fun bind(account: Account) {
            updateIcon(account)
            (itemView.findViewById(R.id.name) as TextView).text = account.name
        }

        private fun updateIcon(account: Account) {
            var color = Color.DKGRAY
            try {
                color = Color.parseColor(account.color)
            } catch (ignore: Exception) {
            }

            val drawable: Drawable
            when (account.type) {
                Account.SAVINGS_TYPE -> drawable = BitmapDrawable(context.resources, BitmapFactory.decodeResource(context.resources, R.drawable.ic_savings))
                Account.BANK_ACCOUNT_TYPE -> drawable = BitmapDrawable(context.resources, BitmapFactory.decodeResource(context.resources, R.drawable.ic_bank_account))
                Account.CARD_TYPE -> drawable = BitmapDrawable(context.resources, BitmapFactory.decodeResource(context.resources, R.drawable.ic_card))
                else -> drawable = BitmapDrawable(context.resources, BitmapFactory.decodeResource(context.resources, R.drawable.ic_wallet))
            }

            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            (itemView.findViewById(R.id.icon) as ImageView).setImageDrawable(drawable)
        }
    }
}
