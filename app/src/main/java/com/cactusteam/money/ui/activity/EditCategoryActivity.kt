package com.cactusteam.money.ui.activity

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.Fragment
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.data.DataUtils
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.ChooseIconFragment
import com.cactusteam.money.ui.fragment.ChooseNewParentCategoryFragment

/**
 * @author vpotapenko
 */
class EditCategoryActivity : BaseDataActivity("EditCategoryActivity") {

    private var categoryId: Long = -1

    private var type = Category.EXPENSE
    private var deleted: Boolean = false

    private var imagePath: String? = null
    private var iconBitmap: Bitmap? = null

    private var currentToolbarColor: ColorDrawable? = null

    private var nameView: EditText? = null
    private var errorNameText: TextView? = null

    private var iconView: ImageView? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_edit_category, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.delete).isVisible = categoryId >= 0 && !deleted
        menu.findItem(R.id.restore).isVisible = deleted

        menu.findItem(R.id.to_subcategory).isVisible = categoryId >= 0 && !deleted

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.delete) {
            deleteCategory()
            return true
        } else if (itemId == R.id.restore) {
            restoreCategory()
            return true
        } else if (itemId == R.id.to_subcategory) {
            convertToSubcategoryClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun convertToSubcategoryClicked() {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.category_will_be_transformed_to_subcategory).setPositiveButton(R.string.ok) { dialog, which -> startConvertToSubcategory() }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun startConvertToSubcategory() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.categoryService
                .getCategories(type)
                .subscribe(
                        { r ->
                            hideBlockingProgress()
                            showNewParentDialog(r.toMutableList())
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun showNewParentDialog(categories: MutableList<Category>) {
        val it = categories.iterator()
        while (it.hasNext()) {
            val category = it.next()
            if (category.id === categoryId) it.remove()
        }
        val fragment = ChooseNewParentCategoryFragment.build(categories) { category -> convertToSubcategory(category) }
        fragment.show(fragmentManager, "dialog")
    }

    private fun convertToSubcategory(newParent: Category) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.categoryService
                .convertToSubcategory(categoryId, newParent.id)
                .subscribe(
                        { r ->
                            hideBlockingProgress()

                            val data = Intent()
                            data.putExtra(UiConstants.EXTRA_DELETED, true)

                            Toast.makeText(this@EditCategoryActivity, R.string.category_was_transformed_to_subcategory, Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun restoreCategory() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.categoryService
                .restoreCategory(categoryId)
                .subscribe(
                        { r ->
                            hideBlockingProgress()

                            setResult(Activity.RESULT_OK)
                            finish()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun deleteCategory() {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.category_will_be_deleted).setPositiveButton(android.R.string.yes) { dialog, which -> removeCategory() }.setNegativeButton(android.R.string.no, null).show()
    }

    private fun removeCategory() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.categoryService
                .deleteCategory(categoryId)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            Toast.makeText(this@EditCategoryActivity, R.string.category_was_deleted, Toast.LENGTH_SHORT).show()

                            val data = Intent()
                            data.putExtra(UiConstants.EXTRA_DELETED, true)

                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }
                )
        compositeSubscription.add(s)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_category)

        initializeToolbar()
        setTitle(if (categoryId >= 0) R.string.edit_category_title else R.string.new_category_title)

        currentToolbarColor = ColorDrawable(resources.getColor(R.color.color_primary))
        updateToolbarColor()

        initializeViewProgress()

        val typeSpinner = findViewById(R.id.category_type) as Spinner
        val adapter = ArrayAdapter(this,
                R.layout.activity_edit_category_item_type,
                android.R.id.text1,
                arrayOf(getString(R.string.expense_label), getString(R.string.income_label)))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        typeSpinner.adapter = adapter
        if (categoryId < 0) typeSpinner.setSelection(if (type == Category.EXPENSE) 0 else 1)

        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                type = if (position == 0) Category.EXPENSE else Category.INCOME

                updateToolbarColor()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        nameView = findViewById(R.id.name) as EditText
        nameView!!.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                saveClicked()
                return@OnEditorActionListener true
            }
            false
        })
        errorNameText = findViewById(R.id.name_error) as TextView
        iconView = findViewById(R.id.icon) as ImageView

        findViewById(R.id.save_btn).setOnClickListener { saveClicked() }

        findViewById(R.id.change_icon_btn).setOnClickListener { changeIconClicked() }

        if (categoryId >= 0) {
            typeSpinner.visibility = View.GONE
            loadCategory()
        }
    }

    private fun loadCategory() {
        showProgress()
        val s = dataManager.categoryService
                .getCategory(categoryId)
                .subscribe(
                        { r ->
                            hideProgress()
                            categoryLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun categoryLoaded(category: Category) {
        deleted = category.deleted

        val supportActionBar = supportActionBar
        supportActionBar?.invalidateOptionsMenu()

        nameView!!.setText(category.name)

        type = category.type
        updateToolbarColor()

        imagePath = category.icon
        if (imagePath != null) requestImageBitmap()
    }

    private fun changeIconClicked() {
        val fragment = ChooseIconFragment.build()
        fragment.iconListener = { path ->
            imagePath = path
            requestImageBitmap()
        }
        fragment.show(fragmentManager, "dialog")
    }

    private fun requestImageBitmap() {
        iconView!!.setImageResource(R.drawable.ic_mock_icon)

        if (iconBitmap != null) {
            iconBitmap!!.recycle()
            iconBitmap = null
        }

        val s = dataManager.systemService
                .getIconImage(imagePath!!, true)
                .subscribe(
                        { r ->
                            iconBitmap = r
                            updateIconView()
                        },
                        { e ->
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun updateIconView() {
        if (imagePath == null || iconBitmap == null) {
            iconView!!.setImageResource(R.drawable.ic_mock_icon)
        } else {
            val drawable = BitmapDrawable(resources, iconBitmap)
            drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
            iconView!!.setImageDrawable(drawable)
        }
    }

    private fun saveClicked() {
        clearError()

        val name = nameView!!.text
        if (name.isNullOrBlank()) {
            errorNameText!!.setText(R.string.category_name_is_required)
            errorNameText!!.visibility = View.VISIBLE
            return
        }

        if (categoryId < 0) {
            createCategory(name.toString())
        } else {
            updateCategory(name.toString())
        }
    }

    private fun updateCategory(name: String) {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.categoryService
                .updateCategory(categoryId, name, imagePath)
                .subscribe(
                        { r ->
                            hideBlockingProgress()

                            val data = Intent()
                            data.putExtra(UiConstants.EXTRA_NAME, name)

                            setResult(Activity.RESULT_OK, data)
                            finish()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun createCategory(name: String) {
        sendEvent("category", "new", name)

        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.categoryService
                .createCategory(type, name, imagePath)
                .subscribe(
                        { r ->
                            hideBlockingProgress()
                            Toast.makeText(this@EditCategoryActivity, R.string.category_was_saved, Toast.LENGTH_SHORT).show()

                            val data = Intent()
                            data.putExtra(UiConstants.EXTRA_ID, r.id)
                            data.putExtra(UiConstants.EXTRA_NAME, r.name)
                            data.putExtra(UiConstants.EXTRA_TYPE, r.type)

                            setResult(Activity.RESULT_OK, data)

                            finish()
                        },
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun clearError() {
        errorNameText!!.visibility = View.GONE
    }

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun updateToolbarColor() {
        val nextColor: ColorDrawable

        if (type == Category.EXPENSE) {
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_expense_color))
        } else {
            nextColor = ColorDrawable(resources.getColor(R.color.toolbar_income_color))
        }

        if (DataUtils.hasLollipop()) {
            window.statusBarColor = UiUtils.darkenColor(nextColor.color)
        }

        val drawable = TransitionDrawable(arrayOf(currentToolbarColor as Drawable, nextColor))
        currentToolbarColor = nextColor

        toolbar!!.setBackgroundDrawable(drawable)
        drawable.startTransition(UiConstants.TOOLBAR_TRANSITION_DURATION)
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                categoryId = extras.getLong(UiConstants.EXTRA_ID)
            }
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                type = extras.getInt(UiConstants.EXTRA_TYPE)
            }
        }
    }

    companion object {

        fun actionStart(activity: Activity, requestCode: Int, categoryId: Long) {
            val intent = Intent(activity, EditCategoryActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, categoryId)

            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(activity: Activity, requestCode: Int, type: Int) {
            val intent = Intent(activity, EditCategoryActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)
            activity.startActivityForResult(intent, requestCode)
        }

        fun actionStart(fragment: Fragment, requestCode: Int, type: Int) {
            val intent = Intent(fragment.activity, EditCategoryActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_TYPE, type)
            fragment.startActivityForResult(intent, requestCode)
        }
    }
}
