package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataUtils

/**
 * @author vpotapenko
 */
class ChooseRateFragment : BaseDialogFragment() {

    var onChooseRateListener: ((sourceCode: String, destCode: String, rate: Double) -> Unit)? = null

    private var sourceCode: String? = null
    private var sourceName: String? = null

    private var destCode: String? = null
    private var destName: String? = null

    private var rate: Double = 0.toDouble()

    private var sourceView: TextView? = null
    private var destView: TextView? = null
    private var rateView: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_rate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(R.string.rate_label)

        val currencyManager = MoneyApp.instance.currencyManager
        sourceName = currencyManager.getCurrencyByCode(sourceCode).displayName
        destName = currencyManager.getCurrencyByCode(destCode).displayName

        sourceView = view.findViewById(R.id.source_currency) as TextView
        destView = view.findViewById(R.id.dest_currency) as TextView
        rateView = view.findViewById(R.id.rate) as TextView

        view.findViewById(R.id.ok_btn).setOnClickListener { okClicked() }
        view.findViewById(R.id.cancel_btn).setOnClickListener { dismiss() }

        view.findViewById(R.id.swap_btn).setOnClickListener { swapClicked() }

        updateViews()
    }

    private fun updateViews() {
        sourceView!!.text = sourceName
        destView!!.text = destName
        rateView!!.text = rate.toString()
    }

    private fun swapClicked() {
        var tmp: String? = sourceCode
        sourceCode = destCode
        destCode = tmp

        tmp = sourceName
        sourceName = destName
        destName = tmp

        updateRateValue()
        rate = DataUtils.round(1 / rate, 2)

        updateViews()
    }

    private fun updateRateValue() {
        try {
            rate = java.lang.Double.parseDouble(rateView!!.text.toString())
        } catch (ignore: Exception) {
        }

    }

    private fun okClicked() {
        updateRateValue()

        if (onChooseRateListener != null) onChooseRateListener!!(sourceCode!!, destCode!!, rate)

        dismiss()
    }

    companion object {

        fun build(sourceCode: String, destCode: String, rate: Double): ChooseRateFragment {
            val fragment = ChooseRateFragment()

            fragment.sourceCode = sourceCode
            fragment.destCode = destCode
            fragment.rate = rate

            return fragment
        }
    }
}
