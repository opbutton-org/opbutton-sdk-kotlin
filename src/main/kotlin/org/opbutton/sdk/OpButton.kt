package org.opbutton.sdk

import android.content.Context
import org.json.JSONObject

object OpButton {
    const val VERSION = "1.0.37"

    @Volatile
    private var instance: OpButtonRuntime? = null

    // ══════════════════════════════════════════════════════
    //  Static API
    // ══════════════════════════════════════════════════════

    @JvmStatic
    fun initialize(config: OpButtonConfig) = initialize(config, null)

    @JvmStatic
    fun initialize(config: OpButtonConfig, context: Context?) {
        if (instance != null) {
            OpButtonLog.w("OpButton.initialize() called more than once, ignoring")
            return
        }

        synchronized(this) {
            if (instance == null) {
                val p = OpButtonRuntime(config, context)
                instance = p
                OpButtonLog.i("OpButton SDK initialized (server: %s)", config.baseUrl)
                p.logConfiguration()

                if (config.autoConnect) {
                    p.startConnection()
                }
            }
        }
    }

    @JvmStatic
    val isInitialized: Boolean
        get() = instance != null

    @JvmStatic
    val status: OpButtonSessionStatus
        get() = instance?.currentStatus() ?: OpButtonSessionStatus.DISCONNECTED

    @JvmStatic
    fun addActionListener(listener: OpButtonActionListener) {
        requireInstance().addActionListener(listener)
    }

    @JvmStatic
    fun removeActionListener(listener: OpButtonActionListener) {
        instance?.removeActionListener(listener)
    }

    @JvmStatic
    fun addSessionListener(listener: OpButtonSessionListener) {
        requireInstance().addSessionListener(listener)
    }

    @JvmStatic
    fun removeSessionListener(listener: OpButtonSessionListener) {
        instance?.removeSessionListener(listener)
    }

    @JvmStatic
    fun getUI(): OpButtonUI = requireInstance().ui

    @JvmStatic
    fun getMetrics(): OpButtonMetrics = requireInstance().metrics

    @JvmStatic
    fun connect() {
        requireInstance().startConnection()
    }

    @JvmStatic
    fun disconnect() {
        instance?.stopConnection()
    }

    @JvmStatic
    fun log(level: OpButtonLogLevel, message: String) {
        val p = instance ?: return
        if (!p.isLoggingEnabled) return
        p.telemetry.bufferLog(OpButtonLogEntry(level, message, null, null, null, p.telemetry.resolveLogAttributes()))
    }

    @JvmStatic
    fun log(level: OpButtonLogLevel, message: String, category: String?, thread: String?) {
        val p = instance ?: return
        if (!p.isLoggingEnabled) return
        p.telemetry.bufferLog(OpButtonLogEntry(level, message, category, thread, null, p.telemetry.resolveLogAttributes()))
    }

    @JvmStatic
    fun log(level: OpButtonLogLevel, message: String, metadata: Map<String, Any?>?) {
        val p = instance ?: return
        if (!p.isLoggingEnabled) return
        p.telemetry.bufferLog(OpButtonLogEntry(level, message, null, null, metadata, p.telemetry.resolveLogAttributes()))
    }

    @JvmStatic
    fun log(level: OpButtonLogLevel, message: String, category: String?, thread: String?, metadata: Map<String, Any?>?) {
        val p = instance ?: return
        if (!p.isLoggingEnabled) return
        p.telemetry.bufferLog(OpButtonLogEntry(level, message, category, thread, metadata, p.telemetry.resolveLogAttributes()))
    }

    @JvmStatic
    fun log(entry: OpButtonLogEntry) {
        val p = instance ?: return
        if (!p.isLoggingEnabled) return
        p.telemetry.bufferLog(entry)
    }

    @JvmStatic
    fun event(message: String) = event(message, null, null, System.currentTimeMillis())

    @JvmStatic
    fun event(message: String, metadata: Map<String, Any?>?) = event(message, null, metadata, System.currentTimeMillis())

    @JvmStatic
    fun event(message: String, metadata: Map<String, Any?>?, clientTimestampMs: Long) = event(message, null, metadata, clientTimestampMs)

    @JvmStatic
    fun event(message: String, category: String?, metadata: Map<String, Any?>?) = event(message, category, metadata, System.currentTimeMillis())

    @JvmStatic
    fun event(message: String, category: String?, metadata: Map<String, Any?>?, clientTimestampMs: Long) {
        bufferStructuredLog("event", message, category, metadata, clientTimestampMs)
    }

    @JvmStatic
    fun revenue(message: String) = revenue(message, null, null, System.currentTimeMillis())

    @JvmStatic
    fun revenue(message: String, metadata: Map<String, Any?>?) = revenue(message, null, metadata, System.currentTimeMillis())

    @JvmStatic
    fun revenue(message: String, metadata: Map<String, Any?>?, clientTimestampMs: Long) = revenue(message, null, metadata, clientTimestampMs)

    @JvmStatic
    fun revenue(message: String, category: String?, metadata: Map<String, Any?>?) = revenue(message, category, metadata, System.currentTimeMillis())

    @JvmStatic
    fun revenue(message: String, category: String?, metadata: Map<String, Any?>?, clientTimestampMs: Long) {
        bufferStructuredLog("revenue", message, category, metadata, clientTimestampMs)
    }

    @JvmStatic
    fun changeScreen(screenType: String, screenName: String) {
        val metadata = linkedMapOf<String, Any?>(
            "opbutton_command" to "change_screen",
            "opbutton_slice_type" to "screen",
            "opbutton_slice_name" to screenName,
            "screen_type" to screenType,
            "screen_name" to screenName
        )
        event("change_screen", "change_screen", metadata)
    }

    @JvmStatic
    fun setInAppProducts(products: List<Map<String, Any?>>) {
        val normalized = products.map { LinkedHashMap(it) }
        val metadata = linkedMapOf<String, Any?>(
            "opbutton_command" to "set_in_app_products",
            "opbutton_purchase_entry_type" to "catalog",
            "in_app_products" to normalized,
            "in_app_product_count" to normalized.size
        )
        bufferStructuredLog("purchase", "set_in_app_products", "catalog", metadata, System.currentTimeMillis())
    }

    @JvmStatic
    fun setOwnedInAppProducts(productIds: List<String>) {
        val normalized = ArrayList(productIds)
        val metadata = linkedMapOf<String, Any?>(
            "opbutton_command" to "set_owned_in_app_products",
            "opbutton_purchase_entry_type" to "owned",
            "owned_in_app_products" to normalized,
            "owned_in_app_product_count" to normalized.size
        )
        bufferStructuredLog("purchase", "set_owned_in_app_products", "owned", metadata, System.currentTimeMillis())
    }

    @JvmStatic
    fun purchaseInApp(transactionId: String?, productIds: List<String>, metadata: Map<String, Any?>?) {
        val normalized = ArrayList(productIds)
        val purchaseMetadata = linkedMapOf<String, Any?>()
        if (!metadata.isNullOrEmpty()) purchaseMetadata.putAll(metadata)

        purchaseMetadata["opbutton_command"] = "purchase_in_app"
        purchaseMetadata["opbutton_purchase_entry_type"] = "purchase"
        purchaseMetadata["in_app_products"] = normalized
        purchaseMetadata["in_app_product_count"] = normalized.size

        if (!transactionId.isNullOrEmpty()) {
            purchaseMetadata["transaction_id"] = transactionId
        }

        val message = if (normalized.isEmpty()) "purchase_in_app" else normalized[0]
        bufferStructuredLog("purchase", message, "purchase", purchaseMetadata, System.currentTimeMillis())
    }

    @JvmStatic
    fun acknowledgeAction(actionId: String, ackPayload: JSONObject?) {
        instance?.acknowledgeAction(actionId, ackPayload)
    }

    @JvmStatic
    fun shutdown() {
        val p: OpButtonRuntime?
        synchronized(this) {
            p = instance
            instance = null
        }
        p?.doShutdown()
    }

    // ══════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════

    private fun requireInstance(): OpButtonRuntime =
        instance ?: throw IllegalStateException("OpButton.initialize() must be called first")

    private fun bufferStructuredLog(
        kind: String,
        message: String,
        category: String?,
        metadata: Map<String, Any?>?,
        clientTimestampMs: Long
    ) {
        val p = instance ?: return
        if (!p.isLoggingEnabled) return

        val resolvedCategory = resolveStructuredCategory(kind, category)
        val resolvedMetadata = mergeStructuredMetadata(kind, metadata)

        p.telemetry.bufferLog(
            OpButtonLogEntry(
                OpButtonLogLevel.INFO,
                message,
                resolvedCategory,
                null,
                resolvedMetadata,
                p.telemetry.resolveLogAttributes(),
                clientTimestampMs
            )
        )
    }

    private fun resolveStructuredCategory(kind: String, category: String?): String {
        if (category.isNullOrEmpty()) return kind
        if (category == kind || category.startsWith("${kind}_")) return category
        return "${kind}_$category"
    }

    private fun mergeStructuredMetadata(kind: String, metadata: Map<String, Any?>?): Map<String, Any?> {
        val merged = linkedMapOf<String, Any?>()
        if (!metadata.isNullOrEmpty()) merged.putAll(metadata)
        merged["opbutton_kind"] = kind
        return merged
    }
}
