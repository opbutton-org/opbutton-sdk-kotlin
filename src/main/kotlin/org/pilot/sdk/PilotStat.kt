package org.pilot.sdk

class PilotStat internal constructor(ui: PilotUI, label: String) : PilotWidget<PilotStat>(ui, "stat") {
    init { put("label", label) }

    fun value(value: String) = apply { put("value", value) }
    fun unit(unit: String) = apply { put("unit", unit) }

    fun valueProvider(provider: PilotValueProvider?) = apply {
        setProvider("value", provider)
    }
}
