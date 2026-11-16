package org.opbutton.sdk

import android.content.Context
import java.net.URI

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
    internal val metricConfig: OpButtonMetricConfigBuilder,
    internal val deviceSecret: String?,
    internal val allowInsecureLocalhost: Boolean
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
        private var deviceSecret: String? = null
        private var allowInsecureLocalhost = false

        fun setDeviceId(deviceId: String) = apply { this.deviceId = deviceId }
        fun setDeviceName(deviceName: String) = apply { this.deviceName = deviceName }
        fun setDeviceSecret(deviceSecret: String) = apply { this.deviceSecret = deviceSecret }
        fun setAllowInsecureLocalhost(allow: Boolean) = apply { this.allowInsecureLocalhost = allow }
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
            require(apiToken.isNotEmpty() && safeHeaderValue(apiToken)) {
                "apiToken is required and must contain only printable ASCII characters"
            }
            require(pollIntervalMs in 250..300000 && actionPollIntervalMs in 250..300000) {
                "poll intervals must be between 250ms and 300000ms"
            }
            require(deviceSecret == null ||
                (deviceSecret!!.length in 32..256 && safeHeaderValue(deviceSecret!!))) {
                "deviceSecret must contain 32 to 256 printable ASCII characters"
            }
            val uri = try { URI(baseUrl) } catch (error: Exception) {
                throw IllegalArgumentException("baseUrl is invalid", error)
            }
            val local = uri.host.equals("localhost", ignoreCase = true) || uri.host == "127.0.0.1" ||
                uri.host == "::1" || uri.host == "[::1]"
            require(uri.scheme.equals("https", ignoreCase = true) ||
                (allowInsecureLocalhost && uri.scheme.equals("http", ignoreCase = true) && local)) {
                "baseUrl must use HTTPS; localhost HTTP requires setAllowInsecureLocalhost(true)"
            }
            require(uri.host != null && uri.userInfo == null && uri.rawQuery == null &&
                uri.rawFragment == null) {
                "baseUrl must have a host and must not contain credentials, a query, or a fragment"
            }
            return OpButtonConfig(
                baseUrl, apiToken, deviceId, deviceName,
                pollIntervalMs, actionPollIntervalMs, autoConnect,
                loggerListener, sessionListener, actionListener,
                sessionAttributes, logConfig, metricConfig, deviceSecret, allowInsecureLocalhost
            )
        }

        private fun safeHeaderValue(value: String): Boolean =
            value.all { it.code in 0x21..0x7e }
    }
}
