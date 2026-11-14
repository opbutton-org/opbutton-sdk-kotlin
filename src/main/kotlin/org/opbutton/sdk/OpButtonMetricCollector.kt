package org.opbutton.sdk

fun interface OpButtonMetricCollector {
    fun collect(out: MutableList<OpButtonMetricEntry>)
}
