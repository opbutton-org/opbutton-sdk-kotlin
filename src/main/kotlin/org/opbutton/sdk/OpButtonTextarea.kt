package org.opbutton.sdk

class OpButtonTextarea internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonTextarea>(ui, "textarea") {
    init { put("label", label) }

    fun rows(rows: Int) = apply { put("rows", rows) }
    fun defaultValue(value: String) = apply { put("defaultValue", value) }

    fun onSubmit(callback: OpButtonWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }

    fun onSubmit(callback: (OpButtonTextareaAction) -> Unit) = apply {
        ui.registerCallback(internalId) { action -> callback(OpButtonTextareaAction(action)) }
    }
}
