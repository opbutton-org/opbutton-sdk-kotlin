package org.pilot.sdk

import org.json.JSONObject

class PilotTab internal constructor(
    private val ui: PilotUI,
    val title: String
) {
    internal val internalId: Int = ui.nextId()
    var id: String = "tab-$internalId"
    var layout: PilotLayout? = null
        private set

    fun setId(id: String): PilotTab = apply { this.id = id }

    fun vertical(): PilotLayout {
        val l = PilotLayout(ui, PilotLayout.Direction.VERTICAL)
        layout = l
        return l
    }

    fun horizontal(): PilotLayout {
        val l = PilotLayout(ui, PilotLayout.Direction.HORIZONTAL)
        layout = l
        return l
    }

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("id", internalId)
        put("title", title)
        layout?.let { put("layout", it.toJson()) }
    }
}
