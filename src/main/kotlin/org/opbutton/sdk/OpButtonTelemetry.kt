package org.opbutton.sdk

import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/** Owns the telemetry outbox and its delivery receipts. */
internal class OpButtonTelemetry(
    private val logConfig: OpButtonLogConfigBuilder,
    private val httpClient: OpButtonHttpClient,
    private val metrics: OpButtonMetrics
) {
    private val logBuffer: MutableList<OpButtonLogEntry> = Collections.synchronizedList(ArrayList())
    private val logOverflowWarned = AtomicBoolean(false)

    fun clear() = logBuffer.clear()
    fun capture() = Batch(drainLogChunk(), metrics.drain())

    inner class Batch(val logs: List<OpButtonLogEntry>, val samples: List<OpButtonMetricEntry>) : java.io.Closeable {
        private var settled = false

        @Synchronized fun commit() {
            if (settled) return
            settled = true
            if (logs.isNotEmpty()) logOverflowWarned.set(false)
        }

        @Synchronized override fun close() {
            if (settled) return
            settled = true
            requeueLogs(logs)
            metrics.requeue(samples)
        }
    }
    fun bufferLog(entry: OpButtonLogEntry) {
        synchronized(logBuffer) {
            if (logBuffer.size >= logConfig.bufferSize) {
                logBuffer.removeAt(0)
                if (logOverflowWarned.compareAndSet(false, true)) {
                    OpButtonLog.w("Log buffer overflow (${logConfig.bufferSize}), dropping oldest entries")
                }
            }
            logBuffer.add(entry)
        }
    }

    private fun drainLogChunk(): List<OpButtonLogEntry> {
        if (logBuffer.isEmpty()) return emptyList()

        synchronized(logBuffer) {
            val count = logBuffer.size.coerceAtMost(logConfig.batchSize)
            val chunk = ArrayList(logBuffer.subList(0, count))
            logBuffer.subList(0, count).clear()
            return chunk
        }
    }

    private fun requeueLogs(chunk: List<OpButtonLogEntry>) {
        if (chunk.isEmpty()) return

        synchronized(logBuffer) {
            logBuffer.addAll(0, chunk)
            while (logBuffer.size > logConfig.bufferSize) {
                logBuffer.removeAt(logBuffer.size - 1)
            }
        }
    }

    @Throws(OpButtonException::class)
    fun flushLogs(token: String) {
        val chunk = drainLogChunk()
        if (chunk.isEmpty()) return

        try {
            httpClient.sendLogs(token, chunk)
        } catch (e: OpButtonException) {
            requeueLogs(chunk)
            throw e
        }
    }

    @Throws(OpButtonException::class)
    fun flushMetrics(token: String) {
        val chunk = metrics.drain()
        if (chunk.isEmpty()) return

        try {
            httpClient.sendMetrics(token, chunk)
        } catch (e: OpButtonException) {
            metrics.requeue(chunk)
            throw e
        }
    }

    fun resolveLogAttributes(): JSONObject? {
        val builder = logConfig.attributes
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
}
