package org.opbutton.sdk

class OpButtonMetricType private constructor(
    val key: String,
    val unit: String,
    val aggregation: OpButtonMetricAggregation
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpButtonMetricType) return false
        return key == other.key
    }

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key

    companion object {
        @JvmField val FPS = OpButtonMetricType("fps", "", OpButtonMetricAggregation.GAUGE)
        @JvmField val FRAME_TIME = OpButtonMetricType("frame_time", "ms", OpButtonMetricAggregation.GAUGE)
        @JvmField val MEMORY = OpButtonMetricType("memory", "bytes", OpButtonMetricAggregation.GAUGE)
        @JvmField val VIDEO_MEMORY = OpButtonMetricType("video_memory", "bytes", OpButtonMetricAggregation.GAUGE)
        @JvmField val CPU_USAGE = OpButtonMetricType("cpu_usage", "%", OpButtonMetricAggregation.GAUGE)
        @JvmField val NETWORK_RX = OpButtonMetricType("network_rx", "bytes/s", OpButtonMetricAggregation.RATE)
        @JvmField val NETWORK_TX = OpButtonMetricType("network_tx", "bytes/s", OpButtonMetricAggregation.RATE)
        @JvmField val BATTERY_LEVEL = OpButtonMetricType("battery_level", "%", OpButtonMetricAggregation.GAUGE)
        @JvmField val BATTERY_CHARGING = OpButtonMetricType("battery_charging", "", OpButtonMetricAggregation.GAUGE)
        @JvmField val DRAW_CALLS = OpButtonMetricType("draw_calls", "", OpButtonMetricAggregation.GAUGE)
        @JvmField val THREAD_COUNT = OpButtonMetricType("thread_count", "", OpButtonMetricAggregation.GAUGE)

        @JvmStatic
        fun create(key: String): OpButtonMetricType =
            OpButtonMetricType(key, "", OpButtonMetricAggregation.GAUGE)

        @JvmStatic
        fun create(key: String, unit: String): OpButtonMetricType =
            OpButtonMetricType(key, unit, OpButtonMetricAggregation.GAUGE)

        @JvmStatic
        fun create(key: String, unit: String, aggregation: OpButtonMetricAggregation): OpButtonMetricType =
            OpButtonMetricType(key, unit, aggregation)
    }
}
