package org.opbutton.sdk

import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class RuntimeContractsTest {
    private val http = OpButtonHttpClient("https://opbutton.invalid", "test")
    private val metrics = OpButtonMetrics()
    private val telemetry = OpButtonTelemetry(OpButtonLogConfigBuilder(), http, metrics)
    private val state = State()
    private val peer = Peer()
    private var current = true
    private var panelErrors = 0
    private val delivery = OpButtonPollDelivery(peer, state, telemetry, { current }, { panelErrors++ })

    @Before fun quietPlatformLogger() { OpButtonLog.setLoggerListener { _, _, _, _ -> } }
    @After fun closeTransport() { http.shutdown(); OpButtonLog.setLoggerListener(null) }

    @Test fun failedPollRestoresTelemetryInOrderAndDoesNotCommitAttributes() {
        val first = log("first")
        val later = log("later")
        telemetry.bufferLog(first)
        metrics.record(OpButtonMetricType.CPU_USAGE, 1.0)
        peer.onPoll = {
            telemetry.bufferLog(later)
            metrics.record(OpButtonMetricType.CPU_USAGE, 2.0)
            state.values["level"] = 2
        }
        peer.failPoll = true
        assertThrows(OpButtonException::class.java) { delivery.exchange("session") }
        assertEquals(0, state.attributesAccepted)
        peer.onPoll = null
        peer.failPoll = false
        delivery.exchange("session")
        assertEquals(listOf(first, later), peer.logs)
        assertEquals(listOf(1.0, 2.0), peer.samples.map { it.value })
        assertEquals(2, peer.attributes?.get("level"))
        assertTrue(state.journal.snapshot(state.values).isEmpty())
        telemetry.capture().use { assertTrue(it.logs.isEmpty()) }
    }

    @Test fun failedPanelDoesNotBlockPollingOrAcknowledgeThePanel() {
        state.panel = JSONObject().put("revision", 7)
        peer.failPanel = true
        assertSame(peer.response, delivery.exchange("session"))
        assertEquals(0, state.panelsAccepted)
        assertEquals(1, state.attributesAccepted)
        assertEquals(1, peer.polls)
        assertEquals(1, panelErrors)
    }

    @Test fun lateResponseDoesNotAcknowledgeAnotherSession() {
        peer.onPoll = { current = false }
        delivery.exchange("old-session")
        assertEquals(0, state.attributesAccepted)
        assertFalse(state.journal.snapshot(state.values).isEmpty())
    }

    @Test fun probePreservesTheOutboxAndReturnsTheResponse() {
        telemetry.bufferLog(log("pending"))
        assertSame(peer.response, delivery.probe("session"))
        assertEquals(0, state.captures)
        assertNull(peer.attributes)
        assertTrue(peer.logs.isEmpty())
        telemetry.capture().use { assertEquals(1, it.logs.size) }
    }

    @Test fun batchSettlementIsIdempotentAndCommittedDataStaysConsumed() {
        telemetry.bufferLog(log("pending"))
        val first = telemetry.capture()
        first.close()
        first.close()
        telemetry.capture().use {
            assertEquals(1, it.logs.size)
            it.commit()
            it.commit()
        }
        telemetry.capture().use { assertTrue(it.logs.isEmpty()) }
    }

    @Test fun unavailableHostDoesNotDrainTheOutboxOrCallTheTransport() {
        telemetry.bufferLog(log("pending"))
        state.unavailable = true
        assertThrows(IllegalStateException::class.java) { delivery.exchange("session") }
        assertEquals(0, peer.polls)
        telemetry.capture().use { assertEquals(1, it.logs.size) }
    }

    @Test fun reservedCommandsAreExclusiveAndKeepTheirCapturedSession() {
        val calls = mutableListOf<String>()
        val commands = mutableMapOf<OpButtonActionType, OpButtonCommand>(OpButtonActionType.LIVE_START to { _, token -> calls.add(token); Unit })
        val router = OpButtonActionRouter(commands, { calls.add("widget") }, { listOf(OpButtonActionListener { calls.add("listener") }) })
        commands.clear()
        router.dispatch(action(OpButtonActionType.LIVE_START), "original-session")
        assertEquals(listOf("original-session"), calls)
    }

    @Test fun callbackFailuresAndSelfRemovalDoNotSkipOtherObservers() {
        val calls = mutableListOf<String>()
        val listeners = mutableListOf<OpButtonActionListener>()
        listeners.add(OpButtonActionListener { calls.add("first"); listeners.removeAt(0); error("listener") })
        listeners.add(OpButtonActionListener { calls.add("second") })
        val router = OpButtonActionRouter(emptyMap(), { calls.add("widget"); error("widget") }, { listeners.toList() })
        router.dispatch(action(OpButtonActionType.CLICK), "session")
        assertEquals(listOf("widget", "first", "second"), calls)
    }

    private fun log(message: String) = OpButtonLogEntry(OpButtonLogLevel.INFO, message, null, null, null, null)
    private fun action(type: OpButtonActionType) = OpButtonAction("action", "session", 1, type, OpButtonActionStatus.DELIVERED, null)

    private class State : OpButtonPollDelivery.State {
        val journal = OpButtonAttributeJournal()
        val values = mutableMapOf<String, Any>("level" to 1)
        var panel: JSONObject? = null
        var captures = 0
        var panelsAccepted = 0
        var attributesAccepted = 0
        var unavailable = false

        override fun capture(): OpButtonPollDelivery.Snapshot {
            check(!unavailable) { "Host paused" }
            captures++
            return OpButtonPollDelivery.Snapshot(journal.snapshot(values), panel)
        }
        override fun panelSent(snapshot: OpButtonPollDelivery.Snapshot) { panelsAccepted++ }
        override fun attributesSent(snapshot: OpButtonPollDelivery.Snapshot) {
            attributesAccepted++
            journal.acknowledge(snapshot.attributes)
        }
    }

    private class Peer : OpButtonPollDelivery.Transport {
        val response = JSONObject()
        var onPoll: (() -> Unit)? = null
        var failPanel = false
        var failPoll = false
        var polls = 0
        var attributes: Map<String, Any>? = null
        var logs = emptyList<OpButtonLogEntry>()
        var samples = emptyList<OpButtonMetricEntry>()

        override fun submitPanel(sessionToken: String, layout: JSONObject): JSONObject {
            if (failPanel) throw OpButtonException(503, "Panel unavailable")
            return response
        }
        override fun pollActions(sessionToken: String, changedAttributes: Map<String, Any>?, logs: List<OpButtonLogEntry>, metrics: List<OpButtonMetricEntry>): JSONObject {
            polls++
            attributes = changedAttributes
            this.logs = logs.toList()
            samples = metrics.toList()
            onPoll?.invoke()
            if (failPoll) throw OpButtonException(503, "Poll unavailable")
            return response
        }
    }
}
