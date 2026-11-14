package org.opbutton.sdk

class OpButtonButton internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonButton>(ui, "button") {
    init { put("label", label) }

    fun variant(variant: String) = apply { put("variant", variant) }
    fun color(color: String) = apply { put("color", color) }
    fun disabled(disabled: Boolean) = apply { put("disabled", disabled) }

    fun onClick(callback: OpButtonWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }
}
