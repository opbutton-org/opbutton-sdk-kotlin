package org.opbutton.sdk

import org.json.JSONObject

/** A poll is a delivery transaction; an unfinished telemetry receipt rolls back. */
internal class OpButtonPollDelivery(
    private val transport: Transport,
    private val state: State,
    private val telemetry: OpButtonTelemetry,
    private val isCurrent: (String) -> Boolean,
    private val onPanelError: (OpButtonException) -> Unit
) {
    interface Transport {
        fun submitPanel(sessionToken: String, layout: JSONObject): JSONObject
        fun pollActions(sessionToken: String, changedAttributes: Map<String, Any>?,
                        logs: List<OpButtonLogEntry>, metrics: List<OpButtonMetricEntry>): JSONObject
    }

    interface State {
        fun capture(): Snapshot
        fun panelSent(snapshot: Snapshot)
        fun attributesSent(snapshot: Snapshot)
    }

    class Snapshot(val attributes: Map<String, Any>, val panel: JSONObject?)

    fun exchange(token: String): JSONObject {
        val snapshot = state.capture()
        snapshot.panel?.let { panel ->
            try {
                transport.submitPanel(token, panel)
                if (isCurrent(token)) state.panelSent(snapshot)
            } catch (error: OpButtonException) {
                OpButtonLog.e("Failed to submit UI", error)
                onPanelError(error)
            }
        }
        return telemetry.capture().use { batch ->
            val response = transport.pollActions(token, snapshot.attributes, batch.logs, batch.samples)
            if (isCurrent(token)) state.attributesSent(snapshot)
            batch.commit()
            response
        }
    }

    fun probe(token: String): JSONObject = transport.pollActions(token, null, emptyList(), emptyList())
}
