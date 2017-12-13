package com.cactusteam.money.ui.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Fragment
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.cactusteam.money.R
import com.cactusteam.money.data.dao.Account
import com.cactusteam.money.data.dao.Debt
import com.cactusteam.money.data.dao.Transaction
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import rx.Observable
import java.util.*

/**
 * @author vpotapenko
 */
class EditDebtActivity : BaseDataActivity("EditDebtActivity") {

    private var debtId: Long = -1
    private var type: Int = 0

    private var iconView: ImageView? = null

    private var accountIconView: ImageView? = null
    private var accountNameView: TextView? = null
    private var nameView: TextView? = null
    private var errorNameView: TextView? = null
    private var phoneView: TextView? = null
    private var tillView: TextView? = null
    private var startView: TextView? = null
    private var amountView: TextView? = null
    private var errorAmountView: TextView? = null

    private val accounts = mutableListOf<Account>()

    private val tillDate = Calendar.getInstance()
    private var startDate: Calendar? = null
    private var account: Account? = null
    private var amount: Double = 0.toDouble()

    private var contactId: Long? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_edit_debt, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.delete).isVisible = debtId >= 0

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.delete) {
            deleteDebt()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteDebt() {
        val view = View.inflate(this, R.layout.activity_edit_debt_deletion, null)
        val deleteTransactions = view.findViewById(R.id.delete_transactions) as CheckBox
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setView(view).setPositiveButton(android.R.string.yes) { dialog, which -> removeDebt(deleteTransactions.isChecked) }.setNegativeButton(android.R.string.no, null).show()
    }

    private fun removeDebt(removeTransactions: Boolean) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.debtService
                .deleteDebt(debtId, removeTransactions)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            Toast.makeText(this, R.string.debt_was_deleted, Toast.LENGTH_SHORT).show()
                            hideBlockingProgress()

                            val data = Intent()
                            data.putExtra(UiConstants.EXTRA_DELETED, true)

                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }
                )
        compositeSubscription.add(s)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_debt)

        initializeToolbar()
        setTitle(if (debtId >= 0) R.string.edit_debt_title else R.string.new_debt_title)

        initializeViewProgress()

        val typeType = findViewById(R.id.debt_type) as TextView
        typeType.setText(if (type == Transaction.INCOME) R.string.borrow_money else R.string.lend_money)
        updateAmountView()

        iconView = findViewById(R.id.icon) as ImageView

        nameView = findViewById(R.id.name) as TextView
        errorNameView = findViewById(R.id.name_error) as TextView
        phoneView = findViewById(R.id.phone) as TextView

        tillView = findViewById(R.id.till_date) as TextView
        findViewById(R.id.till_container).setOnClickListener { tillClicked() }

        startView = findViewById(R.id.start_date) as TextView
        findViewById(R.id.start_container).setOnClickListener { startClicked() }

        amountView = findViewById(R.id.amount) as TextView
        errorAmountView = findViewById(R.id.amount_error) as TextView

        accountNameView = findViewById(R.id.source_account_name) as TextView
        findViewById(R.id.source_account_container).setOnClickListener {
            SelectAccountActivity.actionStart(this, UiConstants.SOURCE_ACCOUNT_REQUEST_CODE)
        }
        accountIconView = findViewById(R.id.source_account_icon) as ImageView

        findViewById(R.id.choose_contact_btn).setOnClickListener { contactClicked() }
        findViewById(R.id.save_btn).setOnClickListener { saveClicked() }

        if (debtId < 0) {
            findViewById(R.id.amount_container).setOnClickListener { amountClicked() }

            tillDate.add(Calendar.DATE, 14)
            updateTillView()

            updateStartView()
        } else {
            findViewById(R.id.amount_container).visibility = View.GONE
            findViewById(R.id.type_container).visibility = View.GONE
        }
        loadAccounts()
    }

    private fun contactClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    UiConstants.PERMISSIONS_REQUEST_READ_CONTACTS.toInt())
        } else {
            showContacts()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.CONTACT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val s = dataManager.systemService
                        .getContact(data!!.data)
                        .subscribe(
                                { contact ->
                                    if (contact != null) {
                                        contactId = contact.id
                                        nameView!!.text = contact.name
                                        phoneView!!.text = contact.phone

                                        loadContactImage()
                                    }
                                },
                                { e -> showError(e.message) },
                                {}
                        )
                compositeSubscription.add(s)
            }
        } else if (requestCode == UiConstants.CALCULATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                amount = Math.abs(data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0))
                updateAmountView()
            }
        } else if (requestCode == UiConstants.SOURCE_ACCOUNT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val id = data.getLongExtra(UiConstants.EXTRA_ID, -1)
                val account = accounts.find { it.id == id }
                if (account != null) updateAccount(account)
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            UiConstants.PERMISSIONS_REQUEST_READ_CONTACTS.toInt() -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showContacts()
                }
                return
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showContacts() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = ContactsContract.Contacts.CONTENT_TYPE
        startActivityForResult(intent, UiConstants.CONTACT_REQUEST_CODE)
    }

    private fun loadContactImage() {
        if (contactId == null) return

        iconView!!.setImageResource(R.drawable.ic_contact)
        val s = dataManager.systemService
                .getContactImage(contactId!!)
                .subscribe(
                        { src ->
                            if (src != null) {
                                val drawable = RoundedBitmapDrawableFactory.create(resources, src)
                                drawable.cornerRadius = Math.min(drawable.minimumWidth, drawable.minimumHeight) / 2.0f

                                iconView!!.setImageDrawable(drawable)
                            }
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                debtId = extras.getLong(UiConstants.EXTRA_ID)
            }
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                type = extras.getInt(UiConstants.EXTRA_TYPE)
            }
        }
    }

    private fun saveClicked() {
        clearErrors()
        if (!isValid) return

        showProgress()
        if (debtId < 0) {
            createDebt()
        } else {
            updateDebt()
        }
    }

    private fun updateDebt() {
        showBlockingProgress(getString(R.string.waiting))

        val name = nameView!!.text.toString()
        val phone = phoneView!!.text.toString()
        val b = dataManager.debtService
                .newDebtBuilder()
                .putId(debtId)
                .putName(name)
                .putAccountId(account!!.id)
                .putTill(tillDate.time)
                .putStart(startDate?.time)

        if (contactId != null) b.putContactId(contactId!!)
        if (!phone.isNullOrBlank()) b.putPhone(phone)

        val s = b.update().subscribe(
                { r -> },
                { e ->
                    hideBlockingProgress()
                    showError(e.message)
                },
                {
                    Toast.makeText(this, R.string.debt_was_saved, Toast.LENGTH_SHORT).show()
                    hideBlockingProgress()

                    val data = Intent()
                    data.putExtra(UiConstants.EXTRA_NAME, name)

                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
        )
        compositeSubscription.add(s)
    }

    private fun createDebt() {
        showBlockingProgress(getString(R.string.waiting))
        val name = nameView!!.text.toString()
        val phone = phoneView!!.text.toString()
        val b = dataManager.debtService
                .newDebtBuilder()
                .putName(name)
                .putAccountId(account!!.id)
                .putTill(tillDate.time)
                .putStart(startDate?.time)

        if (contactId != null) b.putContactId(contactId!!)
        if (!phone.isNullOrBlank()) b.putPhone(phone)

        val s = b.create(type, amount)
                .subscribe(
                        { r -> },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            Toast.makeText(this, R.string.debt_was_saved, Toast.LENGTH_SHORT).show()
                            hideBlockingProgress()

                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                )
        compositeSubscription.add(s)
    }

    private fun clearErrors() {
        errorNameView!!.visibility = View.GONE
    }

    private val isValid: Boolean
        get() {
            val name = nameView!!.text
            if (name.isNullOrBlank()) {
                errorNameView!!.setText(R.string.debt_name_is_required)
                errorNameView!!.visibility = View.VISIBLE
                return false
            }

            if (debtId < 0 && amount <= 0) {
                errorAmountView!!.setText(R.string.amount_must_be_more_than_zero)
                errorAmountView!!.visibility = View.VISIBLE
                return false
            }

            return true
        }

    private fun loadAccounts() {
        showProgress()
        if (debtId < 0) {
            val s = dataManager.accountService
                    .getAccounts()
                    .subscribe(
                            { r -> accountsLoaded(r) },
                            { e ->
                                hideProgress()
                                showError(e.message)
                            },
                            {
                                hideProgress()
                                updateAmountView()
                            }
                    )
            compositeSubscription.add(s)
        } else {
            val o1 = dataManager.accountService.getAccounts()
            val o2 = dataManager.debtService.getDebt(debtId)
            val s = Observable.zip(o1, o2, { i1, i2 ->
                Pair(i1, i2)
            }).subscribe(
                    { r ->
                        accountsLoaded(r.first)
                        debtLoaded(r.second)
                    },
                    { e ->
                        hideProgress()
                        showError(e.message)
                    },
                    { hideProgress() }
            )
            compositeSubscription.add(s)
        }
    }

    private fun accountsLoaded(accounts: List<Account>) {
        this.accounts.clear()
        this.accounts.addAll(accounts)

        updateAccount(accounts[0])
    }

    private fun debtLoaded(debt: Debt) {
        nameView!!.text = debt.name
        phoneView!!.text = debt.phone

        tillDate.time = debt.till
        updateTillView()

        if (debt.start != null) {
            startDate = Calendar.getInstance()
            startDate?.time = debt.start
        }
        updateStartView()

        account = debt.account
        updateAccount(account!!)

        contactId = debt.contactId
        loadContactImage()
    }

    private fun updateAccount(account: Account) {
        this.account = account
        updateAccountView()

        updateAmountView()
    }

    private fun updateAccountView() {
        accountNameView!!.text = account?.name ?: ""

        var color = Color.DKGRAY
        try {
            color = Color.parseColor(account?.color)
        } catch (ignore: Exception) {
        }

        val drawable: Drawable?
        when (account?.type) {
            Account.SAVINGS_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_savings))
            Account.BANK_ACCOUNT_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_bank_account))
            Account.CARD_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_card))
            Account.CASH_TYPE -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
            else -> drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.ic_wallet))
        }

        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        accountIconView!!.setImageDrawable(drawable)
    }

    private fun amountClicked() {
        CalculatorActivity.actionStart(this, UiConstants.CALCULATOR_REQUEST_CODE, amount)
    }

    private fun tillClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            tillDate.set(year, monthOfYear, dayOfMonth)
            updateTillView()
        }, tillDate.get(Calendar.YEAR), tillDate.get(Calendar.MONTH), tillDate.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun startClicked() {
        val cal = startDate ?: Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            if (startDate == null) {
                startDate = Calendar.getInstance()
            }
            startDate?.set(year, monthOfYear, dayOfMonth)
            updateStartView()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateAmountView() {
        if (account == null) return

        val debtAmount = if (type == Transaction.EXPENSE) -amount else amount
        val sourceAmountStr = UiUtils.formatCurrency(debtAmount, account!!.currencyCode)
        amountView!!.text = sourceAmountStr
    }

    private fun updateTillView() {
        tillView!!.text = DateFormat.getDateFormat(this).format(tillDate.time)
    }

    private fun updateStartView() {
        if (startDate != null) {
            startView!!.text = DateFormat.getDateFormat(this).format(startDate?.time)
        } else {
            startView!!.text = "-"
        }
    }

    companion object {

        fun actionStart(fragment: Fragment, requestCode: Int, type: Int) {
            val intent = Intent(fragment.activity, EditDebtActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)
            fragment.startActivityForResult(intent, requestCode)
        }

        fun actionStart(activity: Activity, requestCode: Int, debtId: Long) {
            val intent = Intent(activity, EditDebtActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, debtId)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}
