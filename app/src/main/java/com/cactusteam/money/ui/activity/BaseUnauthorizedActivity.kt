package com.cactusteam.money.ui.activity

/**
 * @author vpotapenko
 */
abstract class BaseUnauthorizedActivity(tag: String) : BaseActivity(tag) {

    override val isLoggedIn: Boolean
        get() = true

    override fun updateLoginTime() {
        // do nothing
    }
}
