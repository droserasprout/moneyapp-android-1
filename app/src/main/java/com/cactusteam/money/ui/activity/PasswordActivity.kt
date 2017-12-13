package com.cactusteam.money.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView

import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.ui.UiConstants

/**
 * @author vpotapenko
 */
class PasswordActivity : BaseUnauthorizedActivity("PasswordActivity") {

    private var setMode: Boolean = false

    private var passwordView: EditText? = null
    private var repeatPasswordView: EditText? = null
    private var passwordError: TextView? = null

    private var iconView: ImageView? = null

    private var pass: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)

        iconView = findViewById(R.id.icon) as ImageView

        passwordView = findViewById(R.id.password_text) as EditText
        passwordView!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                clearError()
            }

            override fun afterTextChanged(s: Editable) {
                checkPassword(false)
            }
        })
        repeatPasswordView = findViewById(R.id.repeat_password_text) as EditText
        repeatPasswordView!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                clearError()
            }

            override fun afterTextChanged(s: Editable) {

            }
        })

        val okButton = findViewById(R.id.ok_btn)
        okButton.setOnClickListener { okClicked() }

        if (setMode) {
            repeatPasswordView!!.visibility = View.VISIBLE
            repeatPasswordView!!.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                    okClicked()
                    return@OnEditorActionListener true
                }
                false
            })
        } else {
            repeatPasswordView!!.visibility = View.GONE
            passwordView!!.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                    checkPassword(true)
                    return@OnEditorActionListener true
                }
                false
            })
            okButton.visibility = View.GONE

            val decoded = MoneyApp.instance.appPreferences.password
            pass = decodePassword(decoded)
        }

        passwordError = findViewById(R.id.password_error) as TextView
    }

    private fun checkPassword(force: Boolean) {
        val text = passwordView!!.text
        if (TextUtils.equals(text, pass)) {
            MoneyApp.instance.appPreferences.lastLoginTime = System.currentTimeMillis()

            setResult(Activity.RESULT_OK)
            finish()
        } else if (force) {
            showPasswordError(getString(R.string.wrong_password))
        }
    }

    private fun clearError() {
        if (passwordError!!.visibility == View.VISIBLE) {
            passwordError!!.visibility = View.GONE
            iconView!!.setImageResource(R.drawable.ic_password_logo)
        }
    }

    private fun okClicked() {
        if (setMode) {
            val text = passwordView!!.text
            if (text.isNullOrBlank()) {
                showPasswordError(getString(R.string.password_must_not_be_empty))
                return
            }
            val repeated = repeatPasswordView!!.text
            if (!TextUtils.equals(text, repeated)) {
                showPasswordError(getString(R.string.wrong_repeated_password))
                return
            }

            val newPassword = encodePassword(text.toString())
            MoneyApp.instance.appPreferences.password = newPassword

            setResult(Activity.RESULT_OK)
            finish()
        } else {
            val text = passwordView!!.text
            val decoded = MoneyApp.instance.appPreferences.password
            val pass = decodePassword(decoded)

            if (TextUtils.equals(text, pass)) {
                MoneyApp.instance.appPreferences.lastLoginTime = System.currentTimeMillis()

                setResult(Activity.RESULT_OK)
                finish()
            } else {
                showPasswordError(getString(R.string.wrong_password))
            }
        }
    }

    private fun showPasswordError(message: String) {
        passwordError!!.visibility = View.VISIBLE
        passwordError!!.text = message
        iconView!!.setImageResource(R.drawable.ic_password_logo_wrong)
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_MODE)) {
                setMode = extras.getBoolean(UiConstants.EXTRA_MODE)
            }
        }
    }

    companion object {

        fun actionStart(activity: Activity, resultCode: Int) {
            val intent = Intent(activity, PasswordActivity::class.java)
            activity.startActivityForResult(intent, resultCode)
        }

        fun actionStart(activity: Activity, resultCode: Int, setMode: Boolean) {
            val intent = Intent(activity, PasswordActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_MODE, setMode)
            activity.startActivityForResult(intent, resultCode)
        }

        private fun encodePassword(password: String): String {
            val encode = Base64.encode(password.toByteArray(), Base64.DEFAULT)
            return String(encode)
        }

        private fun decodePassword(base64Str: String?): String {
            if (base64Str == null) return ""

            val decode = Base64.decode(base64Str, Base64.DEFAULT)
            return String(decode)
        }
    }
}
