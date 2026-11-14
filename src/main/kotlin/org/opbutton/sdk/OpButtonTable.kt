package org.opbutton.sdk

import org.json.JSONArray
import org.json.JSONObject

class OpButtonTable internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonTable>(ui, "table") {
    init { put("label", label) }

    fun columns(columns: Array<Array<String>>) = apply {
        val arr = JSONArray()
        for (col in columns) {
            arr.put(JSONObject().apply {
                put("key", col[0])
                put("label", col[1])
            })
        }
        put("columns", arr)
    }

    fun rows(rows: List<JSONObject>) = apply {
        val arr = JSONArray()
        for (row in rows) {
            arr.put(row)
        }
        put("rows", arr)
    }
}
