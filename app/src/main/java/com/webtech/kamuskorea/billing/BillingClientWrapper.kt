package com.webtech.kamuskorea.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.webtech.kamuskorea.data.UserRepository
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
     * Cara membuat Product ID:
     * 1. Buka Google Play Console
     * 2. Pilih aplikasi Anda
     * 3. Monetization > Subscriptions
     * 4. Create subscription
     * 5. Masukkan Product ID (contoh: "langganan_pro_bulanan")
     * 6. Set harga dan periode billing
     * 7. Aktifkan subscription
     *
     * Untuk testing:
     * - Tambahkan email tester di Google Play Console > Setup > License testing
     * - Gunakan test card atau account yang sudah ditambahkan
     *
     * GANTI NILAI DI BAWAH INI DENGAN PRODUCT ID YANG ANDA BUAT!
     */
    private val productId: String = "langganan_pro_bulanan" // TODO: Ganti dengan Product ID Anda

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _hasActiveSubscription = MutableStateFlow(false)
    val hasActiveSubscription = _hasActiveSubscription.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.d(TAG, "purchasesUpdatedListener dipanggil. Kode: ${billingResult.responseCode}")

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User membatalkan pembelian")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item sudah dimiliki")
                checkSubscriptionStatus()
            }
            else -> {
                Log.e(TAG, "Error pembelian: ${billingResult.debugMessage}")
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
                Log.d(TAG, "onBillingSetupFinished. Kode: ${billingResult.responseCode} - Pesan: ${billingResult.debugMessage}")

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Koneksi SUKSES. Memuat detail produk.")
                    queryProductDetails()
                    checkSubscriptionStatus()
                } else {
                    Log.e(TAG, "Koneksi GAGAL. Kode: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Layanan terputus. Mencoba menghubungkan kembali...")
                // Retry connection with exponential backoff bisa ditambahkan di sini
                startConnection()
            }
        })
    }

    private fun queryProductDetails() {
        Log.d(TAG, "--- Memulai Query Produk ---")
        Log.d(TAG, "ID Produk yang dicari: '$productId'")

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
            Log.d(TAG, "--- Hasil Query Produk ---")
            Log.d(TAG, "Kode Respons: ${billingResult.responseCode} - Pesan: ${billingResult.debugMessage}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                if (productDetailsList.isNotEmpty()) {
                    _productDetails.value = productDetailsList[0]
                    Log.d(TAG, "SUKSES: Detail produk ditemukan untuk '$productId'. Nama: ${productDetailsList[0].name}")
                } else {
                    Log.e(TAG, "GAGAL: Daftar produk KOSONG. Product ID '$productId' tidak ditemukan.")
                    Log.e(TAG, "Pastikan Product ID sudah dibuat di Google Play Console.")
                }
            } else {
                Log.e(TAG, "GAGAL: Query produk gagal.")
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                        Log.e(TAG, "Billing tidak tersedia. Periksa Google Play Services.")
                    }
                    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                        Log.e(TAG, "Fitur tidak didukung di device ini.")
                    }
                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                        Log.e(TAG, "Service tidak tersedia. Periksa koneksi internet.")
                    }
                    BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                        Log.e(TAG, "Developer error. Periksa konfigurasi di Play Console.")
                    }
                }
            }
        }
    }

    fun checkSubscriptionStatus() {
        Log.d(TAG, "Memeriksa status langganan yang sudah ada...")

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasSub = purchases.any {
                    it.products.contains(productId) &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _hasActiveSubscription.value = hasSub

                Log.d(TAG, "Status langganan: ${if (hasSub) "AKTIF" else "TIDAK AKTIF"}")

                // Sinkronkan dengan backend
                scope.launch {
                    userRepository.checkPremiumStatus()
                }
            } else {
                Log.e(TAG, "Error memeriksa purchases: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            } else {
                Log.d(TAG, "Purchase sudah di-acknowledge sebelumnya")
                checkSubscriptionStatus()
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase sedang pending")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            Log.d(TAG, "Pembelian di-acknowledge. Kode: ${billingResult.responseCode}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Acknowledge sukses, refresh status")
                checkSubscriptionStatus()
            } else {
                Log.e(TAG, "Acknowledge gagal: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(productDetails: ProductDetails, activity: Activity) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken

        if (offerToken == null) {
            Log.e(TAG, "Gagal memulai pembelian: Tidak ada offer token ditemukan.")
            return
        }

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

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Error launching billing flow: ${billingResult.debugMessage}")
        }
    }

    /**
     * Call this when the app is being destroyed
     */
    fun endConnection() {
        Log.d(TAG, "Menutup koneksi billing")
        billingClient.endConnection()
    }
}