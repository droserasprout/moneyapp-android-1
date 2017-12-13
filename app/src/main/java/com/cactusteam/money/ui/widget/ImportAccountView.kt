package com.cactusteam.money.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.io.ExistAccountImportStrategy
import com.cactusteam.money.data.io.ImportAccount
import com.cactusteam.money.data.io.NewAccountImportStrategy

/**
 * @author vpotapenko
 */
@SuppressLint("ViewConstructor")
class ImportAccountView(context: Context, private val importAccount: ImportAccount, allAccounts: List<Account>) : LinearLayout(context) {
    private val existAccounts: MutableList<Account>

    private var nameEdit: EditText? = null
    private var actionSpinner: Spinner? = null

    init {

        existAccounts = allAccounts
                .filter { importAccount.currencyCode == null || it.currencyCode == importAccount.currencyCode }
                .toMutableList()

        View.inflate(context, R.layout.activity_import_transactions_action, this)

        initializeView()
    }

    fun apply() {
        val action = actionSpinner!!.selectedItem as AccountAction?
        if (action != null) apply(action)
    }

    private fun apply(action: AccountAction) {
        if (action.type == CREATE_ACCOUNT) {
            val text = nameEdit!!.text
            val newName = if (TextUtils.isEmpty(text)) importAccount.name else text.toString()

            if (importAccount.strategy is NewAccountImportStrategy) {
                val newAccountImportStrategy = importAccount.strategy as NewAccountImportStrategy
                newAccountImportStrategy.name = newName
            } else {
                importAccount.strategy = NewAccountImportStrategy(newName)
            }
        } else if (action.type == CHOOSE_ACCOUNT) {
            if (importAccount.strategy is ExistAccountImportStrategy) {
                val existAccountImportStrategy = importAccount.strategy as ExistAccountImportStrategy
                existAccountImportStrategy.account = action.account!!
            } else {
                importAccount.strategy = ExistAccountImportStrategy(action.account!!)
            }
        }
    }

    private fun initializeView() {
        nameEdit = findViewById(R.id.name_edit) as EditText

        val actionsAdapter = ActionsAdapter(context)
        actionsAdapter.add(AccountAction(CREATE_ACCOUNT, context.getString(R.string.create_account)))

        for (account in existAccounts) {
            val accountAction = AccountAction(CHOOSE_ACCOUNT, context.getString(R.string.use_account, account.name))
            accountAction.account = account

            actionsAdapter.add(accountAction)
        }
        actionSpinner = findViewById(R.id.action_spinner) as Spinner
        actionSpinner!!.adapter = actionsAdapter
        actionSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onActionSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        if (importAccount.strategy is ExistAccountImportStrategy) {
            val existAccountImportStrategy = importAccount.strategy as ExistAccountImportStrategy
            val accountId = existAccountImportStrategy.account.id!!
            for (i in existAccounts.indices) {
                val account = existAccounts[i]
                if (account.id === accountId) {
                    actionSpinner!!.setSelection(i + 1)
                    break
                }
            }
        } else {
            actionSpinner!!.setSelection(0)
        }
    }

    private fun onActionSelected(position: Int) {
        val adapter = actionSpinner!!.adapter as ActionsAdapter
        val accountAction = adapter.getItem(position)

        if (accountAction!!.type == CREATE_ACCOUNT) {
            nameEdit!!.visibility = View.VISIBLE
            nameEdit!!.setText(importAccount.name)
        } else {
            nameEdit!!.visibility = View.GONE
        }
    }

    private inner class ActionsAdapter(context: Context) : ArrayAdapter<AccountAction>(context, 0) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: View.inflate(parent.context, R.layout.activity_import_transactions_action_item, null)

            bindView(view, position)
            (view.findViewById(R.id.name) as TextView).text = importAccount.displayName

            return view
        }

        private fun bindView(view: View, position: Int) {
            val accountAction = getItem(position)
            (view.findViewById(android.R.id.text1) as TextView).text = accountAction!!.actionDescription
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: View.inflate(parent.context, android.R.layout.simple_list_item_1, null)

            bindView(view, position)

            return view
        }
    }

    private class AccountAction(val type: Int, val actionDescription: String) {

        var account: Account? = null
    }

    companion object {

        private val CREATE_ACCOUNT = 0
        private val CHOOSE_ACCOUNT = 1
    }
}
