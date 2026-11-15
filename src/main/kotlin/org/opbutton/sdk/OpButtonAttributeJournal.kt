package org.opbutton.sdk

import org.json.JSONObject

/** A snapshot becomes the baseline only after the server accepts it. */
internal class OpButtonAttributeJournal {
    private val acknowledged = mutableMapOf<String, String>()

    @Synchronized
    fun snapshot(values: Map<String, Any>): Map<String, Any> =
        capture(values).filter { (key, value) -> encode(value) != acknowledged[key] }

    @Synchronized
    fun acknowledge(snapshot: Map<String, Any>?) {
        snapshot?.forEach { (key, value) -> acknowledged[key] = encode(value) }
    }

    companion object {
        fun capture(values: Map<String, Any>): Map<String, Any> = buildMap {
            for ((key, value) in values) {
                try {
                    put(key, JSONObject(encode(value)).get("value"))
                } catch (e: Exception) {
                    OpButtonLog.e("Invalid session attribute: $key", e)
                }
            }
        }

        private fun encode(value: Any) = JSONObject(mapOf("value" to value)).toString()
    }
}
