package org.opbutton.sdk

class OpButtonMetricConfigBuilder {
    internal var isEnabled: Boolean = true
        private set
    internal var sampleIntervalMs: Long = 200
        private set
    internal var bufferSize: Int = 2000
        private set
    internal var batchSize: Int = 200
        private set
    internal val collectors: MutableList<OpButtonMetricCollector> = mutableListOf()

    fun setEnabled(enabled: Boolean) = apply { isEnabled = enabled }

    fun setSampleIntervalMs(ms: Long) = apply {
        sampleIntervalMs = ms.coerceIn(100, 1000)
    }

    fun setBufferSize(size: Int) = apply { bufferSize = size }
    fun setBatchSize(size: Int) = apply { batchSize = size }

    fun addCollector(collector: OpButtonMetricCollector) = apply {
        collectors.add(collector)
    }
}
