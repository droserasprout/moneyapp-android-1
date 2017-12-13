package com.cactusteam.money.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast

import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.ui.UiConstants

/**
 * @author vpotapenko
 */
class PasswordManagementActivity : BaseActivity("PasswordManagementActivity") {

    private var passwordView: TextView? = null
    private var clearButton: View? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.PASSWORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                passwordWasSet()
            }
        }
    }

    private fun passwordWasSet() {
        Toast.makeText(this, R.string.password_was_set, Toast.LENGTH_SHORT).show()
        updateControls()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_management)

        initializeToolbar()

        passwordView = findViewById(R.id.password) as TextView
        findViewById(R.id.set_btn).setOnClickListener { setClicked() }

        clearButton = findViewById(R.id.clear_btn)
        clearButton!!.setOnClickListener { clearClicked() }

        updateControls()
    }

    private fun clearClicked() {
        MoneyApp.instance.appPreferences.clearPassword()
        Toast.makeText(this, R.string.password_was_cleared, Toast.LENGTH_SHORT).show()

        updateControls()
    }

    private fun updateControls() {
        val password = MoneyApp.instance.appPreferences.password
        if (password == null) {
            passwordView!!.visibility = View.GONE
            clearButton!!.visibility = View.GONE
        } else {
            passwordView!!.visibility = View.VISIBLE
            clearButton!!.visibility = View.VISIBLE
        }
    }

    private fun setClicked() {
        PasswordActivity.actionStart(this, UiConstants.PASSWORD_REQUEST_CODE, true)
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, PasswordManagementActivity::class.java)
            context.startActivity(intent)
        }
    }
}
