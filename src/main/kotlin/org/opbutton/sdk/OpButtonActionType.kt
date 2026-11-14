package org.opbutton.sdk

enum class OpButtonActionType(val value: String) {
    CLICK("click"),
    CHANGE("change"),
    TOGGLE("toggle"),
    LIVE_START("live_start"),
    LIVE_UPDATE("live_update"),
    LIVE_STOP("live_stop"),
    LIVE_TAP("live_tap"),
    LIVE_LONG_PRESS("live_long_press"),
    UNKNOWN("");

    companion object {
        internal fun fromValue(value: String): OpButtonActionType =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
