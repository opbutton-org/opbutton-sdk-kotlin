package org.opbutton.sdk

enum class OpButtonMetricAggregation(val key: String) {
    GAUGE("gauge"),
    COUNTER("counter"),
    RATE("rate")
}
