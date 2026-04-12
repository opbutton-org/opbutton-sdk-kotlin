package org.pilot.sdk

class PilotLabel internal constructor(ui: PilotUI, text: String) : PilotWidget<PilotLabel>(ui, "label") {
    init { put("text", text) }

    fun text(text: String) = apply { put("text", text) }
    fun color(color: String) = apply { put("color", color) }

    fun textProvider(provider: PilotValueProvider?) = apply {
        setProvider("text", provider)
    }
}
