package com.webtech.learningkorea.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.webtech.learningkorea.data.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BillingClientDebug"

@Singleton
class BillingClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) {
    /**
     * PENTING: Product ID harus sama dengan yang dibuat di Google Play Console
     *
     * Base Plan: langganan_bulanan_pro
     * Subscription Offers:
     *   - subscription-bulanan (Monthly)
     *   - subscription6bln (6 months)
     *   - subscription1thn (Yearly)
     */
    private val productId: String = "langganan_bulanan_pro"

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _hasActiveSubscription = MutableStateFlow(false)
    val hasActiveSubscription = _hasActiveSubscription.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.d(TAG, "=== PURCHASES UPDATED LISTENER ===")
        Log.d(TAG, "Response Code: ${billingResult.responseCode}")
        Log.d(TAG, "Debug Message: ${billingResult.debugMessage}")

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "✅ Pembelian BERHASIL!")
                if (purchases != null && purchases.isNotEmpty()) {
                    Log.d(TAG, "Jumlah purchases: ${purchases.size}")
                    for (purchase in purchases) {
                        Log.d(TAG, "Processing purchase: ${purchase.orderId}")
                        Log.d(TAG, "Purchase state: ${purchase.purchaseState}")
                        Log.d(TAG, "Products: ${purchase.products}")
                        handlePurchase(purchase)
                    }
                } else {
                    Log.w(TAG, "⚠️ Purchases list is null or empty!")
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "❌ User membatalkan pembelian")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "⚠️ Item sudah dimiliki, refresh status")
                checkSubscriptionStatus()
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                Log.e(TAG, "❌ Billing tidak tersedia di device ini")
            }
            else -> {
                Log.e(TAG, "❌ Error pembelian: ${billingResult.debugMessage}")
            }
        }
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        Log.d(TAG, "BillingClientWrapper diinisialisasi. Memulai koneksi...")
        startConnection()
    }

    private fun startConnection() {
        if (billingClient.isReady) {
            Log.d(TAG, "Koneksi sudah siap, tidak perlu memulai lagi.")
            queryProductDetails()
            checkSubscriptionStatus()
            return
        }

        Log.d(TAG, "Memulai koneksi ke Google Play Billing Service...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "=== BILLING SETUP FINISHED ===")
                Log.d(TAG, "Response Code: ${billingResult.responseCode}")
                Log.d(TAG, "Debug Message: ${billingResult.debugMessage}")

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Koneksi SUKSES. Memuat detail produk.")
                    queryProductDetails()
                    checkSubscriptionStatus()
                } else {
                    Log.e(TAG, "❌ Koneksi GAGAL. Response Code: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ Layanan terputus. Mencoba menghubungkan kembali...")
                startConnection()
            }
        })
    }

    private fun queryProductDetails() {
        Log.d(TAG, "=== QUERY PRODUCT DETAILS ===")
        Log.d(TAG, "Product ID: '$productId'")

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            Log.d(TAG, "=== PRODUCT DETAILS RESULT ===")
            Log.d(TAG, "Response Code: ${billingResult.responseCode}")
            Log.d(TAG, "Debug Message: ${billingResult.debugMessage}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                if (productDetailsList.isNotEmpty()) {
                    _productDetails.value = productDetailsList[0]
                    Log.d(TAG, "✅ SUKSES: Detail produk ditemukan")
                    Log.d(TAG, "Product Name: ${productDetailsList[0].name}")
                    Log.d(TAG, "Product Description: ${productDetailsList[0].description}")

                    val offers = productDetailsList[0].subscriptionOfferDetails
                    if (offers != null && offers.isNotEmpty()) {
                        Log.d(TAG, "Offers available: ${offers.size}")
                        offers.forEach { offer ->
                            Log.d(TAG, "Offer token: ${offer.offerToken}")
                            offer.pricingPhases.pricingPhaseList.forEach { phase ->
                                Log.d(TAG, "Price: ${phase.formattedPrice}")
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "❌ GAGAL: Product list KOSONG untuk ID '$productId'")
                    Log.e(TAG, "Pastikan Product ID sudah dibuat di Google Play Console!")
                }
            } else {
                Log.e(TAG, "❌ GAGAL: Query produk gagal")
                logBillingError(billingResult.responseCode)
            }
        }
    }

    fun checkSubscriptionStatus() {
        Log.d(TAG, "=== CHECKING SUBSCRIPTION STATUS ===")

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            Log.d(TAG, "=== QUERY PURCHASES RESULT ===")
            Log.d(TAG, "Response Code: ${billingResult.responseCode}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Total purchases: ${purchases.size}")

                val hasSub = purchases.any { purchase ->
                    val hasProduct = purchase.products.contains(productId)
                    val isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED

                    Log.d(TAG, "Purchase check:")
                    Log.d(TAG, "  - Order ID: ${purchase.orderId}")
                    Log.d(TAG, "  - Products: ${purchase.products}")
                    Log.d(TAG, "  - Has our product: $hasProduct")
                    Log.d(TAG, "  - State: ${purchase.purchaseState}")
                    Log.d(TAG, "  - Is purchased: $isPurchased")
                    Log.d(TAG, "  - Is acknowledged: ${purchase.isAcknowledged}")

                    hasProduct && isPurchased
                }

                _hasActiveSubscription.value = hasSub
                Log.d(TAG, if (hasSub) "✅ User MEMILIKI langganan aktif" else "❌ User TIDAK memiliki langganan")

                // Sinkronkan dengan backend
                scope.launch {
                    Log.d(TAG, "Memanggil userRepository.checkPremiumStatus()...")
                    userRepository.checkPremiumStatus()
                }
            } else {
                Log.e(TAG, "❌ Error memeriksa purchases")
                logBillingError(billingResult.responseCode)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "=== HANDLING PURCHASE ===")
        Log.d(TAG, "Order ID: ${purchase.orderId}")
        Log.d(TAG, "Purchase Token: ${purchase.purchaseToken}")
        Log.d(TAG, "Purchase State: ${purchase.purchaseState}")
        Log.d(TAG, "Is Acknowledged: ${purchase.isAcknowledged}")
        Log.d(TAG, "Products: ${purchase.products}")

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                Log.d(TAG, "⚠️ Purchase belum di-acknowledge, proses acknowledge...")

                // PERBAIKAN: Kirim ke backend SEBELUM acknowledge
                sendPurchaseToBackend(purchase)

                // Kemudian acknowledge
                acknowledgePurchase(purchase)
            } else {
                Log.d(TAG, "✅ Purchase sudah di-acknowledge sebelumnya")

                // Tetap kirim ke backend untuk memastikan sinkronisasi
                sendPurchaseToBackend(purchase)

                // Refresh status
                checkSubscriptionStatus()
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "⏳ Purchase sedang PENDING (menunggu pembayaran)")
        } else {
            Log.d(TAG, "❓ Purchase state tidak dikenal: ${purchase.purchaseState}")
        }
    }

    private fun sendPurchaseToBackend(purchase: Purchase) {
        Log.d(TAG, "=== SENDING PURCHASE TO BACKEND ===")

        scope.launch {
            try {
                Log.d(TAG, "Calling userRepository.activatePremium()...")
                Log.d(TAG, "Purchase Token: ${purchase.purchaseToken}")

                // PERBAIKAN: Tambahkan fungsi aktivasi premium di UserRepository
                userRepository.activatePremiumWithPurchase(purchase.purchaseToken)

                Log.d(TAG, "✅ Backend activation request sent successfully")

                // Setelah backend diupdate, refresh status lokal
                checkSubscriptionStatus()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error mengirim purchase ke backend: ${e.message}", e)
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        Log.d(TAG, "=== ACKNOWLEDGING PURCHASE ===")

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            Log.d(TAG, "=== ACKNOWLEDGE RESULT ===")
            Log.d(TAG, "Response Code: ${billingResult.responseCode}")
            Log.d(TAG, "Debug Message: ${billingResult.debugMessage}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✅ Acknowledge SUKSES")
                checkSubscriptionStatus()
            } else {
                Log.e(TAG, "❌ Acknowledge GAGAL")
                logBillingError(billingResult.responseCode)
            }
        }
    }

    fun launchPurchaseFlow(
        productDetails: ProductDetails,
        activity: Activity,
        selectedOfferToken: String? = null
    ) {
        Log.d(TAG, "=== LAUNCHING PURCHASE FLOW ===")

        // Gunakan offer token yang dipilih user, atau ambil yang pertama sebagai fallback
        val offerToken = selectedOfferToken
            ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken

        if (offerToken == null) {
            Log.e(TAG, "❌ Gagal: Tidak ada offer token!")
            return
        }

        Log.d(TAG, "Product ID: ${productDetails.productId}")
        Log.d(TAG, "Offer Token: $offerToken")

        val paramsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(paramsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, flowParams)

        Log.d(TAG, "Launch result - Response Code: ${billingResult.responseCode}")
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "❌ Error launching billing flow")
            logBillingError(billingResult.responseCode)
        } else {
            Log.d(TAG, "✅ Billing flow launched successfully")
        }
    }

    private fun logBillingError(responseCode: Int) {
        val errorMessage = when (responseCode) {
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> "Service timeout"
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "Feature not supported"
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "Service disconnected"
            BillingClient.BillingResponseCode.USER_CANCELED -> "User canceled"
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Service unavailable"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Billing unavailable"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "Item unavailable"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Developer error (check configuration)"
            BillingClient.BillingResponseCode.ERROR -> "General error"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "Item already owned"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "Item not owned"
            else -> "Unknown error code: $responseCode"
        }
        Log.e(TAG, "Billing Error: $errorMessage")
    }

    fun endConnection() {
        Log.d(TAG, "=== ENDING BILLING CONNECTION ===")
        billingClient.endConnection()
    }
}