package com.cactusteam.money.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.CurrencyRate
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.ChooseRateFragment
import java.util.*

/**
 * @author vpotapenko
 */
class CurrenciesActivity : BaseDataActivity("CurrenciesActivity") {

    private var listView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currencies)

        initializeToolbar()
        initializeViewProgress()

        listView = findViewById(R.id.list) as RecyclerView
        listView!!.layoutManager = LinearLayoutManager(this)
        listView!!.adapter = ListAdapter()

        loadData()
    }

    private fun clearRate(rate: CurrencyRate) {
        showProgress()
        val s = dataManager.currencyService
                .deleteRate(rate.id)
                .subscribe(
                        {},
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        {
                            hideProgress()
                            loadData()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun rateClicked(rate: CurrencyRate) {
        val fragment = ChooseRateFragment.build(rate.sourceCurrencyCode,
                rate.destCurrencyCode, rate.rate)
        fragment.onChooseRateListener = { sourceCode, destCode, rate -> updateRate(sourceCode, destCode, rate) }
        fragment.show(fragmentManager, "dialog")
    }

    private fun updateRate(sourceCode: String, destCode: String, rate: Double) {
        showProgress()
        val s = dataManager.currencyService
                .updateRate(sourceCode, destCode, rate)
                .subscribe(
                        { r ->
                            hideProgress()
                            loadData()
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun loadData() {
        showProgress()
        val s = dataManager.currencyService
                .getRates()
                .subscribe(
                        { r ->
                            hideProgress()
                            ratesLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun ratesLoaded(rates: List<CurrencyRate>) {
        val adapter = listView!!.adapter as ListAdapter
        adapter.rates.clear()
        adapter.rates.addAll(rates)
        adapter.notifyDataSetChanged()
    }

    private inner class RateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SetTextI18n")
        fun bindRate(rate: CurrencyRate) {
            (itemView.findViewById(R.id.rate_label) as TextView).text = rate.sourceCurrencyCode + " -> " + rate.destCurrencyCode

            val source = UiUtils.formatCurrency(1.0, rate.sourceCurrencyCode)
            val dest = UiUtils.formatCurrency(rate.rate, rate.destCurrencyCode)
            (itemView.findViewById(R.id.rate) as TextView).text = getString(R.string.rate_pattern, source, dest)

            itemView.findViewById(R.id.list_item).setOnClickListener { rateClicked(rate) }

            itemView.findViewById(R.id.clear_btn).setOnClickListener { clearRate(rate) }
        }
    }

    private inner class ListAdapter : RecyclerView.Adapter<RateViewHolder>() {

        val rates = ArrayList<CurrencyRate>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RateViewHolder {
            val v = LayoutInflater.from(this@CurrenciesActivity).inflate(R.layout.activity_currencies_item, parent, false)
            return RateViewHolder(v)
        }

        override fun onBindViewHolder(holder: RateViewHolder, position: Int) {
            holder.bindRate(rates[position])
        }

        override fun getItemCount(): Int {
            return rates.size
        }
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, CurrenciesActivity::class.java)
            context.startActivity(intent)
        }
    }
}
