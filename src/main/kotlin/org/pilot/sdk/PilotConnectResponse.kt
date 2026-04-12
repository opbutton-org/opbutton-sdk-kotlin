package org.pilot.sdk

import org.json.JSONObject

class PilotConnectResponse internal constructor(
    val requestId: String,
    val status: String,
    val sessionToken: String?
) {
    val isPending: Boolean get() = status == "pending"
    val isApproved: Boolean get() = status == "approved"
    val isRejected: Boolean get() = status == "rejected"

    internal companion object {
        fun fromJson(json: JSONObject): PilotConnectResponse {
            val token = if (json.isNull("session_token")) null
            else json.optString("session_token", null)
            return PilotConnectResponse(
                requestId = json.optString("request_id", ""),
                status = json.optString("status", ""),
                sessionToken = token
            )
        }
    }
}
