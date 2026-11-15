package org.opbutton.sdk

import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class OpButtonHttpClient(
    private val baseUrl: String,
    private val apiToken: String
) : OpButtonPollDelivery.Transport {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val emptyBody = "".toRequestBody(jsonMedia)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun shutdown() {
        http.dispatcher.executorService.shutdown()
        http.connectionPool.evictAll()
    }

    // ── Client endpoints ──

    fun connect(deviceId: String, deviceName: String, sessionAttributes: Map<String, Any>): OpButtonConnectResponse {
        val body = JSONObject().apply {
            put("device_id", deviceId)
            put("device_name", deviceName)
            if (sessionAttributes.isNotEmpty()) {
                val attrs = JSONObject()
                for ((key, value) in sessionAttributes) {
                    attrs.put(key, value)
                }
                put("session_attributes", attrs)
            }
        }

        val request = apiTokenRequest("/api/client/connect")
            .post(body.toRequestBody())
            .build()

        return OpButtonConnectResponse.fromJson(execute(request))
    }

    fun pollStatus(requestId: String): OpButtonConnectResponse {
        val request = apiTokenRequest("/api/client/poll-status/$requestId")
            .get()
            .build()

        return OpButtonConnectResponse.fromJson(execute(request))
    }

    fun closeSession(sessionToken: String): Boolean {
        val request = sessionTokenRequest("/api/client/session/close", sessionToken)
            .post(emptyBody)
            .build()

        return execute(request).optBoolean("ok", false)
    }

    override fun submitPanel(sessionToken: String, layout: JSONObject): JSONObject {
        val body = JSONObject().apply {
            put("layout", layout)
        }

        val request = sessionTokenRequest("/api/client/session/panel", sessionToken)
            .post(body.toRequestBody())
            .build()

        return execute(request)
    }

    override fun pollActions(
        sessionToken: String,
        changedAttributes: Map<String, Any>?,
        logs: List<OpButtonLogEntry>,
        metrics: List<OpButtonMetricEntry>
    ): JSONObject {
        val body = JSONObject().apply {
            if (!changedAttributes.isNullOrEmpty()) {
                val attrs = JSONObject()
                for ((key, value) in changedAttributes) {
                    attrs.put(key, value)
                }
                put("session_attributes", attrs)
            }
            if (logs.isNotEmpty()) {
                val logsArray = JSONArray()
                for (entry in logs) {
                    logsArray.put(entry.toJson())
                }
                put("logs", logsArray)
            }
            if (metrics.isNotEmpty()) {
                val metricsArray = JSONArray()
                for (entry in metrics) {
                    metricsArray.put(entry.toJson())
                }
                put("metrics", metricsArray)
            }
        }

        val request = sessionTokenRequest("/api/client/session/actions/poll", sessionToken)
            .post(if (body.length() > 0) body.toRequestBody() else emptyBody)
            .build()

        return execute(request)
    }

    fun acknowledgeAction(sessionToken: String, actionId: String, ackPayload: JSONObject?) {
        val body = JSONObject().apply {
            put("action_id", actionId)
            put("ack_payload", ackPayload ?: JSONObject())
        }

        val request = sessionTokenRequest("/api/client/session/actions/ack", sessionToken)
            .post(body.toRequestBody())
            .build()

        execute(request)
    }

    fun sendLogs(sessionToken: String, logs: List<OpButtonLogEntry>) {
        if (logs.isEmpty()) return

        val logsArray = JSONArray()
        for (entry in logs) {
            logsArray.put(entry.toJson())
        }

        val body = JSONObject().apply {
            put("logs", logsArray)
        }

        val request = sessionTokenRequest("/api/client/session/logs", sessionToken)
            .post(body.toRequestBody())
            .build()

        execute(request)
    }

    fun sendMetrics(sessionToken: String, metrics: List<OpButtonMetricEntry>) {
        if (metrics.isEmpty()) return

        val metricsArray = JSONArray()
        for (entry in metrics) {
            metricsArray.put(entry.toJson())
        }

        val body = JSONObject().apply {
            put("metrics", metricsArray)
        }

        val request = sessionTokenRequest("/api/client/session/metrics", sessionToken)
            .post(body.toRequestBody())
            .build()

        execute(request)
    }

    fun getLivePublisherState(sessionToken: String): JSONObject {
        val request = sessionTokenRequest("/api/client/session/live/publisher", sessionToken)
            .get()
            .build()

        return execute(request)
    }

    // ── Helpers ──

    private fun apiTokenRequest(path: String): Request.Builder =
        Request.Builder()
            .url("$baseUrl$path")
            .header("X-Api-Token", apiToken)

    private fun sessionTokenRequest(path: String, sessionToken: String): Request.Builder =
        Request.Builder()
            .url("$baseUrl$path")
            .header("X-Session-Token", sessionToken)

    private fun JSONObject.toRequestBody() =
        this.toString().toRequestBody(jsonMedia)

    private fun execute(request: Request): JSONObject {
        try {
            http.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val detail = try {
                        JSONObject(responseBody).optString("detail", responseBody)
                    } catch (_: Exception) {
                        responseBody
                    }
                    throw OpButtonException(response.code, "HTTP ${response.code}: $detail")
                }

                if (responseBody.isEmpty()) return JSONObject()
                return JSONObject(responseBody)
            }
        } catch (e: IOException) {
            throw OpButtonException("Network error: ${e.message}", e)
        } catch (e: OpButtonException) {
            throw e
        } catch (e: Exception) {
            throw OpButtonException("Failed to parse server response", e)
        }
    }
}
