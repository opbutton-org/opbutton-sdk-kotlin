package org.opbutton.sdk

import org.json.JSONArray
import org.json.JSONObject

class OpButtonSelect internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonSelect>(ui, "select") {
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

    fun onChange(callback: OpButtonWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }

    fun onChange(callback: (OpButtonSelectAction) -> Unit) = apply {
        ui.registerCallback(internalId) { action -> callback(OpButtonSelectAction(action)) }
    }
}
