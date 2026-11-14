package org.opbutton.sdk

class OpButtonSwitch internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonSwitch>(ui, "switch") {
    init { put("label", label) }

    fun defaultValue(value: Boolean) = apply { put("defaultValue", value) }

    fun onChange(callback: OpButtonWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }

    fun onChange(callback: (OpButtonSwitchAction) -> Unit) = apply {
        ui.registerCallback(internalId) { action -> callback(OpButtonSwitchAction(action)) }
    }
}
