package org.pilot.sdk

fun interface PilotLoggerListener {
    fun onPilotLoggerMessage(level: PilotLogLevel, tag: String, message: String, throwable: Throwable?)
}
