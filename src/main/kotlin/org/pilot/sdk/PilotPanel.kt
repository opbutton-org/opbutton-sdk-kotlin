package org.pilot.sdk

import org.json.JSONArray
import org.json.JSONObject

class PilotPanel {
    private var version = 1
    var revision = 1
        private set
    private val sections = mutableListOf<Section>()

    fun setVersion(version: Int) = apply { this.version = version }
    fun setRevision(revision: Int) = apply { this.revision = revision }
    fun incrementRevision() = apply { revision++ }

    fun addSection(id: String, title: String): Section {
        val section = Section(this, id, title)
        sections.add(section)
        return section
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("version", version)
        put("revision", revision)
        val sectionsArray = JSONArray()
        for (section in sections) {
            sectionsArray.put(section.toJson())
        }
        put("sections", sectionsArray)
    }

    class Section internal constructor(
        private val panel: PilotPanel,
        val id: String,
        private val title: String
    ) {
        private val widgets = mutableListOf<JSONObject>()

        fun addButton(id: String, label: String, variant: String, color: String) = apply {
            widgets.add(JSONObject().apply {
                put("id", id); put("type", "button"); put("label", label)
                put("variant", variant); put("color", color)
            })
        }

        fun addLabel(id: String, text: String, color: String) = apply {
            widgets.add(JSONObject().apply {
                put("id", id); put("type", "label"); put("text", text); put("color", color)
            })
        }

        fun addStat(id: String, label: String, value: String, unit: String) = apply {
            widgets.add(JSONObject().apply {
                put("id", id); put("type", "stat"); put("label", label)
                put("value", value); put("unit", unit)
            })
        }

        fun addInput(id: String, label: String, inputType: String, defaultValue: String) = apply {
            widgets.add(JSONObject().apply {
                put("id", id); put("type", "input"); put("label", label)
                put("inputType", inputType); put("defaultValue", defaultValue)
            })
        }

        fun addSwitch(id: String, label: String, defaultValue: Boolean) = apply {
            widgets.add(JSONObject().apply {
                put("id", id); put("type", "switch"); put("label", label); put("defaultValue", defaultValue)
            })
        }

        fun addSelect(id: String, label: String, options: Array<Array<String>>, defaultValue: String) = apply {
            widgets.add(JSONObject().apply {
                put("id", id); put("type", "select"); put("label", label); put("defaultValue", defaultValue)
                val optionsArray = JSONArray()
                for (opt in options) {
                    optionsArray.put(JSONObject().apply { put("value", opt[0]); put("label", opt[1]) })
                }
                put("options", optionsArray)
            })
        }

        fun addTextarea(id: String, label: String, rows: Int) = apply {
            widgets.add(JSONObject().apply {
                put("id", id); put("type", "textarea"); put("label", label); put("rows", rows)
            })
        }

        fun addTable(id: String, label: String, columns: Array<Array<String>>, rows: List<JSONObject>) = apply {
            widgets.add(JSONObject().apply {
                put("id", id); put("type", "table"); put("label", label)
                val columnsArray = JSONArray()
                for (col in columns) {
                    columnsArray.put(JSONObject().apply { put("key", col[0]); put("label", col[1]) })
                }
                put("columns", columnsArray)
                val rowsArray = JSONArray()
                for (row in rows) { rowsArray.put(row) }
                put("rows", rowsArray)
            })
        }

        fun addLogs(id: String, label: String, maxLines: Int) = apply {
            widgets.add(JSONObject().apply {
                put("id", id); put("type", "logs"); put("label", label); put("maxLines", maxLines)
            })
        }

        fun addWidget(widget: JSONObject) = apply { widgets.add(widget) }

        fun updateStatValue(widgetId: String, newValue: String) = apply {
            widgets.firstOrNull { it.optString("id") == widgetId && it.optString("type") == "stat" }
                ?.put("value", newValue)
        }

        fun updateLabelText(widgetId: String, newText: String) = apply {
            widgets.firstOrNull { it.optString("id") == widgetId && it.optString("type") == "label" }
                ?.put("text", newText)
        }

        fun updateLabelText(widgetId: String, newText: String, newColor: String) = apply {
            widgets.firstOrNull { it.optString("id") == widgetId && it.optString("type") == "label" }
                ?.apply { put("text", newText); put("color", newColor) }
        }

        fun done(): PilotPanel = panel

        internal fun toJson(): JSONObject = JSONObject().apply {
            put("id", id)
            put("title", title)
            val widgetsArray = JSONArray()
            for (widget in widgets) { widgetsArray.put(widget) }
            put("widgets", widgetsArray)
        }
    }
}
