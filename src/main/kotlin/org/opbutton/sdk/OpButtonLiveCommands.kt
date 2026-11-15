package org.opbutton.sdk

import org.json.JSONObject

/** Composes media commands with a captured session; no command calls the global facade. */
internal class OpButtonLiveCommands(
    private val live: OpButtonLiveManager,
    private val background: (() -> Unit) -> Unit,
    private val isCurrent: (String) -> Boolean,
    private val acknowledge: (String, String, JSONObject?) -> Unit
) {
    fun commands(): Map<OpButtonActionType, OpButtonCommand> = mapOf(
        OpButtonActionType.LIVE_START to { action, token -> stream(action, token, live::start) },
        OpButtonActionType.LIVE_UPDATE to { action, token -> stream(action, token, live::update) },
        OpButtonActionType.LIVE_STOP to { action, token -> acknowledge(token, action.id, live.stop()) },
        OpButtonActionType.LIVE_TAP to { action, token -> acknowledge(token, action.id, live.tap(action.payload)) },
        OpButtonActionType.LIVE_LONG_PRESS to { action, token -> acknowledge(token, action.id, live.longPress(action.payload)) }
    )

    private fun stream(action: OpButtonAction, token: String, operation: (String, JSONObject?) -> JSONObject) {
        background {
            if (isCurrent(token)) {
                val result = operation(token, action.payload)
                if (isCurrent(token)) acknowledge(token, action.id, result)
            }
        }
    }
}
