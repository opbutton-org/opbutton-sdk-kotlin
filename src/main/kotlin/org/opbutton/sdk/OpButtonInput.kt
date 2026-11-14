package org.opbutton.sdk

class OpButtonInput internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonInput>(ui, "input") {
    init { put("label", label) }

    fun inputType(type: String) = apply { put("inputType", type) }
    fun defaultValue(value: String) = apply { put("defaultValue", value) }
    fun placeholder(placeholder: String) = apply { put("placeholder", placeholder) }

    fun onSubmit(callback: OpButtonWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }

    fun onSubmit(callback: (OpButtonInputAction) -> Unit) = apply {
        ui.registerCallback(internalId) { action -> callback(OpButtonInputAction(action)) }
    }
}
