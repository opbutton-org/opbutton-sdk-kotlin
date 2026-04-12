package org.pilot.sdk

class PilotButton internal constructor(ui: PilotUI, label: String) : PilotWidget<PilotButton>(ui, "button") {
    init { put("label", label) }

    fun variant(variant: String) = apply { put("variant", variant) }
    fun color(color: String) = apply { put("color", color) }
    fun disabled(disabled: Boolean) = apply { put("disabled", disabled) }

    fun onClick(callback: PilotWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }
}
