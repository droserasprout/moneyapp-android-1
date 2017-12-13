package com.cactusteam.money.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.cactusteam.money.R
import com.cactusteam.money.iab.IabHelper
import com.cactusteam.money.iab.Purchase
import com.cactusteam.money.iab.SkuDetails
import com.cactusteam.money.ui.UiConstants


/**
 * @author vpotapenko
 */
class DonationFragment : BaseMainFragment() {

    val key: String by lazy { K1 + K2 + K3 }

    var contentView: View? = null
    var thanksView: View? = null

    var featureContainer: LinearLayout? = null
    var helper: IabHelper? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_donation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentView = view.findViewById(R.id.content)
        thanksView = view.findViewById(R.id.thanks)

        showContent()

        initializeProgress(view.findViewById(R.id.progress_bar), contentView!!)
        featureContainer = view.findViewById(R.id.features_container) as LinearLayout?

        loadData()
    }

    private fun showContent() {
        contentView?.visibility = View.VISIBLE
        thanksView?.visibility = View.GONE
    }

    private fun showThanks() {
        contentView?.visibility = View.GONE
        thanksView?.visibility = View.VISIBLE
    }

    private fun loadData() {
        showProgress()

        Log.d(TAG, "Creating IAB helper.")
        helper = IabHelper(activity, key)

        // enable debug logging (for a production application, you should set this to false).
        helper?.enableDebugLogging(true)

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.")
        helper?.startSetup(IabHelper.OnIabSetupFinishedListener { result ->
            Log.d(TAG, "Setup finished.")

            if (!result.isSuccess) {
                // Oh noes, there was a problem.
                complain("Problem setting up in-app billing: " + result)
                hideProgress()
                return@OnIabSetupFinishedListener
            }

            // Have we been disposed of in the meantime? If so, quit.
            if (helper == null) {
                hideProgress()
                return@OnIabSetupFinishedListener
            }

            // IAB is fully set up. Now, let's get an inventory of stuff we own.
            Log.d(TAG, "Setup successful. Querying inventory.")
            helper?.queryInventoryAsync(true, SKUS, gotInventoryListener)
        })
    }

    fun complain(message: String) {
        Log.e(TAG, "**** Donation Fragment Error: " + message)
        showError(message)
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    var gotInventoryListener: IabHelper.QueryInventoryFinishedListener = IabHelper.QueryInventoryFinishedListener { result, inventory ->
        Log.d(TAG, "Query inventory finished.")

        // Have we been disposed of in the meantime? If so, quit.
        if (helper == null) return@QueryInventoryFinishedListener

        // Is it a failure?
        if (result.isFailure) {
            complain("Failed to query inventory: " + result)
            hideProgress()
            return@QueryInventoryFinishedListener
        }

        Log.d(TAG, "Query inventory was successful.")

        featureContainer?.removeAllViews()
        SKUS.mapNotNull { inventory.getSkuDetails(it) }
                .forEach { createFeatureView(it) }

        hideProgress()
        Log.d(TAG, "Initial inventory query finished; enabling main UI.")
    }

    private fun createFeatureView(details: SkuDetails) {
        val view = View.inflate(activity, R.layout.fragment_donation_item, null)
        (view.findViewById(R.id.description) as TextView).text = details.description
        (view.findViewById(R.id.price) as TextView).text = details.price

        view.findViewById(R.id.list_item).setOnClickListener { buyItem(details) }

        featureContainer?.addView(view)
    }

    private fun buyItem(details: SkuDetails) {
        showProgress()

        Log.d(TAG, "Launching purchase flow for gas.")
        val payload = details.sku
        helper?.launchPurchaseFlow(activity, details.sku, UiConstants.IAB_REQUEST_CODE, purchaseFinishedListener, payload)
    }

    var purchaseFinishedListener: IabHelper.OnIabPurchaseFinishedListener = IabHelper.OnIabPurchaseFinishedListener { result, purchase ->
        Log.d(TAG, "Purchase finished: $result, purchase: $purchase")

        // if we were disposed of in the meantime, quit.
        if (helper == null) return@OnIabPurchaseFinishedListener

        if (result.isFailure) {
            complain("Error purchasing: " + result)
            hideProgress()
            return@OnIabPurchaseFinishedListener
        }
        if (!verifyDeveloperPayload(purchase)) {
            complain("Error purchasing. Authenticity verification failed.")
            hideProgress()
            return@OnIabPurchaseFinishedListener
        }

        Log.d(TAG, "Purchase successful.")

        Log.d(TAG, "Starting feature consumption.")
        helper?.consumeAsync(purchase, consumeFinishedListener)
    }

    // Called when consumption is complete
    var consumeFinishedListener: IabHelper.OnConsumeFinishedListener = IabHelper.OnConsumeFinishedListener { purchase, result ->
        Log.d(TAG, "Consumption finished. Purchase: $purchase, result: $result")

        // if we were disposed of in the meantime, quit.
        if (helper == null) return@OnConsumeFinishedListener

        if (result.isSuccess) {
            showThanks()
        } else {
            complain("Error while consuming: " + result)
        }

        Log.d(TAG, "End consumption flow.")
    }

    fun verifyDeveloperPayload(p: Purchase): Boolean {
        val payload = p.developerPayload
        return payload == p.sku
    }

    override fun dataChanged() {
        // do nothing
    }

    companion object {
        val TAG = "DonationFragment"

        val K1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg/GNXTsJ9+jqEYMCTDaQTDt5tNgocQYtp9OPDu7IRn1wdE2LU6"
        val K2 = "PiNk3CO9xndQjw69ZQLSR3PZpM6b8WG6ry2kyJIR9HGGIsUqSneaMM02HSQYZ4f1I5+BZexuobqvsoQVqPfs7RPJLuoPDY"
        val K3 = "uZmbPHJ24vCmv0RVA+QiQE9HOrkLemNkHZb0ensk88lVjYOdhlXhvYc4Z9LXMAdZ5xnpmInf08Ulbi5UBL3RJVq+Em4nb8nMz02ISzX4QWMFkha7Cwi5p6O+zAzy5QpYSw3O0apjixOx8xIRtqFhNM5jfktwcjN7eIAZZoy1dJxRs2CFtyxbMr175EC8Vtizbsbo7QIDAQAB"

        val SKUS = listOf(
                "feature_gd_sync",
                "feature_fin_goals",
                "feature_credits",
                "feature_deposits",
                "feature_ios",
                "feature_web",
                "donate"
        )

        fun build(): DonationFragment {
            return DonationFragment()
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        helper?.handleActivityResult(requestCode, resultCode, data)
    }
}