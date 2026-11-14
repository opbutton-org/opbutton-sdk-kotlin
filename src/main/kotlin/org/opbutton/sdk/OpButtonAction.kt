package org.opbutton.sdk

import org.json.JSONObject

class OpButtonAction internal constructor(
    val id: String,
    val sessionId: String,
    val widgetId: Int,
    val actionType: OpButtonActionType,
    val status: OpButtonActionStatus,
    val payload: JSONObject?
) {
    internal companion object {
        fun fromJson(json: JSONObject): OpButtonAction = OpButtonAction(
            id = json.optString("id", ""),
            sessionId = json.optString("session_id", ""),
            widgetId = json.optInt("widget_id", 0),
            actionType = OpButtonActionType.fromValue(json.optString("action_type", "")),
            status = OpButtonActionStatus.fromValue(json.optString("status", "")),
            payload = json.optJSONObject("payload")
        )
    }
}
