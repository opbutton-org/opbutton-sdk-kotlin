package org.opbutton.sdk

import org.json.JSONObject

/** Inline sizing shared by retained widgets and layouts; dimensions are CSS pixels. */
open class OpButtonElement<T : OpButtonElement<T>> internal constructor(internal val ui: OpButtonUI) {
    internal val internalId = ui.nextId()
    private val placement = JSONObject()

    @Suppress("UNCHECKED_CAST")
    private fun place(key: String, value: Any): T {
        placement.put(key, value)
        ui.incrementRevision()
        return this as T
    }

    fun width(pixels: Double): T = place("width", dimension(pixels, true))
    fun contentWidth(): T = place("width", "content")
    fun fill(weight: Double = 1.0): T {
        place("weight", dimension(weight, true))
        return place("width", "fill")
    }
    fun minWidth(pixels: Double): T = place("minWidth", dimension(pixels))
    fun maxWidth(pixels: Double): T = place("maxWidth", dimension(pixels, true))
    fun alignSelf(value: String): T = place("alignSelf", alignment(value))

    internal open fun detach() { ui.registerCallback(internalId, null) }

    internal fun decorate(json: JSONObject): JSONObject = json.apply {
        put("id", internalId)
        if (placement.length() > 0) put("placement", placement)
    }

    internal companion object {
        fun dimension(value: Double, positive: Boolean = false): Double {
            require(value.isFinite() && value <= 4096 && if (positive) value > 0 else value >= 0)
            return value
        }
        fun alignment(value: String): String {
            require(value in setOf("start", "center", "end", "stretch"))
            return value
        }
    }
}
