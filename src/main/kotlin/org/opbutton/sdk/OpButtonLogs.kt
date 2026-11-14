package org.opbutton.sdk

class OpButtonLogs internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonLogs>(ui, "logs") {
    init { put("label", label) }

    fun maxLines(maxLines: Int) = apply { put("maxLines", maxLines) }
}
