package org.pilot.sdk

class PilotLogConfigBuilder {
    internal var isEnabled: Boolean = true
        private set
    internal var logLevel: PilotLogLevel = PilotLogLevel.INFO
        private set
    internal var batchSize: Int = 100
        private set
    internal var bufferSize: Int = 1000
        private set
    internal var attributes: PilotLogAttributeBuilder = PilotLogAttributeBuilder()
        private set

    fun setEnabled(enabled: Boolean) = apply { isEnabled = enabled }
    fun setLogLevel(level: PilotLogLevel) = apply { logLevel = level }
    fun setBatchSize(size: Int) = apply { batchSize = size }
    fun setBufferSize(size: Int) = apply { bufferSize = size }
    fun setAttributes(attributes: PilotLogAttributeBuilder) = apply { this.attributes = attributes }
}
