package org.opbutton.sdk

import org.json.JSONObject

class OpButtonMetricEntry(
    val type: OpButtonMetricType,
    val value: Double,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val metricType: String get() = type.key

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("metric_type", type.key)
        put("value", value)
        put("client_timestamp", timestampMs)
        put("aggregation", type.aggregation.key)
    }
}
