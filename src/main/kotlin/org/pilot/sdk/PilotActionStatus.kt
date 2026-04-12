package org.pilot.sdk

enum class PilotActionStatus(val value: String) {
    PENDING("pending"),
    DELIVERED("delivered"),
    ACKNOWLEDGED("acknowledged"),
    UNKNOWN("");

    companion object {
        internal fun fromValue(value: String): PilotActionStatus =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
