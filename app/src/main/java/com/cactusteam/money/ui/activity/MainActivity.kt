package com.cactusteam.money.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SlidingPaneLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.app.MoneyApp
import com.cactusteam.money.sync.ISyncListener
import com.cactusteam.money.sync.SyncService
import com.cactusteam.money.ui.MainSection
import com.cactusteam.money.ui.UiConstants
import com.cactusteam.money.ui.fragment.*
import java.util.*


class MainActivity : BaseActivity("MainActivity"), ISyncListener {

    private var initialSection: MainSection? = null

    private var drawerToggle: ActionBarDrawerToggle? = null
    private var drawerLayout: DrawerLayout? = null
    private var addTransactionButton: FloatingActionButton? = null

    private val listeners = HashSet<IChangeDataListener>()

    private var navigationView: NavigationView? = null
    private var partialMenu: MainPartialListFragment? = null

    private var syncDescriptionLayout: View? = null
    private var syncCaptionText: TextView? = null

    fun addChangeListener(l: IChangeDataListener) {
        listeners.add(l)
    }

    fun removeDataListener(l: IChangeDataListener) {
        listeners.remove(l)
    }

    private fun notifyDataChanged() {
        for (l in listeners) {
            l.dataChanged()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        updateData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.EDIT_TRANSACTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                updateData()
            }
        } else if (requestCode == UiConstants.BUDGET_PLAN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                updateData()
            }
        } else if (requestCode == UiConstants.DEBT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                updateData()
            }
        } else if (requestCode == UiConstants.IAB_REQUEST_CODE) {
            val fragment = fragmentManager.findFragmentByTag(MainSection.DONATION.name)
            if (fragment != null) (fragment as DonationFragment).handleActivityResult(requestCode, resultCode, data)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun updateData() {
        notifyDataChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectExtras()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addTransactionButton = findViewById(R.id.add_transaction_btb) as FloatingActionButton
        addTransactionButton!!.setOnClickListener { addTransactionClicked() }

        syncDescriptionLayout = findViewById(R.id.sync_desc_layout)
        syncCaptionText = findViewById(R.id.sync_caption) as TextView
        syncDescriptionLayout!!.visibility = View.GONE

        initializeDrawer()

        startSyncService()

        if (savedInstanceState == null) {
            val section = if (initialSection != null) initialSection else MainSection.HOME
            showSection(section)
        }
    }

    private fun injectExtras() {
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey(UiConstants.EXTRA_TYPE)) {
                initialSection = MainSection.find(extras.getString(UiConstants.EXTRA_TYPE))
            }
        }
    }

    fun startSyncService() {
        val syncManager = MoneyApp.instance.syncManager
        if (syncManager.isSyncConnected) {
            SyncService.actionStart(this)
            syncManager.eventsController.addListener(this)
        }
    }

    override fun onDestroy() {
        MoneyApp.instance.syncManager.eventsController.removeListener(this)
        super.onDestroy()
    }

    private fun addTransactionClicked() {
        NewTransactionActivity.ActionBuilder().start(this, UiConstants.EDIT_TRANSACTION_REQUEST_CODE)
    }

    private fun initializeDrawer() {
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout?
        if (isTablet) {
            partialMenu = fragmentManager.findFragmentById(R.id.partial_menu_list) as MainPartialListFragment

            val slidingPaneLayout = findViewById(R.id.sliding_pane_layout) as SlidingPaneLayout
            slidingPaneLayout.sliderFadeColor = Color.TRANSPARENT
            slidingPaneLayout.setShadowResourceLeft(R.drawable.left_panel_dropshadow)
            slidingPaneLayout.closePane()

            toolbar.setNavigationIcon(R.drawable.ic_action_navigation_menu)
            toolbar.setNavigationOnClickListener {
                if (slidingPaneLayout.isOpen) {
                    slidingPaneLayout.closePane()
                } else {
                    slidingPaneLayout.openPane()
                }
            }
        } else {
            drawerLayout!!.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
            drawerToggle = object : ActionBarDrawerToggle(this,
                    drawerLayout,
                    toolbar,
                    R.string.drawer_open,
                    R.string.drawer_close) {
                override fun onDrawerClosed(view: View?) {
                }

                override fun onDrawerOpened(drawerView: View?) {
                }
            }
            drawerToggle!!.isDrawerIndicatorEnabled = true
            drawerLayout!!.addDrawerListener(drawerToggle as ActionBarDrawerToggle)
        }

        navigationView = findViewById(R.id.navigation_view) as NavigationView
        navigationView!!.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> showSection(MainSection.HOME)
                R.id.accounts -> showSection(MainSection.ACCOUNTS)
                R.id.categories -> showSection(MainSection.CATEGORIES)
                R.id.tags -> showSection(MainSection.TAGS)
                R.id.transactions -> showSection(MainSection.TRANSACTIONS)
                R.id.budget -> showSection(MainSection.BUDGET)
                R.id.debts -> showSection(MainSection.DEBTS)
                R.id.reports -> showSection(MainSection.REPORTS)
                R.id.sync -> showSection(MainSection.SYNC)
                R.id.settings -> showSection(MainSection.SETTINGS)
                R.id.donation -> showSection(MainSection.DONATION)
            }

            menuItem.isChecked = true
            closeDrawer()
            true
        }
    }

    private val isTablet: Boolean
        get() = drawerLayout == null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isTablet) drawerToggle!!.onConfigurationChanged(newConfig)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!isTablet) drawerToggle!!.syncState()
    }

    override fun onBackPressed() {
        if (isDrawerOpen) {
            closeDrawer()
        } else if (fragmentManager.findFragmentByTag(MainSection.HOME.name) == null) {
            showSection(MainSection.HOME)
        } else {
            super.onBackPressed()
        }
    }

    private val isDrawerOpen: Boolean
        get() = drawerLayout != null && drawerLayout!!.isDrawerOpen(GravityCompat.START)

    private fun closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout!!.closeDrawer(GravityCompat.START)
        }
    }

    fun showSection(section: MainSection?) {
        if (section == null) return

        val fragmentManager = fragmentManager
        if (fragmentManager.findFragmentByTag(section.name) == null) {
            showSectionFragment(section)
            setTitle(section.titleResId)
        }
        navigationView!!.setCheckedItem(section.id)

        if (isTablet) {
            partialMenu!!.selectSection(section)
        } else {
            drawerLayout!!.closeDrawer(Gravity.START)
        }
    }

    private fun showSectionFragment(section: MainSection?) {
        when (section) {
            MainSection.HOME -> {
                showFragment(R.id.content_frame, HomeFragment.build(), section.name)
                showFab()
            }
            MainSection.ACCOUNTS -> {
                showFragment(R.id.content_frame, AccountsFragment.build(), section.name)
                showFab()
            }
            MainSection.CATEGORIES -> {
                showFragment(R.id.content_frame, CategoriesFragment.build(), section.name)
                showFab()
            }
            MainSection.TAGS -> {
                showFragment(R.id.content_frame, TagsFragment.build(), section.name)
                showFab()
            }
            MainSection.TRANSACTIONS -> {
                showFragment(R.id.content_frame, TransactionsFragment.build(), section.name)
                showFab()
            }
            MainSection.BUDGET -> {
                showFragment(R.id.content_frame, BudgetFragment.build(), section.name)
                hideFab()
            }
            MainSection.DEBTS -> {
                showFragment(R.id.content_frame, DebtsFragment.build(), section.name)
                hideFab()
            }
            MainSection.REPORTS -> {
                showFragment(R.id.content_frame, ReportsFragment.build(), section.name)
                hideFab()
            }
            MainSection.SYNC -> {
                showFragment(R.id.content_frame, SyncFragment.build(), section.name)
                hideFab()
            }
            MainSection.SETTINGS -> {
                showFragment(R.id.content_frame, SettingsFragment.build(), section.name)
                hideFab()
            }
            MainSection.DONATION -> {
                showFragment(R.id.content_frame, DonationFragment.build(), section.name)
                hideFab()
            }
        }
    }

    private fun hideFab() {
        addTransactionButton!!.postDelayed({ addTransactionButton!!.hide() }, 300)
    }

    private fun showFab() {
        addTransactionButton!!.postDelayed({ addTransactionButton!!.show() }, 500)
    }

    override fun syncStarted() {
        syncDescriptionLayout!!.visibility = View.VISIBLE
        syncCaptionText!!.setText(R.string.synchronizing)
    }

    override fun syncFinished() {
        syncDescriptionLayout!!.visibility = View.GONE
        updateData()
    }

    override fun onProgressUpdated(progress: Int, max: Int) {
        val message = getString(R.string.synchronizing) + " " + progress + "/" + max
        syncCaptionText!!.text = message
    }

    interface IChangeDataListener {

        fun dataChanged()
    }

    companion object {

        fun actionStart(context: Context, section: MainSection?) {
            context.startActivity(createIntent(context, section))
        }

        fun createIntent(context: Context, section: MainSection?): Intent {
            val intent = Intent(context, MainActivity::class.java)
            if (section != null) intent.putExtra(UiConstants.EXTRA_TYPE, section.name)

            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            return intent
        }
    }
}
