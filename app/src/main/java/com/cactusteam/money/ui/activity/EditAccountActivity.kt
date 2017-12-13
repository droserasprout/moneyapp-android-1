package com.cactusteam.money.ui.activity

import android.app.Activity
import android.app.AlertDialog
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.TextWatcher
import android.util.Pair
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.currency.MCurrency
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.CurrencyRate
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.ChooseCurrencyFragment
import com.cactusteam.money.ui.fragment.ChooseRateFragment

/**
 * @author vpotapenko
 */
class EditAccountActivity : BaseDataActivity("EditAccountActivity") {

    private var accountId: Long = -1

    private var nameText: EditText? = null
    private var nameLayout: TextInputLayout? = null
    private var currencyView: TextView? = null
    private var rateView: TextView? = null

    private var initialBalanceView: TextView? = null
    private var initialBalance: Double = 0.toDouble()

    private var colorSpinner: Spinner? = null
    private var typeSpinner: Spinner? = null

    private var skipInBalanceView: CheckBox? = null

    private var rateContainer: LinearLayout? = null
    private var rateProgress: View? = null

    private var initialCurrencyCode: String? = null

    private var currency: MCurrency? = null

    private var rate: CurrencyRate? = null

    private var deleted: Boolean = false

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_edit_account, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.delete).isVisible = accountId >= 0 && !deleted
        menu.findItem(R.id.restore).isVisible = deleted

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.delete) {
            deleteAccount()
            return true
        } else if (itemId == R.id.restore) {
            restoreAccount()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun restoreAccount() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.accountService
                .restoreAccount(accountId)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            accountRestored()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun accountRestored() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun deleteAccount() {
        AlertDialog.Builder(this)
                .setTitle(R.string.continue_question)
                .setMessage(R.string.account_will_be_deleted)
                .setPositiveButton(android.R.string.yes) { dialog, which -> removeAccount() }
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    private fun removeAccount() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.accountService
                .deleteAccount(accountId)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            accountDeleted()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun accountDeleted() {
        Toast.makeText(this, R.string.account_was_deleted, Toast.LENGTH_SHORT).show()

        val data = Intent()
        data.putExtra(UiConstants.EXTRA_DELETED, true)

        setResult(Activity.RESULT_OK, data)
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account)

        initializeToolbar()
        setTitle(if (accountId >= 0) R.string.edit_account_title else R.string.new_account_title)

        initializeViewProgress()

        val iconView = findViewById(R.id.icon) as ImageView
        val drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
        drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
        iconView.setImageDrawable(drawable)

        nameText = findViewById(R.id.name) as EditText
        nameText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                nameLayout!!.error = null
            }
        })
        nameLayout = findViewById(R.id.name_layout) as TextInputLayout
        currencyView = findViewById(R.id.currency) as TextView
        findViewById(R.id.currency_container).setOnClickListener { currencyClicked() }
        findViewById(R.id.save_btn).setOnClickListener { saveAccount() }

        val adapter = ColorsAdapter(this)
        adapter.add(resources.getColor(R.color.color1))
        adapter.add(resources.getColor(R.color.color2))
        adapter.add(resources.getColor(R.color.color3))
        adapter.add(resources.getColor(R.color.color4))
        adapter.add(resources.getColor(R.color.color5))
        adapter.add(resources.getColor(R.color.color6))
        adapter.add(resources.getColor(R.color.color7))
        adapter.add(resources.getColor(R.color.color8))
        adapter.add(resources.getColor(R.color.color9))
        adapter.add(resources.getColor(R.color.color10))
        adapter.add(resources.getColor(R.color.color11))
        adapter.add(resources.getColor(R.color.color12))
        adapter.add(resources.getColor(R.color.color13))
        adapter.add(resources.getColor(R.color.color14))
        adapter.add(resources.getColor(R.color.color15))
        adapter.add(resources.getColor(R.color.color16))
        adapter.add(resources.getColor(R.color.color17))
        adapter.add(resources.getColor(R.color.color18))

        colorSpinner = findViewById(R.id.color_type) as Spinner
        colorSpinner!!.adapter = adapter

        val typesAdapter = TypesAdapter(this)
        val types = resources.getStringArray(R.array.account_types)
        val descriptions = resources.getStringArray(R.array.account_types_description)

        types.indices.forEach { typesAdapter.add(Pair(types[it], descriptions[it])) }

        typeSpinner = findViewById(R.id.type_spinner) as Spinner
        typeSpinner!!.adapter = typesAdapter

        skipInBalanceView = findViewById(R.id.skip_in_balance) as CheckBox

        rateProgress = findViewById(R.id.rate_progress_bar)
        rateView = findViewById(R.id.rate) as TextView
        rateContainer = findViewById(R.id.rate_container) as LinearLayout
        rateContainer!!.setOnClickListener { rateClicked() }

        val initialBalanceContainer = findViewById(R.id.initial_balance_container)
        initialBalanceContainer.setOnClickListener { balanceClicked() }
        initialBalanceView = findViewById(R.id.initial_balance) as TextView

        if (accountId >= 0) {
            initialBalanceContainer.visibility = View.GONE
            loadAccount()
        } else {
            initialBalanceContainer.visibility = View.VISIBLE

            val mainCurrencyCode = MoneyApp.instance.appPreferences.mainCurrencyCode
            currency = MoneyApp.instance.currencyManager.getCurrencyByCode(mainCurrencyCode)

            updateInitialBalanceView()
            updateCurrencyView()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.CALCULATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                initialBalance = Math.abs(data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0))
                updateInitialBalanceView()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun balanceClicked() {
        CalculatorActivity.actionStart(this, UiConstants.CALCULATOR_REQUEST_CODE, initialBalance, getString(R.string.start_balance))
    }

    private fun updateInitialBalanceView() {
        val amountStr = UiUtils.formatCurrency(initialBalance, currency!!.currencyCode)
        initialBalanceView!!.text = amountStr
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

    private fun updateRateView() {
        val source = UiUtils.formatCurrency(1.0, rate!!.sourceCurrencyCode)
        val dest = UiUtils.formatCurrency(rate!!.rate, rate!!.destCurrencyCode)

        rateView!!.text = getString(R.string.rate_pattern, source, dest)
    }

    private fun saveAccount() {
        val name = nameText!!.text
        if (name.isNullOrBlank()) {
            nameLayout!!.error = getString(R.string.account_name_is_required)
            return
        }

        val color = colorSpinner!!.selectedItem as Int
        val hexColor = String.format("#%06X", 0xFFFFFF and color)

        if (accountId < 0) {
            createAccount(name.toString(), hexColor, skipInBalanceView!!.isChecked)
        } else {
            updateAccount(name.toString(), hexColor, skipInBalanceView!!.isChecked)
        }
    }

    private fun updateAccount(name: String, color: String, skipInBalance: Boolean) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.accountService
                .updateAccount(
                        accountId,
                        name,
                        typeSpinner!!.selectedItemPosition,
                        currency!!.currencyCode,
                        color,
                        skipInBalance,
                        rate)
                .subscribe(
                        { a ->
                            accountUpdated(a)
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        { hideBlockingProgress() }
                )
        compositeSubscription.add(s)
    }

    private fun accountUpdated(a: Account?) {
        Toast.makeText(this, R.string.account_was_saved, Toast.LENGTH_SHORT).show()
        hideBlockingProgress()

        val data = Intent()
        data.putExtra(UiConstants.EXTRA_COLOR, a?.color)
        setResult(Activity.RESULT_OK, data)

        finish()
    }

    private fun createAccount(name: String, color: String, skipInBalance: Boolean) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.accountService
                .createAccount(
                        name,
                        typeSpinner!!.selectedItemPosition,
                        currency!!.currencyCode,
                        color,
                        skipInBalance,
                        initialBalance)
                .subscribe(
                        { a -> accountCreated(a) },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        { hideBlockingProgress() }
                )
        compositeSubscription.add(s)
    }

    private fun accountCreated(a: Account?) {
        Toast.makeText(this, R.string.account_was_saved, Toast.LENGTH_SHORT).show()

        val data = Intent()
        data.putExtra(UiConstants.EXTRA_ID, a!!.id)
        data.putExtra(UiConstants.EXTRA_NAME, a.name)
        data.putExtra(UiConstants.EXTRA_COLOR, a.color)

        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun loadAccount() {
        showProgress()
        val s = dataManager.accountService
                .getAccount(accountId)
                .subscribe(
                        { a ->
                            accountLoaded(a)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        },
                        { hideProgress() }
                )
        compositeSubscription.add(s)
    }

    private fun accountLoaded(account: Account) {
        deleted = account.deleted

        val supportActionBar = supportActionBar
        supportActionBar?.invalidateOptionsMenu()

        nameText!!.setText(account.name)

        initialCurrencyCode = account.currencyCode
        currency = MoneyApp.instance.currencyManager.getCurrencyByCode(initialCurrencyCode)
        updateCurrencyView()

        typeSpinner!!.setSelection(account.type)
        skipInBalanceView!!.isChecked = account.skipInBalance

        try {
            val color = Color.parseColor(account.color)

            val adapter = colorSpinner!!.adapter
            for (position in 0..adapter.count - 1) {
                val c = adapter.getItem(position) as Int
                if (color == c) {
                    colorSpinner!!.setSelection(position)
                    break
                }
            }
        } catch (ignore: Exception) {
        }

    }

    private fun updateCurrencyView() {
        currencyView!!.text = currency!!.displayString

        if (initialCurrencyCode != null && currency!!.currencyCode != initialCurrencyCode) {
            rateContainer!!.visibility = View.VISIBLE
            loadRate()
        } else {
            rateContainer!!.visibility = View.GONE
        }
    }

    private fun loadRate() {
        fillRateAsStub()

        rateView!!.visibility = View.GONE
        rateProgress!!.visibility = View.VISIBLE

        val s = dataManager.currencyService
                .getRate(initialCurrencyCode!!, currency!!.currencyCode)
                .subscribe(
                        { r ->
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
                        },
                        {
                            rateProgress!!.visibility = View.GONE
                            rateView!!.visibility = View.VISIBLE
                        }
                )
        compositeSubscription.add(s)
    }

    private fun fillRateAsStub() {
        val r = CurrencyRate()
        r.rate = 1.0
        r.sourceCurrencyCode = initialCurrencyCode
        r.destCurrencyCode = currency!!.currencyCode

        rate = r
    }

    private fun currencyClicked() {
        val fragment = ChooseCurrencyFragment.build(getString(R.string.account_currency))
        fragment.onCurrencySelectedListener = { c ->
            currency = c
            updateCurrencyView()
            updateInitialBalanceView()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                accountId = extras.getLong(UiConstants.EXTRA_ID)
            }
        }
    }

    private class ColorsAdapter(context: Context) : ArrayAdapter<Int>(context, 0) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: View.inflate(parent.context, R.layout.activity_edit_account_color_item, null)

            val imageView = view.findViewById(R.id.color_image) as ImageView
            imageView.drawable.setColorFilter(getItem(position)!!, PorterDuff.Mode.SRC_ATOP)

            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: View.inflate(parent.context, R.layout.activity_edit_account_dp_color_item, null)

            val imageView = view.findViewById(R.id.color_image) as ImageView
            imageView.drawable.setColorFilter(getItem(position)!!, PorterDuff.Mode.SRC_ATOP)

            return view
        }
    }

    private class TypesAdapter(context: Context) : ArrayAdapter<Pair<String, String>>(context, 0) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getView(position, convertView, parent, R.layout.activity_edit_account_type)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = getView(position, convertView, parent, R.layout.activity_edit_account_type_dp)

            val item = getItem(position)
            val descriptionView = view.findViewById(R.id.description) as TextView
            if (item!!.second.isNullOrBlank()) {
                descriptionView.visibility = View.GONE
            } else {
                descriptionView.visibility = View.VISIBLE
                descriptionView.text = item.second
            }

            return view
        }

        private fun getView(position: Int, convertView: View?, parent: ViewGroup, layoutId: Int): View {
            val view = convertView ?: View.inflate(parent.context, layoutId, null)

            val item = getItem(position)
            (view.findViewById(R.id.name) as TextView).text = item!!.first

            return view
        }
    }

    companion object {

        fun actionStart(activity: Activity, requestCode: Int, accountId: Long) {
            val intent = Intent(activity, EditAccountActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, accountId)

            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, requestCode: Int) {
            val intent = Intent(fragment.activity, EditAccountActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
