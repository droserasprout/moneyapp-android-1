package com.cactusteam.money.ui.widget

import android.content.Context
import android.os.Build
import android.support.v4.widget.SlidingPaneLayout
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * @author vpotapenko
 */
class CrossFadeSlidingPaneLayout : SlidingPaneLayout {

    private var partialView: View? = null
    private var fullView: View? = null

    // helper flag pre honeycomb used in visibility and click response handling
    // helps avoid unnecessary layouts
    private var wasOpened = false

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (childCount < 1) {
            return
        }

        val panel = getChildAt(0) as? ViewGroup ?: return

        if (panel.childCount != 2) {
            return
        }
        fullView = panel.getChildAt(0)
        partialView = panel.getChildAt(1)

        super.setPanelSlideListener(crossFadeListener)
    }

    override fun setPanelSlideListener(listener: SlidingPaneLayout.PanelSlideListener?) {
        if (listener == null) {
            super.setPanelSlideListener(crossFadeListener)
            return
        }

        super.setPanelSlideListener(object : SlidingPaneLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {
                crossFadeListener.onPanelSlide(panel, slideOffset)
                listener.onPanelSlide(panel, slideOffset)
            }

            override fun onPanelOpened(panel: View) {
                listener.onPanelOpened(panel)
            }

            override fun onPanelClosed(panel: View) {
                listener.onPanelClosed(panel)
            }
        })
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (partialView != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                // "changed" means that views were added or removed
                // we need to move the partial view out of the way in any case (if it's supposed to of course)
                updatePartialViewVisibilityPreHoneycomb(isOpen)
            } else {
                partialView!!.visibility = if (isOpen) View.GONE else View.VISIBLE
            }
        }
    }

    private val crossFadeListener = object : SlidingPaneLayout.SimplePanelSlideListener() {
        override fun onPanelSlide(panel: View?, slideOffset: Float) {
            super.onPanelSlide(panel, slideOffset)
            if (partialView == null || fullView == null) {
                return
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                if (slideOffset == 1f && !wasOpened) {
                    // the layout was just opened, move the partial view off screen
                    updatePartialViewVisibilityPreHoneycomb(true)
                    wasOpened = true
                } else if (slideOffset < 1 && wasOpened) {
                    // the layout just started to close, move the partial view back, so it can be shown animating
                    updatePartialViewVisibilityPreHoneycomb(false)
                    wasOpened = false
                }
            } else {
                partialView!!.visibility = if (isOpen) View.GONE else View.VISIBLE
            }

            partialView!!.alpha = 1 - slideOffset
            fullView!!.alpha = slideOffset
        }
    }

    private fun updatePartialViewVisibilityPreHoneycomb(slidingPaneOpened: Boolean) {
        // below API 11 the top view must be moved so it does not consume clicks intended for the bottom view
        // this applies curiously even when setting its visibility to GONE
        // this might be due to platform limitations or it may have been introduced by NineOldAndroids library
        if (slidingPaneOpened) {
            partialView!!.layout(-partialView!!.width, 0, 0, partialView!!.height)
        } else {
            partialView!!.layout(0, 0, partialView!!.width, partialView!!.height)
        }
    }
}
