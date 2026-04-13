package org.pilot.sdk

class PilotTextarea internal constructor(ui: PilotUI, label: String) : PilotWidget<PilotTextarea>(ui, "textarea") {
    init { put("label", label) }

    fun rows(rows: Int) = apply { put("rows", rows) }
    fun defaultValue(value: String) = apply { put("defaultValue", value) }

    fun onSubmit(callback: PilotWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }

    fun onSubmit(callback: (PilotTextareaAction) -> Unit) = apply {
        ui.registerCallback(internalId) { action -> callback(PilotTextareaAction(action)) }
    }
}
