package com.cactusteam.money.ui.fragment

import android.app.Fragment
import android.os.Bundle
import android.view.View
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.ui.activity.BaseActivity
import rx.subscriptions.CompositeSubscription

/**
 * @author vpotapenko
 */
open class BaseFragment() : Fragment() {

    val compositeSubscription: CompositeSubscription get() = _compositeSubscription!!

    private var progressBar: View? = null
    private var contentView: View? = null

    private var _compositeSubscription: CompositeSubscription? = null

    val baseActivity: BaseActivity
        get() = activity as BaseActivity

    val dataManager: DataManager
        get() = MoneyApp.instance.dataManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _compositeSubscription = CompositeSubscription()
    }

    override fun onDestroyView() {
        _compositeSubscription?.unsubscribe()
        _compositeSubscription = null
        super.onDestroyView()
    }

    fun showError(error: String?) {
        baseActivity.showError(error)
    }

    protected fun initializeProgress(progressBar: View, contentView: View) {
        this.progressBar = progressBar
        this.contentView = contentView
    }

    open protected fun showProgress() {
        progressBar!!.visibility = View.VISIBLE
        contentView!!.visibility = View.GONE
    }

    open protected fun hideProgress() {
        progressBar!!.visibility = View.GONE
        contentView!!.visibility = View.VISIBLE
    }
}
