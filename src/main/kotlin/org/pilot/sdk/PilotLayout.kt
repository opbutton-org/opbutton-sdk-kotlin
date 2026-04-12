package org.pilot.sdk

import org.json.JSONArray
import org.json.JSONObject

class PilotLayout internal constructor(
    private val ui: PilotUI,
    val direction: Direction
) {
    enum class Direction(internal val value: String) {
        VERTICAL("vertical"),
        HORIZONTAL("horizontal")
    }

    private val children = mutableListOf<Any>()

    // ── Sub-layouts ──

    fun addVertical(): PilotLayout {
        val sub = PilotLayout(ui, Direction.VERTICAL)
        children.add(sub)
        ui.incrementRevision()
        return sub
    }

    fun addHorizontal(): PilotLayout {
        val sub = PilotLayout(ui, Direction.HORIZONTAL)
        children.add(sub)
        ui.incrementRevision()
        return sub
    }

    fun addCollapsible(title: String): PilotLayout {
        val collapsible = CollapsibleElement(ui, title)
        children.add(collapsible)
        ui.incrementRevision()
        return collapsible.content
    }

    // ── Padding ──

    fun addPadding(weight: Double): PilotLayout {
        children.add(PaddingElement(weight))
        ui.incrementRevision()
        return this
    }

    // ── Widgets ──

    fun addButton(label: String): PilotButton {
        val w = PilotButton(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addLabel(text: String): PilotLabel {
        val w = PilotLabel(ui, text)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addStat(label: String): PilotStat {
        val w = PilotStat(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addSwitch(label: String): PilotSwitch {
        val w = PilotSwitch(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addInput(label: String): PilotInput {
        val w = PilotInput(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addSelect(label: String): PilotSelect {
        val w = PilotSelect(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addTextarea(label: String): PilotTextarea {
        val w = PilotTextarea(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addTable(label: String): PilotTable {
        val w = PilotTable(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addLogs(label: String): PilotLogs {
        val w = PilotLogs(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    // ── Serialization ──

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("type", "layout")
        put("direction", direction.value)
        val childrenArr = JSONArray()
        for (child in children) {
            when (child) {
                is PilotLayout -> childrenArr.put(child.toJson())
                is PilotWidget<*> -> childrenArr.put(child.toJson())
                is PaddingElement -> childrenArr.put(child.toJson())
                is CollapsibleElement -> childrenArr.put(child.toJson())
            }
        }
        put("children", childrenArr)
    }

    // ── Padding helper ──

    internal class PaddingElement(private val weight: Double) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("type", "padding")
            put("weight", weight)
        }
    }

    // ── Collapsible helper ──

    internal class CollapsibleElement(ui: PilotUI, private val title: String) {
        val content = PilotLayout(ui, Direction.VERTICAL)

        fun toJson(): JSONObject = JSONObject().apply {
            put("type", "collapsible")
            put("title", title)
            put("children", content.toJson().optJSONArray("children"))
        }
    }
}
