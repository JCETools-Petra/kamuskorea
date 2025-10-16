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

// TAG untuk filter di Logcat
private const val TAG = "BillingClientDebug"

@Singleton
class BillingClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) {
    // Ganti dengan ID langganan yang kamu buat di Google Play Console
    private val productId: String = "langganan_pro_bulanan"

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _hasActiveSubscription = MutableStateFlow(false)
    val hasActiveSubscription = _hasActiveSubscription.asStateFlow()

    // Gunakan CoroutineScope internal agar tidak bergantung dari luar
    private val scope = CoroutineScope(Dispatchers.IO)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.d(TAG, "purchasesUpdatedListener dipanggil. Kode: ${billingResult.responseCode}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }
        checkSubscriptionStatus()
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
            return
        }
        Log.d(TAG, "Memulai koneksi ke Google Play Billing Service...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished. Kode Respons: ${billingResult.responseCode} - Pesan: ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Koneksi SUKSES. Memuat detail produk.")
                    queryProductDetails()
                    checkSubscriptionStatus()
                } else {
                    Log.e(TAG, "Koneksi GAGAL.")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Layanan terputus. Mencoba menghubungkan kembali...")
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
                    Log.e(TAG, "GAGAL: Query berhasil, tetapi daftar produk KOSONG. Tidak ada produk dengan ID '$productId' yang ditemukan di Play Console untuk aplikasi ini.")
                }
            } else {
                Log.e(TAG, "GAGAL: Query produk gagal. Ini sering terjadi jika akun penguji belum di-setup dengan benar.")
            }
        }
    }

    fun checkSubscriptionStatus() {
        Log.d(TAG, "Memeriksa status langganan yang sudah ada...")
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            val hasSub = purchases.any { it.products.contains(productId) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            _hasActiveSubscription.value = hasSub

            // **INTEGRASI DENGAN USER REPOSITORY**
            // Update status premium di UserRepository agar seluruh aplikasi tahu
            scope.launch {
                userRepository.updatePremiumStatus(hasSub)
            }
            Log.d(TAG, "Pengecekan selesai. Pengguna memiliki langganan aktif: $hasSub")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            Log.d(TAG, "Pembelian di-acknowledge. Kode: ${billingResult.responseCode}")
            // Setelah acknowledge, cek kembali status langganan untuk memastikan semuanya terupdate
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                checkSubscriptionStatus()
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
        val flowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(paramsList).build()
        billingClient.launchBillingFlow(activity, flowParams)
    }
}