package org.pilot.sdk

import org.json.JSONObject

class PilotAction internal constructor(
    val id: String,
    val sessionId: String,
    val widgetId: Int,
    val actionType: PilotActionType,
    val status: PilotActionStatus,
    val payload: JSONObject?
) {
    internal companion object {
        fun fromJson(json: JSONObject): PilotAction = PilotAction(
            id = json.optString("id", ""),
            sessionId = json.optString("session_id", ""),
            widgetId = json.optInt("widget_id", 0),
            actionType = PilotActionType.fromValue(json.optString("action_type", "")),
            status = PilotActionStatus.fromValue(json.optString("status", "")),
            payload = json.optJSONObject("payload")
        )
    }
}
