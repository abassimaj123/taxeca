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

    /** Initialised to false; FreemiumViewModel will seed from cached value on first launch. */
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    /** Allows external callers to pre-seed the premium state from persisted cache. */
    fun seedPremium(cached: Boolean) {
        if (_isPremium.value) return   // already confirmed by BillingClient — don't downgrade
        _isPremium.value = cached
    }

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

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    private fun connect(delayMs: Long = 0L) {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.w(TAG, "BillingClient: max reconnect attempts reached — giving up")
            return
        }
        scope.launch {
            if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "BillingClient connected (attempt ${reconnectAttempts + 1})")
                        reconnectAttempts = 0 // reset on success
                        scope.launch {
                            queryExistingPurchases()
                            queryProductPrice()
                        }
                    }
                }
                override fun onBillingServiceDisconnected() {
                    reconnectAttempts++
                    val backoffMs = minOf(1000L * (1 shl reconnectAttempts), 30_000L) // 2s, 4s, 8s … 30s cap
                    Log.w(TAG, "BillingClient disconnected — retry #$reconnectAttempts in ${backoffMs}ms")
                    connect(backoffMs)
                }
            })
        }
    }

    private suspend fun queryProductPrice(attempt: Int = 1) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ))
            .build()
        try {
            val result = billingClient.queryProductDetails(params)
            val price = result.productDetailsList
                ?.firstOrNull()
                ?.oneTimePurchaseOfferDetails
                ?.formattedPrice
            if (price != null) {
                _premiumPrice.value = price
                return  // ✅ success
            }
        } catch (e: Exception) {
            Log.w(TAG, "Price fetch error (attempt $attempt): ${e.message}")
        }
        // Retry with backoff: 5s → 20s → 45s (max 3 attempts)
        if (attempt < 3) {
            val delayMs = 5000L * attempt * attempt
            Log.d(TAG, "Retrying price fetch in ${delayMs / 1000}s…")
            kotlinx.coroutines.delay(delayMs)
            queryProductPrice(attempt + 1)
        }
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
            // LATCH-UP: only ever promote to premium here, never downgrade.
            // A routine reconnect query can return OK + empty list from a cold/offline
            // Play Store cache for a user who genuinely owns premium. Writing `false`
            // would persist a bad downgrade (FreemiumViewModel caches every emission) and
            // irreversibly trim a paying user's history (HistoryRepository). Refund
            // revocation is handled server-side: a refunded user simply won't re-promote
            // on a fresh install. See audit 2026-06-12.
            if (hasPremium) _isPremium.value = true
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
        // Already owned locally — don't open a flow that would just return
        // ITEM_ALREADY_OWNED; grant immediately.
        if (_isPremium.value) { onSuccess(); return }

        _onPurchaseSuccess = onSuccess
        _onPurchaseError = onError

        scope.launch {
            // Ensure BillingClient is connected before attempting purchase
            if (!billingClient.isReady) {
                Log.w(TAG, "BillingClient not ready — reconnecting…")
                val connected = suspendConnect()
                if (!connected) {
                    Log.e(TAG, "BillingClient reconnect failed")
                    onError("Billing service unavailable")
                    return@launch
                }
            }

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
            Log.d(TAG, "queryProductDetails responseCode=${detailsResult.billingResult.responseCode}, " +
                    "debugMessage=${detailsResult.billingResult.debugMessage}, " +
                    "products=${detailsResult.productDetailsList?.size ?: 0}")
            val productDetails = detailsResult.productDetailsList?.firstOrNull()
            if (productDetails == null) {
                Log.e(TAG, "Product '$PRODUCT_ID' not found — check Play Console product status")
                onError("Product not found")
                return@launch
            }
            Log.d(TAG, "Launching billing flow for: ${productDetails.productId}, " +
                    "price=${productDetails.oneTimePurchaseOfferDetails?.formattedPrice}")
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
                val result = billingClient.launchBillingFlow(activity, billingFlowParams)
                Log.d(TAG, "launchBillingFlow responseCode=${result.responseCode}, " +
                        "debugMessage=${result.debugMessage}")
                // If the flow did NOT open (responseCode != OK), onPurchasesUpdated will
                // never fire — resolve the pending callbacks here or the UI hangs forever.
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    when (result.responseCode) {
                        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                            unlockAlreadyOwned()
                        BillingClient.BillingResponseCode.USER_CANCELED -> {
                            _onPurchaseError?.invoke("cancelled"); clearCallbacks()
                        }
                        else -> {
                            _onPurchaseError?.invoke(result.debugMessage); clearCallbacks()
                        }
                    }
                }
            }
        }
    }

    /** Marks premium, syncs/acknowledges in the background, and fires the success callback. */
    private fun unlockAlreadyOwned() {
        _isPremium.value = true
        scope.launch { queryExistingPurchases() } // pulls the token + acknowledges if needed
        _onPurchaseSuccess?.invoke()
        clearCallbacks()
    }

    private fun clearCallbacks() {
        _onPurchaseSuccess = null
        _onPurchaseError = null
    }

    /** Suspending connect helper — returns true if connected successfully. */
    private suspend fun suspendConnect(): Boolean {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                    // Reset the background-reconnect budget so a transient 5-disconnect
                    // burst doesn't permanently disable auto price/purchase refresh.
                    if (ok) reconnectAttempts = 0
                    if (cont.isActive) cont.resume(ok) {}
                }
                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) {
                        cont.resume(false) {}
                    }
                }
            })
        }
    }

    fun restorePurchases(onSuccess: () -> Unit, onNone: () -> Unit) {
        scope.launch {
            // Reconnect if the client dropped (e.g. after maxReconnectAttempts was hit),
            // otherwise queryPurchasesAsync returns a non-OK code and a real owner is
            // wrongly told "no purchase found".
            if (!billingClient.isReady) {
                val connected = suspendConnect()
                if (!connected) { onNone(); return@launch }
            }
            queryExistingPurchases()
            if (_isPremium.value) onSuccess() else onNone()
        }
    }

    private fun acknowledgePurchase(purchase: Purchase, attempt: Int = 1) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        scope.launch {
            val result = billingClient.acknowledgePurchase(params)
            // An unacknowledged purchase is auto-refunded by Google after 3 days.
            // Retry transient failures instead of relying solely on the next-launch sweep.
            if (result.responseCode != BillingClient.BillingResponseCode.OK && attempt < 4) {
                Log.w(TAG, "Acknowledge failed (attempt $attempt): ${result.debugMessage} — retrying")
                kotlinx.coroutines.delay(2000L * attempt)
                acknowledgePurchase(purchase, attempt + 1)
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (!purchase.products.contains(PRODUCT_ID)) return@forEach
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            _isPremium.value = true
                            if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
                            _onPurchaseSuccess?.invoke()
                            clearCallbacks()
                        }
                        Purchase.PurchaseState.PENDING -> {
                            // Payment deferred (cash, parental approval). Don't leave the UI
                            // spinning — inform the user; entitlement is granted later via
                            // queryExistingPurchases once it clears to PURCHASED.
                            Log.d(TAG, "Purchase pending — awaiting completion")
                            _onPurchaseError?.invoke("pending")
                            clearCallbacks()
                        }
                        else -> { /* UNSPECIFIED — no action */ }
                    }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // User already owns premium (other device, or local state lost).
                // This is NOT an error — sync entitlement and grant access.
                Log.d(TAG, "Item already owned — syncing entitlement")
                unlockAlreadyOwned()
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
                _onPurchaseError?.invoke("cancelled")
                clearCallbacks()
            }
            else -> {
                Log.d(TAG, "Purchase error: ${result.debugMessage}")
                _onPurchaseError?.invoke(result.debugMessage)
                clearCallbacks()
            }
        }
    }
}
