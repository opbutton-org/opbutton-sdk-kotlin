package org.opbutton.sdk

import android.content.Context

class OpButtonConfig private constructor(
    internal val baseUrl: String,
    internal val apiToken: String,
    internal val deviceId: String?,
    internal val deviceName: String?,
    internal val pollIntervalMs: Long,
    internal val actionPollIntervalMs: Long,
    internal val autoConnect: Boolean,
    internal val loggerListener: OpButtonLoggerListener?,
    internal val sessionListener: OpButtonSessionListener?,
    internal val actionListener: OpButtonActionListener?,
    internal val sessionAttributes: OpButtonSessionAttributeBuilder,
    internal val logConfig: OpButtonLogConfigBuilder,
    internal val metricConfig: OpButtonMetricConfigBuilder
) {
    class Builder(baseUrl: String, private val apiToken: String) {
        private val baseUrl: String = baseUrl.trimEnd('/')
        private var deviceId: String? = null
        private var deviceName: String? = null
        private var pollIntervalMs: Long = 10000
        private var actionPollIntervalMs: Long = 2000
        private var autoConnect: Boolean = true
        private var loggerListener: OpButtonLoggerListener? = null
        private var sessionListener: OpButtonSessionListener? = null
        private var actionListener: OpButtonActionListener? = null
        private var sessionAttributes = OpButtonSessionAttributeBuilder()
        private var logConfig = OpButtonLogConfigBuilder()
        private var metricConfig = OpButtonMetricConfigBuilder()

        fun setDeviceId(deviceId: String) = apply { this.deviceId = deviceId }
        fun setDeviceName(deviceName: String) = apply { this.deviceName = deviceName }
        fun setPollIntervalMs(ms: Long) = apply { this.pollIntervalMs = ms }
        fun setActionPollIntervalMs(ms: Long) = apply { this.actionPollIntervalMs = ms }
        fun setAutoConnect(autoConnect: Boolean) = apply { this.autoConnect = autoConnect }
        fun setLoggerListener(listener: OpButtonLoggerListener?) = apply { this.loggerListener = listener }
        fun setSessionListener(listener: OpButtonSessionListener?) = apply { this.sessionListener = listener }
        fun setActionListener(listener: OpButtonActionListener?) = apply { this.actionListener = listener }
        fun setSessionAttributes(builder: OpButtonSessionAttributeBuilder) = apply { this.sessionAttributes = builder }
        fun setLogConfig(config: OpButtonLogConfigBuilder) = apply { this.logConfig = config }
        fun setMetricConfig(config: OpButtonMetricConfigBuilder) = apply { this.metricConfig = config }

        fun build(): OpButtonConfig {
            require(baseUrl.isNotEmpty()) { "baseUrl is required" }
            require(apiToken.isNotEmpty()) { "apiToken is required" }
            return OpButtonConfig(
                baseUrl, apiToken, deviceId, deviceName,
                pollIntervalMs, actionPollIntervalMs, autoConnect,
                loggerListener, sessionListener, actionListener,
                sessionAttributes, logConfig, metricConfig
            )
        }
    }
}
