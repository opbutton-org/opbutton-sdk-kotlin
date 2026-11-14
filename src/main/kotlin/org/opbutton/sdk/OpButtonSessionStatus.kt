package org.opbutton.sdk

enum class OpButtonSessionStatus {
    DISCONNECTED,
    CONNECTING,
    WAITING_APPROVAL,
    ACTIVE,
    AUTH_FAILED,
    REJECTED,
    CLOSED,
    ERROR
}
