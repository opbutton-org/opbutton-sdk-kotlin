package org.opbutton.sdk

class OpButtonLabel internal constructor(ui: OpButtonUI, text: String) : OpButtonWidget<OpButtonLabel>(ui, "label") {
    init { put("text", text) }

    fun text(text: String) = apply { put("text", text) }
    fun color(color: String) = apply { put("color", color) }

    fun textProvider(provider: OpButtonValueProvider?) = apply {
        setProvider("text", provider)
    }
}
