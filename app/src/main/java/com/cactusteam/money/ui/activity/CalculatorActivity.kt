package com.cactusteam.money.ui.activity

import android.app.Activity
import android.app.Fragment
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.widget.calculator.CalculatorController
import org.json.JSONObject

class CalculatorActivity : BaseActivity("CalculatorActivity"), PopupMenu.OnMenuItemClickListener {

    private var referenceId: String? = null
    private var initialTitle: String? = null
    private var initialAmount: Double = 0.0

    private val controller = CalculatorController()

    private var calcTextView: TextView? = null
    private var preliminaryView: TextView? = null
    private var calcEditScroll: HorizontalScrollView? = null

    private var amountFromClipboard: Double? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        title = if (initialTitle == null) getString(R.string.amount_label) else initialTitle
        initializeToolbar()

        val backspace = findViewById(R.id.backspace) as ImageButton
        backspace.drawable.setColorFilter(resources.getColor(R.color.color_primary), PorterDuff.Mode.SRC_ATOP)
        backspace.setOnClickListener {
            controller.handleBackspace()
            updateText()
        }

        calcTextView = findViewById(R.id.calc_edit) as TextView
        calcEditScroll = findViewById(R.id.calc_edit_scroll) as HorizontalScrollView

        preliminaryView = findViewById(R.id.preliminary_edit) as TextView

        findViewById(R.id.calc_edit_container).setOnClickListener { v -> calcEditClicked(v) }

        findViewById(R.id.num0).setOnClickListener { numClicked(0) }
        findViewById(R.id.num1).setOnClickListener { numClicked(1) }
        findViewById(R.id.num2).setOnClickListener { numClicked(2) }
        findViewById(R.id.num3).setOnClickListener { numClicked(3) }
        findViewById(R.id.num4).setOnClickListener { numClicked(4) }
        findViewById(R.id.num5).setOnClickListener { numClicked(5) }
        findViewById(R.id.num6).setOnClickListener { numClicked(6) }
        findViewById(R.id.num7).setOnClickListener { numClicked(7) }
        findViewById(R.id.num8).setOnClickListener { numClicked(8) }
        findViewById(R.id.num9).setOnClickListener { numClicked(9) }
        findViewById(R.id.ok_btn).setOnClickListener { okClicked() }
        findViewById(R.id.dot).setOnClickListener {
            controller.handleDot()
            updateText()
        }
        findViewById(R.id.num000).setOnClickListener {
            controller.handleNumber(0)
            controller.handleNumber(0)
            controller.handleNumber(0)

            updateText()
        }
        findViewById(R.id.plus).setOnClickListener {
            controller.handlePlus()
            updateText()
        }
        findViewById(R.id.minus).setOnClickListener {
            controller.handleMinus()
            updateText()
        }
        findViewById(R.id.multiply).setOnClickListener {
            controller.handleMultiply()
            updateText()
        }
        findViewById(R.id.divide).setOnClickListener {
            controller.handleDivide()
            updateText()
        }
        findViewById(R.id.ac_btn).setOnClickListener {
            controller.handleClear()
            updateText()
        }

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            setValue(initialAmount)
        }

    }

    private fun restoreState(savedInstanceState: Bundle) {
        if (savedInstanceState.containsKey("controller")) {
            val s = savedInstanceState.getString("controller")
            controller.initialize(JSONObject(s))
            updateText()
        }
        if (savedInstanceState.containsKey("referenceId")) {
            referenceId = savedInstanceState.getString("referenceId")
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString("controller", controller.asJson().toString())
        if (referenceId != null) outState?.putString("referenceId", referenceId!!)

        super.onSaveInstanceState(outState)
    }

    private fun injectExtras() {
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_NAME)) {
                initialTitle = extras.getString(UiConstants.EXTRA_NAME)
            }
            if (extras.containsKey(UiConstants.EXTRA_AMOUNT)) {
                initialAmount = extras.getDouble(UiConstants.EXTRA_AMOUNT)
            }
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                referenceId = extras.getString(UiConstants.EXTRA_ID)
            }
        }
    }

    private fun calcEditClicked(v: View) {
        val popupMenu = PopupMenu(this, v)
        popupMenu.menuInflater.inflate(R.menu.fragment_choose_amount_popup, popupMenu.menu)

        prepareMenu(popupMenu.menu)

        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.show()
    }

    private fun prepareMenu(menu: Menu) {
        prepareAmountFromClipboard()

        menu.findItem(R.id.menu_paste).isVisible = amountFromClipboard != null
    }

    private fun prepareAmountFromClipboard() {
        amountFromClipboard = null

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount <= 0) return

        val text = clip.getItemAt(0).coerceToText(this)
        if (text.isNullOrBlank()) return

        try {
            var textValue = text.toString()
            textValue = textValue.replace(",".toRegex(), ".").replace("\\s".toRegex(), "")
            amountFromClipboard = java.lang.Double.parseDouble(textValue)
        } catch (ignore: Exception) {
        }

    }

    private fun okClicked() {
        val data = Intent()

        val result = controller.calculate()
        data.putExtra(UiConstants.EXTRA_AMOUNT, result)
        if (referenceId != null) data.putExtra(UiConstants.EXTRA_ID, referenceId!!)

        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun setValue(value: Double) {
        controller.setCurrent(value)
        updateText()
    }

    private fun updateText() {
        if (controller.hasPreliminaryAmount()) {
            preliminaryView!!.visibility = View.VISIBLE
            preliminaryView!!.text = controller.preliminaryAmountString
        } else {
            preliminaryView!!.visibility = View.INVISIBLE
        }
        calcTextView!!.text = controller.asString
        calcTextView!!.postDelayed({ calcEditScroll!!.fullScroll(HorizontalScrollView.FOCUS_RIGHT) }, 100L)
    }

    private fun numClicked(num: Int) {
        controller.handleNumber(num)
        updateText()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_clear) {
            controller.handleClear()
            updateText()
            return true
        } else if (item.itemId == R.id.menu_copy) {
            copyClicked()
            return true
        } else if (item.itemId == R.id.menu_paste) {
            if (amountFromClipboard != null) setValue(amountFromClipboard!!)
            return true
        } else {
            return false
        }
    }

    private fun copyClicked() {
        val amount = controller.calculate()

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip = ClipData.newPlainText(null, amount.toString())

        Toast.makeText(this, R.string.amount_was_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    companion object {

        fun actionStart(activity: Activity, requestCode: Int, initialAmount: Double = 0.0, title: String? = null, referenceId: String? = null) {
            val intent = Intent(activity, CalculatorActivity::class.java)

            if (title != null) intent.putExtra(UiConstants.EXTRA_NAME, title)
            if (initialAmount != 0.0) intent.putExtra(UiConstants.EXTRA_AMOUNT, initialAmount)
            if (referenceId != null) intent.putExtra(UiConstants.EXTRA_ID, referenceId)

            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, requestCode: Int, initialAmount: Double = 0.0, title: String? = null, referenceId: String? = null) {
            val intent = Intent(fragment.activity, CalculatorActivity::class.java)

            if (title != null) intent.putExtra(UiConstants.EXTRA_NAME, title)
            if (initialAmount != 0.0) intent.putExtra(UiConstants.EXTRA_AMOUNT, initialAmount)
            if (referenceId != null) intent.putExtra(UiConstants.EXTRA_ID, referenceId)

            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
