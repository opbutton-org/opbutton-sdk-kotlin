package org.pilot.sdk

class PilotException : Exception {
    val httpCode: Int

    constructor(message: String) : super(message) {
        httpCode = 0
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
        httpCode = 0
    }

    constructor(httpCode: Int, message: String) : super(message) {
        this.httpCode = httpCode
    }

    val isNetworkError: Boolean get() = httpCode == 0 && cause != null
    val isSessionGone: Boolean get() = httpCode == 410
    val isUnauthorized: Boolean get() = httpCode == 401
}
