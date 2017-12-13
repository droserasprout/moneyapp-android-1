package com.cactusteam.money.ui.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.ui.activity.ImportTransactionsActivity

/**
 * @author vpotapenko
 */
class ImportTransactionsThirdFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_import_transactions_third, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById(R.id.next_btn).setOnClickListener {
            importActivity.setResult(Activity.RESULT_OK)
            importActivity.finish()
        }

        val importResult = importActivity.importResult
        (view.findViewById(R.id.transactions_count) as TextView).text = importResult!!.newTransactions.toString()

        val sb = StringBuilder()
        for (logItem in importResult.log) {
            sb.append("Line ").append(logItem.line).append(": ").append(logItem.message).append("\n\n")
        }
        (view.findViewById(R.id.log) as TextView).text = sb.toString()
    }

    private val importActivity: ImportTransactionsActivity
        get() = activity as ImportTransactionsActivity

    companion object {

        fun build(): ImportTransactionsThirdFragment {
            return ImportTransactionsThirdFragment()
        }
    }

}
