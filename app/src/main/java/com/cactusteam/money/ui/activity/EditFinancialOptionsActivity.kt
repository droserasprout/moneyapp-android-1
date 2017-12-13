package com.cactusteam.money.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.data.currency.MCurrency
import com.cactusteam.money.data.dao.CurrencyRate
import com.cactusteam.money.data.period.Period
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.ChooseCurrencyFragment
import com.cactusteam.money.ui.fragment.ChooseModelPeriodFragment
import com.cactusteam.money.ui.fragment.ChooseRateFragment

/**
 * @author vpotapenko
 */
class EditFinancialOptionsActivity : BaseActivity("EditFinancialOptionsActivity") {

    private var currencyView: TextView? = null
    private var periodView: TextView? = null
    private var rateView: TextView? = null

    private var rateContainer: LinearLayout? = null
    private var rateProgress: View? = null

    private var mainCurrencyCode: String? = null

    private var currentCurrency: MCurrency? = null
    private var period: Period? = null

    private var rate: CurrencyRate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_financial_options)

        initializeToolbar()

        mainCurrencyCode = application.appPreferences.mainCurrencyCode
        currentCurrency = application.currencyManager.getCurrencyByCode(mainCurrencyCode)

        period = application.period

        rateProgress = findViewById(R.id.rate_progress_bar)
        rateView = findViewById(R.id.rate) as TextView
        rateContainer = findViewById(R.id.rate_container) as LinearLayout
        rateContainer!!.setOnClickListener { rateClicked() }

        findViewById(R.id.main_currency_container).setOnClickListener { currencyClicked() }
        currencyView = findViewById(R.id.main_currency) as TextView
        updateCurrencyView()

        findViewById(R.id.period_container).setOnClickListener { periodClicked() }

        periodView = findViewById(R.id.period) as TextView
        updatePeriodView()

        findViewById(R.id.save_btn).setOnClickListener { saveClicked() }
    }

    private fun saveClicked() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.systemService
                .changeFinancialOptions(currentCurrency!!.currencyCode, period!!, rate)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            setResult(RESULT_OK)
                            finish()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun rateClicked() {
        val fragment = ChooseRateFragment.build(rate!!.sourceCurrencyCode, rate!!.destCurrencyCode, rate!!.rate)
        fragment.onChooseRateListener = { sourceCode, destCode, r ->
            rate = CurrencyRate()
            rate!!.sourceCurrencyCode = sourceCode
            rate!!.destCurrencyCode = destCode
            rate!!.rate = r

            updateRateView()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun updateCurrencyView() {
        currencyView!!.text = currentCurrency!!.displayString

        if (currentCurrency!!.currencyCode == mainCurrencyCode) {
            rateContainer!!.visibility = View.GONE
        } else {
            rateContainer!!.visibility = View.VISIBLE
            loadRate()
        }
    }

    private fun loadRate() {
        fillRateAsStub()

        rateView!!.visibility = View.GONE
        rateProgress!!.visibility = View.VISIBLE

        val s = dataManager.currencyService
                .getRate(mainCurrencyCode!!, currentCurrency!!.currencyCode)
                .subscribe(
                        { r ->
                            rateProgress!!.visibility = View.GONE
                            rateView!!.visibility = View.VISIBLE

                            if (r == null) {
                                fillRateAsStub()
                            } else {
                                rate = r
                            }

                            updateRateView()
                        },
                        { e ->
                            rateProgress!!.visibility = View.GONE
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun updateRateView() {
        val source = UiUtils.formatCurrency(1.0, rate!!.sourceCurrencyCode)
        val dest = UiUtils.formatCurrency(rate!!.rate, rate!!.destCurrencyCode)

        rateView!!.text = getString(R.string.rate_pattern, source, dest)
    }

    private fun fillRateAsStub() {
        rate = CurrencyRate()
        rate!!.rate = 1.0
        rate!!.sourceCurrencyCode = mainCurrencyCode
        rate!!.destCurrencyCode = currentCurrency!!.currencyCode
    }

    private fun currencyClicked() {
        val fragment = ChooseCurrencyFragment.build(getString(R.string.main_currency))
        fragment.onCurrencySelectedListener = { currency ->
            currentCurrency = currency
            updateCurrencyView()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun periodClicked() {
        val fragment = ChooseModelPeriodFragment.build(getString(R.string.period), period)
        fragment.onPeriodSelectedListener = { p ->
            period = p
            updatePeriodView()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun updatePeriodView() {
        val text = UiUtils.formatPeriod(period!!, this)
        periodView!!.text = text
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, EditFinancialOptionsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
