package com.cactusteam.money.ui.widget.home

import android.view.View
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.ui.fragment.HomeFragment

/**
 * @author vpotapenko
 */
abstract class BaseHomeUnit(protected val homeFragment: HomeFragment) : IHomeUnit {

    protected var mainCurrencyCode: String

    protected var _view: View? = null

    init {
        val appPreferences = MoneyApp.instance.appPreferences
        mainCurrencyCode = appPreferences.mainCurrencyCode
    }

    override fun initialize() {
        _view = View.inflate(homeFragment.activity, getLayoutRes(), null)
        initializeView()
    }

    protected abstract fun getLayoutRes(): Int

    protected abstract fun initializeView()

    override fun getView(): View? {
        return _view
    }
}
