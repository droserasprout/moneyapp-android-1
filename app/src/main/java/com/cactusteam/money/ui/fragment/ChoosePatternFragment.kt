package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.data.dao.TransactionPattern
import com.cactusteam.money.ui.UiUtils
import java.util.*

/**
 * @author vpotapenko
 */
class ChoosePatternFragment : BaseDialogFragment() {

    private var onPatternSetListener: ((p: TransactionPattern) -> Unit)? = null
    private var listView: RecyclerView? = null

    private var helpText: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_pattern, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(R.string.choose_pattern)

        listView = view.findViewById(R.id.list) as RecyclerView
        initializeProgress(view.findViewById(R.id.progress_bar), listView!!)

        listView!!.layoutManager = LinearLayoutManager(activity)
        listView!!.adapter = ListAdapter()

        helpText = view.findViewById(R.id.help_text)

        loadPatterns()
    }

    private fun loadPatterns() {
        helpText!!.visibility = View.GONE
        showProgress()
        val s = dataManager.patternService
                .getPatterns()
                .subscribe(
                        { r ->
                            hideProgress()
                            patternsLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun patternsLoaded(patterns: List<TransactionPattern>) {
        if (patterns.isEmpty()) {
            helpText!!.visibility = View.VISIBLE
            listView!!.visibility = View.INVISIBLE
        } else {
            val adapter = listView!!.adapter as ListAdapter

            adapter.patterns.clear()
            adapter.patterns.addAll(patterns)
            adapter.notifyDataSetChanged()
        }
    }

    private fun patternSelected(pattern: TransactionPattern) {
        onPatternSetListener!!(pattern)
        dismiss()
    }

    private inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItem(pattern: TransactionPattern) {
            itemView.findViewById(R.id.list_item).setOnClickListener { patternSelected(pattern) }

            (itemView.findViewById(R.id.name) as TextView).text = pattern.name

            (itemView.findViewById(R.id.account) as TextView).text = pattern.sourceAccount.name
            when (pattern.type) {
                Transaction.EXPENSE -> {
                    (itemView.findViewById(R.id.dest) as TextView).text = pattern.category.name
                    (itemView.findViewById(R.id.type_marker) as ImageView).setImageResource(R.drawable.ic_expense_transaction)
                }
                Transaction.INCOME -> {
                    (itemView.findViewById(R.id.dest) as TextView).text = pattern.category.name
                    (itemView.findViewById(R.id.type_marker) as ImageView).setImageResource(R.drawable.ic_income_transaction)
                }
                Transaction.TRANSFER -> {
                    (itemView.findViewById(R.id.dest) as TextView).text = pattern.destAccount.name
                    (itemView.findViewById(R.id.type_marker) as ImageView).setImageResource(R.drawable.ic_transfer_transaction)
                }
            }

            val amountStr = UiUtils.formatCurrency(pattern.amount, pattern.sourceAccount.currencyCode)
            (itemView.findViewById(R.id.amount) as TextView).text = amountStr
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {

        val patterns = ArrayList<TransactionPattern>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
            val v = LayoutInflater.from(activity).inflate(R.layout.fragment_choose_pattern_item, parent, false)
            return ListViewHolder(v)
        }

        override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
            holder.bindItem(patterns[position])
        }

        override fun getItemCount(): Int {
            return patterns.size
        }
    }

    companion object {

        fun build(onPatternSetListener: ((p: TransactionPattern) -> Unit)?): ChoosePatternFragment {
            val fragment = ChoosePatternFragment()
            fragment.onPatternSetListener = onPatternSetListener
            return fragment
        }
    }
}
