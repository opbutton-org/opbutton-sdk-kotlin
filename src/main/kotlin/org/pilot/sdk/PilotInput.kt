package org.pilot.sdk

class PilotInput internal constructor(ui: PilotUI, label: String) : PilotWidget<PilotInput>(ui, "input") {
    init { put("label", label) }

    fun inputType(type: String) = apply { put("inputType", type) }
    fun defaultValue(value: String) = apply { put("defaultValue", value) }
    fun placeholder(placeholder: String) = apply { put("placeholder", placeholder) }

    fun onSubmit(callback: PilotWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }

    fun onSubmit(callback: (PilotInputAction) -> Unit) = apply {
        ui.registerCallback(internalId) { action -> callback(PilotInputAction(action)) }
    }
}
