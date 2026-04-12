package org.pilot.sdk

class PilotMetricType private constructor(
    val key: String,
    val unit: String,
    val aggregation: PilotMetricAggregation
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PilotMetricType) return false
        return key == other.key
    }

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key

    companion object {
        @JvmField val FPS = PilotMetricType("fps", "", PilotMetricAggregation.GAUGE)
        @JvmField val FRAME_TIME = PilotMetricType("frame_time", "ms", PilotMetricAggregation.GAUGE)
        @JvmField val MEMORY = PilotMetricType("memory", "bytes", PilotMetricAggregation.GAUGE)
        @JvmField val VIDEO_MEMORY = PilotMetricType("video_memory", "bytes", PilotMetricAggregation.GAUGE)
        @JvmField val CPU_USAGE = PilotMetricType("cpu_usage", "%", PilotMetricAggregation.GAUGE)
        @JvmField val NETWORK_RX = PilotMetricType("network_rx", "bytes/s", PilotMetricAggregation.RATE)
        @JvmField val NETWORK_TX = PilotMetricType("network_tx", "bytes/s", PilotMetricAggregation.RATE)
        @JvmField val BATTERY_LEVEL = PilotMetricType("battery_level", "%", PilotMetricAggregation.GAUGE)
        @JvmField val BATTERY_CHARGING = PilotMetricType("battery_charging", "", PilotMetricAggregation.GAUGE)
        @JvmField val DRAW_CALLS = PilotMetricType("draw_calls", "", PilotMetricAggregation.GAUGE)
        @JvmField val THREAD_COUNT = PilotMetricType("thread_count", "", PilotMetricAggregation.GAUGE)

        @JvmStatic
        fun create(key: String): PilotMetricType =
            PilotMetricType(key, "", PilotMetricAggregation.GAUGE)

        @JvmStatic
        fun create(key: String, unit: String): PilotMetricType =
            PilotMetricType(key, unit, PilotMetricAggregation.GAUGE)

        @JvmStatic
        fun create(key: String, unit: String, aggregation: PilotMetricAggregation): PilotMetricType =
            PilotMetricType(key, unit, aggregation)
    }
}
