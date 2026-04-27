package com.taxeca.calculator.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IAPManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID = "premium_upgrade"
        private const val TAG = "IAPManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val _premiumPrice = MutableStateFlow<String?>(null)
    val premiumPrice: StateFlow<String?> = _premiumPrice

    private var _onPurchaseSuccess: (() -> Unit)? = null
    private var _onPurchaseError: ((String) -> Unit)? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient connected")
                    scope.launch {
                        queryExistingPurchases()
                        queryProductPrice()
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "BillingClient disconnected — retrying")
                connect()
            }
        })
    }

    private suspend fun queryProductPrice() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ))
            .build()
        val result = billingClient.queryProductDetails(params)
        result.productDetailsList
            ?.firstOrNull()
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice
            ?.let { _premiumPrice.value = it }
    }

    private suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val hasPremium = result.purchasesList.any { purchase ->
                purchase.products.contains(PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            _isPremium.value = hasPremium
            result.purchasesList
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                .forEach { acknowledgePurchase(it) }
        }
    }

    fun launchPurchase(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _onPurchaseSuccess = onSuccess
        _onPurchaseError = onError

        scope.launch {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
            val detailsResult = billingClient.queryProductDetails(params)
            val productDetails = detailsResult.productDetailsList?.firstOrNull()
            if (productDetails == null) {
                onError("Product not found")
                return@launch
            }
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()
            // Must launch on main thread
            scope.launch(Dispatchers.Main) {
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    fun restorePurchases(onSuccess: () -> Unit, onNone: () -> Unit) {
        scope.launch {
            queryExistingPurchases()
            if (_isPremium.value) onSuccess() else onNone()
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        scope.launch {
            billingClient.acknowledgePurchase(params)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.contains(PRODUCT_ID)) {
                        _isPremium.value = true
                        if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
                        _onPurchaseSuccess?.invoke()
                        _onPurchaseSuccess = null
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
                _onPurchaseError?.invoke("cancelled")
                _onPurchaseError = null
            }
            else -> {
                Log.d(TAG, "Purchase error: ${result.debugMessage}")
                _onPurchaseError?.invoke(result.debugMessage)
                _onPurchaseError = null
            }
        }
    }
}
