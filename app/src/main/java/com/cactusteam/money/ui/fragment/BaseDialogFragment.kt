package com.cactusteam.money.ui.fragment

import android.app.DialogFragment
import android.os.Bundle
import android.view.View

import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.ui.activity.BaseActivity
import rx.subscriptions.CompositeSubscription

/**
 * @author vpotapenko
 */
abstract class BaseDialogFragment : DialogFragment() {

    val compositeSubscription: CompositeSubscription get() = _compositeSubscription!!

    private var progressBar: View? = null
    private var contentView: View? = null

    private var _compositeSubscription: CompositeSubscription? = null

    protected val baseActivity: BaseActivity
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

    protected fun initializeProgress(progressBar: View, contentView: View) {
        this.progressBar = progressBar
        this.contentView = contentView
    }

    protected fun showProgress() {
        progressBar!!.visibility = View.VISIBLE
        contentView!!.visibility = View.GONE
    }

    protected fun showProgressAsInvisible() {
        progressBar!!.visibility = View.VISIBLE
        contentView!!.visibility = View.INVISIBLE
    }

    protected fun hideProgress() {
        progressBar!!.visibility = View.GONE
        contentView!!.visibility = View.VISIBLE
    }

    fun showError(error: String?) {
        baseActivity.showError(error)
    }
}
