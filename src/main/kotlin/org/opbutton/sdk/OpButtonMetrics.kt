package org.opbutton.sdk

import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

class OpButtonMetrics internal constructor() {
    private val collectors = CopyOnWriteArrayList<OpButtonMetricCollector>()
    private val buffer: MutableList<OpButtonMetricEntry> = Collections.synchronizedList(mutableListOf())

    var sampleIntervalMs: Long = 200
        internal set(value) { field = value.coerceIn(100, 1000) }

    var bufferSize: Int = 2000
        internal set

    var batchSize: Int = 200
        internal set

    fun addCollector(collector: OpButtonMetricCollector) {
        collectors.add(collector)
    }

    fun removeCollector(collector: OpButtonMetricCollector) {
        collectors.remove(collector)
    }

    fun record(metricType: OpButtonMetricType, value: Double) {
        bufferEntry(OpButtonMetricEntry(metricType, value))
    }

    fun record(metricType: OpButtonMetricType, value: Double, timestampMs: Long) {
        bufferEntry(OpButtonMetricEntry(metricType, value, timestampMs))
    }

    internal fun sample() {
        val collected = mutableListOf<OpButtonMetricEntry>()
        for (collector in collectors) {
            try {
                collector.collect(collected)
            } catch (e: Exception) {
                OpButtonLog.e("Metric collector threw exception", e)
            }
        }
        for (entry in collected) {
            bufferEntry(entry)
        }
    }

    internal fun drain(): List<OpButtonMetricEntry> {
        if (buffer.isEmpty()) return emptyList()
        synchronized(buffer) {
            val count = minOf(buffer.size, batchSize)
            val chunk = ArrayList(buffer.subList(0, count))
            buffer.subList(0, count).clear()
            return chunk
        }
    }

    internal fun requeue(entries: List<OpButtonMetricEntry>) {
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

    private fun bufferEntry(entry: OpButtonMetricEntry) {
        synchronized(buffer) {
            if (buffer.size >= bufferSize) {
                buffer.removeAt(0)
            }
            buffer.add(entry)
        }
    }
}
