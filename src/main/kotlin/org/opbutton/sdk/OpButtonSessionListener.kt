package org.opbutton.sdk

interface OpButtonSessionListener {
    fun onOpButtonSessionConnecting() {}
    fun onOpButtonSessionWaitingApproval(requestId: String) {}
    fun onOpButtonSessionStarted(sessionToken: String) {}
    fun onOpButtonSessionClosed() {}
    fun onOpButtonSessionRejected() {}
    fun onOpButtonSessionAuthFailed() {}
    fun onOpButtonSessionError(exception: OpButtonException) {}
}
