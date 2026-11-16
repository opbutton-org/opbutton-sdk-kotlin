package org.opbutton.sdk

import org.json.JSONObject

class OpButtonTab internal constructor(
    private val ui: OpButtonUI,
    val title: String
) {
    internal val internalId: Int = ui.nextId()
    var id: String = "tab-$internalId"
    var layout: OpButtonLayout? = null
        private set

    fun setId(id: String): OpButtonTab = apply { this.id = id }

    fun vertical(): OpButtonLayout {
        val l = OpButtonLayout(ui, OpButtonLayout.Direction.VERTICAL)
        layout?.detach()
        ui.incrementRevision()
        layout = l
        return l
    }

    fun horizontal(): OpButtonLayout {
        val l = OpButtonLayout(ui, OpButtonLayout.Direction.HORIZONTAL)
        layout?.detach()
        ui.incrementRevision()
        layout = l
        return l
    }

    fun grid(): OpButtonLayout {
        val child = OpButtonLayout(ui, OpButtonLayout.Direction.GRID)
        layout?.detach()
        ui.incrementRevision()
        layout = child
        return child
    }

    internal fun detach() { layout?.detach() }

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("id", internalId)
        put("title", title)
        layout?.let { put("layout", it.toJson()) }
    }
}
