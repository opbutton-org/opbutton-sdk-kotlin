package org.opbutton.sdk

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PanelLayoutTest {
    @Test fun replacingAndRemovingTreesDetachHandlersAndProviders() {
        val ui = OpButtonUI()
        val tab = ui.addTab("Lifecycle")
        val old = tab.vertical()
        val slider = old.addSlider("Speed").onChange { _: OpButtonSliderAction -> fail("Removed slider dispatched") }
        var samples = 0
        old.addStat("FPS").valueProvider { samples++; "60" }
        val id = slider.internalId
        ui.markSent(ui.getRevision())
        val grid = tab.grid()
        assertTrue(ui.hasUnsent())
        assertTrue(grid.internalId > id)
        ui.pollValues()
        assertEquals(0, samples)
        assertFalse(ui.dispatchAction(OpButtonAction.fromJson(JSONObject().put("widget_id", id))))
        val next = grid.addSlider("Next").onChange { _: OpButtonSliderAction -> fail("Removed tab dispatched") }
        ui.addTab("Spacing").vertical().addPadding(0.0)
        ui.removeTab(tab.id)
        assertFalse(ui.dispatchAction(OpButtonAction.fromJson(JSONObject().put("widget_id", next.internalId))))
    }
    private fun collect(node: JSONObject, ids: MutableSet<Int>) {
        assertTrue("Every node has a unique generated ID", ids.add(node.getInt("id")))
        node.optJSONArray("children")?.let { children ->
            for (index in 0 until children.length()) collect(children.getJSONObject(index), ids)
        }
    }

    @Test fun retainedLayoutContractAndTypedEvents() {
        val ui = OpButtonUI()
        val root = ui.addTab("Layout contract").vertical().gap(12.0).padding(8.0).density("compact")
        val row = root.addHorizontal().gap(8.0).align("end").justify("space-between").wrap(true)
        val button = row.addButton("Apply").width(140.0)
        row.addSelect("Language").options(arrayOf(arrayOf("en", "English"), arrayOf("ru", "Русский")))
            .defaultValue("en").fill(2.0).minWidth(160.0).maxWidth(480.0)
        val radio = root.addRadioGroup("Difficulty").options(arrayOf(arrayOf("easy", "Easy"), arrayOf("hard", "Hard")))
            .defaultValue("easy").optionDirection("horizontal")
        val slider = root.addSlider("Speed").range(0.5, 3.0, 0.1).defaultValue(1.5).unit("x").fill()
        root.addCollapsible("Advanced").gap(4.0).density("comfortable").addGroup("Group").addButton("Inside")
        val grid = root.addGrid().columns(3).minCellWidth(140.0).gap(8.0)
        for (text in listOf("A", "B", "C")) grid.addButton(text)
        val identifier = button.internalId
        ui.markSent(ui.getRevision())
        button.width(160.0)
        assertTrue(ui.hasUnsent())
        assertEquals(identifier, button.internalId)
        val layout = ui.toJson()
        val tab = layout.getJSONArray("tabs").getJSONObject(0)
        collect(tab.getJSONObject("layout"), mutableSetOf(tab.getInt("id")))
        assertEquals(4.0, root.toJson().getJSONArray("children").getJSONObject(3)
            .getJSONArray("children").getJSONObject(0).getDouble("gap"), 0.0)
        var received = 0.0
        slider.onChange { action: OpButtonSliderAction -> received = action.value }
        assertTrue(ui.dispatchAction(OpButtonAction.fromJson(JSONObject().put("widget_id", slider.internalId)
            .put("action_type", "change").put("payload", JSONObject().put("value", 1.7)))))
        assertEquals(1.7, received, 0.0)
        var choice = ""
        radio.onChange { action: OpButtonRadioGroupAction -> choice = action.value }
        ui.dispatchAction(OpButtonAction.fromJson(JSONObject().put("widget_id", radio.internalId)
            .put("action_type", "change").put("payload", JSONObject().put("value", "hard"))))
        assertEquals("hard", choice)
        System.getenv("OPBUTTON_LAYOUT_FIXTURE_DIR")?.let { File(it, "kotlin.json").writeText(layout.toString()) }
    }
}
