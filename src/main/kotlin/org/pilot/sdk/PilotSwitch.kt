package org.pilot.sdk

class PilotSwitch internal constructor(ui: PilotUI, label: String) : PilotWidget<PilotSwitch>(ui, "switch") {
    init { put("label", label) }

    fun defaultValue(value: Boolean) = apply { put("defaultValue", value) }

    fun onChange(callback: PilotWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }

    fun onChange(callback: (PilotSwitchAction) -> Unit) = apply {
        ui.registerCallback(internalId) { action -> callback(PilotSwitchAction(action)) }
    }
}
