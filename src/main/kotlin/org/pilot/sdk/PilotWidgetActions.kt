package org.pilot.sdk

/**
 * Typed action for [PilotSwitch] change events.
 * Provides a strongly-typed [value] instead of raw payload access.
 */
class PilotSwitchAction internal constructor(val action: PilotAction) {
    val value: Boolean get() = action.payload?.optBoolean("value", false) ?: false
}

/**
 * Typed action for [PilotInput] submit events.
 */
class PilotInputAction internal constructor(val action: PilotAction) {
    val value: String get() = action.payload?.optString("value", "") ?: ""
}

/**
 * Typed action for [PilotSelect] change events.
 */
class PilotSelectAction internal constructor(val action: PilotAction) {
    val value: String get() = action.payload?.optString("value", "") ?: ""
}

/**
 * Typed action for [PilotTextarea] submit events.
 */
class PilotTextareaAction internal constructor(val action: PilotAction) {
    val value: String get() = action.payload?.optString("value", "") ?: ""
}
