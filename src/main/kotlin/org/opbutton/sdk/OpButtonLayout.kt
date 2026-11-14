package org.opbutton.sdk

import org.json.JSONArray
import org.json.JSONObject

class OpButtonLayout internal constructor(
    private val ui: OpButtonUI,
    val direction: Direction
) {
    enum class Direction(internal val value: String) {
        VERTICAL("vertical"),
        HORIZONTAL("horizontal")
    }

    private val children = mutableListOf<Any>()

    // ── Sub-layouts ──

    fun addVertical(): OpButtonLayout {
        val sub = OpButtonLayout(ui, Direction.VERTICAL)
        children.add(sub)
        ui.incrementRevision()
        return sub
    }

    fun addHorizontal(): OpButtonLayout {
        val sub = OpButtonLayout(ui, Direction.HORIZONTAL)
        children.add(sub)
        ui.incrementRevision()
        return sub
    }

    fun addCollapsible(title: String): OpButtonLayout {
        val collapsible = CollapsibleElement(ui, title)
        children.add(collapsible)
        ui.incrementRevision()
        return collapsible.content
    }

    // ── Padding ──

    fun addPadding(weight: Double): OpButtonLayout {
        children.add(PaddingElement(weight))
        ui.incrementRevision()
        return this
    }

    // ── Widgets ──

    fun addButton(label: String): OpButtonButton {
        val w = OpButtonButton(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addLabel(text: String): OpButtonLabel {
        val w = OpButtonLabel(ui, text)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addStat(label: String): OpButtonStat {
        val w = OpButtonStat(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addSwitch(label: String): OpButtonSwitch {
        val w = OpButtonSwitch(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addInput(label: String): OpButtonInput {
        val w = OpButtonInput(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addSelect(label: String): OpButtonSelect {
        val w = OpButtonSelect(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addTextarea(label: String): OpButtonTextarea {
        val w = OpButtonTextarea(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addTable(label: String): OpButtonTable {
        val w = OpButtonTable(ui, label)
        children.add(w)
        ui.incrementRevision()
        return w
    }

    fun addLogs(label: String): OpButtonLogs {
        val w = OpButtonLogs(ui, label)
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
                is OpButtonLayout -> childrenArr.put(child.toJson())
                is OpButtonWidget<*> -> childrenArr.put(child.toJson())
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

    internal class CollapsibleElement(ui: OpButtonUI, private val title: String) {
        val content = OpButtonLayout(ui, Direction.VERTICAL)

        fun toJson(): JSONObject = JSONObject().apply {
            put("type", "collapsible")
            put("title", title)
            put("children", content.toJson().optJSONArray("children"))
        }
    }
}
