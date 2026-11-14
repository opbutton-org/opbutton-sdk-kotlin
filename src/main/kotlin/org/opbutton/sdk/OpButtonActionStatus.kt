package org.opbutton.sdk

enum class OpButtonActionStatus(val value: String) {
    PENDING("pending"),
    DELIVERED("delivered"),
    ACKNOWLEDGED("acknowledged"),
    UNKNOWN("");

    companion object {
        internal fun fromValue(value: String): OpButtonActionStatus =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
