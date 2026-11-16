package org.opbutton.sdk


class OpButtonSlider internal constructor(ui: OpButtonUI, label: String) : OpButtonWidget<OpButtonSlider>(ui, "slider") {
    init { put("label", label) }

    fun range(min: Double, max: Double, step: Double) = apply {
        require(min.isFinite() && max.isFinite() && step.isFinite() && (max - min).isFinite() && min < max && step > 0 && step <= max - min)
        put("min", min); put("max", max); put("step", step)
    }
    fun defaultValue(value: Double) = apply { require(value.isFinite()); put("defaultValue", value) }
    fun unit(value: String) = apply { put("unit", value) }
    fun disabled(value: Boolean) = apply { put("disabled", value) }

    fun onChange(callback: OpButtonWidgetCallback?) = apply {
        ui.registerCallback(internalId, callback)
    }

    fun onChange(callback: (OpButtonSliderAction) -> Unit) = apply {
        ui.registerCallback(internalId) { action -> callback(OpButtonSliderAction(action)) }
    }
}
