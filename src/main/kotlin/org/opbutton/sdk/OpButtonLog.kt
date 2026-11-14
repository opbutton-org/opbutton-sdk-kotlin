package org.opbutton.sdk

import android.util.Log

internal object OpButtonLog {
    private const val TAG = "OpButtonSDK"
    private var level: OpButtonLogLevel = OpButtonLogLevel.INFO

    @Volatile
    private var loggerListener: OpButtonLoggerListener? = null

    fun setLevel(level: OpButtonLogLevel) {
        this.level = level
    }

    fun setLoggerListener(listener: OpButtonLoggerListener?) {
        loggerListener = listener
    }

    fun d(message: String, vararg args: Any?) {
        if (level.ordinal <= OpButtonLogLevel.DEBUG.ordinal) {
            val msg = format(message, *args)
            loggerListener?.onOpButtonLoggerMessage(OpButtonLogLevel.DEBUG, TAG, msg, null)
                ?: Log.d(TAG, msg)
        }
    }

    fun i(message: String, vararg args: Any?) {
        if (level.ordinal <= OpButtonLogLevel.INFO.ordinal) {
            val msg = format(message, *args)
            loggerListener?.onOpButtonLoggerMessage(OpButtonLogLevel.INFO, TAG, msg, null)
                ?: Log.i(TAG, msg)
        }
    }

    fun w(message: String, vararg args: Any?) {
        if (level.ordinal <= OpButtonLogLevel.WARNING.ordinal) {
            val msg = format(message, *args)
            loggerListener?.onOpButtonLoggerMessage(OpButtonLogLevel.WARNING, TAG, msg, null)
                ?: Log.w(TAG, msg)
        }
    }

    fun e(message: String, vararg args: Any?) {
        if (level.ordinal <= OpButtonLogLevel.ERROR.ordinal) {
            val msg = format(message, *args)
            loggerListener?.onOpButtonLoggerMessage(OpButtonLogLevel.ERROR, TAG, msg, null)
                ?: Log.e(TAG, msg)
        }
    }

    fun e(message: String, t: Throwable) {
        loggerListener?.onOpButtonLoggerMessage(OpButtonLogLevel.EXCEPTION, TAG, message, t)
            ?: Log.e(TAG, message, t)
    }

    private fun format(message: String, vararg args: Any?): String =
        if (args.isEmpty()) message else String.format(message, *args)
}
