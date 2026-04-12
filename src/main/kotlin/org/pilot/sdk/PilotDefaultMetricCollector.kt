package org.pilot.sdk

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Debug
import android.os.Process

internal class PilotDefaultMetricCollector(
    private val fpsTracker: PilotFpsTracker,
    context: Context?
) : PilotMetricCollector {
    private val appContext: Context? = context?.applicationContext

    private var prevRxBytes: Long = -1
    private var prevTxBytes: Long = -1
    private var prevNetworkTimeMs: Long = 0

    override fun collect(out: MutableList<PilotMetricEntry>) {
        val now = System.currentTimeMillis()

        // FPS & frame time
        val fps = fpsTracker.fps
        val frameTime = fpsTracker.frameTimeMs
        if (fps > 0) {
            out.add(PilotMetricEntry(PilotMetricType.FPS, fps))
            out.add(PilotMetricEntry(PilotMetricType.FRAME_TIME, frameTime))
        }

        // Memory: Java heap + native heap (bytes)
        val runtime = Runtime.getRuntime()
        val javaUsed = runtime.totalMemory() - runtime.freeMemory()
        val nativeUsed = Debug.getNativeHeapAllocatedSize()
        out.add(PilotMetricEntry(PilotMetricType.MEMORY, (javaUsed + nativeUsed).toDouble()))

        // Thread count
        out.add(PilotMetricEntry(PilotMetricType.THREAD_COUNT, Thread.activeCount().toDouble()))

        // Network RX/TX
        collectNetwork(out, now)

        // Battery
        if (appContext != null) {
            collectBattery(out)
        }
    }

    private fun collectNetwork(out: MutableList<PilotMetricEntry>, nowMs: Long) {
        val uid = Process.myUid()
        val rxBytes = TrafficStats.getUidRxBytes(uid)
        val txBytes = TrafficStats.getUidTxBytes(uid)

        if (rxBytes == TrafficStats.UNSUPPORTED.toLong() || txBytes == TrafficStats.UNSUPPORTED.toLong()) {
            return
        }

        if (prevRxBytes >= 0 && prevNetworkTimeMs > 0) {
            val dtMs = nowMs - prevNetworkTimeMs
            if (dtMs > 0) {
                val rxRate = ((rxBytes - prevRxBytes) * 1000.0) / dtMs
                val txRate = ((txBytes - prevTxBytes) * 1000.0) / dtMs
                out.add(PilotMetricEntry(PilotMetricType.NETWORK_RX, maxOf(0.0, rxRate)))
                out.add(PilotMetricEntry(PilotMetricType.NETWORK_TX, maxOf(0.0, txRate)))
            }
        }

        prevRxBytes = rxBytes
        prevTxBytes = txBytes
        prevNetworkTimeMs = nowMs
    }

    @Suppress("deprecation")
    private fun collectBattery(out: MutableList<PilotMetricEntry>) {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = appContext!!.registerReceiver(null, filter) ?: return

            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

            if (level >= 0 && scale > 0) {
                val pct = (level * 100.0) / scale
                out.add(PilotMetricEntry(PilotMetricType.BATTERY_LEVEL, pct))
            }

            out.add(PilotMetricEntry(PilotMetricType.BATTERY_CHARGING, if (plugged > 0) 1.0 else 0.0))
        } catch (e: Exception) {
            PilotLog.d("Battery metric collection failed: %s", e.message)
        }
    }
}
