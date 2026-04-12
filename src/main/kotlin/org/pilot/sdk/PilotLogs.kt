package org.pilot.sdk

class PilotLogs internal constructor(ui: PilotUI, label: String) : PilotWidget<PilotLogs>(ui, "logs") {
    init { put("label", label) }

    fun maxLines(maxLines: Int) = apply { put("maxLines", maxLines) }
}
