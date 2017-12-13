package com.cactusteam.money.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cactusteam.money.R
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.activity.*

/**
 * @author vpotapenko
 */
class SettingsFragment : BaseMainFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById(R.id.financial_options).setOnClickListener { financialOptionsClicked() }
        view.findViewById(R.id.data_management).setOnClickListener { dataManagementClicked() }
        view.findViewById(R.id.password).setOnClickListener { passwordClicked() }
        view.findViewById(R.id.ui_options).setOnClickListener { uiOptionsClicked() }
        view.findViewById(R.id.sorting).setOnClickListener { sortingClicked() }
        view.findViewById(R.id.currencies).setOnClickListener { currenciesClicked() }
        view.findViewById(R.id.reset).setOnClickListener { resetClicked() }
        view.findViewById(R.id.donate_btn).setOnClickListener { donationClicked() }
    }

    private fun donationClicked() {
        mainActivity.showSection(MainSection.DONATION)
    }

    private fun resetClicked() {
        AlertDialog.Builder(activity)
                .setTitle(R.string.continue_question)
                .setMessage(R.string.all_your_settings_will_be_reset_to_default)
                .setPositiveButton(R.string.ok) { dialog, which -> resetData() }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun resetData() {
        baseActivity.showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.systemService
                .resetInterfaceSettings()
                .subscribe(
                        {},
                        { e ->
                            baseActivity.hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            baseActivity.hideBlockingProgress()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun currenciesClicked() {
        CurrenciesActivity.actionStart(activity)
    }

    private fun sortingClicked() {
        SortingSettingsActivity.actionStart(activity)
    }

    private fun uiOptionsClicked() {
        InterfaceSettingsActivity.actionStart(activity)
    }

    private fun passwordClicked() {
        PasswordManagementActivity.actionStart(activity)
    }

    private fun dataManagementClicked() {
        DataManagementActivity.actionStart(activity)
    }

    private fun financialOptionsClicked() {
        EditFinancialOptionsActivity.actionStart(activity)
    }

    override fun dataChanged() {
        // do nothing
    }

    companion object {

        fun build(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
