package com.cactusteam.money.ui.activity

import android.view.View

import com.cactusteam.money.R

/**
 * @author vpotapenko
 */
abstract class BaseDataActivity(tag: String) : BaseActivity(tag) {

    private var progressBar: View? = null
    private var content: View? = null

    protected fun initializeViewProgress() {
        progressBar = findViewById(R.id.progress_bar)
        content = findViewById(R.id.content)
    }

    open protected fun showProgress() {
        progressBar!!.visibility = View.VISIBLE
        content!!.visibility = View.GONE
    }

    open protected fun hideProgress() {
        progressBar!!.visibility = View.GONE
        content!!.visibility = View.VISIBLE
    }
}
