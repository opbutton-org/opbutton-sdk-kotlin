package org.pilot.sdk

import android.content.Context

class PilotConfig private constructor(
    internal val baseUrl: String,
    internal val apiToken: String,
    internal val deviceId: String?,
    internal val deviceName: String?,
    internal val pollIntervalMs: Long,
    internal val actionPollIntervalMs: Long,
    internal val autoConnect: Boolean,
    internal val loggerListener: PilotLoggerListener?,
    internal val sessionListener: PilotSessionListener?,
    internal val actionListener: PilotActionListener?,
    internal val sessionAttributes: PilotSessionAttributeBuilder,
    internal val logConfig: PilotLogConfigBuilder,
    internal val metricConfig: PilotMetricConfigBuilder
) {
    class Builder(baseUrl: String, private val apiToken: String) {
        private val baseUrl: String = baseUrl.trimEnd('/')
        private var deviceId: String? = null
        private var deviceName: String? = null
        private var pollIntervalMs: Long = 10000
        private var actionPollIntervalMs: Long = 2000
        private var autoConnect: Boolean = true
        private var loggerListener: PilotLoggerListener? = null
        private var sessionListener: PilotSessionListener? = null
        private var actionListener: PilotActionListener? = null
        private var sessionAttributes = PilotSessionAttributeBuilder()
        private var logConfig = PilotLogConfigBuilder()
        private var metricConfig = PilotMetricConfigBuilder()

        fun setDeviceId(deviceId: String) = apply { this.deviceId = deviceId }
        fun setDeviceName(deviceName: String) = apply { this.deviceName = deviceName }
        fun setPollIntervalMs(ms: Long) = apply { this.pollIntervalMs = ms }
        fun setActionPollIntervalMs(ms: Long) = apply { this.actionPollIntervalMs = ms }
        fun setAutoConnect(autoConnect: Boolean) = apply { this.autoConnect = autoConnect }
        fun setLoggerListener(listener: PilotLoggerListener?) = apply { this.loggerListener = listener }
        fun setSessionListener(listener: PilotSessionListener?) = apply { this.sessionListener = listener }
        fun setActionListener(listener: PilotActionListener?) = apply { this.actionListener = listener }
        fun setSessionAttributes(builder: PilotSessionAttributeBuilder) = apply { this.sessionAttributes = builder }
        fun setLogConfig(config: PilotLogConfigBuilder) = apply { this.logConfig = config }
        fun setMetricConfig(config: PilotMetricConfigBuilder) = apply { this.metricConfig = config }

        fun build(): PilotConfig {
            require(baseUrl.isNotEmpty()) { "baseUrl is required" }
            require(apiToken.isNotEmpty()) { "apiToken is required" }
            return PilotConfig(
                baseUrl, apiToken, deviceId, deviceName,
                pollIntervalMs, actionPollIntervalMs, autoConnect,
                loggerListener, sessionListener, actionListener,
                sessionAttributes, logConfig, metricConfig
            )
        }
    }
}
