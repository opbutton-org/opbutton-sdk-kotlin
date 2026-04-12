package org.pilot.sdk

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object Pilot {
    const val VERSION = "1.0.34"

    @Volatile
    private var instance: PilotInstance? = null

    // ══════════════════════════════════════════════════════
    //  Static API
    // ══════════════════════════════════════════════════════

    @JvmStatic
    fun initialize(config: PilotConfig) = initialize(config, null)

    @JvmStatic
    fun initialize(config: PilotConfig, context: Context?) {
        if (instance != null) {
            PilotLog.w("Pilot.initialize() called more than once, ignoring")
            return
        }

        synchronized(this) {
            if (instance == null) {
                val p = PilotInstance(config, context)
                instance = p
                PilotLog.i("Pilot SDK initialized (server: %s)", config.baseUrl)

                config.sessionListener?.let { p.sessionListeners.add(it) }
                config.actionListener?.let { p.actionListeners.add(it) }

                val mc = config.metricConfig
                if (mc.isEnabled) {
                    p.metrics.addCollector(PilotDefaultMetricCollector(p.fpsTracker, context))
                    for (collector in mc.collectors) {
                        p.metrics.addCollector(collector)
                    }
                    PilotLog.i("Built-in metrics enabled (sample interval: %dms)", mc.sampleIntervalMs)
                }

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
    val status: PilotSessionStatus
        get() = instance?.status?.get() ?: PilotSessionStatus.DISCONNECTED

    @JvmStatic
    fun addActionListener(listener: PilotActionListener) {
        requireInstance().actionListeners.add(listener)
    }

    @JvmStatic
    fun removeActionListener(listener: PilotActionListener) {
        instance?.actionListeners?.remove(listener)
    }

    @JvmStatic
    fun addSessionListener(listener: PilotSessionListener) {
        requireInstance().sessionListeners.add(listener)
    }

    @JvmStatic
    fun removeSessionListener(listener: PilotSessionListener) {
        instance?.sessionListeners?.remove(listener)
    }

    @JvmStatic
    fun getUI(): PilotUI = requireInstance().ui

    @JvmStatic
    fun getMetrics(): PilotMetrics = requireInstance().metrics

    @JvmStatic
    fun connect() {
        requireInstance().startConnection()
    }

    @JvmStatic
    fun disconnect() {
        instance?.stopConnection()
    }

    @JvmStatic
    fun log(level: PilotLogLevel, message: String) {
        val p = instance ?: return
        if (!p.config.logConfig.isEnabled) return
        p.bufferLog(PilotLogEntry(level, message, null, null, null, p.resolveLogAttributes()))
    }

    @JvmStatic
    fun log(level: PilotLogLevel, message: String, category: String?, thread: String?) {
        val p = instance ?: return
        if (!p.config.logConfig.isEnabled) return
        p.bufferLog(PilotLogEntry(level, message, category, thread, null, p.resolveLogAttributes()))
    }

    @JvmStatic
    fun log(level: PilotLogLevel, message: String, metadata: Map<String, Any?>?) {
        val p = instance ?: return
        if (!p.config.logConfig.isEnabled) return
        p.bufferLog(PilotLogEntry(level, message, null, null, metadata, p.resolveLogAttributes()))
    }

    @JvmStatic
    fun log(level: PilotLogLevel, message: String, category: String?, thread: String?, metadata: Map<String, Any?>?) {
        val p = instance ?: return
        if (!p.config.logConfig.isEnabled) return
        p.bufferLog(PilotLogEntry(level, message, category, thread, metadata, p.resolveLogAttributes()))
    }

    @JvmStatic
    fun log(entry: PilotLogEntry) {
        val p = instance ?: return
        if (!p.config.logConfig.isEnabled) return
        p.bufferLog(entry)
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
            "pilot_command" to "change_screen",
            "pilot_slice_type" to "screen",
            "pilot_slice_name" to screenName,
            "screen_type" to screenType,
            "screen_name" to screenName
        )
        event("change_screen", "change_screen", metadata)
    }

    @JvmStatic
    fun setInAppProducts(products: List<Map<String, Any?>>) {
        val normalized = products.map { it?.let { LinkedHashMap(it) } ?: linkedMapOf() }
        val metadata = linkedMapOf<String, Any?>(
            "pilot_command" to "set_in_app_products",
            "pilot_purchase_entry_type" to "catalog",
            "in_app_products" to normalized,
            "in_app_product_count" to normalized.size
        )
        bufferStructuredLog("purchase", "set_in_app_products", "catalog", metadata, System.currentTimeMillis())
    }

    @JvmStatic
    fun setOwnedInAppProducts(productIds: List<String>) {
        val normalized = ArrayList(productIds)
        val metadata = linkedMapOf<String, Any?>(
            "pilot_command" to "set_owned_in_app_products",
            "pilot_purchase_entry_type" to "owned",
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

        purchaseMetadata["pilot_command"] = "purchase_in_app"
        purchaseMetadata["pilot_purchase_entry_type"] = "purchase"
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
        val p = instance ?: return
        val token = p.sessionToken.get() ?: return
        p.executor?.execute {
            try {
                p.httpClient.acknowledgeAction(token, actionId, ackPayload)
            } catch (e: PilotException) {
                PilotLog.e("Failed to acknowledge action", e)
            }
        }
    }

    @JvmStatic
    fun shutdown() {
        val p: PilotInstance?
        synchronized(this) {
            p = instance
            instance = null
        }
        p?.doShutdown()
    }

    // ══════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════

    private fun requireInstance(): PilotInstance =
        instance ?: throw IllegalStateException("Pilot.initialize() must be called first")

    private fun bufferStructuredLog(
        kind: String,
        message: String,
        category: String?,
        metadata: Map<String, Any?>?,
        clientTimestampMs: Long
    ) {
        val p = instance ?: return
        if (!p.config.logConfig.isEnabled) return

        val resolvedCategory = resolveStructuredCategory(kind, category)
        val resolvedMetadata = mergeStructuredMetadata(kind, metadata)

        p.bufferLog(
            PilotLogEntry(
                PilotLogLevel.INFO,
                message,
                resolvedCategory,
                null,
                resolvedMetadata,
                p.resolveLogAttributes(),
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
        merged["pilot_kind"] = kind
        return merged
    }
}

// ══════════════════════════════════════════════════════
//  Internal instance — holds all mutable state
// ══════════════════════════════════════════════════════

internal class PilotInstance(
    val config: PilotConfig,
    context: Context?
) {
    val httpClient = PilotHttpClient(config.baseUrl, config.apiToken)
    val sessionToken = AtomicReference<String>(null)
    val status = AtomicReference(PilotSessionStatus.DISCONNECTED)
    val ui = PilotUI()
    val metrics = PilotMetrics()
    val fpsTracker = PilotFpsTracker()
    val actionListeners = CopyOnWriteArrayList<PilotActionListener>()
    val sessionListeners = CopyOnWriteArrayList<PilotSessionListener>()

    private val requestId = AtomicReference<String>(null)
    private val running = AtomicBoolean(false)
    private val actionPollInFlight = AtomicBoolean(false)

    private val logBuffer: MutableList<PilotLogEntry> = Collections.synchronizedList(ArrayList())
    private val logOverflowWarned = AtomicBoolean(false)
    private val sessionAttributeCache = ConcurrentHashMap<String, Any>()

    var executor: ScheduledExecutorService? = null
        private set
    private var actionPollFuture: ScheduledFuture<*>? = null
    private var metricSampleFuture: ScheduledFuture<*>? = null

    private val liveManager: PilotLiveManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var currentActionPollIntervalMs: Long = config.actionPollIntervalMs

    init {
        liveManager = PilotLiveManager(context, httpClient, ::updateLiveMode)
        PilotLog.setLevel(config.logConfig.logLevel)
        PilotLog.setLoggerListener(config.loggerListener)

        val mc = config.metricConfig
        if (mc.isEnabled) {
            metrics.sampleIntervalMs = mc.sampleIntervalMs
            metrics.bufferSize = mc.bufferSize
            metrics.batchSize = mc.batchSize
        }
    }

    fun startConnection() {
        if (!running.compareAndSet(false, true)) {
            PilotLog.w("Already connecting/connected")
            return
        }

        executor = Executors.newScheduledThreadPool(3)
        executor!!.execute(::connectAndWaitApproval)
    }

    fun stopConnection() {
        if (!running.compareAndSet(true, false)) return

        liveManager.onSessionClosed()
        fpsTracker.stop()
        cancelScheduledTasks()

        val token = sessionToken.getAndSet(null)
        if (token != null && executor?.isShutdown == false) {
            executor?.execute {
                try {
                    flushLogs(token)
                    flushMetrics(token)
                    httpClient.closeSession(token)
                } catch (_: PilotException) {
                }
                setStatus(PilotSessionStatus.CLOSED)
                notifySessionClosed()
            }
        } else {
            setStatus(PilotSessionStatus.DISCONNECTED)
        }

        shutdownExecutor()
    }

    fun doShutdown() {
        PilotLog.i("Shutting down Pilot SDK")
        fpsTracker.stop()
        stopConnection()
        liveManager.shutdown()
        httpClient.shutdown()
        actionListeners.clear()
        sessionListeners.clear()
        logBuffer.clear()
        metrics.clear()
    }

    fun bufferLog(entry: PilotLogEntry) {
        synchronized(logBuffer) {
            if (logBuffer.size >= config.logConfig.bufferSize) {
                logBuffer.removeAt(0)
                if (logOverflowWarned.compareAndSet(false, true)) {
                    PilotLog.w("Log buffer overflow (${config.logConfig.bufferSize}), dropping oldest entries")
                }
            }
            logBuffer.add(entry)
        }
    }

    fun resolveLogAttributes(): JSONObject? {
        val builder = config.logConfig.attributes
        val staticAttrs = builder.staticAttributes
        val dynamicAttrs = builder.dynamicAttributes

        if (staticAttrs.isEmpty() && dynamicAttrs.isEmpty()) return null

        val attributes = JSONObject()
        for ((key, value) in staticAttrs) {
            try { attributes.put(key, value) } catch (_: Exception) {}
        }

        for ((key, provider) in dynamicAttrs) {
            try {
                val value = provider.getValue()
                attributes.put(key, value ?: JSONObject.NULL)
            } catch (_: Exception) {}
        }

        return if (attributes.length() == 0) null else attributes
    }

    // ── Connection logic ──

    private fun connectAndWaitApproval() {
        var retryDelayMs = 2000L
        val maxRetryDelayMs = 30000L
        var retryCount = 0

        while (running.get()) {
            try {
                doConnectAndWaitApproval()
                return
            } catch (e: PilotException) {
                when {
                    e.isUnauthorized -> {
                        PilotLog.e("Authentication failed", e)
                        setStatus(PilotSessionStatus.AUTH_FAILED)
                        running.set(false)
                        notifyAuthFailed()
                        return
                    }
                    !e.isNetworkError -> {
                        PilotLog.e("Server error, stopping connection: %s", e.message)
                        setStatus(PilotSessionStatus.ERROR)
                        running.set(false)
                        notifyError(e)
                        return
                    }
                    else -> {
                        retryCount++
                        PilotLog.w("Connection attempt %d failed (network): %s, retrying in %dms", retryCount, e.message, retryDelayMs)
                        setStatus(PilotSessionStatus.CONNECTING)

                        try {
                            Thread.sleep(retryDelayMs)
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            running.set(false)
                            return
                        }

                        retryDelayMs = (retryDelayMs * 2).coerceAtMost(maxRetryDelayMs)
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                running.set(false)
                return
            }
        }
    }

    @Throws(PilotException::class, InterruptedException::class)
    private fun doConnectAndWaitApproval() {
        setStatus(PilotSessionStatus.CONNECTING)
        notifyConnecting()

        val deviceId = config.deviceId.takeUnless { it.isNullOrEmpty() }
            ?: "${Build.BRAND}-${Build.MODEL}"
        val deviceName = config.deviceName.takeUnless { it.isNullOrEmpty() }
            ?: "${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

        val resp = httpClient.connect(deviceId, deviceName, resolveAllSessionAttributes())
        requestId.set(resp.requestId)

        PilotLog.i("Connect request sent, request_id=%s, status=%s", resp.requestId, resp.status)

        if (resp.isApproved && resp.sessionToken != null) {
            onApproved(resp.sessionToken)
            return
        }

        if (resp.isRejected) {
            onRejected()
            return
        }

        setStatus(PilotSessionStatus.WAITING_APPROVAL)
        notifyWaitingApproval(resp.requestId)

        while (running.get()) {
            Thread.sleep(config.pollIntervalMs)

            val pollResp = httpClient.pollStatus(resp.requestId)

            if (pollResp.isApproved && pollResp.sessionToken != null) {
                onApproved(pollResp.sessionToken)
                return
            }

            if (pollResp.isRejected) {
                onRejected()
                return
            }
        }
    }

    private fun onApproved(token: String) {
        sessionToken.set(token)
        setStatus(PilotSessionStatus.ACTIVE)

        PilotLog.i("Session approved and active")
        notifySessionStarted(token)

        if (ui.hasTabs()) {
            val snapshot = buildUISnapshotOnMainThread()
            if (snapshot != null) {
                try {
                    httpClient.submitPanel(token, snapshot)
                    ui.markSent()
                    PilotLog.d("Initial UI submitted (revision=%d)", ui.getRevision())
                } catch (e: PilotException) {
                    PilotLog.e("Failed to submit initial UI", e)
                    notifyError(e)
                }
            }
        }

        scheduleActionPolling(token, config.actionPollIntervalMs)

        if (config.metricConfig.isEnabled) {
            fpsTracker.start()
            val sampleMs = metrics.sampleIntervalMs
            metricSampleFuture = executor?.scheduleAtFixedRate(
                { metrics.sample() },
                sampleMs,
                sampleMs,
                TimeUnit.MILLISECONDS
            )
        }
    }

    private fun onRejected() {
        PilotLog.w("Connection request rejected")
        setStatus(PilotSessionStatus.REJECTED)
        running.set(false)
        notifyRejected()
    }

    // ── Action polling ──

    private fun doPollActions(token: String) {
        if (!running.get() || !actionPollInFlight.compareAndSet(false, true)) return

        try {
            val changedAttrs = resolveChangedSessionAttributes()
            val logChunk = drainLogChunk()
            val metricChunk = metrics.drain()

            val uiSnapshot = snapshotUIOnMainThread()

            if (uiSnapshot != null) {
                try {
                    httpClient.submitPanel(token, uiSnapshot)
                    ui.markSent()
                    PilotLog.d("UI submitted (revision=%d)", ui.getRevision())
                } catch (e: PilotException) {
                    PilotLog.e("Failed to submit UI", e)
                    notifyError(e)
                }
            }

            try {
                val json = httpClient.pollActions(token, changedAttrs, logChunk, metricChunk)
                val actionsArr = json.optJSONArray("actions")
                if (actionsArr != null && actionsArr.length() > 0) {
                    for (i in 0 until actionsArr.length()) {
                        val actionJson = actionsArr.optJSONObject(i) ?: continue
                        val action = PilotAction.fromJson(actionJson)
                        dispatchActionOnMainThread(action)
                    }
                }
                if (logChunk.isNotEmpty()) {
                    logOverflowWarned.set(false)
                }
            } catch (e: PilotException) {
                requeueLogs(logChunk)
                metrics.requeue(metricChunk)

                if (e.isNetworkError) {
                    PilotLog.w("Network error during action poll, will retry")
                } else {
                    PilotLog.w("Action poll failed with server error, attempting reconnect")
                    attemptReconnect(token)
                }
            }
        } finally {
            actionPollInFlight.set(false)
        }
    }

    private fun snapshotUIOnMainThread(): JSONObject? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ui.pollValues()
            return if (ui.hasUnsent()) ui.toJson() else null
        }

        val task = FutureTask {
            ui.pollValues()
            if (ui.hasUnsent()) ui.toJson() else null
        }
        mainHandler.post(task)

        return try {
            task.get()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: ExecutionException) {
            PilotLog.e("Failed to snapshot UI on main thread", e)
            null
        }
    }

    private fun buildUISnapshotOnMainThread(): JSONObject? {
        if (Looper.myLooper() == Looper.getMainLooper()) return ui.toJson()

        val task = FutureTask { ui.toJson() }
        mainHandler.post(task)

        return try {
            task.get()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: ExecutionException) {
            PilotLog.e("Failed to build UI snapshot on main thread", e)
            null
        }
    }

    private fun dispatchActionOnMainThread(action: PilotAction) {
        mainHandler.post {
            var handled = false
            try {
                handled = handleInternalAction(action)
                if (!handled) ui.dispatchAction(action)
            } catch (ex: Exception) {
                PilotLog.e("Widget callback threw exception", ex)
            }

            if (!handled) {
                for (listener in actionListeners) {
                    try {
                        listener.onPilotActionReceived(action)
                    } catch (ex: Exception) {
                        PilotLog.e("Action listener threw exception", ex)
                    }
                }
            }
        }
    }

    private fun scheduleActionPolling(token: String, intervalMs: Long) {
        val exec = executor ?: return
        if (exec.isShutdown || !running.get()) return

        actionPollFuture?.cancel(false)
        actionPollFuture = null

        currentActionPollIntervalMs = intervalMs
        actionPollFuture = exec.scheduleAtFixedRate(
            { doPollActions(token) },
            0,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
    }

    private fun updateLiveMode(enabled: Boolean, requestedPollIntervalMs: Long) {
        val token = sessionToken.get() ?: return
        val exec = executor ?: return
        if (exec.isShutdown) return

        val targetIntervalMs = if (enabled) {
            (if (requestedPollIntervalMs > 0) requestedPollIntervalMs else 500L)
                .coerceIn(200L, config.actionPollIntervalMs)
        } else {
            config.actionPollIntervalMs
        }

        if (targetIntervalMs == currentActionPollIntervalMs) return

        exec.execute { scheduleActionPolling(token, targetIntervalMs) }
    }

    private fun handleInternalAction(action: PilotAction): Boolean {
        when (action.actionType) {
            PilotActionType.LIVE_START -> {
                val token = sessionToken.get()
                if (token == null) {
                    Pilot.acknowledgeAction(action.id, buildInternalAck(false, "No active session available for streaming"))
                } else {
                    val actionId = action.id
                    val payload = action.payload
                    executor?.execute {
                        val result = liveManager.start(token, payload)
                        Pilot.acknowledgeAction(actionId, result)
                    }
                }
                return true
            }
            PilotActionType.LIVE_UPDATE -> {
                val token = sessionToken.get()
                if (token == null) {
                    Pilot.acknowledgeAction(action.id, buildInternalAck(false, "No active session available for streaming"))
                } else {
                    val actionId = action.id
                    val payload = action.payload
                    executor?.execute {
                        val result = liveManager.update(token, payload)
                        Pilot.acknowledgeAction(actionId, result)
                    }
                }
                return true
            }
            PilotActionType.LIVE_STOP -> {
                Pilot.acknowledgeAction(action.id, liveManager.stop())
                return true
            }
            PilotActionType.LIVE_TAP -> {
                Pilot.acknowledgeAction(action.id, liveManager.tap(action.payload))
                return true
            }
            PilotActionType.LIVE_LONG_PRESS -> {
                Pilot.acknowledgeAction(action.id, liveManager.longPress(action.payload))
                return true
            }
            else -> return false
        }
    }

    // ── Log buffering ──

    private fun drainLogChunk(): List<PilotLogEntry> {
        if (logBuffer.isEmpty()) return emptyList()

        synchronized(logBuffer) {
            val count = logBuffer.size.coerceAtMost(config.logConfig.batchSize)
            val chunk = ArrayList(logBuffer.subList(0, count))
            logBuffer.subList(0, count).clear()
            return chunk
        }
    }

    private fun requeueLogs(chunk: List<PilotLogEntry>) {
        if (chunk.isEmpty()) return

        synchronized(logBuffer) {
            logBuffer.addAll(0, chunk)
            while (logBuffer.size > config.logConfig.bufferSize) {
                logBuffer.removeAt(logBuffer.size - 1)
            }
        }
    }

    @Throws(PilotException::class)
    private fun flushLogs(token: String) {
        val chunk = drainLogChunk()
        if (chunk.isEmpty()) return

        try {
            httpClient.sendLogs(token, chunk)
        } catch (e: PilotException) {
            requeueLogs(chunk)
            throw e
        }
    }

    @Throws(PilotException::class)
    private fun flushMetrics(token: String) {
        val chunk = metrics.drain()
        if (chunk.isEmpty()) return

        try {
            httpClient.sendMetrics(token, chunk)
        } catch (e: PilotException) {
            metrics.requeue(chunk)
            throw e
        }
    }

    // ── Session attributes ──

    private fun resolveAllSessionAttributes(): Map<String, Any> {
        val builder = config.sessionAttributes
        val merged = ConcurrentHashMap(builder.staticAttributes)

        for ((key, provider) in builder.dynamicAttributes) {
            try {
                val value = provider.getValue()
                val resolved = value ?: JSONObject.NULL
                merged[key] = resolved
                sessionAttributeCache[key] = resolved
            } catch (e: Exception) {
                PilotLog.e("Session attribute provider failed: $key", e)
            }
        }

        return merged
    }

    private fun resolveChangedSessionAttributes(): Map<String, Any>? {
        val dynamicAttrs = config.sessionAttributes.dynamicAttributes
        if (dynamicAttrs.isEmpty()) return null

        var changed: MutableMap<String, Any>? = null

        for ((key, provider) in dynamicAttrs) {
            try {
                val value = provider.getValue()
                val resolved = value ?: JSONObject.NULL
                val cached = sessionAttributeCache[key]

                if (resolved != cached) {
                    sessionAttributeCache[key] = resolved
                    if (changed == null) changed = ConcurrentHashMap()
                    changed[key] = resolved
                }
            } catch (e: Exception) {
                PilotLog.e("Session attribute provider failed: $key", e)
            }
        }

        return changed
    }

    // ── Reconnection ──

    private fun attemptReconnect(token: String) {
        cancelScheduledTasks()
        setStatus(PilotSessionStatus.CONNECTING)

        var retryDelay = 2000L
        val maxDelay = 30000L

        while (running.get()) {
            try {
                Thread.sleep(retryDelay)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }

            if (!running.get()) return

            try {
                httpClient.pollActions(token, null, emptyList(), emptyList())

                PilotLog.i("Reconnect successful, session still active")
                setStatus(PilotSessionStatus.ACTIVE)
                scheduleActionPolling(token, currentActionPollIntervalMs)
                return
            } catch (e: PilotException) {
                if (e.isNetworkError) {
                    PilotLog.w("Reconnect: no network, retrying in %dms", retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(maxDelay)
                    continue
                }

                PilotLog.w("Session lost (server error), starting fresh connection")
                resetAndRestart()
                return
            }
        }
    }

    private fun resetAndRestart() {
        running.set(false)
        sessionToken.set(null)
        liveManager.onSessionClosed()
        setStatus(PilotSessionStatus.DISCONNECTED)
        notifySessionClosed()
        startConnection()
    }

    private fun setStatus(newStatus: PilotSessionStatus) {
        status.set(newStatus)
    }

    private fun cancelScheduledTasks() {
        actionPollFuture?.cancel(false)
        actionPollFuture = null
        metricSampleFuture?.cancel(false)
        metricSampleFuture = null
    }

    private fun shutdownExecutor() {
        executor?.takeIf { !it.isShutdown }?.shutdown()
    }

    // ── Listener notifications ──

    private fun notifyConnecting() = sessionListeners.forEach { it.onPilotSessionConnecting() }
    private fun notifyWaitingApproval(reqId: String) = sessionListeners.forEach { it.onPilotSessionWaitingApproval(reqId) }
    private fun notifySessionStarted(token: String) = sessionListeners.forEach { it.onPilotSessionStarted(token) }
    private fun notifySessionClosed() = sessionListeners.forEach { it.onPilotSessionClosed() }
    private fun notifyRejected() = sessionListeners.forEach { it.onPilotSessionRejected() }
    private fun notifyAuthFailed() = sessionListeners.forEach { it.onPilotSessionAuthFailed() }
    private fun notifyError(exception: PilotException) = sessionListeners.forEach { it.onPilotSessionError(exception) }

    companion object {
        private fun buildInternalAck(ok: Boolean, status: String) = JSONObject().apply {
            put("ok", ok)
            put("status", status)
        }
    }
}
