package com.cactusteam.money.ui

/**
 * @author vpotapenko
 */
class UiObjectRef {

    var ref: Any? = null

    fun <T> getRefAs(clazz: Class<T>): T {
        return clazz.cast(ref)
    }
}
