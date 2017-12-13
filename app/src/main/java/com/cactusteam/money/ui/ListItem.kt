package com.cactusteam.money.ui

/**
 * @author vpotapenko
 */
class ListItem {

    var type: Int = 0
    var obj: Any? = null
    var selected: Boolean =false

    constructor(type: Int, obj: Any?) {
        this.type = type
        this.obj = obj
    }

    constructor() {
    }
}
