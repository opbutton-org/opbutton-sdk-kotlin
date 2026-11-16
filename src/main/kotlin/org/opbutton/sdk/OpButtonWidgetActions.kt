package org.opbutton.sdk

/**
 * Typed action for [OpButtonSwitch] change events.
 * Provides a strongly-typed [value] instead of raw payload access.
 */
class OpButtonSwitchAction internal constructor(val action: OpButtonAction) {
    val value: Boolean get() = action.payload?.optBoolean("value", false) ?: false
}

/**
 * Typed action for [OpButtonInput] submit events.
 */
class OpButtonInputAction internal constructor(val action: OpButtonAction) {
    val value: String get() = action.payload?.optString("value", "") ?: ""
}

/**
 * Typed action for [OpButtonSelect] change events.
 */
class OpButtonSelectAction internal constructor(val action: OpButtonAction) {
    val value: String get() = action.payload?.optString("value", "") ?: ""
}

/**
 * Typed action for [OpButtonTextarea] submit events.
 */
class OpButtonTextareaAction internal constructor(val action: OpButtonAction) {
    val value: String get() = action.payload?.optString("value", "") ?: ""
}

class OpButtonRadioGroupAction internal constructor(val action: OpButtonAction) {
    val value: String get() = action.payload?.optString("value", "") ?: ""
}

class OpButtonSliderAction internal constructor(val action: OpButtonAction) {
    val value: Double get() = action.payload?.optDouble("value", 0.0) ?: 0.0
}
