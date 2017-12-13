package com.cactusteam.money.ui.widget.home

import android.view.View

/**
 * @author vpotapenko
 */
interface IHomeUnit : IBaseUnit {

    fun initialize()

    fun update()

    fun getView(): View?
}
