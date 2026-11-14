package org.opbutton.sdk

class OpButtonStat internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonStat>(ui, "stat") {
    init { put("label", label) }

    fun value(value: String) = apply { put("value", value) }
    fun unit(unit: String) = apply { put("unit", unit) }

    fun valueProvider(provider: OpButtonValueProvider?) = apply {
        setProvider("value", provider)
    }
}
