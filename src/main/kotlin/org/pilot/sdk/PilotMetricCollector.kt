package org.pilot.sdk

fun interface PilotMetricCollector {
    fun collect(out: MutableList<PilotMetricEntry>)
}
