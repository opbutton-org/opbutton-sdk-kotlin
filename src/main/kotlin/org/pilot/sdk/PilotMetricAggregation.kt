package org.pilot.sdk

enum class PilotMetricAggregation(val key: String) {
    GAUGE("gauge"),
    COUNTER("counter"),
    RATE("rate")
}
