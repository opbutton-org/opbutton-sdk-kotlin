package org.opbutton.sdk

import org.json.JSONObject

class OpButtonConnectResponse internal constructor(
    val requestId: String,
    val status: String,
    val sessionToken: String?
) {
    val isPending: Boolean get() = status == "pending"
    val isApproved: Boolean get() = status == "approved"
    val isRejected: Boolean get() = status == "rejected"

    internal companion object {
        fun fromJson(json: JSONObject): OpButtonConnectResponse {
            val token = if (json.isNull("session_token")) null
            else json.optString("session_token")
            return OpButtonConnectResponse(
                requestId = json.optString("request_id", ""),
                status = json.optString("status", ""),
                sessionToken = token
            )
        }
    }
}
