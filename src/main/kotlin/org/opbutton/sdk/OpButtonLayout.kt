package org.opbutton.sdk

import org.json.JSONArray
import org.json.JSONObject

class OpButtonLayout internal constructor(ui: OpButtonUI, val direction: Direction) : OpButtonElement<OpButtonLayout>(ui) {
    enum class Direction(internal val value: String) { VERTICAL("vertical"), HORIZONTAL("horizontal"), GRID("grid") }
    private val children = mutableListOf<Any>()
    private val settings = JSONObject()
    private fun option(key: String, value: Any) = apply { settings.put(key, value); ui.incrementRevision() }
    fun gap(pixels: Double) = option("gap", dimension(pixels))
    fun padding(pixels: Double) = option("padding", dimension(pixels))
    fun align(value: String) = option("align", alignment(value))
    fun justify(value: String) = apply {
        require(value in setOf("start", "center", "end", "space-between"))
        option("justify", value)
    }
    fun wrap(value: Boolean) = option("wrap", value)
    fun density(value: String) = apply { require(value in setOf("compact", "comfortable")); option("density", value) }
    fun columns(value: Int) = apply { require(value in 1..12); option("gridColumns", value) }
    fun minCellWidth(pixels: Double) = option("minCellWidth", dimension(pixels, true))
    fun title(value: String) = option("title", value)
    private fun addLayout(direction: Direction): OpButtonLayout {
        val child = OpButtonLayout(ui, direction)
        children.add(child)
        ui.incrementRevision()
        return child
    }
    fun addVertical() = addLayout(Direction.VERTICAL)
    fun addHorizontal() = addLayout(Direction.HORIZONTAL)
    fun addGrid() = addLayout(Direction.GRID)
    fun addGroup(title: String) = addVertical().title(title)
    fun addPadding(weight: Double) = apply { children.add(PaddingElement(ui, weight)); ui.incrementRevision() }
    fun addCollapsible(title: String): OpButtonLayout {
        val child = CollapsibleElement(ui, title)
        children.add(child)
        ui.incrementRevision()
        return child.content
    }
    fun addButton(label: String): OpButtonButton {
        val widget = OpButtonButton(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addLabel(label: String): OpButtonLabel {
        val widget = OpButtonLabel(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addStat(label: String): OpButtonStat {
        val widget = OpButtonStat(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addSwitch(label: String): OpButtonSwitch {
        val widget = OpButtonSwitch(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addInput(label: String): OpButtonInput {
        val widget = OpButtonInput(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addSelect(label: String): OpButtonSelect {
        val widget = OpButtonSelect(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addTextarea(label: String): OpButtonTextarea {
        val widget = OpButtonTextarea(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addTable(label: String): OpButtonTable {
        val widget = OpButtonTable(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addLogs(label: String): OpButtonLogs {
        val widget = OpButtonLogs(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addRadioGroup(label: String): OpButtonRadioGroup {
        val widget = OpButtonRadioGroup(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    fun addSlider(label: String): OpButtonSlider {
        val widget = OpButtonSlider(ui, label)
        children.add(widget)
        ui.incrementRevision()
        return widget
    }

    internal override fun detach() { children.forEach { (it as OpButtonElement<*>).detach() }; super.detach() }

    internal fun toJson(): JSONObject = decorate(JSONObject().apply {
        put("type", "layout"); put("direction", direction.value)
        for (key in settings.keys()) put(key, settings.get(key))
        put("children", JSONArray().apply {
            for (child in children) put(when (child) {
                is OpButtonLayout -> child.toJson()
                is OpButtonWidget<*> -> child.toJson()
                is PaddingElement -> child.toJson()
                is CollapsibleElement -> child.toJson()
                else -> error("Unknown layout child")
            })
        })
    })
    private class PaddingElement(ui: OpButtonUI, val weight: Double) : OpButtonElement<PaddingElement>(ui) {
        fun toJson() = decorate(JSONObject().put("type", "padding").put("weight", weight))
    }
    private class CollapsibleElement(ui: OpButtonUI, val title: String) : OpButtonElement<CollapsibleElement>(ui) {
        override fun detach() { content.detach(); super.detach() }
        val content = OpButtonLayout(ui, Direction.VERTICAL)
        fun toJson() = decorate(JSONObject().put("type", "collapsible").put("title", title)
            .put("children", JSONArray().put(content.toJson())))
    }
}
