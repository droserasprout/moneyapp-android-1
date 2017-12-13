package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.cactusteam.money.R
import com.cactusteam.money.data.period.Period
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.WeekDay
import java.util.*

/**
 * @author vpotapenko
 */
class ChooseModelPeriodFragment : BaseDialogFragment() {

    var onPeriodSelectedListener: ((period: Period) -> Unit)? = null

    private var title: CharSequence? = null
    private var initializePeriod: Period? = null

    private var dayOfMonthSpinner: Spinner? = null
    private var dayOfWeekSpinner: Spinner? = null

    private var typeSpinner: Spinner? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_model_period, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.setTitle(title)

        var adapter: ArrayAdapter<*> = ArrayAdapter(activity,
                R.layout.fragment_choose_model_period_type_label,
                android.R.id.text1,
                arrayOf(getString(R.string.period_month_label), getString(R.string.period_week_label)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        typeSpinner = view.findViewById(R.id.period_type) as Spinner
        typeSpinner!!.adapter = adapter
        typeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                handleTypeSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        val values = ArrayList<String>()
        for (i in 1..28) {
            values.add(i.toString())
        }
        adapter = ArrayAdapter(activity,
                R.layout.fragment_choose_model_period_start_day,
                android.R.id.text1, values)
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        dayOfMonthSpinner = view.findViewById(R.id.day_of_month) as Spinner
        dayOfMonthSpinner!!.adapter = adapter

        val days = UiUtils.weekDays
        adapter = ArrayAdapter(activity,
                R.layout.fragment_choose_model_period_start_day,
                android.R.id.text1, days)
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        dayOfWeekSpinner = view.findViewById(R.id.day_of_week) as Spinner
        dayOfWeekSpinner!!.adapter = adapter

        view.findViewById(R.id.ok_btn).setOnClickListener { okClicked() }
        view.findViewById(R.id.cancel_btn).setOnClickListener { dismiss() }

        if (initializePeriod!!.type == Period.MONTH_TYPE) {
            typeSpinner!!.setSelection(0)
            dayOfMonthSpinner!!.setSelection(initializePeriod!!.startFrom - 1)
        } else if (initializePeriod!!.type == Period.WEEK_TYPE) {
            typeSpinner!!.setSelection(1)

            for (i in days.indices) {
                val day = days[i]
                if (day.index == initializePeriod!!.startFrom) {
                    dayOfWeekSpinner!!.setSelection(i)
                    break
                }
            }
        }
    }

    private fun okClicked() {
        val period: Period

        val position = typeSpinner!!.selectedItemPosition
        if (position == 1) {
            val type = Period.WEEK_TYPE

            val weekDay = dayOfWeekSpinner!!.selectedItem as WeekDay
            period = Period(type, weekDay.index)
        } else {
            val type = Period.MONTH_TYPE

            period = Period(type, dayOfMonthSpinner!!.selectedItemPosition + 1)
        }

        onPeriodSelectedListener!!(period)
        dismiss()
    }

    private fun handleTypeSelected(position: Int) {
        when (position) {
            0 -> {
                dayOfMonthSpinner!!.visibility = View.VISIBLE
                dayOfWeekSpinner!!.visibility = View.GONE
            }
            1, 2 -> {
                dayOfMonthSpinner!!.visibility = View.GONE
                dayOfWeekSpinner!!.visibility = View.VISIBLE
            }
        }
    }

    companion object {

        fun build(title: CharSequence, initializePeriod: Period?): ChooseModelPeriodFragment {
            val fragment = ChooseModelPeriodFragment()
            fragment.title = title
            fragment.initializePeriod = initializePeriod
            return fragment
        }
    }
}
