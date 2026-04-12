package org.pilot.sdk

import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

class PilotMetrics internal constructor() {
    private val collectors = CopyOnWriteArrayList<PilotMetricCollector>()
    private val buffer: MutableList<PilotMetricEntry> = Collections.synchronizedList(mutableListOf())

    var sampleIntervalMs: Long = 200
        internal set(value) { field = value.coerceIn(100, 1000) }

    var bufferSize: Int = 2000
        internal set

    var batchSize: Int = 200
        internal set

    fun addCollector(collector: PilotMetricCollector) {
        collectors.add(collector)
    }

    fun removeCollector(collector: PilotMetricCollector) {
        collectors.remove(collector)
    }

    fun record(metricType: PilotMetricType, value: Double) {
        bufferEntry(PilotMetricEntry(metricType, value))
    }

    fun record(metricType: PilotMetricType, value: Double, timestampMs: Long) {
        bufferEntry(PilotMetricEntry(metricType, value, timestampMs))
    }

    internal fun sample() {
        val collected = mutableListOf<PilotMetricEntry>()
        for (collector in collectors) {
            try {
                collector.collect(collected)
            } catch (e: Exception) {
                PilotLog.e("Metric collector threw exception", e)
            }
        }
        for (entry in collected) {
            bufferEntry(entry)
        }
    }

    internal fun drain(): List<PilotMetricEntry> {
        if (buffer.isEmpty()) return emptyList()
        synchronized(buffer) {
            val count = minOf(buffer.size, batchSize)
            val chunk = ArrayList(buffer.subList(0, count))
            buffer.subList(0, count).clear()
            return chunk
        }
    }

    internal fun requeue(entries: List<PilotMetricEntry>) {
        synchronized(buffer) {
            buffer.addAll(0, entries)
            while (buffer.size > bufferSize) {
                buffer.removeAt(buffer.size - 1)
            }
        }
    }

    internal fun hasData(): Boolean = buffer.isNotEmpty()

    internal fun clear() {
        buffer.clear()
        collectors.clear()
    }

    private fun bufferEntry(entry: PilotMetricEntry) {
        synchronized(buffer) {
            if (buffer.size >= bufferSize) {
                buffer.removeAt(0)
            }
            buffer.add(entry)
        }
    }
}
