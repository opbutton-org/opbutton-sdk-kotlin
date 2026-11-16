package org.opbutton.sdk

import org.json.JSONArray
import org.json.JSONObject

class OpButtonRadioGroup internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonRadioGroup>(ui, "radio") {
    init { put("label", label) }

    fun options(options: Array<Array<String>>) = apply {
        val arr = JSONArray()
        for (opt in options) {
            arr.put(JSONObject().apply {
                put("value", opt[0])
                put("label", opt[1])
            })
        }
        put("options", arr)
    }

    fun defaultValue(value: String) = apply { put("defaultValue", value) }

    fun disabled(value: Boolean) = apply { put("disabled", value) }
    fun optionDirection(value: String) = apply {
        require(value in setOf("horizontal", "vertical")); put("optionDirection", value)
    }

    fun onChange(callback: OpButtonWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }

    fun onChange(callback: (OpButtonRadioGroupAction) -> Unit) = apply {
        ui.registerCallback(internalId) { action -> callback(OpButtonRadioGroupAction(action)) }
    }
}
