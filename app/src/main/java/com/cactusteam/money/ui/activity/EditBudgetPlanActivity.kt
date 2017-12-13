package com.cactusteam.money.ui.activity

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Pair
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.dao.BudgetPlan
import com.cactusteam.money.data.dao.Category
import com.cactusteam.money.data.dao.Subcategory
import com.cactusteam.money.data.dao.Tag
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.UiUtils
import com.cactusteam.money.ui.fragment.SelectPlanDependenciesFragment
import java.util.*

/**
 * @author vpotapenko
 */
class EditBudgetPlanActivity : BaseDataActivity("EditBudgetPlanActivity") {

    private var planId: Long = -1
    private var copy: Boolean = false

    private var mainCurrencyCode: String? = null
    private var limit: Double = 0.toDouble()
    private val from = Calendar.getInstance()
    private val to = Calendar.getInstance()

    private val categories = TreeSet(CATEGORY_COMPARATOR)
    private val subcategories = TreeSet(SUBCATEGORY_COMPARATOR)
    private val tags = TreeSet(TAG_COMPARATOR)

    private var nameView: TextView? = null
    private var errorNameView: TextView? = null
    private var errorDependenciesView: TextView? = null
    private var fromView: TextView? = null
    private var toView: TextView? = null
    private var limitView: TextView? = null
    private var errorLimitView: TextView? = null

    private var typeSpinner: Spinner? = null

    private var dependenciesContainer: LinearLayout? = null

    private var fromDateContainer: View? = null
    private var toDateContainer: View? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.activity_edit_budget_plan, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.delete).isVisible = planId >= 0

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.delete) {
            deletePlan()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deletePlan() {
        AlertDialog.Builder(this).setTitle(R.string.continue_question).setMessage(R.string.budget_plan_will_be_deleted).setPositiveButton(android.R.string.yes) { dialog, which -> removePlan() }.setNegativeButton(android.R.string.no, null).show()
    }

    private fun removePlan() {
        showBlockingProgress(getString(R.string.waiting))
        val s = dataManager.budgetService.deleteBudget(planId)
                .subscribe(
                        {},
                        { e ->
                            hideBlockingProgress()
                            showError(e.message)
                        },
                        {
                            hideBlockingProgress()
                            Toast.makeText(this@EditBudgetPlanActivity, R.string.budget_plan_was_deleted, Toast.LENGTH_SHORT).show()

                            val data = Intent()
                            data.putExtra(UiConstants.EXTRA_DELETED, true)

                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }
                )
        compositeSubscription.add(s)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.CALCULATOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                limit = Math.abs(data.getDoubleExtra(UiConstants.EXTRA_AMOUNT, 0.0))
                updateLimitView()
                clearErrors()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_budget_plan)

        initializeToolbar()
        setTitle(if (planId >= 0 && !copy) R.string.edit_plan_title else R.string.new_plan_title)

        initializeViewProgress()

        findViewById(R.id.add_dependency_btn).setOnClickListener { showDependencyDialog() }

        nameView = findViewById(R.id.name) as TextView
        nameView!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // do nothing
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // do nothing
            }

            override fun afterTextChanged(s: Editable) {
                if (errorNameView!!.visibility == View.VISIBLE) clearErrors()
            }
        })
        errorNameView = findViewById(R.id.name_error) as TextView

        val adapter = TypeAdapter(this)
        val types = resources.getStringArray(R.array.budget_types)
        val descriptions = resources.getStringArray(R.array.budget_types_description)
        types.indices.forEach { adapter.add(Pair(types[it], descriptions[it])) }
        typeSpinner = findViewById(R.id.type_spinner) as Spinner
        typeSpinner!!.adapter = adapter
        typeSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateOnChangeType()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // do nothing
            }
        }

        dependenciesContainer = findViewById(R.id.dependencies_container) as LinearLayout
        dependenciesContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))

        errorDependenciesView = findViewById(R.id.dependencies_error) as TextView

        fromView = findViewById(R.id.from_date) as TextView
        fromDateContainer = findViewById(R.id.from_date_container)
        fromDateContainer!!.setOnClickListener { fromDateClicked() }

        toView = findViewById(R.id.to_date) as TextView
        toDateContainer = findViewById(R.id.to_date_container)
        toDateContainer!!.setOnClickListener { toDateClicked() }

        limitView = findViewById(R.id.amount) as TextView
        errorLimitView = findViewById(R.id.amount_error) as TextView
        findViewById(R.id.amount_container).setOnClickListener { amountClicked() }

        findViewById(R.id.save_btn).setOnClickListener { saveClicked() }

        val moneyApp = MoneyApp.instance
        mainCurrencyCode = moneyApp.appPreferences.mainCurrencyCode

        switchToFullPeriodDates()

        if (planId < 0) {
            updateLimitView()
            typeSpinner!!.setSelection(1)
        } else {
            loadPlan()
        }
    }

    private fun switchToFullPeriodDates() {
        val period = application.period
        val fullCurrent = period.fullCurrent

        from.time = fullCurrent.first
        updateFromDateView()

        to.time = fullCurrent.second
        updateToDateView()
    }

    private fun updateOnChangeType() {
        val itemPosition = typeSpinner!!.selectedItemPosition
        if (itemPosition == 0) {
            fromDateContainer!!.visibility = View.VISIBLE
            toDateContainer!!.visibility = View.VISIBLE
        } else if (itemPosition == 1) {
            fromDateContainer!!.visibility = View.GONE
            toDateContainer!!.visibility = View.GONE

            switchToFullPeriodDates()
        }

    }

    private fun loadPlan() {
        showProgress()
        val s = dataManager.budgetService.getBudget(planId)
                .subscribe(
                        { r ->
                            hideProgress()
                            planLoaded(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun planLoaded(plan: BudgetPlan) {
        if (copy) {
            planId = -1 // it's a copy, so clear source plan id
        } else {
            from.time = plan.start
            updateFromDateView()

            to.time = plan.finish
            updateToDateView()
        }

        limit = plan.limit
        updateLimitView()

        typeSpinner!!.setSelection(plan.type)

        for (dependency in plan.dependencies) {
            val obj = plan.dependencyObjects[dependency]
            if (dependency.refType == DataConstants.CATEGORY_TYPE) {
                addCategoryDependency(obj as Category)
            } else if (dependency.refType == DataConstants.SUBCATEGORY_TYPE) {
                addSubcategoryDependency(obj as Subcategory)
            } else if (dependency.refType == DataConstants.TAG_TYPE) {
                addTagDependency(obj as Tag)
            }
        }
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_ID)) {
                planId = extras.getLong(UiConstants.EXTRA_ID)
            }
            if (extras.containsKey(UiConstants.EXTRA_COPY)) {
                copy = extras.getBoolean(UiConstants.EXTRA_COPY)
            }
        }
    }

    private fun saveClicked() {
        clearErrors()
        if (!isValid) return

        if (planId < 0) {
            createPlan()
        } else {
            updatePlan()
        }
    }

    private fun updatePlan() {
        val name = nameView!!.text.toString()
        showProgress()

        val b = dataManager.budgetService
                .newBudgetBuilder()
                .putId(planId)
                .putName(name)
                .putLimit(limit)
                .putFrom(from.time)
                .putTo(to.time)
                .putType(typeSpinner!!.selectedItemPosition)

        for (category in categories) {
            b.putDependency(DataConstants.CATEGORY_TYPE, category.id.toString())
        }

        for (subcategory in subcategories) {
            b.putDependency(DataConstants.SUBCATEGORY_TYPE, subcategory.id.toString())
        }

        for (tag in tags) {
            b.putDependency(DataConstants.TAG_TYPE, tag.id.toString())
        }

        val s = b.update()
                .subscribe(
                        { r ->
                            hideProgress()
                            planUpdated(r)
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun planUpdated(budgetPlan: BudgetPlan?) {
        Toast.makeText(this, R.string.budget_plan_was_saved, Toast.LENGTH_SHORT).show()

        val data = Intent()
        data.putExtra(UiConstants.EXTRA_NAME, budgetPlan?.name)

        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun createPlan() {
        showProgress()
        val b = dataManager.budgetService.newBudgetBuilder()
        b.putName(nameView!!.text.toString())
                .putLimit(limit)
                .putFrom(from.time)
                .putTo(to.time)
                .putType(typeSpinner!!.selectedItemPosition)

        for (category in categories) {
            b.putDependency(DataConstants.CATEGORY_TYPE, category.id.toString())
        }

        for (subcategory in subcategories) {
            b.putDependency(DataConstants.SUBCATEGORY_TYPE, subcategory.id.toString())
        }

        for (tag in tags) {
            b.putDependency(DataConstants.TAG_TYPE, tag.id.toString())
        }

        val s = b.create()
                .subscribe(
                        { r ->
                            hideProgress()
                            planCreated()
                        },
                        { e ->
                            hideProgress()
                            showError(e.message)
                        }
                )
        compositeSubscription.add(s)
    }

    private fun planCreated() {
        Toast.makeText(this@EditBudgetPlanActivity, R.string.budget_plan_was_saved, Toast.LENGTH_SHORT).show()
        hideProgress()

        if (copy) {
            val data = Intent()
            data.putExtra(UiConstants.EXTRA_COPY, true)
            setResult(Activity.RESULT_OK, data)
        } else {
            setResult(Activity.RESULT_OK)
        }

        finish()
    }

    private fun clearErrors() {
        errorDependenciesView!!.visibility = View.GONE
        errorNameView!!.visibility = View.GONE
        errorLimitView!!.visibility = View.GONE
    }

    private val isValid: Boolean
        get() {
            if (!hasDependencies()) {
                errorDependenciesView!!.setText(R.string.at_least_one_dependency_is_required)
                errorDependenciesView!!.visibility = View.VISIBLE
                return false
            }

            if (limit <= 0) {
                errorLimitView!!.setText(R.string.amount_must_be_more_than_zero)
                errorLimitView!!.visibility = View.VISIBLE
                return false
            }

            val name = nameView!!.text
            if (name.isNullOrBlank()) {
                errorNameView!!.setText(R.string.plan_name_is_required)
                errorNameView!!.visibility = View.VISIBLE
                return false
            }

            return true
        }

    private fun hasDependencies(): Boolean {
        return !categories.isEmpty() || !subcategories.isEmpty() || !tags.isEmpty()
    }

    private fun amountClicked() {
        CalculatorActivity.actionStart(this, UiConstants.CALCULATOR_REQUEST_CODE, limit)
    }

    private fun updateLimitView() {
        val amountStr = UiUtils.formatCurrency(limit, mainCurrencyCode)
        limitView!!.text = amountStr
    }

    private fun toDateClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            to.set(year, monthOfYear, dayOfMonth)
            to.set(Calendar.HOUR_OF_DAY, 23)
            to.set(Calendar.MINUTE, 59)
            to.set(Calendar.SECOND, 59)
            to.set(Calendar.MILLISECOND, 999)
            updateToDateView()
        }, to.get(Calendar.YEAR), to.get(Calendar.MONTH), to.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateToDateView() {
        toView!!.text = DateFormat.getDateFormat(this).format(to.time)
    }

    private fun fromDateClicked() {
        val datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            from.set(year, monthOfYear, dayOfMonth)
            from.set(Calendar.HOUR_OF_DAY, 0)
            from.clear(Calendar.MINUTE)
            from.clear(Calendar.SECOND)
            from.clear(Calendar.MILLISECOND)
            updateFromDateView()
        }, from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun updateFromDateView() {
        fromView!!.text = DateFormat.getDateFormat(this).format(from.time)
    }

    private fun showDependencyDialog() {
        val fragment = SelectPlanDependenciesFragment.build(object : SelectPlanDependenciesFragment.PlanDependenciesListener {
            override fun categorySelected(category: Category) {
                if (categories.contains(category)) return

                addCategoryDependency(category)
            }

            override fun subcategorySelected(subcategory: Subcategory) {
                if (subcategories.contains(subcategory)) return

                addSubcategoryDependency(subcategory)
            }

            override fun tagSelected(tag: Tag) {
                if (tags.contains(tag)) return

                addTagDependency(tag)
            }
        })
        fragment.show(fragmentManager, "dialog")
    }

    private fun addTagDependency(tag: Tag) {
        clearDependenciesContainer()
        tags.add(tag)
        val view = View.inflate(this@EditBudgetPlanActivity, R.layout.activity_edit_budget_plan_dependency, null)

        val s = UiUtils.formatDataObject(this@EditBudgetPlanActivity, DataConstants.TAG_TYPE, tag)
        (view.findViewById(R.id.name) as TextView).text = s

        if (nameView!!.text.isNullOrBlank()) nameView!!.text = tag.name

        view.findViewById(R.id.list_item).setOnClickListener {
            tags.remove(tag)
            removeFromDependenciesContainer(view)
        }
        dependenciesContainer!!.addView(view)

        clearErrors()
    }

    private fun clearDependenciesContainer() {
        if (!hasDependencies()) dependenciesContainer!!.removeAllViews()
    }

    private fun addSubcategoryDependency(subcategory: Subcategory) {
        clearDependenciesContainer()
        subcategories.add(subcategory)
        val view = View.inflate(this@EditBudgetPlanActivity, R.layout.activity_edit_budget_plan_dependency, null)

        val s = UiUtils.formatDataObject(this@EditBudgetPlanActivity, DataConstants.SUBCATEGORY_TYPE, subcategory)
        (view.findViewById(R.id.name) as TextView).text = s

        if (nameView!!.text.isNullOrBlank())
            nameView!!.text = subcategory.name

        view.findViewById(R.id.list_item).setOnClickListener {
            subcategories.remove(subcategory)
            removeFromDependenciesContainer(view)
        }
        dependenciesContainer!!.addView(view)

        clearErrors()
    }

    private fun removeFromDependenciesContainer(view: View) {
        dependenciesContainer!!.removeView(view)
        if (!hasDependencies()) {
            dependenciesContainer!!.addView(View.inflate(this, R.layout.view_no_data, null))
        }
    }

    private fun addCategoryDependency(category: Category) {
        clearDependenciesContainer()
        categories.add(category)

        val view = View.inflate(this@EditBudgetPlanActivity, R.layout.activity_edit_budget_plan_dependency, null)

        val s = UiUtils.formatDataObject(this@EditBudgetPlanActivity, DataConstants.CATEGORY_TYPE, category)
        (view.findViewById(R.id.name) as TextView).text = s

        if (nameView!!.text.isNullOrBlank()) nameView!!.text = category.name

        view.findViewById(R.id.list_item).setOnClickListener {
            categories.remove(category)
            removeFromDependenciesContainer(view)
        }
        dependenciesContainer!!.addView(view)

        clearErrors()
    }

    private class TypeAdapter(context: Context) : ArrayAdapter<Pair<String, String>>(context, 0) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getView(position, convertView, parent, R.layout.activity_edit_budget_plan_type_item)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = getView(position, convertView, parent, R.layout.activity_edit_budget_plan_type_item_dp)

            val item = getItem(position)
            (view.findViewById(R.id.description) as TextView).text = item!!.second

            return view
        }

        private fun getView(position: Int, convertView: View?, parent: ViewGroup, layoutId: Int): View {
            val view = convertView ?: View.inflate(parent.context, layoutId, null)

            val item = getItem(position)
            (view.findViewById(R.id.name) as TextView).text = item!!.first

            return view
        }
    }

    companion object {

        private val CATEGORY_COMPARATOR = Comparator<com.cactusteam.money.data.dao.Category> { lhs, rhs -> lhs.name.compareTo(rhs.name) }

        private val SUBCATEGORY_COMPARATOR = Comparator<com.cactusteam.money.data.dao.Subcategory> { lhs, rhs -> lhs.name.compareTo(rhs.name) }

        private val TAG_COMPARATOR = Comparator<com.cactusteam.money.data.dao.Tag> { lhs, rhs -> lhs.name.compareTo(rhs.name) }

        fun actionStart(fragment: Fragment, requestCode: Int) {
            val intent = Intent(fragment.activity, EditBudgetPlanActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }

        fun actionStart(activity: Activity, requestCode: Int, planId: Long, copy: Boolean) {
            val intent = Intent(activity, EditBudgetPlanActivity::class.java)
            intent.putExtra(UiConstants.EXTRA_ID, planId)
            intent.putExtra(UiConstants.EXTRA_COPY, copy)

            activity.startActivityForResult(intent, requestCode)
        }
    }
}
