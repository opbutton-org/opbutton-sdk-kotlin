package org.pilot.sdk

import android.util.Log

internal object PilotLog {
    private const val TAG = "PilotSDK"
    private var level: PilotLogLevel = PilotLogLevel.INFO

    @Volatile
    private var loggerListener: PilotLoggerListener? = null

    fun setLevel(level: PilotLogLevel) {
        this.level = level
    }

    fun setLoggerListener(listener: PilotLoggerListener?) {
        loggerListener = listener
    }

    fun d(message: String, vararg args: Any?) {
        if (level.ordinal <= PilotLogLevel.DEBUG.ordinal) {
            val msg = format(message, *args)
            loggerListener?.onPilotLoggerMessage(PilotLogLevel.DEBUG, TAG, msg, null)
                ?: Log.d(TAG, msg)
        }
    }

    fun i(message: String, vararg args: Any?) {
        if (level.ordinal <= PilotLogLevel.INFO.ordinal) {
            val msg = format(message, *args)
            loggerListener?.onPilotLoggerMessage(PilotLogLevel.INFO, TAG, msg, null)
                ?: Log.i(TAG, msg)
        }
    }

    fun w(message: String, vararg args: Any?) {
        if (level.ordinal <= PilotLogLevel.WARNING.ordinal) {
            val msg = format(message, *args)
            loggerListener?.onPilotLoggerMessage(PilotLogLevel.WARNING, TAG, msg, null)
                ?: Log.w(TAG, msg)
        }
    }

    fun e(message: String, vararg args: Any?) {
        if (level.ordinal <= PilotLogLevel.ERROR.ordinal) {
            val msg = format(message, *args)
            loggerListener?.onPilotLoggerMessage(PilotLogLevel.ERROR, TAG, msg, null)
                ?: Log.e(TAG, msg)
        }
    }

    fun e(message: String, t: Throwable) {
        loggerListener?.onPilotLoggerMessage(PilotLogLevel.EXCEPTION, TAG, message, t)
            ?: Log.e(TAG, message, t)
    }

    private fun format(message: String, vararg args: Any?): String =
        if (args.isEmpty()) message else String.format(message, *args)
}
