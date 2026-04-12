package org.pilot.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal class PilotLiveManager(
    context: Context?,
    private val httpClient: PilotHttpClient,
    private val callback: (enabled: Boolean, actionPollIntervalMs: Long) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val isLive = AtomicBoolean(false)

    private val application: Application? = resolveApplication(context)
    private val lifecycleCallbacks: Application.ActivityLifecycleCallbacks?
    private val liveKitPublisher: PilotLiveKitPublisher? = application?.let { PilotLiveKitPublisher(it) }

    @Volatile private var currentActivity = WeakReference<Activity>(null)
    @Volatile private var overlayView = WeakReference<PilotLiveOverlayView>(null)
    @Volatile private var settings = LiveSettings.low()

    init {
        if (application != null) {
            val lc = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

                override fun onActivityStarted(activity: Activity) {
                    if (activity is PilotScreenCaptureActivity) return
                    currentActivity = WeakReference(activity)
                    attachOverlay(activity)
                }

                override fun onActivityResumed(activity: Activity) {
                    if (activity is PilotScreenCaptureActivity) return
                    currentActivity = WeakReference(activity)
                    attachOverlay(activity)
                }

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivityStopped(activity: Activity) {
                    if (currentActivity.get() === activity) {
                        currentActivity = WeakReference(null)
                    }
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                override fun onActivityDestroyed(activity: Activity) {
                    val overlay = overlayView.get()
                    if (overlay != null && overlay.context === activity) {
                        removeOverlay(overlay)
                        overlayView = WeakReference(null)
                    }
                    if (currentActivity.get() === activity) {
                        currentActivity = WeakReference(null)
                    }
                }
            }
            application.registerActivityLifecycleCallbacks(lc)
            lifecycleCallbacks = lc
        } else {
            lifecycleCallbacks = null
        }
    }

    fun start(sessionToken: String, payload: JSONObject?): JSONObject {
        if (application == null || liveKitPublisher == null) {
            return buildAck(false, "Live requires Pilot.initialize(config, applicationContext)")
        }

        val wasLive = isLive.get()
        stopLiveRuntime()
        if (wasLive) callback(false, 0L)

        try {
            val publisherSession = fetchPublisherSession(sessionToken, LiveSettings.fromPayload(payload))
            val s = publisherSession.settings

            liveKitPublisher.start(
                publisherSession.serverUrl,
                publisherSession.participantToken,
                s.presetName,
                s.maxDimension,
                s.framesPerSecond
            )

            settings = s
            isLive.set(true)
            requestScreenCapturePermission()
            callback(true, s.actionPollIntervalMs)

            val metadata = linkedMapOf<String, Any?>(
                "preset" to s.presetName,
                "max_dimension" to s.maxDimension,
                "fps" to s.framesPerSecond
            )
            publisherSession.roomName?.let { metadata["room_name"] = it }
            publisherSession.participantIdentity?.let { metadata["participant_identity"] = it }
            metadata["video_track_name"] = publisherSession.videoTrackName
            Pilot.event("live_started", "live", metadata)

            return buildAck(true, "live_started").apply {
                put("preset", s.presetName)
                put("max_dimension", s.maxDimension)
                put("fps", s.framesPerSecond)
                put("room_name", publisherSession.roomName)
                put("video_track_name", publisherSession.videoTrackName)
                put("waiting_for_activity", getCurrentActivity() == null)
            }
        } catch (e: PilotException) {
            stopLiveRuntime()
            PilotLog.e("Failed to start LiveKit live", e)
            Pilot.event("live_start_failed", "live", mapOf("message" to e.message, "http_code" to e.httpCode))
            return buildAck(false, e.message ?: "Failed to start live")
        } catch (e: Exception) {
            stopLiveRuntime()
            PilotLog.e("Failed to start LiveKit live", e)
            Pilot.event("live_start_failed", "live", mapOf("message" to e.message))
            return buildAck(false, e.message ?: "Failed to start live")
        }
    }

    fun update(sessionToken: String, payload: JSONObject?): JSONObject {
        if (!isLive.get()) return buildAck(false, "Live is not active")
        if (liveKitPublisher == null) return buildAck(false, "LiveKit publisher is not available")

        try {
            val publisherSession = fetchPublisherSession(sessionToken, LiveSettings.fromPayload(payload))
            val s = publisherSession.settings
            val screenShareActive = liveKitPublisher.updateQuality(s.presetName, s.maxDimension, s.framesPerSecond)

            settings = s
            callback(true, s.actionPollIntervalMs)

            val metadata = linkedMapOf<String, Any?>(
                "preset" to s.presetName,
                "max_dimension" to s.maxDimension,
                "fps" to s.framesPerSecond,
                "screen_share_active" to screenShareActive
            )
            publisherSession.roomName?.let { metadata["room_name"] = it }
            publisherSession.participantIdentity?.let { metadata["participant_identity"] = it }
            metadata["video_track_name"] = publisherSession.videoTrackName
            Pilot.event("live_updated", "live", metadata)

            return buildAck(true, if (screenShareActive) "live_updated" else "live_updated_pending_capture").apply {
                put("preset", s.presetName)
                put("max_dimension", s.maxDimension)
                put("fps", s.framesPerSecond)
                put("room_name", publisherSession.roomName)
                put("video_track_name", publisherSession.videoTrackName)
                put("screen_share_active", screenShareActive)
            }
        } catch (e: PilotException) {
            PilotLog.e("Failed to update LiveKit live quality", e)
            Pilot.event("live_update_failed", "live", mapOf("message" to e.message, "http_code" to e.httpCode))
            return buildAck(false, e.message ?: "Failed to update live quality")
        } catch (e: Exception) {
            PilotLog.e("Failed to update LiveKit live quality", e)
            Pilot.event("live_update_failed", "live", mapOf("message" to e.message))
            return buildAck(false, e.message ?: "Failed to update live quality")
        }
    }

    fun stop(): JSONObject {
        val wasLive = isLive.get()
        stopLiveRuntime()
        callback(false, 0L)

        if (wasLive) Pilot.event("live_stopped", "live", null)
        return buildAck(true, if (wasLive) "live_stopped" else "live_already_stopped")
    }

    fun onSessionClosed() {
        stopLiveRuntime()
    }

    fun shutdown() {
        onSessionClosed()
        application?.let { app ->
            lifecycleCallbacks?.let { app.unregisterActivityLifecycleCallbacks(it) }
        }
        executor.shutdownNow()
    }

    fun tap(payload: JSONObject?): JSONObject {
        if (!isLive.get()) return buildAck(false, "Live is not active")

        val activity = getCurrentActivity() ?: return buildAck(false, "No resumed activity available for tap")
        val point = resolveTouchPoint(activity, payload) ?: return buildAck(false, "Unable to resolve tap coordinates")

        val downTime = SystemClock.uptimeMillis()
        dispatchTouchEvent(activity, MotionEvent.ACTION_DOWN, downTime, downTime, point)
        dispatchTouchEvent(activity, MotionEvent.ACTION_UP, downTime, downTime + 1L, point)
        showTapOverlay(activity, point)

        Pilot.event("live_tap", "live_input", mapOf(
            "normalized_x" to point.normalizedX,
            "normalized_y" to point.normalizedY,
            "x" to point.x,
            "y" to point.y
        ))
        return buildAck(true, "tap_sent")
    }

    fun longPress(payload: JSONObject?): JSONObject {
        if (!isLive.get()) return buildAck(false, "Live is not active")

        val activity = getCurrentActivity() ?: return buildAck(false, "No resumed activity available for long press")
        val point = resolveTouchPoint(activity, payload) ?: return buildAck(false, "Unable to resolve long press coordinates")

        val durationMs = (payload?.optLong("duration_ms", 800L) ?: 800L).coerceIn(250L, 2000L)
        val downTime = SystemClock.uptimeMillis()
        dispatchTouchEvent(activity, MotionEvent.ACTION_DOWN, downTime, downTime, point)
        showPressOverlay(activity, point)

        mainHandler.postDelayed({
            dispatchTouchEvent(activity, MotionEvent.ACTION_UP, downTime, SystemClock.uptimeMillis(), point)
            showReleaseOverlay(activity, point)
        }, durationMs)

        Pilot.event("live_long_press", "live_input", mapOf(
            "normalized_x" to point.normalizedX,
            "normalized_y" to point.normalizedY,
            "x" to point.x,
            "y" to point.y,
            "duration_ms" to durationMs
        ))

        return buildAck(true, "long_press_sent").apply { put("duration_ms", durationMs) }
    }

    // ── Private ──

    private fun requestScreenCapturePermission() {
        PilotScreenCaptureActivity.callback = PilotScreenCaptureActivity.ScreenCaptureCallback { resultCode, data ->
            if (resultCode == Activity.RESULT_OK && data != null) {
                executor.execute {
                    if (!isLive.get() || liveKitPublisher == null) return@execute
                    try {
                        liveKitPublisher.enableScreenShare(data)
                        Pilot.event("screen_share_enabled", "live", null)
                    } catch (e: Exception) {
                        PilotLog.e("Failed to enable screen share", e)
                        stop()
                    }
                }
            } else {
                PilotLog.w("Screen capture permission denied")
                executor.execute { stop() }
            }
        }

        val activity = getCurrentActivity()
        if (activity != null) {
            activity.startActivity(Intent(activity, PilotScreenCaptureActivity::class.java))
        } else if (application != null) {
            val intent = Intent(application, PilotScreenCaptureActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(intent)
        } else {
            PilotLog.e("No context available to request screen capture permission")
            executor.execute { stop() }
        }
    }

    private fun stopLiveRuntime() {
        isLive.set(false)
        PilotScreenCaptureActivity.callback = null
        clearOverlay()
        liveKitPublisher?.stop()
    }

    private fun fetchPublisherSession(sessionToken: String, requestedSettings: LiveSettings): PublisherSession {
        val response = httpClient.getLivePublisherState(sessionToken)
        val statusMessage = response.optString("status_message", null)?.trim()?.ifEmpty { null }

        if (!response.optBoolean("configured", false)) {
            throw PilotException(statusMessage ?: "LiveKit is not configured on the server")
        }
        if (!response.optBoolean("requested", false)) {
            throw PilotException(statusMessage ?: "Live request is no longer active")
        }

        val serverUrl = response.optString("server_url", null)?.trim()?.ifEmpty { null }
            ?: throw PilotException("Server returned incomplete live credentials")
        val participantToken = response.optString("participant_token", null)?.trim()?.ifEmpty { null }
            ?: throw PilotException("Server returned incomplete live credentials")
        val videoTrackName = response.optString("video_track_name", null)?.trim()?.ifEmpty { null }
            ?: throw PilotException("Server returned incomplete live credentials")

        val presetName = response.optString("preset", requestedSettings.presetName).trim().ifEmpty { requestedSettings.presetName }
        val maxDimension = response.optInt("max_dimension", requestedSettings.maxDimension).coerceIn(240, 1440)
        val fps = response.optInt("fps", requestedSettings.framesPerSecond).coerceIn(1, 6)
        val actionPollIntervalMs = response.optLong("action_poll_interval_ms", requestedSettings.actionPollIntervalMs).coerceIn(200L, 2000L)

        return PublisherSession(
            serverUrl = serverUrl,
            participantToken = participantToken,
            roomName = response.optString("room_name", null)?.trim()?.ifEmpty { null },
            participantIdentity = response.optString("participant_identity", null)?.trim()?.ifEmpty { null },
            videoTrackName = videoTrackName,
            settings = LiveSettings(presetName, maxDimension, fps, actionPollIntervalMs)
        )
    }

    private fun getCurrentActivity(): Activity? = currentActivity.get()

    private fun dispatchTouchEvent(activity: Activity, action: Int, downTime: Long, eventTime: Long, point: TouchPoint) {
        val event = MotionEvent.obtain(downTime, eventTime, action, point.x, point.y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        activity.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun resolveTouchPoint(activity: Activity, payload: JSONObject?): TouchPoint? {
        val rootView = activity.window.decorView
        val width = rootView.width
        val height = rootView.height
        if (width <= 0 || height <= 0) return null

        val normalizedX = (payload?.optDouble("normalized_x", 0.5) ?: 0.5).coerceIn(0.0, 1.0).toFloat()
        val normalizedY = (payload?.optDouble("normalized_y", 0.5) ?: 0.5).coerceIn(0.0, 1.0).toFloat()

        return TouchPoint(normalizedX * width, normalizedY * height, normalizedX, normalizedY)
    }

    private fun showTapOverlay(activity: Activity, point: TouchPoint) {
        attachOverlay(activity).showTap(point.x, point.y)
    }

    private fun showPressOverlay(activity: Activity, point: TouchPoint) {
        attachOverlay(activity).showPress(point.x, point.y)
    }

    private fun showReleaseOverlay(activity: Activity, point: TouchPoint) {
        attachOverlay(activity).showRelease(point.x, point.y)
    }

    private fun clearOverlay() {
        mainHandler.post { overlayView.get()?.clearIndicator() }
    }

    private fun attachOverlay(activity: Activity): PilotLiveOverlayView {
        val existing = overlayView.get()
        val decorView = activity.window.decorView as ViewGroup
        if (existing != null && existing.parent === decorView) return existing

        existing?.let { removeOverlay(it) }

        val view = PilotLiveOverlayView(activity)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        decorView.addView(view, params)
        overlayView = WeakReference(view)
        return view
    }

    private fun removeOverlay(view: PilotLiveOverlayView) {
        (view.parent as? ViewGroup)?.removeView(view)
    }

    companion object {
        private fun buildAck(ok: Boolean, status: String): JSONObject = JSONObject().apply {
            put("ok", ok)
            put("status", status)
        }

        private fun resolveApplication(context: Context?): Application? {
            if (context == null) return null
            if (context is Application) return context
            val appContext = context.applicationContext
            return appContext as? Application
        }
    }

    // ── Inner data classes ──

    private data class LiveSettings(
        val presetName: String,
        val maxDimension: Int,
        val framesPerSecond: Int,
        val actionPollIntervalMs: Long
    ) {
        companion object {
            fun low() = LiveSettings("low", 540, 2, 500L)
            fun balanced() = LiveSettings("balanced", 720, 3, 400L)
            fun high() = LiveSettings("high", 1080, 4, 300L)

            fun fromPayload(payload: JSONObject?): LiveSettings {
                if (payload == null) return low()

                val preset = payload.optString("preset", "low")
                val base = when (preset) {
                    "balanced" -> balanced()
                    "high" -> high()
                    else -> low()
                }

                return LiveSettings(
                    presetName = if (preset in listOf("low", "balanced", "high")) preset else base.presetName,
                    maxDimension = payload.optInt("max_dimension", base.maxDimension).coerceIn(240, 1440),
                    framesPerSecond = payload.optInt("fps", base.framesPerSecond).coerceIn(1, 6),
                    actionPollIntervalMs = payload.optLong("action_poll_interval_ms", base.actionPollIntervalMs).coerceIn(200L, 2000L)
                )
            }
        }
    }

    private data class PublisherSession(
        val serverUrl: String,
        val participantToken: String,
        val roomName: String?,
        val participantIdentity: String?,
        val videoTrackName: String,
        val settings: LiveSettings
    )

    private data class TouchPoint(
        val x: Float,
        val y: Float,
        val normalizedX: Float,
        val normalizedY: Float
    )
}
