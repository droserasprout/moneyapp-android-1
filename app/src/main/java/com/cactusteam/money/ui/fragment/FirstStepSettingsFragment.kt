package com.cactusteam.money.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.currency.MCurrency
import com.cactusteam.money.data.period.Period
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.activity.FirstStartActivity

class FirstStepSettingsFragment : BaseFragment() {

    private var currencyView: TextView? = null
    private var periodView: TextView? = null

    private var mainCurrency: MCurrency? = null
    private var period: Period? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_first_step, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById(R.id.main_currency_container).setOnClickListener { handleCurrencyClicked() }
        currencyView = view.findViewById(R.id.main_currency) as TextView

        mainCurrency = MoneyApp.instance.currencyManager.localeCurrency
        updateCurrencyView()

        view.findViewById(R.id.period_container).setOnClickListener { handlePeriodClicked() }

        periodView = view.findViewById(R.id.period) as TextView

        period = Period(Period.MONTH_TYPE, 1)
        updatePeriodView()

        view.findViewById(R.id.next_btn).setOnClickListener { nextClicked() }

    }

    private fun nextClicked() {
        baseActivity.showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.systemService
                .initializeModel(period!!, mainCurrency!!.currencyCode)
                .subscribe(
                        {},
                        { e ->
                            baseActivity.hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            baseActivity.hideBlockingProgress()
                            (activity as FirstStartActivity).modelSettingsSaved()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun handlePeriodClicked() {
        val fragment = ChooseModelPeriodFragment.build(getString(R.string.period), period)
        fragment.onPeriodSelectedListener = { p ->
            period = p
            updatePeriodView()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun updatePeriodView() {
        val text = UiUtils.formatPeriod(period!!, activity)
        periodView!!.text = text
    }

    private fun updateCurrencyView() {
        currencyView!!.text = mainCurrency!!.displayString
    }

    private fun handleCurrencyClicked() {
        val fragment = ChooseCurrencyFragment.build(getString(R.string.main_currency))
        fragment.onCurrencySelectedListener = { currency ->
            mainCurrency = currency
            updateCurrencyView()
        }
        fragment.show(fragmentManager, "dialog")
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity.setTitle(R.string.tracker_options_title)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {

        fun build(): FirstStepSettingsFragment {
            return FirstStepSettingsFragment()
        }
    }
}
