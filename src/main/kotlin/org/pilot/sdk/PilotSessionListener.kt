package org.pilot.sdk

interface PilotSessionListener {
    fun onPilotSessionConnecting() {}
    fun onPilotSessionWaitingApproval(requestId: String) {}
    fun onPilotSessionStarted(sessionToken: String) {}
    fun onPilotSessionClosed() {}
    fun onPilotSessionRejected() {}
    fun onPilotSessionAuthFailed() {}
    fun onPilotSessionError(exception: PilotException) {}
}
