package org.pilot.sdk

enum class PilotSessionStatus {
    DISCONNECTED,
    CONNECTING,
    WAITING_APPROVAL,
    ACTIVE,
    AUTH_FAILED,
    REJECTED,
    CLOSED,
    ERROR
}
