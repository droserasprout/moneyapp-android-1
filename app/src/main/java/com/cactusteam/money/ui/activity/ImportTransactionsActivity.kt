package com.cactusteam.money.ui.activity

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.cactusteam.money.R
import com.cactusteam.money.data.io.ImportResult
import com.cactusteam.money.data.io.ImportSchema
import com.cactusteam.money.data.io.ImporterFactory
import com.cactusteam.money.ui.fragment.ImportTransactionsFirstFragment
import com.cactusteam.money.ui.fragment.ImportTransactionsSecondFragment
import com.cactusteam.money.ui.fragment.ImportTransactionsThirdFragment
import java.io.File

/**
 * @author vpotapenko
 */
class ImportTransactionsActivity : BaseActivity("ImportTransactionsActivity") {

    var importResult: ImportResult? = null
    var schema: ImportSchema? = null

    var sourceFile: File? = null
    var format = ImporterFactory.MONEY_APP_CSV

    private var stepIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_transactions)

        initializeToolbar()

        if (savedInstanceState == null) {
            showFirstStepFragment()
        }
    }

    private fun showFirstStepFragment() {
        stepIndex = 0
        showFragment(R.id.content_frame, ImportTransactionsFirstFragment.build(), "first")
    }

    fun nextStep() {
        when (stepIndex) {
            0 -> showSecondStepFragment()
            1 -> showThirdStepFragment()
        }
    }

    fun previousStep() {
        when (stepIndex) {
            1 -> showFirstStepFragment()
            2 -> showSecondStepFragment()
        }
    }

    private fun showThirdStepFragment() {
        stepIndex = 2
        showFragment(R.id.content_frame, ImportTransactionsThirdFragment.build(), "third")
    }

    private fun showSecondStepFragment() {
        stepIndex = 1
        showFragment(R.id.content_frame, ImportTransactionsSecondFragment.build(), "second")
    }

    override fun onBackPressed() {
        if (stepIndex == 1) {
            previousStep()
        } else {
            super.onBackPressed()
        }
    }

    companion object {

        fun actionStart(fragment: Fragment, requestCode: Int) {
            val intent = Intent(fragment.activity, ImportTransactionsActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }

        fun actionStart(context: Context) {
            val intent = Intent(context, ImportTransactionsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
