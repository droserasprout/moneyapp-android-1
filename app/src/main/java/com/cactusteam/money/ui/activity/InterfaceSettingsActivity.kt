package com.cactusteam.money.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp

/**
 * @author vpotapenko
 */
class InterfaceSettingsActivity : BaseActivity("InterfaceSettingsActivity") {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interface_settings)

        initializeToolbar()

        val appPreferences = MoneyApp.instance.appPreferences

        val balanceTypesSpinner = findViewById(R.id.balance_types) as Spinner
        var adapter: ArrayAdapter<*> = ArrayAdapter(this,
                R.layout.activity_interface_settings_balance_type,
                android.R.id.text1,
                arrayOf(getString(R.string.current_period_balance), getString(R.string.current_month_balance), getString(R.string.last_30_days_balance)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        balanceTypesSpinner.adapter = adapter
        balanceTypesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                appPreferences.mainBalanceType = position
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }
        balanceTypesSpinner.setSelection(appPreferences.mainBalanceType)

        val transactionDateFormatSpinner = findViewById(R.id.transactions_date_format_mode) as Spinner
        adapter = ArrayAdapter(this,
                R.layout.activity_interface_settings_transaction_format,
                android.R.id.text1,
                arrayOf(getString(R.string.relative_format), getString(R.string.exact_format), getString(R.string.detailed_format), getString(R.string.exact2_format), getString(R.string.detailed2_format)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        transactionDateFormatSpinner.adapter = adapter
        transactionDateFormatSpinner.setSelection(appPreferences.transactionFormatDateMode)
        transactionDateFormatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                appPreferences.transactionFormatDateMode = position
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        val openAmountSwitch = findViewById(R.id.open_amount_transaction) as Switch
        openAmountSwitch.isChecked = appPreferences.isOpenAmountTransaction
        openAmountSwitch.setOnCheckedChangeListener { compoundButton, b -> appPreferences.isOpenAmountTransaction = b }

        findViewById(R.id.units_settings_btn).setOnClickListener { showHomeSettings() }
    }

    private fun showHomeSettings() {
        HomeSettingsActivity.actionStart(this)
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, InterfaceSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
