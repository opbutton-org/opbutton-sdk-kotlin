package org.pilot.sdk

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PilotLogEntry private constructor(
    val level: String,
    val message: String,
    private val category: String?,
    private val thread: String?,
    private val metadata: Map<String, Any?>?,
    private val attributes: JSONObject?,
    private val clientTimestamp: String
) {
    constructor(
        level: PilotLogLevel,
        message: String,
        metadata: Map<String, Any?>? = null
    ) : this(level.value, message, null, null, metadata, null, formatTimestamp(System.currentTimeMillis()))

    constructor(
        level: PilotLogLevel,
        message: String,
        category: String?,
        thread: String?,
        metadata: Map<String, Any?>?
    ) : this(level.value, message, category, thread, metadata, null, formatTimestamp(System.currentTimeMillis()))

    constructor(
        level: PilotLogLevel,
        message: String,
        category: String?,
        thread: String?,
        metadata: Map<String, Any?>?,
        attributes: JSONObject?
    ) : this(level.value, message, category, thread, metadata, attributes, formatTimestamp(System.currentTimeMillis()))

    constructor(
        level: PilotLogLevel,
        message: String,
        category: String?,
        thread: String?,
        metadata: Map<String, Any?>?,
        attributes: JSONObject?,
        clientTimestampMs: Long
    ) : this(level.value, message, category, thread, metadata, attributes, formatTimestamp(clientTimestampMs))

    constructor(
        level: String,
        message: String,
        metadata: Map<String, Any?>?
    ) : this(level, message, null, null, metadata, null, formatTimestamp(System.currentTimeMillis()))

    constructor(
        level: String,
        message: String,
        category: String?,
        thread: String?,
        metadata: Map<String, Any?>?
    ) : this(level, message, category, thread, metadata, null, formatTimestamp(System.currentTimeMillis()))

    constructor(
        level: String,
        message: String,
        category: String?,
        thread: String?,
        metadata: Map<String, Any?>?,
        attributes: JSONObject?
    ) : this(level, message, category, thread, metadata, attributes, formatTimestamp(System.currentTimeMillis()))

    constructor(
        level: String,
        message: String,
        category: String?,
        thread: String?,
        metadata: Map<String, Any?>?,
        attributes: JSONObject?,
        clientTimestampMs: Long
    ) : this(level, message, category, thread, metadata, attributes, formatTimestamp(clientTimestampMs))

    internal fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("level", level)
        json.put("message", message)
        json.put("client_timestamp", clientTimestamp)
        category?.let { json.put("category", it) }
        thread?.let { json.put("thread", it) }
        if (!metadata.isNullOrEmpty()) {
            val metaJson = JSONObject()
            for ((key, value) in metadata) {
                metaJson.put(key, value ?: JSONObject.NULL)
            }
            json.put("metadata", metaJson)
        }
        attributes?.let { json.put("attributes", it) }
        return json
    }

    companion object {
        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        private fun formatTimestamp(timestampMs: Long): String = synchronized(ISO_FORMAT) {
            ISO_FORMAT.format(Date(timestampMs))
        }

        @JvmStatic fun debug(message: String) = PilotLogEntry(PilotLogLevel.DEBUG, message)
        @JvmStatic fun info(message: String) = PilotLogEntry(PilotLogLevel.INFO, message)
        @JvmStatic fun warning(message: String) = PilotLogEntry(PilotLogLevel.WARNING, message)
        @JvmStatic fun error(message: String) = PilotLogEntry(PilotLogLevel.ERROR, message)
        @JvmStatic fun critical(message: String) = PilotLogEntry(PilotLogLevel.CRITICAL, message)
    }
}
