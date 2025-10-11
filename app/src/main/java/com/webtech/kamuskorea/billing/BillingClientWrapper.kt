package com.webtech.kamuskorea.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillingClientWrapper(
    private val context: Context,
    private val onPurchaseVerified: (purchaseToken: String, productId: String) -> Unit,
    private val productId: String = "1probulanan"
) {
    // TAG untuk mempermudah filter di Logcat
    private val TAG = "BillingClientWrapper"

    private lateinit var billingClient: BillingClient
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _hasActiveSubscription = MutableStateFlow(false)
    val hasActiveSubscription = _hasActiveSubscription.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            Log.e(TAG, "Purchase error. Response code: ${billingResult.responseCode}, Message: ${billingResult.debugMessage}")
        }
    }

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished. Response code: ${billingResult.responseCode}, Message: ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Client setup successful. Querying products...")
                    queryProductDetails()
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing Client setup failed!")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected. Retrying...")
                initialize()
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        coroutineScope.launch {
            val result = billingClient.queryProductDetails(params)
            Log.d(TAG, "queryProductDetails finished. Response code: ${result.billingResult.responseCode}, Message: ${result.billingResult.debugMessage}")
            result.productDetailsList?.let {
                Log.d(TAG, "Found ${it.size} product(s).")
            }
            result.productDetailsList?.firstOrNull()?.let { productDetails ->
                _productDetails.update { productDetails }
                Log.d(TAG, "Product details updated successfully for ${productDetails.name}")
            } ?: run {
                Log.w(TAG, "No product details found for ID: $productId")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val productDetails = _productDetails.value ?: return
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Panggil callback untuk memulai verifikasi server
                        onPurchaseVerified(purchase.purchaseToken, purchase.products.first())
                    }
                }
            }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var hasSub = false
                for (purchase in purchases) {
                    if (purchase.isAcknowledged && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        hasSub = true
                        break
                    }
                }
                _hasActiveSubscription.update { hasSub }
            }
        }
    }
}