package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.currency.CurrencyManager
import com.cactusteam.money.data.currency.MCurrency
import com.cactusteam.money.ui.ListItem
import java.util.*

/**
 * @author vpotapenko
 */
class ChooseCurrencyFragment : BaseDialogFragment() {

    var onCurrencySelectedListener: ((c: MCurrency) -> Unit)? = null

    private var title: CharSequence? = null
    private var currencyManager: CurrencyManager? = null
    private var listView: RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_currency, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(title)

        listView = view.findViewById(R.id.list) as RecyclerView
        currencyManager = MoneyApp.instance.currencyManager

        val adapter = ListAdapter()
        val currencies = currencyManager!!.loadShort()
        for (currency in currencies) {
            adapter.currencies.add(ListItem(CURRENCY_ITEM, currency))
        }
        adapter.currencies.add(ListItem(SHOW_ALL_BUTTON, null))

        listView!!.adapter = adapter
        listView!!.layoutManager = LinearLayoutManager(activity)
    }

    private fun handleCurrencySelected(currency: MCurrency) {
        if (onCurrencySelectedListener != null)
            onCurrencySelectedListener!!(currency)
        dismiss()
    }

    private fun showAllCurrencies() {
        val adapter = listView!!.adapter as ListAdapter
        adapter.currencies.clear()

        val currencies = currencyManager!!.loadAll()
        for (currency in currencies) {
            adapter.currencies.add(ListItem(CURRENCY_ITEM, currency))
        }
        adapter.notifyDataSetChanged()
    }

    interface OnCurrencySelectedListener {

        fun onCurrencySelected(currency: MCurrency)
    }

    private inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal fun bind(currency: MCurrency) {
            val nameView = itemView.findViewById(R.id.name) as TextView
            nameView.text = currency.displayString
            nameView.setOnClickListener { handleCurrencySelected(currency) }
        }

        internal fun bindShowAllButton() {
            itemView.findViewById(R.id.show_all_btn).setOnClickListener { showAllCurrencies() }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {

        val currencies = ArrayList<ListItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
            var layoutId = 0
            when (viewType) {
                CURRENCY_ITEM -> layoutId = R.layout.fragment_choose_currency_item
                SHOW_ALL_BUTTON -> layoutId = R.layout.fragment_choose_currency_show_all_item
            }
            val v = LayoutInflater.from(activity).inflate(layoutId, parent, false)

            return ListViewHolder(v)
        }

        override fun getItemViewType(position: Int): Int {
            return currencies[position].type
        }

        override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
            val type = getItemViewType(position)
            when (type) {
                CURRENCY_ITEM -> holder.bind(currencies[position].obj as MCurrency)
                SHOW_ALL_BUTTON -> holder.bindShowAllButton()
            }
        }

        override fun getItemCount(): Int {
            return currencies.size
        }
    }

    companion object {

        private val CURRENCY_ITEM = 0
        private val SHOW_ALL_BUTTON = 1

        fun build(title: CharSequence): ChooseCurrencyFragment {
            val fragment = ChooseCurrencyFragment()
            fragment.title = title

            return fragment
        }
    }

}
