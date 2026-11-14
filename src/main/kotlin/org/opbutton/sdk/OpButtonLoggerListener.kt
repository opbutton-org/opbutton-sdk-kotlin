package org.opbutton.sdk

fun interface OpButtonLoggerListener {
    fun onOpButtonLoggerMessage(level: OpButtonLogLevel, tag: String, message: String, throwable: Throwable?)
}
