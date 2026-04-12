package org.pilot.sdk

import org.json.JSONArray
import org.json.JSONObject

class PilotTable internal constructor(ui: PilotUI, label: String) : PilotWidget<PilotTable>(ui, "table") {
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
