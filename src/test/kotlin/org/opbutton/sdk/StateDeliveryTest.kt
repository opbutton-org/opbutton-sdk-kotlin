package org.opbutton.sdk

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StateDeliveryTest {
    @Test fun failedDeliveryRemainsPendingAndAcknowledgesOnlyItsSnapshot() {
        val journal = OpButtonAttributeJournal()
        val first = journal.snapshot(mapOf("level" to 1))
        assertEquals(1, journal.snapshot(mapOf("level" to 1))["level"])
        journal.acknowledge(first)
        assertEquals(2, journal.snapshot(mapOf("level" to 2))["level"])
        journal.acknowledge(journal.snapshot(mapOf("level" to 2)))
        assertTrue(journal.snapshot(mapOf("level" to 2)).isEmpty())
    }

    @Test fun mutableValuesAreDetachedAndNativeTypesSurvive() {
        val journal = OpButtonAttributeJournal()
        val nested = JSONObject().put("level", 1)
        val values = mutableMapOf("state" to nested, "optional" to JSONObject.NULL, "enabled" to false)
        val snapshot = journal.snapshot(values)
        nested.put("level", 2)
        journal.acknowledge(snapshot)
        assertEquals(1, (snapshot["state"] as JSONObject).getInt("level"))
        assertSame(JSONObject.NULL, snapshot["optional"])
        assertTrue(journal.snapshot(values).containsKey("state"))
        values["enabled"] = "false"
        assertEquals("false", journal.snapshot(values)["enabled"])
    }

    @Test fun panelAcknowledgementDoesNotHideNewerEditsOrMoveBackwards() {
        val ui = OpButtonUI()
        val button = ui.addTab("Controls").vertical().addButton("Run")
        val sent = ui.getRevision()
        button.disabled(true)
        ui.markSent(sent)
        assertTrue(ui.hasUnsent())
        ui.markSent(ui.getRevision())
        ui.markSent(sent)
        assertFalse(ui.hasUnsent())
    }
}
