package org.opbutton.sdk

class OpButtonLogConfigBuilder {
    internal var isEnabled: Boolean = true
        private set
    internal var logLevel: OpButtonLogLevel = OpButtonLogLevel.INFO
        private set
    internal var batchSize: Int = 100
        private set
    internal var bufferSize: Int = 1000
        private set
    internal var attributes: OpButtonLogAttributeBuilder = OpButtonLogAttributeBuilder()
        private set

    fun setEnabled(enabled: Boolean) = apply { isEnabled = enabled }
    fun setLogLevel(level: OpButtonLogLevel) = apply { logLevel = level }
    fun setBatchSize(size: Int) = apply { batchSize = size }
    fun setBufferSize(size: Int) = apply { bufferSize = size }
    fun setAttributes(attributes: OpButtonLogAttributeBuilder) = apply { this.attributes = attributes }
}
