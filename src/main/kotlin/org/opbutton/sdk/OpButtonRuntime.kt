package org.opbutton.sdk

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Coordinates one initialized SDK instance behind the public facade. */
internal class OpButtonRuntime(
    private val config: OpButtonConfig,
    context: Context?
) {
    private val httpClient = OpButtonHttpClient(config.baseUrl, config.apiToken)
    private val sessionToken = AtomicReference<String>(null)
    private val status = AtomicReference(OpButtonSessionStatus.DISCONNECTED)
    val ui = OpButtonUI()
    val metrics = OpButtonMetrics()
    private val fpsTracker = OpButtonFpsTracker()
    private val actionListeners = CopyOnWriteArrayList<OpButtonActionListener>()
    private val sessionListeners = CopyOnWriteArrayList<OpButtonSessionListener>()

    private val requestId = AtomicReference<String>(null)
    private val running = AtomicBoolean(false)
    private val actionPollInFlight = AtomicBoolean(false)

    val telemetry: OpButtonTelemetry
    private val hostState: OpButtonHostState
    private val delivery: OpButtonPollDelivery
    private val deviceIdentity = OpButtonDeviceIdentity.forContext(context)
    private val credentialStore = OpButtonDeviceCredentialStore(context)

    private var executor: ScheduledExecutorService? = null
    private var actionPollFuture: ScheduledFuture<*>? = null
    private var metricSampleFuture: ScheduledFuture<*>? = null

    private val liveManager: OpButtonLiveManager
    private val actions: OpButtonActionRouter
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var currentActionPollIntervalMs: Long = config.actionPollIntervalMs

    init {
        telemetry = OpButtonTelemetry(config.logConfig, httpClient, metrics)
        hostState = OpButtonHostState(config.sessionAttributes, ui, mainHandler)
        delivery = OpButtonPollDelivery(httpClient, hostState, telemetry, ::isCurrentSession, ::notifyError)
        liveManager = OpButtonLiveManager(context, httpClient, ::updateLiveMode)
        val liveCommands = OpButtonLiveCommands(liveManager, ::executeBackground, ::isCurrentSession, ::acknowledgeForSession)
        actions = OpButtonActionRouter(liveCommands.commands(), { ui.dispatchAction(it) }, { actionListeners.toList() })
        OpButtonLog.setLevel(config.logConfig.logLevel)
        OpButtonLog.setLoggerListener(config.loggerListener)

        val mc = config.metricConfig
        if (mc.isEnabled) {
            metrics.sampleIntervalMs = mc.sampleIntervalMs
            metrics.bufferSize = mc.bufferSize
            metrics.batchSize = mc.batchSize
        }
        config.sessionListener?.let { sessionListeners.add(it) }
        config.actionListener?.let { actionListeners.add(it) }

        if (mc.isEnabled) {
            metrics.addCollector(OpButtonDefaultMetricCollector(fpsTracker, context))
            for (collector in mc.collectors) {
                metrics.addCollector(collector)
            }
        }

    }

    fun logConfiguration() {
        if (config.metricConfig.isEnabled)
            OpButtonLog.i("Built-in metrics enabled (sample interval: %dms)", config.metricConfig.sampleIntervalMs)
    }

    val isLoggingEnabled: Boolean get() = config.logConfig.isEnabled
    fun currentStatus(): OpButtonSessionStatus = status.get()
    fun addActionListener(listener: OpButtonActionListener) { actionListeners.add(listener) }
    fun removeActionListener(listener: OpButtonActionListener) { actionListeners.remove(listener) }
    fun addSessionListener(listener: OpButtonSessionListener) { sessionListeners.add(listener) }
    fun removeSessionListener(listener: OpButtonSessionListener) { sessionListeners.remove(listener) }

    fun startConnection() {
        if (!running.compareAndSet(false, true)) {
            OpButtonLog.w("Already connecting/connected")
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
                    telemetry.flushLogs(token)
                    telemetry.flushMetrics(token)
                    httpClient.closeSession(token)
                } catch (_: OpButtonException) {
                }
                setStatus(OpButtonSessionStatus.CLOSED)
                notifySessionClosed()
            }
        } else {
            setStatus(OpButtonSessionStatus.DISCONNECTED)
        }

        shutdownExecutor()
    }

    fun doShutdown() {
        OpButtonLog.i("Shutting down OpButton SDK")
        fpsTracker.stop()
        stopConnection()
        liveManager.shutdown()
        httpClient.shutdown()
        actionListeners.clear()
        sessionListeners.clear()
        telemetry.clear()
        metrics.clear()
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
            } catch (e: OpButtonException) {
                when {
                    e.isUnauthorized -> {
                        OpButtonLog.e("Authentication failed", e)
                        setStatus(OpButtonSessionStatus.AUTH_FAILED)
                        running.set(false)
                        notifyAuthFailed()
                        return
                    }
                    !e.isNetworkError -> {
                        OpButtonLog.e("Server error, stopping connection: %s", e.message)
                        setStatus(OpButtonSessionStatus.ERROR)
                        running.set(false)
                        notifyError(e)
                        return
                    }
                    else -> {
                        retryCount++
                        OpButtonLog.w("Connection attempt %d failed (network): %s, retrying in %dms", retryCount, e.message, retryDelayMs)
                        setStatus(OpButtonSessionStatus.CONNECTING)

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

    @Throws(OpButtonException::class, InterruptedException::class)
    private fun doConnectAndWaitApproval() {
        setStatus(OpButtonSessionStatus.CONNECTING)
        notifyConnecting()

        val deviceId = deviceIdentity.resolve(config.deviceId)
        val deviceName = config.deviceName.takeUnless { it.isNullOrEmpty() }
            ?: "${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        val deviceSecret = credentialStore.getOrCreate(config.baseUrl, deviceId, config.deviceSecret)
        httpClient.setDeviceSecret(deviceSecret)

        val attributes = hostState.resolveAllSessionAttributes()
        val resp = httpClient.connect(deviceId, deviceName, attributes)
        resp.deviceSecret?.let {
            httpClient.setDeviceSecret(it)
            credentialStore.save(config.baseUrl, deviceId, it)
        }
        hostState.acknowledgeAll(attributes)
        requestId.set(resp.requestId)

        OpButtonLog.i("Connect request sent, request_id=%s, status=%s", resp.requestId, resp.status)

        if (resp.isApproved && resp.sessionToken != null) {
            onApproved(resp.sessionToken)
            return
        }

        if (resp.isRejected) {
            onRejected()
            return
        }

        setStatus(OpButtonSessionStatus.WAITING_APPROVAL)
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
        setStatus(OpButtonSessionStatus.ACTIVE)

        OpButtonLog.i("Session approved and active")
        notifySessionStarted(token)

        if (ui.hasTabs()) {
            val snapshot = hostState.buildUISnapshotOnMainThread()
            if (snapshot != null) {
                try {
                    httpClient.submitPanel(token, snapshot)
                    ui.markSent(snapshot.getInt("revision"))
                    OpButtonLog.d("Initial UI submitted (revision=%d)", ui.getRevision())
                } catch (e: OpButtonException) {
                    OpButtonLog.e("Failed to submit initial UI", e)
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
        OpButtonLog.w("Connection request rejected")
        setStatus(OpButtonSessionStatus.REJECTED)
        running.set(false)
        notifyRejected()
    }

    // ── Action polling ──

    private fun doPollActions(token: String) {
        if (!isCurrentSession(token) || !actionPollInFlight.compareAndSet(false, true)) return
        try {
            val response = delivery.exchange(token)
            processPollResponse(token, response)
        } catch (error: OpButtonException) {
            if (!isCurrentSession(token)) return
            if (error.isNetworkError) {
                OpButtonLog.w("Network error during action poll, will retry")
            } else {
                OpButtonLog.w("Action poll failed with server error, attempting reconnect")
                attemptReconnect(token)
            }
        } finally {
            actionPollInFlight.set(false)
        }
    }

    private fun isCurrentSession(token: String) = running.get() && token == sessionToken.get()

    private fun processPollResponse(token: String, response: JSONObject) {
        if (!isCurrentSession(token)) return
        val actions = response.optJSONArray("actions") ?: return
        for (i in 0 until actions.length()) {
            val action = actions.optJSONObject(i) ?: continue
            dispatchActionOnMainThread(OpButtonAction.fromJson(action), token)
        }
    }

    private fun dispatchActionOnMainThread(action: OpButtonAction, token: String) {
        mainHandler.post { if (isCurrentSession(token)) actions.dispatch(action, token) }
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

    // ── Log buffering ──

    // ── Session attributes ──

    // ── Reconnection ──

    private fun attemptReconnect(token: String) {
        cancelActionPolling()
        setStatus(OpButtonSessionStatus.CONNECTING)

        var retryDelay = 2000L
        val maxDelay = 30000L

        while (isCurrentSession(token)) {
            try {
                Thread.sleep(retryDelay)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }

            if (!isCurrentSession(token)) return

            try {
                val response = delivery.probe(token)
                if (!isCurrentSession(token)) return
                processPollResponse(token, response)

                OpButtonLog.i("Reconnect successful, session still active")
                setStatus(OpButtonSessionStatus.ACTIVE)
                scheduleActionPolling(token, currentActionPollIntervalMs)
                return
            } catch (e: OpButtonException) {
                if (!isCurrentSession(token)) return
                if (e.isNetworkError) {
                    OpButtonLog.w("Reconnect: no network, retrying in %dms", retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(maxDelay)
                    continue
                }

                OpButtonLog.w("Session lost (server error), starting fresh connection")
                resetAndRestart()
                return
            }
        }
    }

    private fun resetAndRestart() {
        cancelScheduledTasks()
        shutdownExecutor()
        running.set(false)
        sessionToken.set(null)
        liveManager.onSessionClosed()
        setStatus(OpButtonSessionStatus.DISCONNECTED)
        notifySessionClosed()
        startConnection()
    }

    private fun setStatus(newStatus: OpButtonSessionStatus) {
        status.set(newStatus)
    }

    private fun cancelActionPolling() {
        actionPollFuture?.cancel(false)
        actionPollFuture = null
    }

    private fun cancelScheduledTasks() {
        cancelActionPolling()
        metricSampleFuture?.cancel(false)
        metricSampleFuture = null
    }

    private fun shutdownExecutor() {
        executor?.takeIf { !it.isShutdown }?.shutdown()
    }

    // ── Listener notifications ──

    private fun notifyConnecting() = sessionListeners.forEach { it.onOpButtonSessionConnecting() }
    private fun notifyWaitingApproval(reqId: String) = sessionListeners.forEach { it.onOpButtonSessionWaitingApproval(reqId) }
    private fun notifySessionStarted(token: String) = sessionListeners.forEach { it.onOpButtonSessionStarted(token) }
    private fun notifySessionClosed() = sessionListeners.forEach { it.onOpButtonSessionClosed() }
    private fun notifyRejected() = sessionListeners.forEach { it.onOpButtonSessionRejected() }
    private fun notifyAuthFailed() = sessionListeners.forEach { it.onOpButtonSessionAuthFailed() }
    private fun notifyError(exception: OpButtonException) = sessionListeners.forEach { it.onOpButtonSessionError(exception) }

    fun acknowledgeAction(actionId: String, payload: JSONObject?) {
        sessionToken.get()?.let { acknowledgeForSession(it, actionId, payload) }
    }

    private fun acknowledgeForSession(token: String, actionId: String, payload: JSONObject?) {
        executeBackground {
            if (isCurrentSession(token)) {
                try { httpClient.acknowledgeAction(token, actionId, payload) }
                catch (error: OpButtonException) { OpButtonLog.e("Failed to acknowledge action", error) }
            }
        }
    }

    private fun executeBackground(operation: () -> Unit) {
        val worker = executor?.takeUnless { it.isShutdown } ?: return
        try { worker.execute(operation) }
        catch (_: java.util.concurrent.RejectedExecutionException) {
            // Shutdown may race with scheduling; the closed session owns no more work.
        }
    }
}
