package com.cactusteam.money.ui.fragment

import android.os.Bundle
import android.view.View

import com.cactusteam.money.ui.activity.MainActivity

/**
 * @author vpotapenko
 */
abstract class BaseMainFragment : BaseFragment(), MainActivity.IChangeDataListener {

    val mainActivity: MainActivity get() = activity as MainActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is MainActivity) {
            val activity = activity as MainActivity
            activity.addChangeListener(this)
        }
    }

    override fun onDestroyView() {
        if (activity is MainActivity) {
            val activity = activity as MainActivity
            activity.removeDataListener(this)
        }

        super.onDestroyView()
    }
}
