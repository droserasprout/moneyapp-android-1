package com.cactusteam.money.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar

import com.cactusteam.money.R
import com.cactusteam.money.ui.fragment.FirstStepSettingsFragment

/**
 * @author vpotapenko
 */
class FirstStartActivity : BaseActivity("FirstStartActivity") {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_start)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) showSettingsFragment()
    }

    private fun showSettingsFragment() {
        showFragment(R.id.content_frame, FirstStepSettingsFragment.build(), "settings")
    }

    fun modelSettingsSaved() {
        MainActivity.actionStart(this, null)
        finish()
    }

    companion object {

        fun actionStart(context: Context) {
            val intent = Intent(context, FirstStartActivity::class.java)
            context.startActivity(intent)
        }
    }
}
